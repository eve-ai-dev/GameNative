#!/usr/bin/env python3
"""Test Y700 contract guard failures. Usage: python3 scripts/test_verify_y700_contract.py; Example: python3 scripts/test_verify_y700_contract.py"""

from __future__ import annotations

import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
VERIFIER = ROOT / "scripts" / "verify_y700_contract.py"
FILES = (
    "app/build.gradle.kts",
    "app/src/main/java/com/winlator/winhandler/WinHandler.java",
    "app/src/main/cpp/evshim/evshim.c",
    ".github/workflows/pluvia-pr-check.yml",
    ".github/workflows/y700-protected-integration.yml",
)


class VerifyY700ContractTest(unittest.TestCase):
    def make_fixture(self) -> Path:
        temp_dir = Path(tempfile.mkdtemp(prefix="y700-contract-"))
        self.addCleanup(shutil.rmtree, temp_dir)
        for relative_path in FILES:
            source = ROOT / relative_path
            destination = temp_dir / relative_path
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, destination)
        return temp_dir

    def run_verifier(self, root: Path) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            ["python3", str(VERIFIER), str(root)],
            check=False,
            capture_output=True,
            text=True,
        )

    def test_current_repository_passes(self) -> None:
        result = self.run_verifier(ROOT)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("Y700_CONTRACT=PASS", result.stdout)

    def test_missing_java_to_jni_invocation_fails(self) -> None:
        fixture = self.make_fixture()
        path = fixture / "app/src/main/java/com/winlator/winhandler/WinHandler.java"
        path.write_text(
            path.read_text(encoding="utf-8").replace(
                "initializeSharedMemory(context.getFilesDir().getAbsolutePath(), MAX_PLAYERS)",
                "missingInitialization(context.getFilesDir().getAbsolutePath(), MAX_PLAYERS)",
            ),
            encoding="utf-8",
        )
        result = self.run_verifier(fixture)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("initialize native shared memory", result.stderr)

    def test_missing_isolated_package_suffix_fails(self) -> None:
        fixture = self.make_fixture()
        path = fixture / "app/build.gradle.kts"
        path.write_text(
            path.read_text(encoding="utf-8").replace(
                'applicationIdSuffix = ".joycontest"',
                'applicationIdSuffix = ".broken"',
            ),
            encoding="utf-8",
        )
        result = self.run_verifier(fixture)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("application ID suffix", result.stderr)


if __name__ == "__main__":
    unittest.main()
