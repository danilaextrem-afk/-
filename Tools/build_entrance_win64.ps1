param([Parameter(Mandatory)][string]$EngineRoot, [string]$Blender = "blender")
$ErrorActionPreference = "Stop"
$projectRoot = Split-Path $PSScriptRoot
$project = Join-Path $projectRoot "Razlom.uproject"
$batches = Join-Path $EngineRoot "Engine/Build/BatchFiles"
$editor = Join-Path $EngineRoot "Engine/Binaries/Win64/UnrealEditor-Cmd.exe"
$report = Join-Path $projectRoot ("Saved/EntranceTests/" + [Guid]::NewGuid())
& "$batches/Build.bat" RazlomEditor Win64 Development "-Project=$project" -WaitMutex
if ($LASTEXITCODE -ne 0) { throw "Editor target failed ($LASTEXITCODE)" }
& $Blender -b --python-exit-code 1 -P "$projectRoot/Tools/Visual/build_entrance.py" -- --mesh-only
if ($LASTEXITCODE -ne 0) { throw "Entrance mesh assembly failed ($LASTEXITCODE)" }
& $editor $project -run=PythonScript "-script=$projectRoot/Tools/Visual/import_entrance.py" -unattended -nullrhi
if ($LASTEXITCODE -ne 0) { throw "Entrance mesh import failed ($LASTEXITCODE)" }
& $editor $project /Engine/Maps/Entry -game -unattended -nosplash -nullrhi -nosound '-ExecCmds=Automation RunTests Razlom.Entrance.Startup' '-TestExit=Automation Test Queue Empty' "-ReportExportPath=$report"
if ($LASTEXITCODE -ne 0) { throw "Entrance smoke failed ($LASTEXITCODE)" }
$results = Get-Content -Raw "$report/index.json" | ConvertFrom-Json
$smoke = @($results.tests | Where-Object { $_.fullTestPath -eq "Razlom.Entrance.Startup" })
if ($smoke.Count -ne 1 -or $smoke[0].state -ne "Success") { throw "Missing successful entrance smoke result" }
& "$batches/RunUAT.bat" BuildCookRun "-project=$project" -platform=Win64 -clientconfig=Development -build -cook -stage -pak -archive "-archivedirectory=$projectRoot/Builds/Entrance" -unattended
if ($LASTEXITCODE -ne 0) { throw "Win64 packaging failed ($LASTEXITCODE)" }
