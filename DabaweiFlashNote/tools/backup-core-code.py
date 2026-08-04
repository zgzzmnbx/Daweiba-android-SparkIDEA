#!/usr/bin/env python3
"""Create a compact source backup for DabaweiFlashNote.

The backup intentionally keeps only files needed to restore the app source:
Android source/resources/tests, build scripts, project notes, and icon sources.
Generated build outputs, APKs, and previous backups are excluded.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import re
import sys
import zipfile
from pathlib import Path


PROJECT_NAME = "DabaweiFlashNote"
BACKUP_DIR_NAME = "90-版本代码备份"
INCLUDE_PATHS = (
    "app/src",
    "tools",
    "icon",
    "README.md",
)
EXCLUDE_DIRS = {
    ".git",
    ".gradle",
    ".idea",
    "build",
    "apk-archive",
    BACKUP_DIR_NAME,
}


def parse_build_version(project_root: Path) -> tuple[str, str]:
    build_script = project_root / "tools" / "build-apk.ps1"
    text = build_script.read_text(encoding="utf-8")
    version_code = find_ps_value(text, "versionCode") or "unknown"
    version_name = find_ps_value(text, "versionName") or version_code
    return version_code, version_name


def find_ps_value(text: str, name: str) -> str | None:
    match = re.search(rf"^\s*\${re.escape(name)}\s*=\s*\"([^\"]+)\"", text, re.MULTILINE)
    return match.group(1).strip() if match else None


def safe_name(value: str) -> str:
    value = value.strip() or "unknown"
    return re.sub(r'[\\/:*?"<>|\s]+', "-", value).strip("-")


def iter_core_files(project_root: Path) -> list[Path]:
    files: list[Path] = []
    for relative in INCLUDE_PATHS:
        path = project_root / relative
        if not path.exists():
            continue
        if path.is_file():
            files.append(path)
            continue
        for item in path.rglob("*"):
            if not item.is_file():
                continue
            if any(part in EXCLUDE_DIRS for part in item.relative_to(project_root).parts):
                continue
            files.append(item)
    return sorted(set(files))


def main() -> int:
    parser = argparse.ArgumentParser(description="Backup compact core app source.")
    parser.add_argument("--name", default=PROJECT_NAME, help="Backup display name.")
    parser.add_argument("--version-code", default=None, help="Version code override.")
    parser.add_argument("--version-name", default=None, help="Version name override.")
    parser.add_argument("--date", default=None, help="Date override, e.g. 20260617.")
    parser.add_argument("--quiet", action="store_true", help="Only print the backup path.")
    args = parser.parse_args()

    project_root = Path(__file__).resolve().parents[1]
    parsed_code, parsed_name = parse_build_version(project_root)
    version_code = args.version_code or parsed_code
    version_name = args.version_name or parsed_name
    date_text = args.date or dt.datetime.now().strftime("%Y%m%d")

    backup_dir = project_root / BACKUP_DIR_NAME
    backup_dir.mkdir(parents=True, exist_ok=True)
    backup_name = f"{safe_name(args.name)}-{safe_name(version_name)}-{safe_name(date_text)}.zip"
    backup_path = backup_dir / backup_name

    files = iter_core_files(project_root)
    manifest = {
        "name": args.name,
        "versionCode": version_code,
        "versionName": version_name,
        "createdAt": dt.datetime.now().isoformat(timespec="seconds"),
        "projectRoot": str(project_root),
        "includedPaths": list(INCLUDE_PATHS),
        "fileCount": len(files),
    }

    with zipfile.ZipFile(backup_path, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
        archive.writestr("backup-manifest.json", json.dumps(manifest, ensure_ascii=False, indent=2))
        for file_path in files:
            archive.write(file_path, file_path.relative_to(project_root).as_posix())

    if args.quiet:
        return 0
    else:
        size_kb = backup_path.stat().st_size / 1024
        print(f"Backed up {len(files)} files to {backup_path} ({size_kb:.1f} KB)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
