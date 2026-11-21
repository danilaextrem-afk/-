"""Import the assembled courtyard frontage and reject missing geometry or bad units."""
from pathlib import Path
import unreal

root = Path(unreal.Paths.convert_relative_path_to_full(unreal.Paths.project_dir()))
source = root / "SourceArt/Integration/EntranceNine.glb"
if not source.is_file():
    raise RuntimeError(f"Missing assembled mesh: {source}")
task = unreal.AssetImportTask()
task.filename = str(source)
task.destination_path = "/Game/EntranceNine"
task.automated = True
task.replace_existing = True
task.save = True
unreal.AssetToolsHelpers.get_asset_tools().import_asset_tasks([task])
meshes = [obj for obj in task.get_objects() if isinstance(obj, unreal.StaticMesh)]
if len(meshes) != 1:
    raise RuntimeError(f"Expected one assembled static mesh, imported {len(meshes)}")
mesh = meshes[0]
dest = "/Game/EntranceNine/SM_EntranceNine"
if mesh.get_path_name().split(".")[0] != dest:
    if not unreal.EditorAssetLibrary.rename_loaded_asset(mesh, dest):
        raise RuntimeError("Cannot assign the runtime mesh path")
bounds = mesh.get_bounds().box_extent
if not (255 <= max(bounds.x, bounds.y) <= 265 and 230 <= bounds.z <= 240):
    raise RuntimeError(f"Unexpected entrance half-extents in centimetres: {bounds}")
mesh.get_editor_property("body_setup").set_editor_property(
    "collision_trace_flag", unreal.CollisionTraceFlag.CTF_USE_COMPLEX_AS_SIMPLE)
if not unreal.EditorAssetLibrary.save_loaded_asset(mesh, only_if_is_dirty=False):
    raise RuntimeError("Could not save the game mesh")
