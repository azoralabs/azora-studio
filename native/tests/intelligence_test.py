"""Exercise emitted Azora intelligence, not a duplicate Python lexer."""
import os
from pathlib import Path
import subprocess
import sys

binary, temporary = sys.argv[1:]
root = Path(temporary) / "intelligence"
(root / "src").mkdir(parents=True)

def inspect(source):
    (root / "src/main.az").write_text(source)
    return subprocess.run([binary], env={**os.environ, "AZORA_STUDIO_PROJECT": str(root),
                          "AZORA_STUDIO_ACTION": "structure"}, capture_output=True, text=True)

result = inspect('// func ghost() {\nfunc real() { println("[}") }\n/* nested /* } */ { */\n')
assert result.returncode == 0, result.stdout + result.stderr
assert result.stdout.splitlines() == ["symbol\t23\t4\t2\t6"], result.stdout
result = inspect("func bad() { ] }\n")
assert result.returncode != 0
assert "diagnostic\t13\t1\t1\t14\tmismatched closing delimiter" in result.stdout, result.stdout
result = inspect("/* unterminated")
assert result.returncode == 1
assert result.stdout.strip() == "diagnostic\t15\t0\t1\t16\tunterminated block comment", result.stdout
result = inspect('func main() { "unterminated')
assert result.returncode != 0 and "unterminated literal" in result.stdout
# Read-error and missing-native-tool paths release every successfully opened owner.
result = subprocess.run([binary], env={**os.environ, "AZORA_STUDIO_PROJECT": str(root),
                        "AZORA_STUDIO_ACTION": "build", "AZORA_STUDIO_BUILDER": "", "AZORA_NATIVE_COMPILER": ""},
                        capture_output=True, text=True)
assert result.returncode == 1 and "absolute native executable" in result.stdout
assert "native handle leak" not in result.stdout, result.stdout
print("native structural spans, comment/literal exclusion and fail-closed build probes passed")
