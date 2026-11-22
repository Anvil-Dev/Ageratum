"""校验锁定源码的主包、源码包、模组描述符和 Maven 文件。"""

import json
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

import tomllib

from common import digest, read_properties, release_type, require, staging_digest, summary


def collect(source, line, sha, version, dest):
    actual_sha = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=source, text=True).strip()
    require(actual_sha == sha, f"Expected {sha} for {line}, found {actual_sha}")
    release_type(version)
    properties = read_properties((source / "gradle.properties").read_text(encoding="utf-8"))
    catalog = tomllib.loads((source / "gradle/libs.versions.toml").read_text(encoding="utf-8"))
    game = properties["minecraft_version"]
    require(game == line == catalog["versions"]["minecraft"], f"Wrong Minecraft target for {line}: {game}")
    require(properties["mod_id"] == "ageratum", "Incorrect mod ID")
    group = properties["maven_group"]
    artifact_id = f"ageratum-neoforge-{game}"
    stem = f"{artifact_id}-{version}"
    staging = source / "build/release/staging"
    artifact_dir = staging / group.replace(".", "/") / artifact_id
    prefix = artifact_dir / version / stem
    require((artifact_dir / "maven-metadata.xml").is_file(), "Missing Maven repository metadata")
    pom = ET.parse(str(prefix) + ".pom").getroot()
    ns = {"m": "http://maven.apache.org/POM/4.0.0"}
    coords = [pom.findtext("m:" + key, namespaces=ns) for key in ("groupId", "artifactId", "version")]
    require(coords == [group, artifact_id, version], f"Invalid Maven coordinates: {coords}")
    module = json.loads(Path(str(prefix) + ".module").read_text(encoding="utf-8"))
    require([module["component"][key] for key in ("group", "module", "version")] == [group, artifact_id, version],
            "Invalid Gradle module coordinates")
    jar = source / "build/libs" / (stem + ".jar")
    with zipfile.ZipFile(jar) as archive:
        descriptor = tomllib.loads(archive.read("META-INF/neoforge.mods.toml").decode())
        require(any(m["modId"] == "ageratum" and m["version"] == version for m in descriptor["mods"]),
                "Invalid Ageratum mod descriptor")

    dest.mkdir(parents=True, exist_ok=True)
    hashes = {}
    for suffix in (".jar", "-sources.jar"):
        name = stem + suffix
        original = jar.parent / name
        hashes[name] = digest(original)
        require(hashes[name] == digest(Path(str(prefix) + suffix)), f"Maven and mod-site files differ: {name}")
        shutil.copyfile(original, dest / name)
    entry = {"sha": sha, "game": game, "jar": jar.name, "files": hashes,
             "staging_sha256": staging_digest(staging), "maven_group": group, "maven_artifact": artifact_id,
             "modrinth": properties["modrinth_id"], "curseforge": properties["curseforge_id"]}
    (dest / "entry.json").write_text(json.dumps(entry, indent=2) + "\n", encoding="utf-8")
    summary(f"MC {game}: 主包、源码包及 Maven 坐标校验通过。")


if __name__ == "__main__":
    collect(Path(sys.argv[1]), *sys.argv[2:5], Path(sys.argv[5]))
