#!/usr/bin/env python3
"""Unreal Editor Python asset import for original 1348 Izba PBR textures.

Run inside UE5 Editor with Python Editor Script Plugin enabled, after generating
the PNG pack. DRAFT: editor-level integration is not yet executed or certified.
Set RAZLOM_IZBA_PBR_DIR to the directory holding texture_manifest.json.
"""
import json
import os
from pathlib import Path
import unreal

SRC = Path(os.environ.get("RAZLOM_IZBA_PBR_DIR",
   "Content/Art/Environment/Medieval1348/IzbaKit/Textures")).resolve()
DEST = "/Game/Art/Environment/Medieval1348/IzbaKit/Textures"
MAT_DEST = "/Game/Art/Environment/Medieval1348/IzbaKit/Materials"

def must(ok, label):
    if not ok:
        raise RuntimeError("RAZLOM UE5 material import failed: " + label)

def import_texture(asset_tools, filepath, destination, role):
    task = unreal.AssetImportTask()
    task.set_editor_property("filename", str(filepath))
    task.set_editor_property("destination_path", destination)
    task.set_editor_property("automated", True)
    task.set_editor_property("replace_existing", True)
    task.set_editor_property("replace_existing_settings", False)
    task.set_editor_property("save", True)
    asset_tools.import_asset_tasks([task])
    result_paths = list(task.get_editor_property("imported_object_paths"))
    must(result_paths, "Imported texture path missing: " + str(filepath))
    texture = unreal.EditorAssetLibrary.load_asset(str(result_paths[0]))
    must(texture is not None, "Texture not loadable: " + str(filepath))
    if role == "BaseColor":
        texture.set_editor_property("srgb", True)
        texture.set_editor_property("compression_settings",
            unreal.TextureCompressionSettings.TC_DEFAULT)
    elif role == "Normal_DX":
        texture.set_editor_property("srgb", False)
        texture.set_editor_property("compression_settings",
            unreal.TextureCompressionSettings.TC_NORMALMAP)
    elif role == "ORM":
        texture.set_editor_property("srgb", False)
        texture.set_editor_property("compression_settings",
            unreal.TextureCompressionSettings.TC_MASKS)
    must(unreal.EditorAssetLibrary.save_loaded_asset(texture),
         "Texture save failed: " + str(filepath))
    return texture

def material_for(asset_tools, slot, color, normal, orm):
    name = "M_" + slot + "_1348"
    path = MAT_DEST + "/" + name
    if unreal.EditorAssetLibrary.does_asset_exist(path):
        material = unreal.EditorAssetLibrary.load_asset(path)
        must(material is not None, "Cannot load material: " + path)
    else:
        factory = unreal.MaterialFactoryNew()
        material = asset_tools.create_asset(name, MAT_DEST, unreal.Material, factory)
        must(material is not None, "Cannot create material: " + path)

    edit = unreal.MaterialEditingLibrary
    # Deliberate controlled regeneration; this script owns only M_*_1348 under MAT_DEST.
    edit.delete_all_material_expressions(material)
    def sample(texture, x, y, label):
        node = edit.create_material_expression(material,
                      unreal.MaterialExpressionTextureSample, x, y)
        must(node is not None, "Cannot create texture node: " + label)
        node.set_editor_property("texture", texture)
        return node
    c = sample(color,-630,-280,"base")
    n = sample(normal,-630,-40,"normal")
    o = sample(orm,-630,240,"orm")
    # Named RGB/R/G/B ports allow one packed 3-channel ORM texture.
    for expr, output, prop in (
        (c, "RGB", unreal.MaterialProperty.MP_BASE_COLOR),
        (n, "RGB", unreal.MaterialProperty.MP_NORMAL),
        (o, "R", unreal.MaterialProperty.MP_AMBIENT_OCCLUSION),
        (o, "G", unreal.MaterialProperty.MP_ROUGHNESS),
        (o, "B", unreal.MaterialProperty.MP_METALLIC),
    ):
        must(edit.connect_material_property(expr, output, prop),
             "Failed to connect " + name + " output " + output + " to " + str(prop))
    edit.recompile_material(material)
    must(unreal.EditorAssetLibrary.save_loaded_asset(material),
         "Could not save material: " + path)
    return material

def main():
    manifest_file = SRC/"texture_manifest.json"
    must(manifest_file.is_file(), "Missing manifest: " + str(manifest_file))
    manifest = json.loads(manifest_file.read_text(encoding="utf8"))
    must(len(manifest["maps"]) == 7, "Expected seven material recipes")
    unreal.EditorAssetLibrary.make_directory(DEST)
    unreal.EditorAssetLibrary.make_directory(MAT_DEST)
    tools = unreal.AssetToolsHelpers.get_asset_tools()
    for recipe in manifest["maps"]:
        slot=recipe["materialSlot"]
        textures = {}
        for role,filename in recipe["maps"].items():
            file=SRC/filename
            must(file.is_file(), "Missing PNG: " + str(file))
            textures[role]=import_texture(tools,file,DEST,role)
        mat=material_for(tools,slot,textures["BaseColor"],textures["Normal_DX"],textures["ORM"])
        unreal.log("RAZLOM Izba material: " + mat.get_path_name())
    unreal.log("RAZLOM Izba 1348 texture import complete: 21 PNGs, 7 material graphs. UE5 screenshot/visual review remains required.")

if __name__ == "__main__":
    main()
