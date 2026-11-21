#include "EntranceEpisode.h"
#include "Components/PointLightComponent.h"
#include "Components/StaticMeshComponent.h"
#include "Components/TextRenderComponent.h"
#include "Engine/PointLight.h"
#include "Engine/StaticMeshActor.h"
#include "Engine/TextRenderActor.h"
#include "Engine/World.h"
#include "GameFramework/PlayerStart.h"

void AEntranceGameMode::InitGame(const FString& MapName, const FString& Options, FString& ErrorMessage)
{
    Super::InitGame(MapName, Options, ErrorMessage);
    UWorld* World = GetWorld();
    // This GameMode owns a transient blockout on the engine Entry map.
    auto Mesh = [World](UStaticMesh* Asset, FVector Center, FVector SizeCm,
                        FName Tag = NAME_None, bool Solid = true)
    {
        auto* Actor = World->SpawnActor<AStaticMeshActor>(Center, FRotator::ZeroRotator);
        check(Actor);
        auto* Component = Actor->GetStaticMeshComponent();
        Component->SetMobility(EComponentMobility::Movable);
        Component->SetStaticMesh(Asset);
        Actor->SetActorScale3D(SizeCm / 100.0);
        Component->SetCollisionProfileName(Solid ? TEXT("BlockAllDynamic") : TEXT("NoCollision"));
        if (!Tag.IsNone()) Actor->Tags.Add(Tag);
        return Actor;
    };
    auto Label = [World](const TCHAR* Value, FVector Location)
    {
        auto* Actor = World->SpawnActor<ATextRenderActor>(Location, FRotator(0, 180, 0));
        auto* Text = Actor->GetTextRender();
        Text->SetText(FText::FromString(Value));
        Text->SetHorizontalAlignment(EHTA_Center);
        Text->SetWorldSize(18.f);
    };
    Mesh(Block, {400,0,-15}, {1600,680,30});
    // A01M04 workshop and A01M05 entrance share a traversable threshold.
    Mesh(Block, {-850,0,-15}, {900,680,30});
    Mesh(Block, {-1295,0,145}, {10,680,290});
    Mesh(Block, {-850,-335,145}, {900,10,290});
    Mesh(Block, {-850,335,145}, {900,10,290});
    Mesh(Block, {-395,-260,145}, {10,160,290});
    Mesh(Block, {-395,170,145}, {10,340,290});
    Mesh(Block, {-395,-90,260}, {10,180,60});
    Mesh(Block, {-990,-75,45}, {100,80,90});
    Mesh(Block, {-1000,-75,94}, {22,12,8}, TEXT("Phone"));
    Label(TEXT("Телефон"), {-990,-75,122});
    Mesh(Cylinder, {-590,85,82}, {50,50,164}, TEXT("Ilya"));
    Label(TEXT("Илья"), {-590,85,185});
    Mesh(Block, {1195,0,145}, {10,680,290});
    Mesh(Block, {400,-335,145}, {1600,10,290});
    Mesh(Block, {400,335,48}, {1600,10,96});
    Mesh(Block, {400,335,265}, {1600,10,50});
    for (int32 X = -395; X < 1200; X += 200)
        Mesh(Block, {double(X),335,167}, {8,10,150});
    Mesh(Block, {720,-250,115}, {16,170,230});
    Mesh(Block, {720,165,115}, {16,330,230});
    Mesh(Block, {720,-82,216}, {16,165,28});
    Label(TEXT("9"), {706,-82,204});
    Mesh(Cylinder, {10,-100,82}, {48,48,164}, TEXT("Neighbor"));
    Label(TEXT("Соседка"), {8,-100,185});
    Mesh(Block, {340,265,80}, {115,65,10});
    Mesh(Block, {340,250,90}, {30,16,9}, TEXT("Evidence"));
    Label(TEXT("Ключ"), {338,248,120});
    auto* Shade = Mesh(Sphere, {670,-82,82}, {58,58,164}, TEXT("Shadow"));
    // Visibility traces can hit the shade while its overlap with the capsule triggers recoil.
    Shade->GetStaticMeshComponent()->SetCollisionResponseToChannel(ECC_Pawn, ECR_Ignore);
    Mesh(Cylinder, {855,-82,82}, {50,50,164}, TEXT("Tenant"));
    for (int32 X = -350; X <= 1150; X += 75)
        Mesh(Cylinder, {double(X),430,165}, {48,48,330}, TEXT("Epoch1348"), false);

    for (int32 X = -1000; X <= 1000; X += 400)
    {
        auto* Light = World->SpawnActor<APointLight>(FVector(X,0,245), FRotator::ZeroRotator);
        auto* Component = CastChecked<UPointLightComponent>(Light->GetLightComponent());
        Component->SetMobility(EComponentMobility::Movable);
        Component->SetIntensity(4500.f);
        Component->SetAttenuationRadius(820.f);
        Component->SetLightColor(FLinearColor(1.f, .85f, .65f));
    }
    World->SpawnActor<APlayerStart>(FVector(-235,-100,94), FRotator::ZeroRotator);
}
