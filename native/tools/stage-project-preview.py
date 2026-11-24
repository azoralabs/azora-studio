"""Stage the Azora modules a project explicitly registers for Studio preview."""

import json
from pathlib import Path
import re
import shutil
import sys

engine_tools, project, destination = map(Path, sys.argv[1:])
project = project.resolve()
sys.path.insert(0, str(engine_tools))
import azon

manifest = project / "azora.azon"
if not manifest.is_file():
    sys.exit(0)
source = manifest.read_text(encoding="utf-8")
try:
    config = json.loads(source)
except json.JSONDecodeError:
    config = azon.loads(source)
paths = config.get("project", {}).get("editorPreviewSources", [])
if not isinstance(paths, list):
    sys.exit("project.editorPreviewSources must be an array of Azora source paths")
for entry in paths:
    if not isinstance(entry, str):
        sys.exit("project.editorPreviewSources accepts source paths")
    relative = Path(entry)
    path = (project / relative).resolve()
    if relative.is_absolute() or not path.is_relative_to(project) or path.suffix != ".az" or not path.is_file():
        sys.exit(f"Invalid Studio preview source: {entry}")
    header = re.search(r"^\s*module\s+([A-Za-z_]\w*(?:\.[A-Za-z_]\w*)*)\s*$",
                       path.read_text(encoding="utf-8"), re.M)
    if header is None:
        sys.exit(f"Studio preview source needs a module: {entry}")
    target = destination.joinpath(*header[1].split(".")).with_suffix(".az")
    if target.exists():
        sys.exit(f"Duplicate Studio preview module: {header[1]}")
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(path, target)
    print(f"import {header[1]}")
