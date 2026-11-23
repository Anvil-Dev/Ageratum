"""Exercise the release entry points offline with repository/publication boundary doubles."""

import hashlib
import json
import os
import tempfile
import unittest
import xml.etree.ElementTree as ET
import zipfile
from contextlib import contextmanager
from pathlib import Path
from unittest.mock import Mock, call, patch

import artifacts
import maven
import mod_sites
import release
from common import ANCHOR, BRANCHES

TAG = "0.0.1"
SHAS = {line: f"{i:040x}" for i, line in enumerate(BRANCHES, 1)}
ENV = {
    "GITHUB_REF": "refs/heads/releases/1.21.1",
    "GITHUB_SHA": SHAS[ANCHOR],
    "GITHUB_REPOSITORY": "example/Ageratum",
    "GITHUB_RUN_ID": "123",
    "RELEASE_TAG": TAG,
    "DRY_RUN": "true",
    "GENERATE_RELEASE_NOTES": "false",
    "BUMP_PATCH": "true",
}


@contextmanager
def temporary_workspace():
    with tempfile.TemporaryDirectory() as directory:
        before = Path.cwd()
        os.chdir(directory)
        try:
            yield Path(directory)
        finally:
            os.chdir(before)


def props(line):
    java = 21 if line == "1.21.1" else 25
    return (f"minecraft_version={line}\njava_version={java}\nupload=true\n"
            "mod_id=ageratum\nmod_version=0.0.1\nmaven_group=dev.anvilcraft.resource\n"
            "modrinth_id=9YApuDd3\ncurseforge_id=1500610\n").encode()


def upload_entry(line, folder, sha):
    jar = f"ageratum-neoforge-{line}-{TAG}.jar"
    folder.mkdir(parents=True)
    hashes = {}
    for name in (jar, jar[:-4] + "-sources.jar"):
        data = name.encode()
        (folder / name).write_bytes(data)
        hashes[name] = hashlib.sha256(data).hexdigest()
    entry = {"game": line, "sha": sha, "jar": jar, "files": hashes}
    (folder / "entry.json").write_text(json.dumps(entry))
    return entry


class ReleasePipelineTest(unittest.TestCase):
    def prepare(self, api, current, output):
        api.side_effect = lambda path: {"object": {"sha": next(
            SHAS[line] for line, branch in BRANCHES.items() if path == "git/ref/heads/" + branch)}}
        current.side_effect = lambda _, sha: props(next(line for line, value in SHAS.items() if value == sha))
        release.prepare()
        matrix = json.loads(dict(call.args for call in output.call_args_list)["matrix"])
        self.assertEqual([(m["line"], m["sha"], m["java"]) for m in matrix], [
            ("1.21.1", SHAS["1.21.1"], "21"), ("26.1.2", SHAS["26.1.2"], "25"),
            ("26.2", SHAS["26.2"], "25"), ("26.3", SHAS["26.3"], "25"),
        ])

    @patch.dict(os.environ, ENV)
    def test_dry_run_verifies_every_target_without_publishing(self):
        with (temporary_workspace() as root,
              patch.object(release, "artifact", return_value=None),
              patch.object(release, "github") as api,
              patch.object(release, "contents") as current,
              patch.object(release, "output") as output):
            self.prepare(api, current, output)
            for line, sha in SHAS.items():
                upload_entry(line, root / "bundles" / f"build-{line}" / "dist", sha)
            api.reset_mock()
            release.seal()
            api.assert_not_called()

    @patch.dict(os.environ, ENV)
    def test_corrupt_last_target_prevents_real_tag_and_release_writes(self):
        with (temporary_workspace() as root,
              patch.object(release, "artifact", return_value=None),
              patch.object(release, "github") as api,
              patch.object(release, "contents") as current,
              patch.object(release, "output") as output):
            self.prepare(api, current, output)
            for line, sha in SHAS.items():
                folder = root / "bundles" / f"build-{line}" / "dist"
                entry = upload_entry(line, folder, sha)
                if line == "26.3":
                    (folder / entry["jar"]).write_bytes(b"oops")
            api.reset_mock()
            with patch.dict(os.environ, {"DRY_RUN": "false"}):
                with self.assertRaisesRegex(ValueError, "Damaged artifact"):
                    release.seal()
            api.assert_not_called()

    @patch.dict(os.environ, ENV)
    def test_qualified_release_does_not_request_a_bump(self):
        with (temporary_workspace(),
              patch.object(release, "artifact", return_value=None),
              patch.object(release, "github") as api,
              patch.object(release, "contents") as current,
              patch.object(release, "output") as output,
              patch.dict(os.environ, {"RELEASE_TAG": "0.0.1+rc.1"})):
            self.prepare(api, current, output)
            self.assertEqual(dict(call.args for call in output.call_args_list)["bump_patch"], "false")

    @patch.dict(os.environ, ENV)
    def test_prepare_wont_allow_an_old_release_after_a_bump(self):
        with (temporary_workspace(),
              patch.object(release, "artifact", return_value=None),
              patch.object(release, "github", return_value={"object": {"sha": SHAS[ANCHOR]}}),
              patch.object(release, "contents", return_value=props(ANCHOR).replace(b"0.0.1", b"0.0.2"))):
            with self.assertRaisesRegex(ValueError, "would be older"):
                release.prepare()
            self.assertFalse(Path("release-plan.json").exists())

    @patch.dict(os.environ, ENV)
    def test_publication_waits_for_all_platform_receipts_then_publishes_once(self):
        with temporary_workspace() as root:
            targets = {line: upload_entry(line, root / line, sha) for line, sha in SHAS.items()}
            plan = {"version": TAG, "targets": targets}
            assets = [{"name": name, "digest": "sha256:" + sha, "state": "uploaded"}
                      for target in targets.values() for name, sha in target["files"].items()]
            draft = {"tag_name": TAG, "id": 1, "draft": True, "assets": assets, "html_url": "https://example.org/release"}
            with (patch.object(release, "tag_manifest", return_value=plan),
                  patch.object(release, "release_by_tag", return_value=draft),
                  patch.object(release, "github", return_value=dict(draft, draft=False)) as api,
                  patch.object(release, "commit_status", side_effect=lambda _sha, context:
                               None if context.endswith("26.3/curseforge/sources") else {"state": "success"})):
                with self.assertRaisesRegex(ValueError, "curseforge/sources has not published 26.3"):
                    release.publish(TAG)
                api.assert_not_called()
                with patch.object(release, "commit_status", return_value={"state": "success"}):
                    release.publish(TAG)
                    api.assert_called_once_with("releases/1",
                                                {"draft": False, "prerelease": False, "make_latest": "true"},
                                                method="PATCH")

    def test_bump_does_not_write_if_a_release_is_still_draft(self):
        with (patch.object(release, "tag_manifest", return_value={"version": TAG}),
              patch.object(release, "release_by_tag", return_value={"draft": True}),
              patch.object(release, "github") as api):
            with self.assertRaisesRegex(ValueError, "requires a public release"):
                release.bump(TAG, "26.3")
            api.assert_not_called()

    @patch.dict(os.environ, ENV)
    def test_bump_writes_only_a_fast_forward_commit_to_each_release_branch(self):
        for line in ("1.21.1", "26.1.2", "26.2", "26.3"):
            with self.subTest(line=line):
                self.check_bump(line)

    def check_bump(self, line, race=None):
        branch = f"releases/{line}"
        ref = f"git/ref/heads/{branch}"
        live_sha = "current-live"
        current = props(line) + b"\n# preserved live setting\n"
        expected = current.replace(b"mod_version=0.0.1", b"mod_version=0.0.2")
        title = "chore(release): 将版本推进到 0.0.2 [skip ci]"
        with (patch.object(release, "tag_manifest", return_value={"version": TAG, "targets": {
                  line: {"sha": SHAS[line]}}}),
              patch.object(release, "release_by_tag", return_value={"draft": False}),
              patch.object(release, "contents", side_effect=[current, props(line)]) as read,
              patch.object(release, "summary") as summary,
              patch.object(release, "github") as api):
            api.side_effect = [
                {"object": {"sha": live_sha}}, {"tree": {"sha": "base-tree"}},
                {"sha": "new-tree"}, {"sha": "new-commit"},
                {"object": {"sha": "another-tip" if race == "ref-read" else live_sha}},
                RuntimeError("HTTP 422: Update is not a fast forward") if race == "patch" else {},
            ]
            if race:
                error = ValueError if race == "ref-read" else RuntimeError
                message = "changed during the bump" if race == "ref-read" else "not a fast forward"
                with self.assertRaisesRegex(error, message):
                    release.bump(TAG, line)
                summary.assert_not_called()
            else:
                release.bump(TAG, line)
                summary.assert_called_once_with(
                    f"{branch}: [{title}](https://github.com/example/Ageratum/commit/new-commit)")
            self.assertEqual(read.call_args_list, [
                call("gradle.properties", live_sha), call("gradle.properties", SHAS[line]),
            ])
            calls = [
                call(ref),
                call("git/commits/current-live"),
                call("git/trees", {"base_tree": "base-tree", "tree": [
                    {"path": "gradle.properties", "mode": "100644", "type": "blob", "content": expected.decode()}]}),
                call("git/commits", {"message": title, "tree": "new-tree", "parents": [live_sha]}),
                call(ref),
            ]
            if race != "ref-read":
                calls.append(call(f"git/refs/heads/{branch}", {"sha": "new-commit", "force": False}, method="PATCH"))
            self.assertEqual(api.call_args_list, calls)

    @patch.dict(os.environ, ENV)
    def test_bump_rejects_concurrent_branch_updates_without_creating_a_pr_or_ref(self):
        self.check_bump("26.3", race="ref-read")
        self.check_bump("26.3", race="patch")

    @patch.dict(os.environ, ENV)
    def test_bump_is_idempotent_and_preserves_diverged_versions(self):
        line = "26.2"
        with (patch.object(release, "tag_manifest", return_value={"version": TAG, "targets": {
                  line: {"sha": SHAS[line]}}}),
              patch.object(release, "release_by_tag", return_value={"draft": False}),
              patch.object(release, "github", return_value={"object": {"sha": "live-tip"}}) as api,
              patch.object(release, "contents") as read,
              patch.object(release, "summary") as summary):
            read.side_effect = [props(line).replace(b"0.0.1", b"0.0.2")]
            release.bump(TAG, line)
            api.assert_called_once_with("git/ref/heads/releases/26.2")
            summary.assert_called_once_with("releases/26.2 is already at 0.0.2.")
            api.reset_mock()
            read.side_effect = [props(line).replace(b"0.0.1", b"0.0.3"), props(line)]
            with self.assertRaisesRegex(ValueError, "expected 0.0.1 from the locked source"):
                release.bump(TAG, line)
            api.assert_called_once_with("git/ref/heads/releases/26.2")

    def test_ambiguous_mod_site_upload_is_not_repeated(self):
        upload = Mock()
        with (patch.object(mod_sites, "commit_status", return_value={"state": "pending"}),
              patch.object(mod_sites, "record") as record):
            with self.assertRaisesRegex(ValueError, "outcome is uncertain"):
                mod_sites.send("sha", "release", upload)
            upload.assert_not_called()
            record.assert_not_called()


class ArtifactCollectorTest(unittest.TestCase):
    def test_collect_preserves_the_bytes_across_all_destinations(self):
        with temporary_workspace() as root:
            line = "26.2"
            sha = SHAS[line]
            b = root / "build"
            libs = b / "libs"
            libs.mkdir(parents=True)
            (root / "gradle").mkdir()
            (root / "gradle.properties").write_bytes(props(line))
            (root / "gradle/libs.versions.toml").write_text('[versions]\nminecraft = "26.2"\n')
            group = "dev.anvilcraft.resource"
            artifact = "ageratum-neoforge-26.2"
            staged = b / "release/staging" / group.replace(".", "/") / artifact / TAG
            staged.mkdir(parents=True)
            (staged.parent / "maven-metadata.xml").write_text("<metadata/>")
            stem = f"{artifact}-{TAG}"
            (staged / f"{stem}.pom").write_text(
                '<project xmlns="http://maven.apache.org/POM/4.0.0">'
                f"<groupId>{group}</groupId><artifactId>{artifact}</artifactId><version>{TAG}</version></project>"
            )
            (staged / f"{stem}.module").write_text(json.dumps(
                {"component": {"group": group, "module": artifact, "version": TAG}}))
            with zipfile.ZipFile(libs / f"{stem}.jar", "w") as jar:
                jar.writestr("META-INF/neoforge.mods.toml", f'[[mods]]\nmodId="ageratum"\nversion="{TAG}"\n')
            (libs / f"{stem}-sources.jar").write_bytes(b"sources")
            for name in (f"{stem}.jar", f"{stem}-sources.jar"):
                (staged / name).write_bytes((libs / name).read_bytes())
            with patch.object(artifacts.subprocess, "check_output", return_value=sha):
                dest = b / "release/dist"
                artifacts.collect(root, line, sha, TAG, dest)
                self.assertEqual((dest / f"{stem}.jar").read_bytes(), (libs / f"{stem}.jar").read_bytes())
                (staged / f"{stem}-sources.jar").write_bytes(b"not the build")
                with self.assertRaisesRegex(ValueError, "Maven and mod-site files differ"):
                    artifacts.collect(root, line, sha, TAG, dest)


class MavenMetadataTest(unittest.TestCase):
    @staticmethod
    def metadata(*versions, latest, release):
        node = ET.fromstring(
            f"<metadata><groupId>example.mod</groupId><artifactId>example</artifactId>"
            f"<versioning><latest>{latest}</latest><release>{release}</release><versions/>"
            f"<lastUpdated>20261122000000</lastUpdated></versioning></metadata>"
        )
        for version in versions:
            ET.SubElement(node.find("versioning/versions"), "version").text = version
        return node

    def test_preserves_newer_stable_maven_release_when_publishing_older_or_qualified_build(self):
        remote = self.metadata("0.0.1", "0.0.3", latest="0.0.3", release="0.0.3")
        for offered, latest in (("0.0.2", "0.0.3"), ("0.0.4+rc.1", "0.0.4+rc.1")):
            with self.subTest(offered=offered):
                local = self.metadata(offered, latest=offered, release=offered)
                maven.merge_metadata(local, remote)
                self.assertEqual(local.findtext("versioning/latest"), latest)
                self.assertEqual(local.findtext("versioning/release"), "0.0.3")
                self.assertEqual([e.text for e in local.findall("versioning/versions/version")],
                                 ["0.0.1", "0.0.3", offered])
