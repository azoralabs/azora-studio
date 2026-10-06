"""Qualify real native compiler through the emitted Studio services ABI."""
import json
import os
from pathlib import Path
import subprocess
import sys

binary, temporary, compiler = sys.argv[1:]
root = Path(temporary) / "native-tool"
root.mkdir()
(root / "src").mkdir()
base = {**os.environ, "AZORA_STUDIO_PROJECT": str(root), "AZORA_NATIVE_COMPILER": compiler}

def action(name):
    result = subprocess.run([binary], env={**base, "AZORA_STUDIO_ACTION": name},
                            capture_output=True, text=True, timeout=60)
    assert result.returncode == 0, result.stdout + result.stderr
    assert "native handle leak" not in result.stdout, result.stdout
    return result.stdout

source = 'import std.io\nfunc main() { println("native Studio tool workflow") }\n'
(root / "src/main.az").write_text(source)
result = json.loads(action("inspect"))
assert result["protocol"] == 1 and result["diagnostics"] == [], result
assert "built " in action("build")
assert "native Studio tool workflow" in action("play")
source = 'import std.io\nfunc main() { println(missing) }\n'
(root / "src/main.az").write_text(source)
result = json.loads(action("inspect"))
diag = next(d for d in result["diagnostics"] if d["code"] == "AZ-SYM-0001")
assert "missing" in diag["message"] and diag["start"] == source.index("missing"), diag
assert diag["startLine"] == 1 and diag["startByte"] == 22, diag
assert diag["source"] == str((root / "src/main.az").resolve()), diag
print("native Studio compiler inspect/build/play and semantic diagnostic protocol passed")

# Reuse a real Engine source fixture through the native template endpoint. This
# checks project creation/build/play without substituting a Python/JVM builder.
engine = os.environ.get("AZORA_ENGINE_HOME")
if engine:
    project = Path(temporary) / "engine-project"
    fixture = Path(engine) / "tests/headless-ecs"
    result = subprocess.run([binary], env={**base, "AZORA_STUDIO_PROJECT": str(project),
        "AZORA_STUDIO_ACTION": "template", "AZORA_STUDIO_TEMPLATE": str(fixture)},
        capture_output=True, text=True, timeout=60)
    assert result.returncode == 0, result.stdout + result.stderr
    assert (project / "src/main.az").read_bytes() == (fixture / "src/main.az").read_bytes()
    assert (project / "azora.azon").exists()
    for name in ("build", "play"):
        result = subprocess.run([binary], env={**base, "AZORA_STUDIO_PROJECT": str(project),
            "AZORA_STUDIO_ACTION": name}, capture_output=True, text=True, timeout=60)
        assert result.returncode == 0, result.stdout + result.stderr
        assert "native handle leak" not in result.stdout
        if name == "play": assert "headless ecs passed" in result.stdout, result.stdout
    print("native Studio template-create/build/play actual Engine ECS project passed")
