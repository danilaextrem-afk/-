// Compiles the investigation rules shared with AEntranceCharacter, without an engine.
#include "../Source/Razlom/Public/VoiceInvestigation.h"
#include <cassert>
#include <iostream>

int main()
{
    using Stage = EVoiceStage;
    using Focus = EVoiceFocus;
    using namespace VoiceInvestigation;
    int checks = 0;
    for (bool helped : {false, true})
    {
        for (auto stage : {Stage::Listen, Stage::Identify, Stage::Reveal, Stage::Recorded})
        {
            for (auto focus : {Focus::Other, Focus::Door, Focus::Tenant, Focus::Neighbor, Focus::Mark})
            {
                for (bool lit : {false, true})
                {
                    Stage expected = stage;
                    if (stage == Stage::Listen && focus == Focus::Door) expected = Stage::Identify;
                    if (stage == Stage::Identify && focus == (helped ? Focus::Tenant : Focus::Neighbor))
                        expected = Stage::Reveal;
                    if (stage == Stage::Reveal && lit && focus == Focus::Mark) expected = Stage::Recorded;
                    assert(Inspect(stage, helped, focus, lit) == expected);
                    ++checks;
                }
            }
        }
    }
    for (bool resolved : {false, true})
    {
        assert(ValidCheckpoint(1, resolved, 0));
        assert(!ValidCheckpoint(1, resolved, 1));
        assert(ValidCheckpoint(2, resolved, 0));
        for (unsigned stage = 1; stage <= 3; ++stage)
            assert(ValidCheckpoint(2, resolved, stage) == resolved);
        assert(!ValidCheckpoint(2, resolved, 4));
        assert(!ValidCheckpoint(3, resolved, 0));
    }
    std::cout << checks << " transition combinations and checkpoint validation passed\n";
}
