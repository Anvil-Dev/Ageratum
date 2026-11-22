"""上传已校验的 Maven 文件；重试前逐文件核对远端内容。"""

import base64
import hashlib
import os
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from urllib.error import HTTPError
from urllib.parse import quote, urlsplit
from urllib.request import Request, urlopen

from common import commit_status, record, require, staging_digest, summary, tag_manifest


def request(path, data=None):
    base = os.environ.get("MAVEN_URL", "").rstrip("/")
    require(urlsplit(base).scheme == "https", "MAVEN_URL must be an HTTPS repository URL")
    auth = base64.b64encode(f"{os.environ['MAVEN_USERNAME']}:{os.environ['MAVEN_PASSWORD']}".encode()).decode()
    req = Request(base + "/" + quote(path, safe="/"), data=data,
                  headers={"Authorization": "Basic " + auth, "Content-Type": "application/octet-stream"},
                  method="GET" if data is None else "PUT")
    try:
        with urlopen(req, timeout=120) as response:
            return response.read()
    except HTTPError as error:
        if data is None and error.code == 404:
            return None
        raise RuntimeError(f"Maven {req.get_method()} {path}: HTTP {error.code}") from error


def upload(path, data, *, mutable=False):
    existing = request(path)
    if existing == data:
        return
    require(existing is None or mutable, f"Conflicting Maven file: {path}")
    request(path, data)
    require(request(path) == data, f"Maven did not preserve the uploaded file: {path}")


def version_key(version):
    match = re.fullmatch(r"(\d+)\.(\d+)\.(\d+)(?:\+([a-z-]+)\.(\d+))?", version)
    if not match:
        return None
    major, minor, patch, qualifier, qualifier_version = match.groups()
    rank = {"pr": 0, "build": 1, "alpha": 2, "beta": 3, "pre-release": 4, "rc": 5, None: 6}
    if qualifier not in rank:
        return None
    return int(major), int(minor), int(patch), rank[qualifier], int(qualifier_version or 0)


def merge_metadata(local, remote):
    """Merge versions without moving the stable Maven pointer to a prerelease or older release."""
    if remote is not None:
        require(all(remote.findtext(key) == local.findtext(key) for key in ("groupId", "artifactId")),
                "Conflicting Maven metadata coordinates")

    local_versioning = local.find("versioning")
    remote_versioning = remote.find("versioning") if remote is not None else None
    versions = local_versioning.find("versions")
    previously_published = [] if remote_versioning is None else [
        node.text for node in remote_versioning.findall("versions/version") if node.text
    ]
    all_versions = list(dict.fromkeys(previously_published + [n.text for n in versions if n.text]))
    versions.clear()
    for version in all_versions:
        ET.SubElement(versions, "version").text = version

    for field, acceptable in (("latest", all_versions), ("release", [v for v in all_versions if "+" not in v])):
        previous = remote_versioning.findtext(field) if remote_versioning is not None else None
        comparable = [version for version in acceptable if version_key(version) is not None]
        if previous and version_key(previous) is None:
            # Respect pointers written by publishers using a version scheme we don't understand.
            selected = previous
        else:
            selected = max(comparable, key=version_key, default=None)
        node = local_versioning.find(field)
        if selected:
            if node is None:
                node = ET.SubElement(local_versioning, field)
            node.text = selected
        elif node is not None:
            local_versioning.remove(node)

    prior_update = remote_versioning.findtext("lastUpdated") if remote_versioning is not None else None
    local_update = local_versioning.find("lastUpdated")
    if prior_update and local_update is not None:
        local_update.text = max(local_update.text or "", prior_update)



def deploy(tag, line, directory):
    plan = tag_manifest(tag)
    target = plan["targets"][line]
    context = f"release/{tag}/{line}/maven"
    prior = commit_status(target["sha"], context)
    if prior and prior["state"] == "success":
        summary(f"Maven · {target['game']}: 复用成功回执 {prior['description']}")
        return
    root = Path(directory)
    require(staging_digest(root) == target["staging_sha256"], "Staging files changed after validation")
    prefix = f"{target['maven_group'].replace('.', '/')}/{target['maven_artifact']}"
    version_dir = root / prefix / plan["version"]
    files = sorted(version_dir.iterdir())
    require(files, "Empty Maven publication")
    record(target["sha"], context, "pending", "uploading verified Maven files")
    for index, file in enumerate(files, 1):
        summary(f"Maven · {target['game']}: {index}/{len(files)} {file.name}")
        upload(file.relative_to(root).as_posix(), file.read_bytes())

    metadata_path = prefix + "/maven-metadata.xml"
    metadata = ET.parse(root / metadata_path).getroot()
    remote = request(metadata_path)
    merge_metadata(metadata, ET.fromstring(remote) if remote else None)
    data = ET.tostring(metadata, encoding="utf-8", xml_declaration=True)
    upload(metadata_path, data, mutable=True)
    for algorithm in ("md5", "sha1", "sha256", "sha512"):
        upload(metadata_path + "." + algorithm, hashlib.new(algorithm, data).hexdigest().encode(), mutable=True)
    receipt = "sha256:" + target["staging_sha256"]
    record(target["sha"], context, "success", receipt)
    summary(f"Maven · {target['game']}: 已发布并校验，{receipt}")


if __name__ == "__main__":
    try:
        deploy(*sys.argv[1:])
    except Exception as error:
        summary(f"Maven 待恢复 · {' / '.join(sys.argv[1:3])}: {error}")
        raise
