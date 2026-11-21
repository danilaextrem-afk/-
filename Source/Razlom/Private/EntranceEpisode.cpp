#include "EntranceEpisode.h"
#include "RazlomTimeGlimpseComponent.h"
#include "Camera/CameraComponent.h"
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
    if (UGameplayStatics::DoesSaveGameExist(Slot, 0))
    {
        const auto* Data = Cast<UEntranceSave>(UGameplayStatics::LoadGameFromSlot(Slot, 0));
        if (Data && Data->Version == 1 && Data->Step <= EEntranceStep::Left)
            Step = Data->Step;
        else
        {
            bSaveBlocked = true;
            Message = TEXT("Сохранение не удалось прочитать. Исходный слот сохранён.");
        }
    }
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

bool AEntranceCharacter::Facing(FName Tag, float Range) const
{
    FHitResult Hit;
    const FVector Start = Camera->GetComponentLocation();
    const FVector End = Start + Camera->GetForwardVector() * (Range + 350.f);
    FCollisionQueryParams Query(SCENE_QUERY_STAT(EntranceFocus), false, this);
    return GetWorld()->LineTraceSingleByChannel(Hit, Start, End, ECC_Visibility, Query)
        && Hit.GetActor() && Hit.GetActor()->ActorHasTag(Tag)
        && FVector::DistSquared(GetActorLocation(), Hit.ImpactPoint) <= FMath::Square(Range);
}

bool AEntranceCharacter::Commit(EEntranceStep Next)
{
    if (bSaveBlocked) return false;
    auto* Data = CastChecked<UEntranceSave>(UGameplayStatics::CreateSaveGameObject(UEntranceSave::StaticClass()));
    Data->Step = Next;
    if (!UGameplayStatics::SaveGameToSlot(Data, Slot, 0))
    {
        Message = TEXT("Не удалось записать контрольную точку. Повторите действие.");
        return false;
    }
    Step = Next;
    UpdateScene();
    return true;
}

void AEntranceCharacter::UpdateScene()
{
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
    if (Step == EEntranceStep::Neighbor && Facing(TEXT("Neighbor"), 240) && Commit(EEntranceStep::Evidence))
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
    case EEntranceStep::Neighbor: return TEXT("Подойдите к соседке у входа и поговорите [E]");
    case EEntranceStep::Evidence: return TEXT("Осмотрите ключ на подоконнике [E]");
    case EEntranceStep::Shadow: return Vision->IsGlimpseActive() ? TEXT("Русь, 1348") : TEXT("Подпустите тень и отбейтесь [ЛКМ]");
    case EEntranceStep::Tenant: return TEXT("Жилец за дверью. Помочь [E] или уйти [Q]");
    case EEntranceStep::Helped: return TEXT("Жилец спасён. Его показания сохранены.");
    default: return TEXT("Жилец остался за дверью. Выбор сохранён.");
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
