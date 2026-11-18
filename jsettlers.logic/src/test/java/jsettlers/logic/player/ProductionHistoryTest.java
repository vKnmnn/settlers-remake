package jsettlers.logic.player;

import java.lang.reflect.Field;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.player.ProductionHistory;
import jsettlers.testutils.TestUtils;
import org.junit.Test;
import static org.junit.Assert.*;

public class ProductionHistoryTest {
    @Test
    public void assignsBoundaryEventsAndIdleIntervals() {
        ProductionStatistic statistic = new ProductionStatistic();
        statistic.materialProduced(EMaterialType.PLANK, 59_999);
        statistic.materialProduced(EMaterialType.PLANK, 60_000);
        statistic.materialProduced(EMaterialType.GOLD, 60_001);
        assertArrayEquals(new int[] {1, 1, 0}, statistic.historyAt(EMaterialType.PLANK, 3, 120_000).getAmounts());
        assertArrayEquals(new int[] {0, 1, 0}, statistic.historyAt(EMaterialType.GOLD, 3, 120_000).getAmounts());
        assertEquals(2, statistic.getAmountProduced(EMaterialType.PLANK));
    }

    @Test
    public void wraparoundDoesNotResurrectOldCountsAfterLongIdlePeriod() {
        ProductionStatistic statistic = new ProductionStatistic();
        statistic.materialProduced(EMaterialType.GOLD, 0);
        statistic.materialProduced(EMaterialType.PLANK, 60 * 60_000);
        assertArrayEquals(new int[60], statistic.historyAt(EMaterialType.GOLD, 60, 60 * 60_000).getAmounts());
        assertArrayEquals(new int[60], statistic.historyAt(EMaterialType.PLANK, 60, 120 * 60_000).getAmounts());
        assertEquals(1, statistic.getAmountProduced(EMaterialType.GOLD));
    }

    @Test
    public void snapshotCannotModifyHistoryOrAdvanceRecording() {
        ProductionStatistic statistic = new ProductionStatistic();
        statistic.materialProduced(EMaterialType.GOLD, 0);
        ProductionHistory snapshot = statistic.historyAt(EMaterialType.GOLD, 1, 0);
        snapshot.getAmounts()[0] = 100;
        statistic.historyAt(EMaterialType.GOLD, 60, 999_999);
        assertArrayEquals(new int[] {1}, statistic.historyAt(EMaterialType.GOLD, 1, 0).getAmounts());
        assertEquals(60_000, snapshot.getEndTimeMillis());
        assertEquals(0, snapshot.getAvailableSinceMillis());
    }

    @Test
    public void saveLoadPreservesBucketsAndContinuesCurrentInterval() throws Exception {
        ProductionStatistic statistic = new ProductionStatistic();
        statistic.materialProduced(EMaterialType.GOLD, 60_000);
        ProductionStatistic restored = TestUtils.serializeAndDeserialize(statistic);
        restored.materialProduced(EMaterialType.GOLD, 65_000);
        assertArrayEquals(new int[] {0, 2}, restored.historyAt(EMaterialType.GOLD, 2, 65_000).getAmounts());
        assertEquals(2, restored.getAmountProduced(EMaterialType.GOLD));
        assertEquals(1, statistic.getAmountProduced(EMaterialType.GOLD));
    }

    @Test
    public void legacySaveKeepsTotalsWithoutInventingHistory() throws Exception {
        ProductionStatistic statistic = new ProductionStatistic();
        statistic.materialProduced(EMaterialType.GOLD, 180_000);
        for (String name : new String[] {"history", "bucketIds"}) {
            Field field = ProductionStatistic.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(statistic, null); // missing fields deserialize as null in old saves
        }
        ProductionStatistic restored = TestUtils.serializeAndDeserialize(statistic);
        assertEquals(1, restored.getAmountProduced(EMaterialType.GOLD));
        assertArrayEquals(new int[4], restored.historyAt(EMaterialType.GOLD, 4, 180_000).getAmounts());
        assertEquals(180_000, restored.historyAt(EMaterialType.GOLD, 4, 180_000).getAvailableSinceMillis());
    }
}
