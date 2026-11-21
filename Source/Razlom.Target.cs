using UnrealBuildTool;
using System.Collections.Generic;

public class RazlomTarget : TargetRules
{
    public RazlomTarget(TargetInfo Target) : base(Target)
    {
        Type = TargetType.Game;
        DefaultBuildSettings = BuildSettingsVersion.V5;
        ExtraModuleNames.Add("Razlom");
    }
}
