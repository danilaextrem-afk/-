#include "EntranceEpisode.h"

void AEntranceCharacter::InvestigateArchive(bool Withhold)
{
    const AActor* Target = FocusedActor(220.f);
    if (!Target) return;
    EArchiveAction Action;
    if (Withhold)
    {
        if (!Target->ActorHasTag(TEXT("Marina")) || ArchiveStage != EArchiveStage::Decide) return;
        Action = EArchiveAction::Withhold;
    }
    else if (Target->ActorHasTag(TEXT("Marina")))
        Action = ArchiveStage == EArchiveStage::Decide ? EArchiveAction::Share : EArchiveAction::Talk;
    else if (Target->ActorHasTag(TEXT("ArchivePlan"))) Action = EArchiveAction::ReadPlan;
    else if (Target->ActorHasTag(TEXT("ArchivePhoto"))) Action = EArchiveAction::InspectPhoto;
    else return;

    const bool Helped = Step == EEntranceStep::Helped;
    const EArchiveStage Next = ArchiveInvestigation::Advance(ArchiveStage, Action, Helped);
    if (Next == ArchiveStage || !WriteCheckpoint(Step, VoiceStage, Next)) return;
    switch (Next)
    {
    case EArchiveStage::Plan:
        Message = Helped ? TEXT("Марина: «Саша мне позвонил. На подлиннике плана уцелела подпись. Посмотри».")
            : TEXT("Марина: «За этот лист я отвечаю перед архивом. Дам копию, но легенда на ней обрезана».");
        break;
    case EArchiveStage::Photo:
        Message = TEXT("На плане тот же знак, но названия нет. На стенде висит старый снимок этого двора.");
        break;
    case EArchiveStage::Decide:
        Message = Helped ? TEXT("Подпись на плане: «Подклет часовни». Контур совпадает с нынешней котельной.")
            : TEXT("На обороте фото: «Котельная на фундаменте часовни». Два окна совпадают с планом.");
        break;
    case EArchiveStage::Shared:
        Message = TEXT("Марина получила снимок знака. Адрес подвала и имя источника внесены в дело.");
        break;
    case EArchiveStage::Private:
        Message = TEXT("Кирилл записал адрес подвала. Копия снимка осталась только у него.");
        break;
    default: break;
    }
}

FString AEntranceCharacter::ArchiveObjective() const
{
    switch (ArchiveStage)
    {
    case EArchiveStage::Meet: return TEXT("Журналистка • Выйдите во двор и поговорите с Мариной у лавки [E]");
    case EArchiveStage::Plan: return TEXT("Сверьте знак с архивным планом на столе [E]");
    case EArchiveStage::Photo: return TEXT("Проверьте фотографию котельной на стенде [E]");
    case EArchiveStage::Decide: return TEXT("У Марины: передать снимок [E] или сохранить источник в тайне [Q]");
    case EArchiveStage::Shared: return TEXT("Адрес найден • Снимок передан Марине.");
    default: return TEXT("Адрес найден • Снимок остался у Кирилла.");
    }
}
