#pragma once

#include "CoreMinimal.h"
#include "Components/ActorComponent.h"
#include "TimerManager.h"
#include "RazlomTimeGlimpseComponent.generated.h"

/** UI/camera/audio enter and exit signals. Never replaces the gameplay pawn. */
DECLARE_DYNAMIC_MULTICAST_DELEGATE_OneParam(FRazlomGlimpseSignal, FName, GlimpseId);

/**
 * Rare 3–5 second time vision. This component intentionally NEVER teleports,
 * swaps worlds, alters the controlled pawn or commits SaveGame state.
 * Presentation in the other epoch is delegated to Blueprint listeners.
 */
UCLASS(ClassGroup=(Razlom), meta=(BlueprintSpawnableComponent))
class RAZLOM_API URazlomTimeGlimpseComponent : public UActorComponent
{
    GENERATED_BODY()

public:
    URazlomTimeGlimpseComponent();

    /** Must be 3–5 seconds; overlapping starts are rejected. */
    UFUNCTION(BlueprintCallable, Category="Razlom|Time Glimpse")
    bool StartGlimpse(FName GlimpseId, float DurationSeconds = 4.0f);

    /** Explicit cancellation restores the presentation state. */
    UFUNCTION(BlueprintCallable, Category="Razlom|Time Glimpse")
    void CancelGlimpse();

    UFUNCTION(BlueprintPure, Category="Razlom|Time Glimpse")
    bool IsGlimpseActive() const { return bIsActive; }

    UPROPERTY(BlueprintAssignable, Category="Razlom|Time Glimpse")
    FRazlomGlimpseSignal OnGlimpseStarted;

    UPROPERTY(BlueprintAssignable, Category="Razlom|Time Glimpse")
    FRazlomGlimpseSignal OnGlimpseEnded;

protected:
    virtual void EndPlay(const EEndPlayReason::Type EndPlayReason) override;

private:
    void FinishGlimpse();

    UPROPERTY(Transient)
    bool bIsActive = false;

    UPROPERTY(Transient)
    FName ActiveGlimpseId = NAME_None;

    FTimerHandle FinishTimer;
};
