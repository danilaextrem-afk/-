"""Cheap source-level checks only; these DO NOT substitute for Godot runtime/APK tests."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def test_files_exist():
    for path in ['project.godot','scenes/main.tscn','scripts/game.gd','scripts/player.gd','scripts/enemy.gd','docs/ART_DIRECTION.md','docs/ANDROID_EXPORT.md']:
        assert (ROOT / path).is_file(), path


def test_scene_reference():
    scene = (ROOT/'scenes/main.tscn').read_text(encoding='utf-8')
    assert 'res://scripts/game.gd' in scene


def test_input_actions():
    project = (ROOT/'project.godot').read_text(encoding='utf-8')
    for name in ('move_forward','move_backward','move_left','move_right','interact','sprint','toggle_flashlight','quick_save'):
        assert re.search(r'^'+name+r'=\{', project, re.M), name
    assert 'renderer/rendering_method.mobile="gl_compatibility"' in project


def test_free_roam_survival_contract():
    code = (ROOT/'scripts/game.gd').read_text(encoding='utf-8')
    for term in ('_spawn_enemy(', '_spawn_pickup(', '_anomaly(', '_save_game(', '_load_save(', '_interact(', '_create_hud(', 'user://razlom_zona_save_v1.json'):
        assert term in code, term
    assert not re.search(r'func\s+(?:_?spawn_boss|_?boss)\b', code)
    assert 'mission_required' not in code.lower()


def test_no_absolute_file_references():
    for script in (ROOT/'scripts').glob('*.gd'):
        data = script.read_text(encoding='utf-8')
        assert '/mnt/data/' not in data
        assert 'https://' not in data


if __name__ == '__main__':
    checks = [test_files_exist, test_scene_reference, test_input_actions, test_free_roam_survival_contract, test_no_absolute_file_references]
    for check in checks:
        check()
        print('PASS',check.__name__)
    print(f'Source checks: {len(checks)} passed (runtime not checked).')
