#!/usr/bin/env python3
"""Report a GitHub cloud runner's capabilities, without claiming it is a UE5 GPU workstation."""
import json
import os
import platform
import shutil
import subprocess
from pathlib import Path
from datetime import datetime,timezone

def run(*cmd):
    try:
        p=subprocess.run(cmd, capture_output=True, text=True, timeout=15, check=False)
        return {"exit_code":p.returncode,"stdout":p.stdout[:4000],"stderr":p.stderr[:2000]}
    except (FileNotFoundError,subprocess.TimeoutExpired,PermissionError) as e:
        return {"error":str(e)}

def main():
    out=Path("build/cloud_probe");out.mkdir(parents=True,exist_ok=True)
    disk=shutil.disk_usage(".")
    data={
      "project":"RAZLOM: CHERNY ZVON",
      "timestamp_utc":datetime.now(timezone.utc).isoformat(),
      "os":platform.platform(),
      "machine":platform.machine(),
      "python":platform.python_version(),
      "cores":os.cpu_count(),
      "disk_total_gb":round(disk.total/1024**3,2),
      "disk_free_gb":round(disk.free/1024**3,2),
      "github_runner":os.environ.get("RUNNER_NAME","unknown"),
      "runner_os":os.environ.get("RUNNER_OS","unknown"),
      "nvidia_smi":run("nvidia-smi","--query-gpu=name,memory.total","--format=csv,noheader"),
      "blender":run("blender","--version"),
      "ue5_editor":(
       shutil.which("UnrealEditor-Cmd.exe")
       or shutil.which("UnrealEditor-Cmd")
       or shutil.which("UnrealEditor.exe")
       or "NOT_INSTALLED"
      ),
      "can_claim_ue5_gpu_4k":"NO — only real UE5 install, GPU identification and visual/performance test can validate"
    }
    (out / "runner_report.json").write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding="utf-8")
    print(json.dumps(data,ensure_ascii=False,indent=2))
if __name__=="__main__":
    main()
