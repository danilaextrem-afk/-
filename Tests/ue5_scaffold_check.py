#!/usr/bin/env python3
"""Check *file contracts* of UE5 foundation. This is not a UE build/test."""
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
project=json.loads((ROOT/'Razlom.uproject').read_text(encoding='utf-8'))
assert project['FileVersion']==3
assert any(x.get('Name')=='Razlom' and x.get('Type')=='Runtime' for x in project['Modules'])
mandatory=[
 'Source/Razlom.Target.cs','Source/RazlomEditor.Target.cs',
 'Source/Razlom/Razlom.Build.cs','Source/Razlom/Private/Razlom.cpp',
 'Source/Razlom/Public/RazlomTimeGlimpseComponent.h',
 'Source/Razlom/Private/RazlomTimeGlimpseComponent.cpp',
 'Config/DefaultEngine.ini','Tools/ue5_prepare_import.py'
]
for item in mandatory:
    assert (ROOT/item).exists(),f'Missing: {item}'
header=(ROOT/'Source/Razlom/Public/RazlomTimeGlimpseComponent.h').read_text()
impl=(ROOT/'Source/Razlom/Private/RazlomTimeGlimpseComponent.cpp').read_text()
assert 'GENERATED_BODY()' in header
assert 'StartGlimpse' in header and 'OnGlimpseEnded' in header
assert 'DurationSeconds < 3.0f || DurationSeconds > 5.0f' in impl
assert 'OnGlimpseStarted.Broadcast' in impl and 'OnGlimpseEnded.Broadcast' in impl
assert 'SetTimer' in impl and 'ClearTimer' in impl
assert 'TeleportTo(' not in impl and 'SetActorLocation(' not in impl
assert '3840' in (ROOT/'Tests/UE5/README.md').read_text()
print('PASS: UE5 source scaffold and 3–5 sec vision contract file checks')
print('NOTE: No C++ compilation, UE editor import, GPU screenshot, or 4K FPS validation performed.')
