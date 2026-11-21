#pragma once

#include "VoiceInvestigation.h"

enum class EArchiveStage : std::uint8_t { Meet, Plan, Photo, Decide, Shared, Private };
enum class EArchiveAction { Talk, ReadPlan, InspectPhoto, Share, Withhold };

namespace ArchiveInvestigation
{
inline bool ValidCheckpoint(int Version, bool Resolved, bool Helped, unsigned Voice, unsigned Archive)
{
    if (Version < 3)
        return Archive == 0 && VoiceInvestigation::ValidCheckpoint(Version, Resolved, Voice);
    return Version == 3 && VoiceInvestigation::ValidCheckpoint(2, Resolved, Voice)
        && Archive <= static_cast<unsigned>(EArchiveStage::Private)
        && (Archive == 0 || Voice == static_cast<unsigned>(EVoiceStage::Recorded))
        && !(Helped && Archive == static_cast<unsigned>(EArchiveStage::Photo));
}

inline EArchiveStage Advance(EArchiveStage Stage, EArchiveAction Action, bool Helped)
{
    using S = EArchiveStage;
    using A = EArchiveAction;
    switch (Stage)
    {
    case S::Meet: if (Action == A::Talk) return S::Plan; break;
    case S::Plan: if (Action == A::ReadPlan) return Helped ? S::Decide : S::Photo; break;
    case S::Photo: if (Action == A::InspectPhoto) return S::Decide; break;
    case S::Decide:
        if (Action == A::Share) return S::Shared;
        if (Action == A::Withhold) return S::Private;
        break;
    default: break;
    }
    return Stage;
}
}
