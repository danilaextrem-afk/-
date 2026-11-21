#pragma once

#include "CoreMinimal.h"
#include "GameFramework/Character.h"
#include "GameFramework/GameModeBase.h"
#include "GameFramework/HUD.h"
#include "GameFramework/SaveGame.h"
#include "EntranceEpisode.generated.h"

class UStaticMesh;
class UCameraComponent;
class URazlomTimeGlimpseComponent;

UENUM()
enum class EEntranceStep : uint8 { Neighbor, Evidence, Shadow, Tenant, Helped, Left, Phone, Sender, Ilya };

UCLASS()
class RAZLOM_API UEntranceSave : public USaveGame
{
    GENERATED_BODY()
public:
    UPROPERTY(SaveGame) int32 Version = 1;
    UPROPERTY(SaveGame) EEntranceStep Step = EEntranceStep::Neighbor;
};

UCLASS()
class RAZLOM_API AEntranceCharacter : public ACharacter
{
    GENERATED_BODY()
public:
    AEntranceCharacter();
    virtual void Tick(float DeltaSeconds) override;
    FString Objective() const;
    FString Message;
protected:
    virtual void BeginPlay() override;
    virtual void SetupPlayerInputComponent(UInputComponent* Input) override;
private:
    void Forward(float Value);
    void Right(float Value);
    void Interact();
    void Leave();
    void Strike();
    bool Facing(FName Tag, float Range) const;
    bool Commit(EEntranceStep Next);
    void UpdateScene();
    UFUNCTION() void EndVision(FName Id);

    UPROPERTY() TObjectPtr<UCameraComponent> Camera;
    UPROPERTY() TObjectPtr<URazlomTimeGlimpseComponent> Vision;
    UPROPERTY() TObjectPtr<AActor> Shadow;
    EEntranceStep Step = EEntranceStep::Phone;
    FVector ShadowOrigin = FVector::ZeroVector;
    bool bSaveBlocked = false;
};

UCLASS()
class RAZLOM_API AEntranceHUD : public AHUD
{
    GENERATED_BODY()
public:
    virtual void DrawHUD() override;
};

UCLASS()
class RAZLOM_API AEntranceGameMode : public AGameModeBase
{
    GENERATED_BODY()
public:
    AEntranceGameMode();
    virtual void InitGame(const FString& MapName, const FString& Options, FString& ErrorMessage) override;
private:
    UPROPERTY() TObjectPtr<UStaticMesh> Block;
    UPROPERTY() TObjectPtr<UStaticMesh> Cylinder;
    UPROPERTY() TObjectPtr<UStaticMesh> Sphere;
};
