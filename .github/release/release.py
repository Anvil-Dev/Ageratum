"""锁定所有受支持的分支，供构建与发布任务复用同一份计划。"""

import json
import os
import re
import sys
from pathlib import Path

from common import (ANCHOR, BRANCHES, LINES, VERSION_LINE, artifact, commit_status, contents, digest, gh, github,
                    mod_version, output, release_type, repo, require, summary, tag_manifest)

from common import VERSION_CORE, read_properties
from release_notes import generate as generate_notes


def prepare():
    require(os.environ["GITHUB_REF"] == "refs/heads/" + BRANCHES[ANCHOR], "Run Create Multi-Version Releases from releases/1.21.1")
    tag = os.environ.get("RELEASE_TAG", "")
    release_type(tag)
    plan_file = Path("release-plan.json")
    saved = artifact("source-plan")
    if saved:
        gh("run", "download", os.environ["GITHUB_RUN_ID"], "-n", "source-plan")
        plan = json.loads(plan_file.read_text())
        require(plan["tag"] == tag, "Input tag differs from the original release plan")
    else:
        targets = {}
        for line in LINES:
            sha = github("git/ref/heads/" + BRANCHES[line])["object"]["sha"]
            if line == ANCHOR:
                require(sha == os.environ["GITHUB_SHA"], "Default branch advanced before preflight; dispatch again")
            props = contents("gradle.properties", sha)
            java = re.search(rb"(?m)^java_version=(\d+)\s*$", props).group(1).decode()
            props_map = read_properties(props.decode())
            require(props_map.get("minecraft_version") == line, f"{BRANCHES[line]} does not target Minecraft {line}")
            require(props_map.get("upload") == "true", f"Distribution disabled on {BRANCHES[line]}")
            require(all(props_map.get(key) == expected for key, expected in
                        (("modrinth_id", "9YApuDd3"), ("curseforge_id", "1500610"))),
                    f"Unexpected mod-site project in {BRANCHES[line]}")
            current = mod_version(props)
            require(re.fullmatch(VERSION_CORE, current), f"Unrecognized development version on {BRANCHES[line]}")
            require(tuple(map(int, tag.partition("+")[0].split("."))) >= tuple(map(int, current.split("."))),
                    f"Release {tag} would be older than {BRANCHES[line]} ({current})")
            targets[line] = {"sha": sha, "java": java}
        plan = {"repo": repo(), "tag": tag, "version": tag,
                "run": os.environ["GITHUB_RUN_ID"], "targets": targets}
        if os.environ.get("GENERATE_RELEASE_NOTES") == "true":
            plan.update(generate_notes(plan, ANCHOR))
        plan_file.write_text(json.dumps(plan, indent=2) + "\n")
    if os.environ.get("DRY_RUN") != "true":
        existing = tag_manifest(plan["tag"])
        if existing:
            require(existing["run"] == plan["run"],
                    f"{plan['tag']} belongs to run {existing['run']}; resume that run")
    rows = [dict(line=line, **target) for line, target in plan["targets"].items()]
    output("matrix", json.dumps(rows))
    output("version", plan["version"])
    output("tag", plan["tag"])
    bump_patch = os.environ.get("BUMP_PATCH", "false") == "true" and "+" not in plan["version"]
    output("bump_patch", str(bump_patch).lower())
    output("new", str(not saved).lower())
    summary(f"**{plan['tag']}**\n\n" + "\n".join(f"- {BRANCHES[r['line']]} @ {r['sha']}" for r in rows))


def release_by_tag(tag):
    release = github(f"releases/tags/{tag}", missing=True)
    if release:
        return release
    # 按 tag 查询可能不返回草稿，需要继续检查发布列表。
    page = 1
    while True:
        releases = github(f"releases?per_page=100&page={page}")
        for item in releases:
            if item["tag_name"] == tag:
                return item
        if len(releases) < 100:
            return None
        page += 1


def verify_assets(release, targets):
    expected = {name: "sha256:" + checksum for t in targets.values() for name, checksum in t["files"].items()}
    actual = {a["name"]: a.get("digest") for a in release["assets"] if a["state"] == "uploaded"}
    require(actual == expected and len(release["assets"]) == len(expected) == 2 * len(LINES),
            f"Incomplete or conflicting GitHub assets for {release['tag_name']}")


def seal():
    plan = json.loads(Path("release-plan.json").read_text())
    tag = plan["tag"]
    prerelease = release_type(plan["version"]) != "release"
    notes = [f"Ageratum {plan['version']} · NeoForge", ""]
    if plan.get("release_notes"):
        notes.extend([plan["release_notes"], ""])
    notes.extend(["| Minecraft | Source | JAR | SHA-256 |", "|---|---|---|---|"])
    paths = {}
    for line, target in plan["targets"].items():
        folder = Path("bundles") / f"build-{line}" / "dist"
        entry = json.loads((folder / "entry.json").read_text())
        require(entry["sha"] == target["sha"] and entry["game"] == line, f"Wrong build for {line}")
        target.update(entry)
        names = {entry["jar"], entry["jar"][:-4] + "-sources.jar"}
        require(set(entry["files"]) == names, f"Wrong asset set for {line}")
        for name, checksum in entry["files"].items():
            require(digest(folder / name) == checksum, f"Damaged artifact: {name}")
            require(name not in paths, f"Duplicate asset: {name}")
            paths[name] = folder / name
        name = entry["jar"]
        sha = target["sha"]
        notes.append(f"| {entry['game']} | [{BRANCHES[line]} @ {sha[:8]}](https://github.com/{repo()}/commit/{sha})"
                     f" | {name} | {entry['files'][name]} |")
    if os.environ.get("DRY_RUN") == "true":
        summary("\n".join(notes))
        return
    existing = tag_manifest(tag)
    if existing:
        require(existing == plan, f"{tag} records different artifacts or sources")
    else:
        obj = github("git/tags", {"tag": tag, "message": f"Ageratum {tag}\n\n" + json.dumps(plan, sort_keys=True),
                                 "object": plan["targets"][ANCHOR]["sha"], "type": "commit"})
        github("git/refs", {"ref": "refs/tags/" + tag, "sha": obj["sha"]})
    release = release_by_tag(tag)
    if not release:
        release = github("releases", {"tag_name": tag, "name": f"Ageratum {plan['version']}",
                                      "target_commitish": plan["targets"][ANCHOR]["sha"],
                                      "body": "\n".join(notes), "draft": True, "prerelease": prerelease})
    require(release.get("prerelease", False) == prerelease, f"Conflicting release type for {tag}")
    assets = {a["name"]: a for a in release["assets"]}
    for name, path in paths.items():
        old = assets.get(name)
        if old and old["state"] == "uploaded":
            require(old.get("digest") == "sha256:" + digest(path), f"Conflicting GitHub asset: {name}")
            continue
        require(release["draft"], f"Published release missing {name}")
        if old and old["state"] == "starter":
            github(f"releases/assets/{old['id']}", method="DELETE")
        gh("release", "upload", tag, str(path))
    verify_assets(github(f"releases/{release['id']}"), plan["targets"])
    summary(f"Verified {len(paths)} assets for {tag}.")


def publish(tag):
    plan = tag_manifest(tag)
    release = release_by_tag(tag)
    prerelease = release_type(plan["version"]) != "release"
    require(release.get("prerelease", False) == prerelease, f"Conflicting release type for {tag}")
    verify_assets(release, plan["targets"])
    for line, target in plan["targets"].items():
        for destination in ("maven", "modrinth", "curseforge/jar", "curseforge/sources"):
            status = commit_status(target["sha"], f"release/{tag}/{line}/{destination}")
            require(status and status["state"] == "success", f"{destination} has not published {line}")
    if release["draft"]:
        release = github(f"releases/{release['id']}", {
            "draft": False, "prerelease": prerelease, "make_latest": "false" if prerelease else "true",
        }, method="PATCH")
    summary(f"Published [{tag}]({release['html_url']}).")


def bump(tag, line):
    plan = tag_manifest(tag)
    require(line in LINES and not release_by_tag(tag)["draft"], "Bump requires a public release")
    version = plan["version"]
    if "+" in version:
        summary(f"{tag} has a version qualifier; skipping the patch bump for {BRANCHES[line]}.")
        return
    major, minor, patch = version.split(".")
    next_version = f"{major}.{minor}.{int(patch) + 1}"
    base = BRANCHES[line]
    ref = "git/ref/heads/" + base
    sha = github(ref)["object"]["sha"]
    props = contents("gradle.properties", sha)
    current = mod_version(props)
    if current == next_version:
        summary(f"{base} is already at {next_version}.")
        return
    expected = mod_version(contents("gradle.properties", plan["targets"][line]["sha"]))
    require(current == expected, f"{base} is at {current}; expected {expected} from the locked source")
    title = f"chore(release): 将版本推进到 {next_version} [skip ci]"
    updated = VERSION_LINE.sub(lambda m: m[1] + next_version.encode() + m[3], props)
    base_tree = github("git/commits/" + sha)["tree"]["sha"]
    tree = github("git/trees", {"base_tree": base_tree, "tree": [
        {"path": "gradle.properties", "mode": "100644", "type": "blob", "content": updated.decode()}]})
    commit = github("git/commits", {"message": title, "tree": tree["sha"], "parents": [sha]})
    require(github(ref)["object"]["sha"] == sha, f"{base} changed during the bump; rerun the failed job")
    github("git/refs/heads/" + base, {"sha": commit["sha"], "force": False}, method="PATCH")
    summary(f"{base}: [{title}](https://github.com/{repo()}/commit/{commit['sha']})")


if __name__ == "__main__":
    {"prepare": prepare, "seal": seal, "publish": publish, "bump": bump}[sys.argv[1]](*sys.argv[2:])
