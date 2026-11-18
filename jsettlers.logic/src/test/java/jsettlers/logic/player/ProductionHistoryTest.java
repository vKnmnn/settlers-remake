package jsettlers.logic.player;

import java.lang.reflect.Field;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.player.ProductionHistory;
import jsettlers.testutils.TestUtils;
import org.junit.Test;
import static org.junit.Assert.*;

public class ProductionHistoryTest {
    private static final int STEP = ProductionStatistic.BUCKET_MILLIS;

    @Test
    public void assignsBoundaryEventsAndIdleIntervals() {
        ProductionStatistic statistic = new ProductionStatistic();
        statistic.materialProduced(EMaterialType.PLANK, STEP - 1);
        statistic.materialProduced(EMaterialType.PLANK, STEP);
        statistic.materialProduced(EMaterialType.GOLD, STEP + 1);
        assertArrayEquals(new int[] {1, 1, 0}, tail(statistic.historyAt(EMaterialType.PLANK, 1, 2 * STEP).getAmounts(), 3));
        assertArrayEquals(new int[] {0, 1, 0}, tail(statistic.historyAt(EMaterialType.GOLD, 1, 2 * STEP).getAmounts(), 3));
        assertEquals(2, statistic.getAmountProduced(EMaterialType.PLANK));
    }

    @Test
    public void wraparoundDoesNotResurrectOldCountsAfterLongIdlePeriod() {
        ProductionStatistic statistic = new ProductionStatistic();
        statistic.materialProduced(EMaterialType.GOLD, 0);
        statistic.materialProduced(EMaterialType.PLANK, 256 * STEP);
        assertArrayEquals(new int[256], statistic.historyAt(EMaterialType.GOLD, 60, 256 * STEP).getAmounts());
        assertArrayEquals(new int[256], statistic.historyAt(EMaterialType.PLANK, 60, 512 * STEP).getAmounts());
        assertEquals(1, statistic.getAmountProduced(EMaterialType.GOLD));
    }

    @Test
    public void snapshotCannotModifyHistoryOrAdvanceRecording() {
        ProductionStatistic statistic = new ProductionStatistic();
        statistic.materialProduced(EMaterialType.GOLD, 0);
        ProductionHistory snapshot = statistic.historyAt(EMaterialType.GOLD, 1, 0);
        snapshot.getAmounts()[0] = 100;
        statistic.historyAt(EMaterialType.GOLD, 60, 999_999);
        assertArrayEquals(new int[] {1}, tail(statistic.historyAt(EMaterialType.GOLD, 1, 0).getAmounts(), 1));
        assertEquals(STEP, snapshot.getEndTimeMillis());
        assertEquals(0, snapshot.getAvailableSinceMillis());
    }

    @Test
    public void saveLoadPreservesBucketsAndContinuesCurrentInterval() throws Exception {
        ProductionStatistic statistic = new ProductionStatistic();
        statistic.materialProduced(EMaterialType.GOLD, STEP);
        ProductionStatistic restored = TestUtils.serializeAndDeserialize(statistic);
        restored.materialProduced(EMaterialType.GOLD, STEP + 5_000);
        assertArrayEquals(new int[] {0, 2}, tail(restored.historyAt(EMaterialType.GOLD, 1, STEP + 5_000).getAmounts(), 2));
        assertEquals(2, restored.getAmountProduced(EMaterialType.GOLD));
        assertEquals(1, statistic.getAmountProduced(EMaterialType.GOLD));
    }

    @Test
    public void legacySaveKeepsTotalsWithoutInventingHistory() throws Exception {
        ProductionStatistic statistic = new ProductionStatistic();
        statistic.materialProduced(EMaterialType.GOLD, 3 * STEP);
        for (String name : new String[] {"history", "bucketIds"}) {
            Field field = ProductionStatistic.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(statistic, null); // missing fields deserialize as null in old saves
        }
        ProductionStatistic restored = TestUtils.serializeAndDeserialize(statistic);
        assertEquals(1, restored.getAmountProduced(EMaterialType.GOLD));
        assertArrayEquals(new int[5], restored.historyAt(EMaterialType.GOLD, 1, 3 * STEP).getAmounts());
        assertEquals(3 * STEP, restored.historyAt(EMaterialType.GOLD, 4, 3 * STEP).getAvailableSinceMillis());
    }

    @Test
    public void originalKernelPreservesConstantRateAndIgnoresUnknownPast() {
        int[] values = new int[20];
        java.util.Arrays.fill(values, 10);
        ProductionHistory history = new ProductionHistory(values, STEP, 20 * STEP, 0);
        for (double value : history.getSmoothedAmounts()) assertEquals(10, value, 0.0001);
        ProductionHistory partial = new ProductionHistory(new int[] {0, 0, 10}, STEP, 3 * STEP, 2 * STEP);
        assertEquals(10, partial.getSmoothedAmounts()[2], 0.0001);
        history.getSmoothedAmounts()[0] = 99;
        assertEquals(10, history.getSmoothedAmounts()[0], 0.0001);
    }

    @Test
    public void changingWindowDoesNotChangeSmoothingAndHighProductionIsNotClipped() {
        ProductionStatistic statistic = new ProductionStatistic();
        for (int i = 0; i < 150; i++) statistic.materialProduced(EMaterialType.GOLD, 20 * STEP);
        ProductionHistory shortView = statistic.historyAt(EMaterialType.GOLD, 15, 21 * STEP);
        ProductionHistory fullView = statistic.historyAt(EMaterialType.GOLD, 60, 21 * STEP);
        assertEquals(150, shortView.getAmounts()[shortView.getAmounts().length - 2]);
        assertEquals(fullView.getSmoothedAmounts()[255],
                shortView.getSmoothedAmounts()[shortView.getAmounts().length - 1], 0.0001);
    }

    private int[] tail(int[] values, int length) {
        return java.util.Arrays.copyOfRange(values, values.length - length, values.length);
    }
}
