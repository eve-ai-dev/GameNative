#!/usr/bin/env python3
"""Verify Y700 fork invariants. Usage: python3 scripts/verify_y700_contract.py [repo-root]; Example: python3 scripts/verify_y700_contract.py ."""

from __future__ import annotations

import sys
from pathlib import Path


EXPECTED_CERT_SHA256 = "eb2a3fac589273c9b1b38a1c6199c5f6fb13d11f382c48ca14ac51bb714310a9"


def read(root: Path, relative_path: str) -> str:
    path = root / relative_path
    if not path.is_file():
        raise FileNotFoundError(f"required file missing: {relative_path}")
    return path.read_text(encoding="utf-8")


def verify(root: Path) -> list[str]:
    errors: list[str] = []
    try:
        gradle = read(root, "app/build.gradle.kts")
        java = read(root, "app/src/main/java/com/winlator/winhandler/WinHandler.java")
        native = read(root, "app/src/main/cpp/evshim/evshim.c")
        workflow = read(root, ".github/workflows/pluvia-pr-check.yml")
        integration_workflow = read(root, ".github/workflows/y700-protected-integration.yml")
    except FileNotFoundError as exc:
        return [str(exc)]

    required_gradle = {
        'isolated release application ID suffix': 'applicationIdSuffix = ".joycontest"',
        'isolated release version suffix': 'versionNameSuffix = "-joycon-test"',
        'stable Joy-Con signing config': 'create("joycontest")',
    }
    for label, token in required_gradle.items():
        if token not in gradle:
            errors.append(f"{label} missing from app/build.gradle.kts")

    declaration = "private static native boolean initializeSharedMemory(String basePath, int players);"
    invocation = "initializeSharedMemory(context.getFilesDir().getAbsolutePath(), MAX_PLAYERS)"
    buffer_mapping = "extraGamepadBuffers[i].order(ByteOrder.LITTLE_ENDIAN);"
    if java.count(declaration) != 1:
        errors.append("WinHandler must declare initializeSharedMemory exactly once")
    if java.count(invocation) != 1:
        errors.append("WinHandler must initialize native shared memory from Context.getFilesDir exactly once")
    elif buffer_mapping not in java or java.index(invocation) < java.index(buffer_mapping):
        errors.append("WinHandler must initialize native shared memory after Java maps all gamepad files")

    native_requirements = {
        "JNI initializeSharedMemory export": "Java_com_winlator_winhandler_WinHandler_initializeSharedMemory",
        "runtime app-storage path mapping": "setup_shm(players, base)",
        "Wine environment path mapping": "setup_shm(players, NULL)",
        "deferred Java-process mapping": "Java process awaiting explicit app storage initialization",
    }
    for label, token in native_requirements.items():
        if token not in native:
            errors.append(f"evshim {label} missing")

    delivery_requirements = {
        "stable-branch dispatch gate": 'test "$GITHUB_REF_NAME" = "y700/stable"',
        "native contract command": "python3 scripts/verify_y700_contract.py .",
        "JNI symbol verification": "Java_com_winlator_winhandler_WinHandler_initializeSharedMemory",
        "isolated package verification": 'test "$PACKAGE" = "app.gamenative.joycontest"',
        "stable signing certificate verification": EXPECTED_CERT_SHA256,
        "Legacy tests": ":app:testLegacyDebugUnitTest",
        "Modern tests": ":app:testModernDebugUnitTest",
    }
    for label, token in delivery_requirements.items():
        if token not in workflow:
            errors.append(f"delivery workflow {label} missing")

    integration_requirements = {
        "Y700 PR trigger": 'branches: [ "y700/stable" ]',
        "required check name": "name: Y700 contract",
        "contract regression tests": "python3 scripts/test_verify_y700_contract.py",
        "native bridge verification": "Java_com_winlator_winhandler_WinHandler_initializeSharedMemory",
        "isolated package verification": 'test "$PACKAGE" = "app.gamenative.joycontest"',
        "Legacy tests": ":app:testLegacyDebugUnitTest",
        "Modern tests": ":app:testModernDebugUnitTest",
    }
    for label, token in integration_requirements.items():
        if token not in integration_workflow:
            errors.append(f"protected integration workflow {label} missing")

    return errors


def main(argv: list[str]) -> int:
    root = Path(argv[1] if len(argv) > 1 else ".").resolve()
    errors = verify(root)
    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        print(f"Y700_CONTRACT=FAIL errors={len(errors)}", file=sys.stderr)
        return 1
    print("Y700_CONTRACT=PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
