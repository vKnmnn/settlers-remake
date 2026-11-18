package jsettlers.logic.player;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;

import jsettlers.common.ai.EPlayerType;
import jsettlers.common.movable.ESpellType;
import jsettlers.common.player.ECivilisation;
import org.junit.Test;

import static org.junit.Assert.*;

public class EndgameStatisticTest {
    @Test public void scoreCombinesLifetimeProductionAndBattleResults() {
        Player player = player(ECivilisation.ROMAN);
        EndgameStatistic statistic = player.getEndgameStatistic();
        repeat(3, statistic::recordSettlerCreated);
        repeat(4, statistic::recordBuildingMaterialUsed);
        repeat(5, statistic::recordFoodConsumed);
        repeat(6, statistic::recordMinedMaterial);
        repeat(3, statistic::incrementAmountOfProducedGold);
        repeat(2, statistic::incrementAmountOfProducedSoldiers);
        repeat(2, statistic::recordBattleWon);
        player.getMannaInformation().increaseMannaByBigTemple();
        repeat(3, player.getMannaInformation()::increaseManna);

        assertEquals(3, statistic.getSettlersCreated());
        assertEquals(4, statistic.getBuildingMaterialsUsed());
        assertEquals(5, statistic.getFoodConsumed());
        assertEquals(6, statistic.getMinedMaterials());
        assertEquals(3, statistic.getGoldValue());
        assertEquals(11, statistic.getManaProduced());
        assertEquals(2, statistic.getSoldiersRecruited());
        assertEquals(2, statistic.getBattlePoints());
        assertEquals(52, statistic.getScore());
        assertTrue(statistic.isComplete());

        assertTrue(player.getMannaInformation().useSpell(ESpellType.ROMAN_EYE));
        assertEquals(1, player.getMannaInformation().getAmountOfManna());
        assertEquals("Spending mana must not erase production", 11, statistic.getManaProduced());
        assertEquals(52, statistic.getScore());
    }

    @Test public void egyptianGoldContributesHalfWeightWithoutLosingOddScorePoints() {
        EndgameStatistic roman = player(ECivilisation.ROMAN).getEndgameStatistic();
        EndgameStatistic egyptian = player(ECivilisation.EGYPTIAN).getEndgameStatistic();
        repeat(3, roman::incrementAmountOfProducedGold);
        repeat(3, egyptian::incrementAmountOfProducedGold);
        assertEquals(3, roman.getGoldValue());
        assertEquals(1, egyptian.getGoldValue());
        assertEquals(6, roman.getScore());
        assertEquals(3, egyptian.getScore());
        assertEquals(3, egyptian.getAmountOfProducedGold());
    }

    @Test public void extendedCountersSurviveBothSignedAndUnsignedShortOverflowAndReload() throws Exception {
        Player player = player(ECivilisation.ROMAN);
        EndgameStatistic statistic = player.getEndgameStatistic();
        repeat(70000, statistic::incrementAmountOfProducedSoldiers);
        repeat(70000, statistic::incrementAmountOfProducedGold);
        repeat(70000, player.getMannaInformation()::increaseManna);
        assertEquals(70000, statistic.getSoldiersRecruited());
        assertEquals(70000, statistic.getGoldValue());
        assertEquals(70000, statistic.getManaProduced());
        assertEquals(350000, statistic.getScore());

        EndgameStatistic loaded = roundTrip(player).getEndgameStatistic();
        assertEquals(70000, loaded.getSoldiersRecruited());
        assertEquals(70000, loaded.getGoldValue());
        assertEquals(70000, loaded.getManaProduced());
        assertEquals(350000, loaded.getScore());
        assertTrue(loaded.isComplete());
        loaded.incrementAmountOfProducedSoldiers();
        assertEquals(70001, loaded.getSoldiersRecruited());
    }

    @Test public void roundTripRetainsAllNewCategoriesAndEgyptianWeight() throws Exception {
        Player player = player(ECivilisation.EGYPTIAN);
        EndgameStatistic original = player.getEndgameStatistic();
        original.recordSettlerCreated();
        original.recordBuildingMaterialUsed();
        original.recordFoodConsumed();
        original.recordMinedMaterial();
        original.recordBattleWon();
        original.incrementAmountOfProducedSoldiers();
        original.incrementAmountOfProducedGold();
        player.getMannaInformation().increaseMannaByBigTemple();

        EndgameStatistic loaded = roundTrip(player).getEndgameStatistic();
        assertEquals(1, loaded.getSettlersCreated());
        assertEquals(1, loaded.getBuildingMaterialsUsed());
        assertEquals(1, loaded.getFoodConsumed());
        assertEquals(1, loaded.getMinedMaterials());
        assertEquals(1, loaded.getBattlePoints());
        assertEquals(1, loaded.getSoldiersRecruited());
        assertEquals(8, loaded.getManaProduced());
        assertEquals(21, loaded.getScore());
        loaded.incrementAmountOfProducedGold();
        assertEquals(1, loaded.getGoldValue());
        assertEquals(22, loaded.getScore());
    }

    @Test public void legacyFieldsMigrateAsUnsignedButRemainExplicitlyIncomplete() throws Exception {
        Player player = player(ECivilisation.ROMAN);
        EndgameStatistic original = player.getEndgameStatistic();
        // Missing fields in pre-extension saves receive these Java serialization defaults.
        set(original, "extendedCounters", false);
        set(original, "complete", false);
        set(original, "amountOfProducedSoldiers", (short) 40000);
        set(original, "amountOfProducedGold", (short) 50000);
        set(player.getMannaInformation(), "extendedTotal", false);
        set(player.getMannaInformation(), "totalManna", (short) 45000);

        Player loadedPlayer = roundTrip(player);
        EndgameStatistic loaded = loadedPlayer.getEndgameStatistic();
        assertEquals(40000, loaded.getSoldiersRecruited());
        assertEquals(50000, loaded.getGoldValue());
        assertEquals(45000, loaded.getManaProduced());
        assertEquals(225000, loaded.getScore());
        assertEquals(0, loaded.getFoodConsumed());
        assertFalse(loaded.isComplete());
        loaded.incrementAmountOfProducedSoldiers();
        loaded.recordFoodConsumed();
        loadedPlayer.getMannaInformation().increaseManna();
        EndgameStatistic twiceLoaded = roundTrip(loadedPlayer).getEndgameStatistic();
        assertEquals(40001, twiceLoaded.getSoldiersRecruited());
        assertEquals(45001, twiceLoaded.getManaProduced());
        assertEquals(1, twiceLoaded.getFoodConsumed());
        assertFalse(twiceLoaded.isComplete());
    }

    @Test public void mannaMigrationAndTempleLimitPreserveLifetimeTotal() throws Exception {
        MannaInformation original = new MannaInformation(ECivilisation.ASIAN);
        set(original, "extendedTotal", false);
        set(original, "totalManna", (short) 60000);
        MannaInformation loaded = roundTrip(original);
        assertEquals(60000, loaded.getLifetimeMannaProduced());
        loaded.increaseMannaByBigTemple();
        loaded.stopFutureManaIncreasingByBigTemple();
        loaded.increaseMannaByBigTemple();
        loaded.increaseManna();
        assertEquals(60009, loaded.getLifetimeMannaProduced());
        assertEquals(60009, roundTrip(loaded).getLifetimeMannaProduced());
    }

    private static Player player(ECivilisation civilisation) {
        return new Player((byte) 0, new Team((byte) 0), (byte) 1, EPlayerType.HUMAN, civilisation);
    }

    private static void repeat(int count, Runnable operation) {
        for (int i = 0; i < count; i++) operation.run();
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) { output.writeObject(value); }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (T) input.readObject();
        }
    }
}
