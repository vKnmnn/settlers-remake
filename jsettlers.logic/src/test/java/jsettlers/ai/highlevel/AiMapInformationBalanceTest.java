package jsettlers.ai.highlevel;

import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.player.ECivilisation;
import org.junit.Test;

import static org.junit.Assert.*;

public class AiMapInformationBalanceTest {
    @Test
    public void reducingManaForLimitedSpaceDoesNotInventGemDeposits() {
        boolean foundReducedEconomy = false;
        for (int grass = 100; grass <= 25000; grass += 100) {
            int[] counts = AiMapInformation.calculateBuildingCounts(10, 3, 2, 0, 3, 1, 0,
                    grass, 40, ECivilisation.AMAZON);
            assertEquals("No gems exist at grass budget " + grass, 0,
                    counts[EBuildingType.GEMSMINE.ordinal]);
            int breweries = counts[EBuildingType.MEAD_BREWERY.ordinal];
            foundReducedEconomy |= breweries > 0 && breweries < 3;
        }
        assertTrue("Exercise a viable economy with its mana budget reduced", foundReducedEconomy);
    }

    @Test
    public void constrainedManaPlanningPreservesExistingGemBudget() {
        boolean foundReducedEconomy = false;
        for (int grass = 100; grass <= 25000; grass += 100) {
            int[] counts = AiMapInformation.calculateBuildingCounts(10, 3, 2, 4, 3, 1, 0,
                    grass, 40, ECivilisation.AMAZON);
            int breweries = counts[EBuildingType.MEAD_BREWERY.ordinal];
            if (breweries > 0 && breweries < 3) {
                assertEquals(4, counts[EBuildingType.GEMSMINE.ordinal]);
                foundReducedEconomy = true;
            }
        }
        assertTrue("Exercise a viable economy with its mana budget reduced", foundReducedEconomy);
    }
}
