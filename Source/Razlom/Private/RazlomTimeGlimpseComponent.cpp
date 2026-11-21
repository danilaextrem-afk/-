#include "RazlomTimeGlimpseComponent.h"
#include "Engine/World.h"

URazlomTimeGlimpseComponent::URazlomTimeGlimpseComponent()
{
    PrimaryComponentTick.bCanEverTick = false;
}

bool URazlomTimeGlimpseComponent::StartGlimpse(FName GlimpseId, float DurationSeconds)
{
    UWorld* World = GetWorld();
    if (!World || bIsActive || GlimpseId.IsNone() || !FMath::IsFinite(DurationSeconds)
        || DurationSeconds < 3.0f || DurationSeconds > 5.0f)
    {
        return false;
    }

    bIsActive = true;
    ActiveGlimpseId = GlimpseId;
    World->GetTimerManager().SetTimer(
        FinishTimer, this, &URazlomTimeGlimpseComponent::FinishGlimpse,
        DurationSeconds, false);
    OnGlimpseStarted.Broadcast(ActiveGlimpseId);
    return true;
}

void URazlomTimeGlimpseComponent::FinishGlimpse()
{
    if (!bIsActive) return;
    const FName FinishedId = ActiveGlimpseId;
    bIsActive = false;
    ActiveGlimpseId = NAME_None;
    FinishTimer.Invalidate();
    OnGlimpseEnded.Broadcast(FinishedId);
}

void URazlomTimeGlimpseComponent::CancelGlimpse()
{
    if (UWorld* World = GetWorld())
    {
        World->GetTimerManager().ClearTimer(FinishTimer);
    }
    FinishGlimpse();
}

void URazlomTimeGlimpseComponent::EndPlay(const EEndPlayReason::Type EndPlayReason)
{
    CancelGlimpse();
    Super::EndPlay(EndPlayReason);
}
