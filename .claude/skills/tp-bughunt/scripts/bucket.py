#!/usr/bin/env python3
"""Triages a tP's source files into small review buckets for the sweep mode.

    bucket.py <src-repo> [--max 5] [--out buckets.json]

Takes every .java file under src/main/java and every .fxml/.css file under
src/main/resources, and packs them into buckets of at most --max files, so
each bucket agent can read all of its files in full.

Files that are reviewed best together stay together:
  - FooCommand.java and FooCommandParser.java form one "feature:Foo" unit;
  - a UI class and the .fxml/.css of the same stem form one unit;
  - everything else is a unit of one file.
Units are packed in path order within one area (the first package below the
common root: logic, model, storage, ui, commons, or the root itself). The
partly filled buckets left at the end of each area are then combined into
"mixed" buckets, so small areas do not each cost an agent. Tests are not bucketed; bucket
agents read the matching tests on their own.

Prints a summary table and writes JSON:
  [{"id": "b01", "area": "logic", "files": ["src/main/java/...", ...]}, ...]
"""
import argparse
import json
import os
import re
import sys

JAVA_ROOT = os.path.join("src", "main", "java")
RES_ROOT = os.path.join("src", "main", "resources")
RES_EXTENSIONS = (".fxml", ".css")
FEATURE = re.compile(r"^(?P<stem>[A-Z]\w*?)Command(Parser)?\.java$")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("repo")
    ap.add_argument("--max", type=int, default=5)
    ap.add_argument("--out", default="buckets.json")
    args = ap.parse_args()

    java = list_files(args.repo, JAVA_ROOT, (".java",))
    if not java:
        sys.exit(f"no .java files under {os.path.join(args.repo, JAVA_ROOT)}")
    resources = list_files(args.repo, RES_ROOT, RES_EXTENSIONS)
    root = os.path.commonpath([os.path.dirname(f) for f in java])

    units = group_units(java, resources, root)
    buckets = pack(units, args.max)
    with open(args.out, "w", encoding="utf-8") as f:
        json.dump(buckets, f, indent=2)
        f.write("\n")

    total = sum(len(b["files"]) for b in buckets)
    print(f"{total} files -> {len(buckets)} buckets (max {args.max}) -> {args.out}")
    for b in buckets:
        names = ", ".join(os.path.basename(p) for p in b["files"])
        print(f"  {b['id']}  {b['area']:<9} {names}")


def list_files(repo, sub, extensions):
    base = os.path.join(repo, sub)
    found = []
    for dirpath, _, files in os.walk(base):
        for name in files:
            if name.endswith(extensions):
                found.append(os.path.relpath(os.path.join(dirpath, name), repo))
    return sorted(found)


def area_of(path, root):
    rel = os.path.relpath(os.path.dirname(path), root)
    return "root" if rel == "." else rel.split(os.sep)[0]


def group_units(java, resources, root):
    """Returns [(area, sort_key, [files])], one entry per unit."""
    units = {}
    for path in java:
        name = os.path.basename(path)
        match = FEATURE.match(name)
        if match and area_of(path, root) == "logic":
            key = ("logic", "feature:" + match.group("stem"))
        else:
            key = (area_of(path, root), path)
        units.setdefault(key, []).append(path)

    by_stem = {}
    for key, files in units.items():
        if key[0] == "ui" and len(files) == 1:
            by_stem[os.path.splitext(os.path.basename(files[0]))[0]] = key
    for path in resources:
        stem = os.path.splitext(os.path.basename(path))[0]
        key = by_stem.get(stem, ("ui", path))
        units.setdefault(key, []).append(path)

    return sorted((area, unit_key, files) for (area, unit_key), files in units.items())


def pack(units, limit):
    buckets = []
    current, current_area = [], None
    for area, _, files in units:
        for i in range(0, len(files), limit):
            chunk = files[i:i + limit]
            if current and (area != current_area or len(current) + len(chunk) > limit):
                buckets.append((current_area, current))
                current = []
            current, current_area = current + chunk, area
    if current:
        buckets.append((current_area, current))
    buckets = merge_partial(buckets, limit)
    return [{"id": f"b{n:02d}", "area": area, "files": files}
            for n, (area, files) in enumerate(buckets, 1)]


def merge_partial(buckets, limit):
    """Combines partly filled buckets from different areas, largest first, to save agents."""
    full = [b for b in buckets if len(b[1]) == limit]
    partial = sorted((b for b in buckets if len(b[1]) < limit), key=lambda b: -len(b[1]))
    merged = []
    for area, files in partial:
        for i, (m_area, m_files) in enumerate(merged):
            if len(m_files) + len(files) <= limit:
                merged[i] = (m_area if m_area == area else "mixed", m_files + files)
                break
        else:
            merged.append((area, files))
    order = {f: n for n, (_, files) in enumerate(buckets) for f in files}
    return sorted(full + merged, key=lambda b: order[b[1][0]])


if __name__ == "__main__":
    main()
