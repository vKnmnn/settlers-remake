package jsettlers.ai.economy;

import java.util.ArrayList;
import java.util.List;

import jsettlers.common.buildings.EBuildingType;

import static jsettlers.common.buildings.EBuildingType.*;

/** Orders the configured Amazon mana economy with suppliers before their consumers. */
final class MeadProductionPlan {
    private MeadProductionPlan() {
    }

    static List<EBuildingType> create(int beekeepingCount, int breweryCount) {
        List<EBuildingType> result = new ArrayList<>();
        int plannedBeekeeping = 0;
        for (int brewery = 1; brewery <= breweryCount; brewery++) {
            // Round each cumulative share up, supplying the first brewery as well as the last.
            int suppliedBeekeeping = (int) (((long) beekeepingCount * brewery + breweryCount - 1) / breweryCount);
            while (plannedBeekeeping < suppliedBeekeeping) {
                result.add(BEEKEEPING);
                plannedBeekeeping++;
            }
            result.add(WATERWORKS);
            result.add(MEAD_BREWERY);
            result.add(TEMPLE);
        }
        return result;
    }
}
