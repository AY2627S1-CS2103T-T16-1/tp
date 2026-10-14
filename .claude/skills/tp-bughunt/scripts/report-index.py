#!/usr/bin/env python3
"""Builds <workdir>/reports/INDEX.md from the report folders of a sweep.

    report-index.py <workdir>

Reads the front matter of every reports/<id>/report.md, and of triage.md when
triage has run, and writes one table row per report. When triage has run,
rows are sorted by verdict and then severity; before it, by report id. Run it
after the bucket wave (the triagers read it for duplicates) and again after
the triage wave (the final ranking).
"""
import os
import sys

SEVERITY_ORDER = ["severity.High", "severity.Medium", "severity.Low", "severity.VeryLow"]
VERDICT_ORDER = ["CONFIRMED", "WEAK", "DUPLICATE", "REJECTED", "-"]
COLUMNS = ["id", "title", "type", "severity", "confidence", "mode", "verdict", "duplicate-of"]


def main():
    if len(sys.argv) != 2:
        sys.exit("usage: report-index.py <workdir>")
    reports_dir = os.path.join(sys.argv[1], "reports")
    rows = [read_report(reports_dir, name) for name in sorted(os.listdir(reports_dir))
            if os.path.isfile(os.path.join(reports_dir, name, "report.md"))]
    if any(r["verdict"] != "-" for r in rows):
        rows.sort(key=rank)

    lines = ["# Sweep reports", "", f"{len(rows)} reports. Type and severity are the "
             "triager's where triage has run, else the reporter's.", "",
             "| " + " | ".join(COLUMNS) + " |", "|" + " --- |" * len(COLUMNS)]
    for r in rows:
        cells = [f"[{r['id']}]({r['id']}/report.md)"] + [r.get(c, "") for c in COLUMNS[1:]]
        lines.append("| " + " | ".join(cell.replace("|", "\\|") for cell in cells) + " |")
    out = os.path.join(reports_dir, "INDEX.md")
    with open(out, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")
    print(f"{len(rows)} reports -> {out}")


def read_report(reports_dir, name):
    row = {"id": name, "verdict": "-"}
    row.update(front_matter(os.path.join(reports_dir, name, "report.md")))
    triage = os.path.join(reports_dir, name, "triage.md")
    if os.path.isfile(triage):
        found = front_matter(triage)
        row.update({k: v for k, v in found.items() if k in ("verdict", "duplicate-of", "type", "severity")})
    row["id"] = name
    return row


def front_matter(path):
    with open(path, encoding="utf-8") as f:
        lines = f.read().splitlines()
    if not lines or lines[0].strip() != "---":
        return {}
    fields = {}
    for line in lines[1:]:
        if line.strip() == "---":
            break
        key, sep, value = line.partition(":")
        if sep:
            fields[key.strip()] = value.strip()
    return fields


def rank(row):
    verdict = row["verdict"] if row["verdict"] in VERDICT_ORDER else "-"
    severity = row.get("severity", "")
    severity_rank = SEVERITY_ORDER.index(severity) if severity in SEVERITY_ORDER else len(SEVERITY_ORDER)
    return VERDICT_ORDER.index(verdict), severity_rank, row["id"]


if __name__ == "__main__":
    main()
