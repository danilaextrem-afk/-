#pragma once

#include <cstdint>

// Values are serialized. Append future stages without renumbering checkpoints.
enum class EVoiceStage : std::uint8_t { Listen, Identify, Reveal, Recorded };
enum class EVoiceFocus { Other, Door, Tenant, Neighbor, Mark };

namespace VoiceInvestigation
{
inline bool ValidCheckpoint(int Version, bool EntranceResolved, unsigned Stage)
{
    if (Version == 1) return Stage == 0;
    return Version == 2 && Stage <= static_cast<unsigned>(EVoiceStage::Recorded)
        && (EntranceResolved || Stage == 0);
}

inline EVoiceStage Inspect(EVoiceStage Current, bool Helped, EVoiceFocus Focus, bool LightOn)
{
    switch (Current)
    {
    case EVoiceStage::Listen:
        if (Focus == EVoiceFocus::Door) return EVoiceStage::Identify;
        break;
    case EVoiceStage::Identify:
        if (Focus == (Helped ? EVoiceFocus::Tenant : EVoiceFocus::Neighbor))
            return EVoiceStage::Reveal;
        break;
    case EVoiceStage::Reveal:
        if (Focus == EVoiceFocus::Mark && LightOn) return EVoiceStage::Recorded;
        break;
    default: break;
    }
    return Current;
}
}
