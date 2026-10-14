"""Helpers for tp-bughunt proof-of-concept scripts.

A report's poc.py imports this module, sets up state, runs commands through the
sandboxed harness against <workdir>/target.jar, and exits:

    0  the bug reproduced (prints the evidence)
    1  it did not reproduce (prints what happened instead)
    2  the PoC itself could not run (harness bootstrap failure, bad set-up)
    3  the effect is only visible in the GUI; the PoC sets up the state and
       says what to look for (poc.needs_gui), and a triager confirms it

Minimal poc.py:

    import sys; sys.path.insert(0, "<skill>/scripts")
    from poclib import Poc
    poc = Poc(__file__)
    out = poc.run(["add n/Ann p/91234567 e/" + "a." * 20000 + "a@b.co a/x"])
    poc.expect(out[0].verdict == "CRASH", "long email crashes the parser", out)

Every run gets a fresh run directory under the report's own folder, so PoCs
are independent and can be re-run by triagers in any order.
"""
import json
import os
import shutil
import subprocess
import sys
from dataclasses import dataclass

SKILL_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
HARNESS = os.path.join(SKILL_DIR, "harness", "TpHarness.java")
SANDBOX = os.path.join(SKILL_DIR, "scripts", "sandbox-run.sh")


@dataclass
class Result:
    command: str
    verdict: str   # OK | REJECT | CRASH | HANG | BOOTSTRAP-FAILED
    detail: str

    def __str__(self):
        return f"{self.verdict}: {self.detail[:400]}"


class Poc:
    def __init__(self, poc_file, workdir=None, main_app=None, timeout_ms=5000):
        self.report_dir = os.path.dirname(os.path.abspath(poc_file))
        self.workdir = workdir or _find_workdir(self.report_dir)
        self.jar = os.path.join(self.workdir, "target.jar")
        self.main_app = main_app
        self.timeout_ms = timeout_ms
        self.runs = 0
        self.rundir = None

    def fresh(self, seed_data=None, data_file=None, prefs=None):
        """Creates a new empty run directory, optionally seeding data/preferences files.

        seed_data: dict or str written to data_file (relative path, e.g. 'data/tab.json').
        prefs: dict or str written to preferences.json.
        """
        self.runs += 1
        self.rundir = os.path.join(self.report_dir, "run", str(self.runs))
        shutil.rmtree(self.rundir, ignore_errors=True)
        os.makedirs(self.rundir)
        shutil.copy(self.jar, os.path.join(self.rundir, "target.jar"))
        if seed_data is not None:
            _write(os.path.join(self.rundir, data_file or "data/addressbook.json"), seed_data)
        if prefs is not None:
            _write(os.path.join(self.rundir, "preferences.json"), prefs)
        return self.rundir

    def run(self, commands, fresh=True, **seed):
        """Runs commands (raw strings, sent verbatim) and returns one Result per command.

        Pass fresh=False to continue in the current run directory (state persists
        through the data file, as across app restarts).
        """
        if fresh or self.rundir is None:
            self.fresh(**seed)
        lines = []
        for c in commands:
            if "\n" in c or "\r" in c:
                c = "#! " + c.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\u000d")
            lines.append(c)
        args = [SANDBOX, self.rundir, "--", "java", "-cp", "target.jar", HARNESS,
                "--timeout-ms", str(self.timeout_ms)]
        if self.main_app:
            args += ["--main-app", self.main_app]
        proc = subprocess.run(args, input="\n".join(lines) + "\n", capture_output=True,
                              text=True, encoding="utf-8")
        with open(os.path.join(self.rundir, "harness-stderr.txt"), "w", encoding="utf-8") as f:
            f.write(proc.stderr)
        return _parse(proc.stdout, commands)

    def data(self, data_file=None):
        """Returns the parsed data file of the current run directory, or None."""
        path = _data_path(self.rundir, data_file)
        if path is None:
            return None
        with open(path, encoding="utf-8-sig") as f:
            return json.load(f)

    def data_text(self, data_file=None):
        path = _data_path(self.rundir, data_file)
        if path is None:
            return None
        with open(path, encoding="utf-8") as f:
            return f.read()

    def expect(self, condition, claim, evidence=None):
        """Exits 0 with the evidence if condition holds, else 1."""
        if isinstance(evidence, list) and evidence and isinstance(evidence[0], Result):
            if any(r.verdict == "BOOTSTRAP-FAILED" for r in evidence):
                print("POC-ERROR harness could not bootstrap:", evidence[0].detail)
                sys.exit(2)
            evidence = "\n".join(f"  {r.command[:80]!r} -> {r}" for r in evidence)
        print(("REPRODUCED " if condition else "NOT-REPRODUCED ") + claim)
        if evidence:
            print(evidence)
        sys.exit(0 if condition else 1)

    def needs_gui(self, what_to_look_for, commands=None):
        """Exits 3 after printing the GUI steps; the run directory holds the prepared state."""
        print("NEEDS-GUI " + what_to_look_for)
        print("  run directory: " + str(self.rundir))
        for c in commands or []:
            print(f"  then enter: {c[:200]!r}")
        sys.exit(3)


def _parse(stdout, commands):
    results = []
    for line in stdout.splitlines():
        if line.startswith("BOOTSTRAP-FAILED"):
            return [Result("", "BOOTSTRAP-FAILED", line)]
        for verdict in ("OK", "REJECT", "CRASH", "HANG"):
            if line.startswith(verdict + " "):
                cmd = commands[len(results)] if len(results) < len(commands) else ""
                results.append(Result(cmd, verdict, line[len(verdict) + 1:]))
    return results


def _data_path(rundir, data_file):
    if rundir is None:
        return None
    if data_file:
        p = os.path.join(rundir, data_file)
        return p if os.path.exists(p) else None
    data_dir = os.path.join(rundir, "data")
    if not os.path.isdir(data_dir):
        return None
    files = sorted(f for f in os.listdir(data_dir) if f.endswith(".json"))
    return os.path.join(data_dir, files[0]) if files else None


def _write(path, content):
    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(content if isinstance(content, str) else json.dumps(content, indent=2))


def _find_workdir(start):
    d = start
    while d != os.path.dirname(d):
        if os.path.exists(os.path.join(d, "target.jar")) and os.path.isdir(os.path.join(d, "reports")):
            return d
        d = os.path.dirname(d)
    raise SystemExit("POC-ERROR cannot find a workdir (target.jar + reports/) above " + start)
