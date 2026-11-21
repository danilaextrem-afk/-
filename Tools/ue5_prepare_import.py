"""Run INSIDE Unreal Editor Python (UE5) to import RAZLOM draft OBJ geometry.

Run: Unreal Engine editor -> Tools / Execute Python Script -> this file.
This does not material-bind PBR maps. Visually verify every mesh manually.
"""
from pathlib import Path
import unreal

ROOT = Path(unreal.Paths.project_dir())
OBJ_FILES = (
    ("HeroProps", "SM_BlackBell_Hero_v2.obj"),
    ("OriginalProps", "SM_SevenBellsShrine_1348.obj"),
    ("OriginalProps", "SM_VeiledOrderGate_1348.obj"),
    ("OriginalProps", "SM_MoscowApartmentEntry_2026.obj"),
)
base = ROOT / "Content" / "Art"
tools = unreal.AssetToolsHelpers.get_asset_tools()
tasks = []
for category, filename in OBJ_FILES:
    path = base / category / filename
    if not path.exists():
        unreal.log_warning("RAZLOM import: missing " + str(path))
        continue
    task = unreal.AssetImportTask()
    task.set_editor_property("filename", str(path))
    task.set_editor_property("destination_path", "/Game/ImportedRazlom/" + category)
    task.set_editor_property("automated", True)
    task.set_editor_property("save", True)
    task.set_editor_property("replace_existing", False)
    tasks.append(task)

if not tasks:
    raise RuntimeError("No RAZLOM source meshes available at " + str(base))
tools.import_asset_tasks(tasks)
for task in tasks:
    imported = task.get_editor_property("imported_object_paths")
    unreal.log("RAZLOM import: " + str(imported) + " from " + str(task.get_editor_property("filename")))
unreal.log_warning("Material/normal/tangent/Nanite validation and real UE5 screenshots are still REQUIRED.")
