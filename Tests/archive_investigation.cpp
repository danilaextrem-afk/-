#include "../Source/Razlom/Public/ArchiveInvestigation.h"
#include <cassert>
#include <iostream>

int main()
{
    using namespace ArchiveInvestigation;
    using S = EArchiveStage;
    using A = EArchiveAction;
    for (bool helped : {false, true})
    {
        for (bool share : {false, true})
        {
            S stage = Advance(S::Meet, A::Talk, helped);
            assert(stage == S::Plan);
            assert(Advance(stage, A::Share, helped) == stage);
            stage = Advance(stage, A::ReadPlan, helped);
            if (!helped)
            {
                assert(stage == S::Photo);
                assert(Advance(stage, A::Withhold, helped) == stage);
                stage = Advance(stage, A::InspectPhoto, helped);
            }
            assert(stage == S::Decide);
            stage = Advance(stage, share ? A::Share : A::Withhold, helped);
            assert(stage == (share ? S::Shared : S::Private));
            for (auto action : {A::Talk, A::ReadPlan, A::InspectPhoto, A::Share, A::Withhold})
                assert(Advance(stage, action, helped) == stage);
        }
    }
    int checked = 0;
    for (int version = 0; version <= 4; ++version)
        for (int ending = 0; ending <= 2; ++ending)
            for (unsigned voice = 0; voice <= 4; ++voice)
                for (unsigned archive = 0; archive <= 6; ++archive)
                {
                    bool expected = version >= 1 && version <= 3;
                    if (version == 1 || ending == 0) expected &= voice == 0;
                    expected &= voice < 4 && archive < 6;
                    if (version < 3 || voice != 3) expected &= archive == 0;
                    if (ending == 2 && archive == 2) expected = false;
                    assert(ValidCheckpoint(version, ending != 0, ending == 2, voice, archive) == expected);
                    ++checked;
                }
    std::cout << "4 archive routes and " << checked << " checkpoint tuples passed\n";
}
