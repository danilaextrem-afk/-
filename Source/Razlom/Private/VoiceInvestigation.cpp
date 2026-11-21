#include "EntranceEpisode.h"
#include "Components/AudioComponent.h"
#include "Components/SpotLightComponent.h"
#include "Sound/SoundWaveProcedural.h"
#include "TimerManager.h"

bool AEntranceCharacter::EntranceResolved() const
{
    return Step == EEntranceStep::Helped || Step == EEntranceStep::Left;
}

void AEntranceCharacter::ToggleLight()
{
    PhoneLight->SetVisibility(!PhoneLight->IsVisible());
    UpdateScene();
}

void AEntranceCharacter::PlayDoorKnock()
{
    if (!VoiceDoor) return;
    Knock->Stop();
    constexpr int32 Rate = 22050;
    constexpr float Duration = .84f;
    TArray<int16> Samples;
    Samples.SetNumZeroed(FMath::CeilToInt(Rate * Duration));
    // Three dull, uneven impacts behind a closed door.
    for (float Start : {0.f, .22f, .51f})
    {
        for (int32 I = 0; I < Rate / 4; ++I)
        {
            const float T = float(I) / Rate;
            const float Body = FMath::Sin(2.f * PI * 126.f * T)
                + .34f * FMath::Sin(2.f * PI * 341.f * T);
            Samples[int32(Start * Rate) + I] += int16(12500.f * FMath::Exp(-25.f * T) * Body);
        }
    }
    auto* Wave = NewObject<USoundWaveProcedural>(Knock);
    Wave->SetSampleRate(Rate);
    Wave->NumChannels = 1;
    Wave->Duration = Duration;
    Wave->QueueAudio(reinterpret_cast<const uint8*>(Samples.GetData()), Samples.Num() * sizeof(int16));
    Knock->SetSound(Wave);
    Knock->SetWorldLocation(VoiceDoor->GetActorLocation() + FVector(-8, 0, 43));
    Knock->Play();
    GetWorldTimerManager().SetTimer(KnockEnd, this, &AEntranceCharacter::StopDoorKnock, Duration + .2f);
}

void AEntranceCharacter::StopDoorKnock()
{
    Knock->Stop();
    Knock->SetSound(nullptr);
}

void AEntranceCharacter::InvestigateVoice()
{
    const bool Helped = Step == EEntranceStep::Helped;
    const AActor* Target = FocusedActor(200.f);
    if (!Target) return;
    EVoiceFocus Focus = EVoiceFocus::Other;
    if (Target->ActorHasTag(TEXT("VoiceDoor"))) Focus = EVoiceFocus::Door;
    else if (Target->ActorHasTag(TEXT("Tenant"))) Focus = EVoiceFocus::Tenant;
    else if (Target->ActorHasTag(TEXT("Neighbor"))) Focus = EVoiceFocus::Neighbor;
    else if (Target->ActorHasTag(TEXT("VeilMark"))) Focus = EVoiceFocus::Mark;

    const EVoiceStage Next = VoiceInvestigation::Inspect(VoiceStage, Helped, Focus, PhoneLight->IsVisible());
    if (Focus == EVoiceFocus::Door && VoiceStage != EVoiceStage::Recorded)
        PlayDoorKnock();
    if (Next == VoiceStage || !WriteCheckpoint(Step, Next)) return;
    switch (Next)
    {
    case EVoiceStage::Identify:
        Message = TEXT("Из-за двери голосом соседки: «Открой, Кирилл». Три удара. На площадке соседка молчит.");
        break;
    case EVoiceStage::Reveal:
        Message = Helped ? TEXT("Жилец: «Он повторяет последнее, что слышал. Посвети справа от косяка».")
            : TEXT("Соседка: «Я твоего имени не знала. Когда стучит, у косяка осыпается краска».");
        break;
    case EVoiceStage::Recorded:
        Message = TEXT("Кирилл заносит в телефон рисунок из-под штукатурки и время стука.");
        break;
    default: break;
    }
}
