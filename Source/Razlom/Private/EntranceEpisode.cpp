#include "EntranceEpisode.h"
#include "RazlomTimeGlimpseComponent.h"
#include "Camera/CameraComponent.h"
#include "Components/AudioComponent.h"
#include "Components/SpotLightComponent.h"
#include "Components/CapsuleComponent.h"
#include "Components/InputComponent.h"
#include "Components/StaticMeshComponent.h"
#include "Engine/Canvas.h"
#include "Engine/StaticMesh.h"
#include "Engine/World.h"
#include "GameFramework/CharacterMovementComponent.h"
#include "GameFramework/SpringArmComponent.h"
#include "Kismet/GameplayStatics.h"
#include "UObject/ConstructorHelpers.h"

namespace
{
const FString Slot(TEXT("Razlom_A01M05_v1"));
void ShowActors(UWorld* World, FName Tag, bool Visible)
{
    TArray<AActor*> Actors;
    UGameplayStatics::GetAllActorsWithTag(World, Tag, Actors);
    for (AActor* Actor : Actors)
    {
        Actor->SetActorHiddenInGame(!Visible);
    }
}
}

AEntranceGameMode::AEntranceGameMode()
{
    static ConstructorHelpers::FObjectFinder<UStaticMesh> CubeAsset(TEXT("/Engine/BasicShapes/Cube.Cube"));
    static ConstructorHelpers::FObjectFinder<UStaticMesh> CylinderAsset(TEXT("/Engine/BasicShapes/Cylinder.Cylinder"));
    static ConstructorHelpers::FObjectFinder<UStaticMesh> SphereAsset(TEXT("/Engine/BasicShapes/Sphere.Sphere"));
    Block = CubeAsset.Object;
    Cylinder = CylinderAsset.Object;
    Sphere = SphereAsset.Object;
    DefaultPawnClass = AEntranceCharacter::StaticClass();
    HUDClass = AEntranceHUD::StaticClass();
}

AEntranceCharacter::AEntranceCharacter()
{
    PrimaryActorTick.bCanEverTick = true;
    GetCapsuleComponent()->InitCapsuleSize(34.f, 90.f);
    bUseControllerRotationYaw = false;
    GetCharacterMovement()->bOrientRotationToMovement = true;
    GetCharacterMovement()->MaxWalkSpeed = 340.f;
    auto* Arm = CreateDefaultSubobject<USpringArmComponent>(TEXT("CameraArm"));
    Arm->SetupAttachment(RootComponent);
    Arm->SetRelativeLocation(FVector(0, 0, 55));
    Arm->TargetArmLength = 220.f;
    Arm->bUsePawnControlRotation = true;
    Camera = CreateDefaultSubobject<UCameraComponent>(TEXT("Camera"));
    Camera->SetupAttachment(Arm, USpringArmComponent::SocketName);
    PhoneLight = CreateDefaultSubobject<USpotLightComponent>(TEXT("PhoneLight"));
    PhoneLight->SetupAttachment(Camera);
    PhoneLight->SetRelativeLocation(FVector(15, 8, 0));
    PhoneLight->SetIntensity(5500.f);
    PhoneLight->SetOuterConeAngle(24.f);
    PhoneLight->SetAttenuationRadius(750.f);
    PhoneLight->SetVisibility(false);
    Knock = CreateDefaultSubobject<UAudioComponent>(TEXT("DoorKnock"));
    Knock->SetupAttachment(RootComponent);
    Knock->SetAbsolute(true, false, false);
    Knock->bAutoActivate = false;
    Knock->bOverrideAttenuation = true;
    Knock->AttenuationOverrides.bSpatialize = true;
    Knock->AttenuationOverrides.AttenuationShapeExtents.X = 80.f;
    Knock->AttenuationOverrides.FalloffDistance = 1200.f;
    Vision = CreateDefaultSubobject<URazlomTimeGlimpseComponent>(TEXT("TimeGlimpse"));
    auto* Body = CreateDefaultSubobject<UStaticMeshComponent>(TEXT("BlockoutBody"));
    Body->SetupAttachment(RootComponent);
    static ConstructorHelpers::FObjectFinder<UStaticMesh> Shape(TEXT("/Engine/BasicShapes/Cylinder.Cylinder"));
    Body->SetStaticMesh(Shape.Object);
    Body->SetRelativeScale3D(FVector(.48, .48, 1.65));
    Body->SetCollisionEnabled(ECollisionEnabled::NoCollision);
}

void AEntranceCharacter::BeginPlay()
{
    Super::BeginPlay();
    TArray<AActor*> Found;
    UGameplayStatics::GetAllActorsWithTag(this, TEXT("Shadow"), Found);
    if (Found.Num() == 1)
    {
        Shadow = Found[0];
        ShadowOrigin = Shadow->GetActorLocation();
    }
    Found.Reset();
    UGameplayStatics::GetAllActorsWithTag(this, TEXT("VoiceDoor"), Found);
    if (Found.Num() == 1) VoiceDoor = Found[0];
    if (UGameplayStatics::DoesSaveGameExist(Slot, 0))
    {
        const auto* Data = Cast<UEntranceSave>(UGameplayStatics::LoadGameFromSlot(Slot, 0));
        if (Data && Data->Step <= EEntranceStep::Ilya &&
            ArchiveInvestigation::ValidCheckpoint(Data->Version,
                Data->Step == EEntranceStep::Helped || Data->Step == EEntranceStep::Left,
                Data->Step == EEntranceStep::Helped, Data->VoiceCheckpoint, Data->ArchiveCheckpoint))
        {
            ArchiveStage = static_cast<EArchiveStage>(Data->ArchiveCheckpoint);
            Step = Data->Step;
            VoiceStage = static_cast<EVoiceStage>(Data->VoiceCheckpoint);
        }
        else
        {
            bSaveBlocked = true;
            Message = TEXT("Сохранение не удалось прочитать. Исходный слот сохранён.");
        }
    }
    if (Step == EEntranceStep::Phone || Step == EEntranceStep::Sender || Step == EEntranceStep::Ilya)
        SetActorLocation(FVector(-1180, -75, 94), false, nullptr, ETeleportType::TeleportPhysics);
    Vision->OnGlimpseEnded.AddDynamic(this, &AEntranceCharacter::EndVision);
    UpdateScene();
}

void AEntranceCharacter::SetupPlayerInputComponent(UInputComponent* Input)
{
    Super::SetupPlayerInputComponent(Input);
    Input->BindAxis("Forward", this, &AEntranceCharacter::Forward);
    Input->BindAxis("Right", this, &AEntranceCharacter::Right);
    Input->BindAxis("Turn", this, &APawn::AddControllerYawInput);
    Input->BindAxis("Look", this, &APawn::AddControllerPitchInput);
    Input->BindAction("Light", IE_Pressed, this, &AEntranceCharacter::ToggleLight);
    Input->BindAction("Use", IE_Pressed, this, &AEntranceCharacter::Interact);
    Input->BindAction("Leave", IE_Pressed, this, &AEntranceCharacter::Leave);
    Input->BindAction("Strike", IE_Pressed, this, &AEntranceCharacter::Strike);
}

void AEntranceCharacter::Forward(float Value)
{
    AddMovementInput(FRotator(0, GetControlRotation().Yaw, 0).Vector(), Value);
}
void AEntranceCharacter::Right(float Value)
{
    AddMovementInput(FRotationMatrix(FRotator(0, GetControlRotation().Yaw, 0)).GetUnitAxis(EAxis::Y), Value);
}

AActor* AEntranceCharacter::FocusedActor(float Range) const
{
    FHitResult Hit;
    const FVector Start = Camera->GetComponentLocation();
    const FVector End = Start + Camera->GetForwardVector() * (Range + 350.f);
    FCollisionQueryParams Query(SCENE_QUERY_STAT(EntranceFocus), false, this);
    return GetWorld()->LineTraceSingleByChannel(Hit, Start, End, ECC_Visibility, Query)
        && Hit.GetActor()
        && FVector::DistSquared(GetActorLocation(), Hit.ImpactPoint) <= FMath::Square(Range) ? Hit.GetActor() : nullptr;
}

bool AEntranceCharacter::Facing(FName Tag, float Range) const
{
    const AActor* Target = FocusedActor(Range);
    return Target && Target->ActorHasTag(Tag);
}

bool AEntranceCharacter::Commit(EEntranceStep Next)
{
    return WriteCheckpoint(Next, VoiceStage, ArchiveStage);
}

bool AEntranceCharacter::WriteCheckpoint(EEntranceStep Next, EVoiceStage NextVoice, EArchiveStage NextArchive)
{
    if (bSaveBlocked) return false;
    auto* Data = CastChecked<UEntranceSave>(UGameplayStatics::CreateSaveGameObject(UEntranceSave::StaticClass()));
    Data->ArchiveCheckpoint = static_cast<uint8>(NextArchive);
    Data->Step = Next;
    Data->VoiceCheckpoint = static_cast<uint8>(NextVoice);
    if (!UGameplayStatics::SaveGameToSlot(Data, Slot, 0))
    {
        Message = TEXT("Не удалось записать контрольную точку. Повторите действие.");
        return false;
    }
    ArchiveStage = NextArchive;
    Step = Next;
    VoiceStage = NextVoice;
    UpdateScene();
    return true;
}

void AEntranceCharacter::UpdateScene()
{
    const bool MarkVisible = EntranceResolved() && PhoneLight->IsVisible()
        && VoiceStage >= EVoiceStage::Reveal;
    ShowActors(GetWorld(), TEXT("VeilStroke"), MarkVisible);
    TArray<AActor*> Marks;
    UGameplayStatics::GetAllActorsWithTag(this, TEXT("VeilMark"), Marks);
    for (AActor* Mark : Marks)
    {
        Mark->SetActorHiddenInGame(!MarkVisible);
        Mark->SetActorEnableCollision(MarkVisible);
    }
    ShowActors(GetWorld(), TEXT("Epoch1348"), Vision->IsGlimpseActive());
    ShowActors(GetWorld(), TEXT("Tenant"), Step == EEntranceStep::Tenant || Step == EEntranceStep::Helped);
    if (Shadow)
    {
        const bool Active = Step == EEntranceStep::Shadow && !Vision->IsGlimpseActive();
        Shadow->SetActorHiddenInGame(!Active);
        Shadow->SetActorEnableCollision(Active);
    }
    TArray<AActor*> Tenants;
    UGameplayStatics::GetAllActorsWithTag(this, TEXT("Tenant"), Tenants);
    for (AActor* Actor : Tenants)
        Actor->SetActorEnableCollision(Step == EEntranceStep::Tenant || Step == EEntranceStep::Helped);
}

void AEntranceCharacter::Interact()
{
    if (EntranceResolved())
    {
        if (VoiceStage == EVoiceStage::Recorded) InvestigateArchive(false);
        else InvestigateVoice();
        return;
    }
    if (Step == EEntranceStep::Phone && Facing(TEXT("Phone"), 210) && Commit(EEntranceStep::Sender))
        Message = TEXT("На записи горят крыши. Человек в кольчуге оборачивается. У него лицо Кирилла.");
    else if (Step == EEntranceStep::Sender && Facing(TEXT("Phone"), 210) && Commit(EEntranceStep::Ilya))
        Message = TEXT("Отправитель — Илья. Файл переслан в 02:17, в поле «Камера» прочерк.");
    else if (Step == EEntranceStep::Ilya && Facing(TEXT("Ilya"), 240) && Commit(EEntranceStep::Neighbor))
        Message = TEXT("Илья: «Я в это время спал. У девятого подъезда женщина ждёт. Сама увидела тебя на записи».");
    else if (Step == EEntranceStep::Neighbor && Facing(TEXT("Neighbor"), 240) && Commit(EEntranceStep::Evidence))
        Message = TEXT("Соседка: «Он оставил ключ на подоконнике. Я слышала, как он стучал из пустой квартиры».");
    else if (Step == EEntranceStep::Evidence && Facing(TEXT("Evidence"), 190) && Commit(EEntranceStep::Shadow))
    {
        Message = TEXT("На ключе свежая глина. За окном на миг вырастает частокол.");
        Vision->StartGlimpse(TEXT("A01M05_Palisade"), 4.f);
        UpdateScene();
    }
    else if (Step == EEntranceStep::Tenant && Facing(TEXT("Tenant"), 240) && Commit(EEntranceStep::Helped))
        Message = TEXT("Кирилл выводит жильца на площадку. Теперь у него есть свидетель.");
}

void AEntranceCharacter::EndVision(FName Id)
{
    UpdateScene();
}

void AEntranceCharacter::Leave()
{
    if (VoiceStage == EVoiceStage::Recorded) { InvestigateArchive(true); return; }
    if (Step == EEntranceStep::Tenant && Facing(TEXT("Tenant"), 240) && Commit(EEntranceStep::Left))
        Message = TEXT("Кирилл отступает от двери. Стук прекращается.");
}

void AEntranceCharacter::Strike()
{
    if (Step == EEntranceStep::Shadow && !Vision->IsGlimpseActive() && Facing(TEXT("Shadow"), 200)
        && Commit(EEntranceStep::Tenant))
        Message = TEXT("Тень отступила. За ней в дверном проёме виден жилец.");
}

void AEntranceCharacter::Tick(float DeltaSeconds)
{
    Super::Tick(DeltaSeconds);
    if (!Shadow || Step != EEntranceStep::Shadow || Vision->IsGlimpseActive()) return;
    const FVector Here = Shadow->GetActorLocation();
    FVector ToPlayer = GetActorLocation() - Here;
    ToPlayer.Z = 0;
    if (ToPlayer.Size() < 80.f)
    {
        // Repel the player in the encounter and reset the shade's approach.
        LaunchCharacter(ToPlayer.GetSafeNormal() * 270.f + FVector(0,0,110), true, true);
        Shadow->SetActorLocation(ShadowOrigin);
    }
    else if (ToPlayer.Size() < 850.f)
        Shadow->AddActorWorldOffset(ToPlayer.GetSafeNormal() * FMath::Min(105.f * DeltaSeconds, ToPlayer.Size()), true);
}

FString AEntranceCharacter::Objective() const
{
    switch (Step)
    {
    case EEntranceStep::Phone: return TEXT("Сообщение с героем · Прочитайте сообщение на телефоне у верстака [E]");
    case EEntranceStep::Sender: return TEXT("Откройте сведения о записи на телефоне [E]");
    case EEntranceStep::Ilya: return TEXT("Расспросите Илью у выхода из мастерской [E]");
    case EEntranceStep::Neighbor: return TEXT("Подойдите к соседке у входа и поговорите [E]");
    case EEntranceStep::Evidence: return TEXT("Осмотрите ключ на подоконнике [E]");
    case EEntranceStep::Shadow: return Vision->IsGlimpseActive() ? TEXT("Русь, 1348") : TEXT("Подпустите тень и отбейтесь [ЛКМ]");
    case EEntranceStep::Tenant: return TEXT("Жилец за дверью. Помочь [E] или уйти [Q]");
    default: break;
    }
    switch (VoiceStage)
    {
    case EVoiceStage::Listen: return TEXT("Чужой голос • Прислушайтесь у дальней двери [E]");
    case EVoiceStage::Identify: return Step == EEntranceStep::Helped
        ? TEXT("Спросите спасённого жильца, кто говорил [E]")
        : TEXT("Вернитесь к соседке и проверьте услышанное [E]");
    case EVoiceStage::Reveal: return TEXT("Включите фонарь [F]. Осмотрите скол справа от дальней двери [E]");
    default: return ArchiveObjective();
    }
}

void AEntranceHUD::DrawHUD()
{
    Super::DrawHUD();
    const auto* Hero = Cast<AEntranceCharacter>(GetOwningPawn());
    if (!Hero || !Canvas) return;
    DrawText(Hero->Objective(), FLinearColor::White, 32, 30);
    DrawText(Hero->Message, FLinearColor::White, 32, 60);
    DrawRect(FLinearColor::White, Canvas->ClipX * .5f - 2, Canvas->ClipY * .5f - 2, 4, 4);
}
