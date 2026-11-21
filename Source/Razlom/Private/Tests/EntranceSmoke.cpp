#if WITH_DEV_AUTOMATION_TESTS
#include "EntranceEpisode.h"
#include "RazlomTimeGlimpseComponent.h"
#include "Engine/Engine.h"
#include "Engine/World.h"
#include "Kismet/GameplayStatics.h"
#include "Misc/AutomationTest.h"

IMPLEMENT_SIMPLE_AUTOMATION_TEST(FEntranceSmoke, "Razlom.Entrance.Startup",
    EAutomationTestFlags::ClientContext | EAutomationTestFlags::ProductFilter)

bool FEntranceSmoke::RunTest(const FString& Parameters)
{
    UWorld* World = nullptr;
    for (const FWorldContext& Context : GEngine->GetWorldContexts())
        if (Context.WorldType == EWorldType::Game) World = Context.World();
    if (!TestNotNull(TEXT("Game world"), World)) return false;
    auto* Hero = Cast<AEntranceCharacter>(UGameplayStatics::GetPlayerPawn(World, 0));
    if (!TestNotNull(TEXT("Possessed Kirill"), Hero)) return false;
    for (FName Tag : {FName(TEXT("Phone")), FName(TEXT("Ilya")), FName(TEXT("Neighbor")), FName(TEXT("Evidence")),
        FName(TEXT("Shadow")), FName(TEXT("Tenant")), FName(TEXT("VoiceDoor")), FName(TEXT("VeilMark")),
        FName(TEXT("Marina")), FName(TEXT("ArchivePlan")), FName(TEXT("ArchivePhoto")), FName(TEXT("EntranceNineFacade"))})
    {
        TArray<AActor*> Actors;
        UGameplayStatics::GetAllActorsWithTag(World, Tag, Actors);
        TestEqual(*Tag.ToString(), Actors.Num(), 1);
    }
    for (EEntranceStep Ending : {EEntranceStep::Helped, EEntranceStep::Left})
    {
        auto* Save = NewObject<UEntranceSave>();
        Save->Step = Ending;
        Save->VoiceCheckpoint = static_cast<uint8>(EVoiceStage::Recorded);
        Save->ArchiveCheckpoint = static_cast<uint8>(EArchiveStage::Private);
        TArray<uint8> Bytes;
        if (!TestTrue(TEXT("Serialize witness checkpoint"), UGameplayStatics::SaveGameToMemory(Save, Bytes))) return false;
        const auto* Restored = Cast<UEntranceSave>(UGameplayStatics::LoadGameFromMemory(Bytes));
        if (!TestNotNull(TEXT("Restore witness checkpoint"), Restored)) return false;
        TestTrue(TEXT("Tenant choice retained"), Restored->Step == Ending);
        TestEqual(TEXT("Archive choice retained"), Restored->ArchiveCheckpoint, Save->ArchiveCheckpoint);
        TestEqual(TEXT("Voice checkpoint retained"), Restored->VoiceCheckpoint, Save->VoiceCheckpoint);
    }
    auto* Vision = Hero->FindComponentByClass<URazlomTimeGlimpseComponent>();
    if (!TestNotNull(TEXT("Time glimpse component"), Vision)) return false;
    const FTransform Before = Hero->GetActorTransform();
    TestFalse(TEXT("Reject short duration"), Vision->StartGlimpse(TEXT("Test"), 2.f));
    TestTrue(TEXT("Start four second glimpse"), Vision->StartGlimpse(TEXT("Test"), 4.f));
    TestFalse(TEXT("Reject overlapping glimpse"), Vision->StartGlimpse(TEXT("Overlap"), 3.f));
    Vision->CancelGlimpse();
    TestFalse(TEXT("Cancel clears active state"), Vision->IsGlimpseActive());
    TestTrue(TEXT("Same pawn"), Hero == UGameplayStatics::GetPlayerPawn(World, 0));
    TestTrue(TEXT("Same transform"), Hero->GetActorTransform().Equals(Before));
    return true;
}
#endif
