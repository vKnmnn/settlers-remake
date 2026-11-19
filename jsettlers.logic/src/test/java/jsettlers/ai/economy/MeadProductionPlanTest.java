package jsettlers.ai.economy;

import java.util.Arrays;
import java.util.List;

import jsettlers.common.buildings.EBuildingType;
import org.junit.Test;

import static jsettlers.common.buildings.EBuildingType.*;
import static org.junit.Assert.*;

public class MeadProductionPlanTest {
    @Test
    public void singleBreweryGetsItsHoneySuppliersFirst() {
        assertEquals(Arrays.asList(BEEKEEPING, BEEKEEPING, WATERWORKS, MEAD_BREWERY, TEMPLE),
                MeadProductionPlan.create(2, 1));
    }

    @Test
    public void suppliersAreDistributedAcrossBreweriesWithoutTriplication() {
        assertEquals(Arrays.asList(
                BEEKEEPING, BEEKEEPING, BEEKEEPING, WATERWORKS, MEAD_BREWERY, TEMPLE,
                BEEKEEPING, BEEKEEPING, WATERWORKS, MEAD_BREWERY, TEMPLE),
                MeadProductionPlan.create(5, 2));
    }

    @Test
    public void unevenRatiosRetainAllConfiguredSuppliers() {
        List<EBuildingType> plan = MeadProductionPlan.create(7, 3);
        assertEquals(7, plan.stream().filter(type -> type == BEEKEEPING).count());
        assertEquals(3, plan.stream().filter(type -> type == MEAD_BREWERY).count());
        assertEquals(3, plan.stream().filter(type -> type == WATERWORKS).count());
        assertEquals(3, plan.stream().filter(type -> type == TEMPLE).count());
        assertEquals(BEEKEEPING, plan.get(0));
        assertEquals(TEMPLE, plan.get(plan.size() - 1));
    }

    @Test
    public void noBreweriesDoNotPlanUnusedSuppliersOrDivideByZero() {
        assertTrue(MeadProductionPlan.create(5, 0).isEmpty());
    }
}
