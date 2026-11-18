package jsettlers.graphics.map.controls.original.panel.content.settlers.production;

import jsettlers.common.player.ProductionHistory;
import org.junit.Test;
import static org.junit.Assert.*;

public class ProductionGraphTest {
    @Test public void emptyHistoryDoesNotInventMeasurements() {
        ProductionGraph.Plot plot = ProductionGraph.Plot.create(null, 15);
        assertFalse(plot.hasHistory);
        assertEquals(1, plot.maximum);
        assertEquals(1f, plot.knownFrom, 0f);
    }
    @Test public void keepsUnknownHistoryBlankAndScalesOnlyVisibleCounts() {
        ProductionHistory history = new ProductionHistory(new int[] {999, 4, 4}, 60000, 180000, 60000);
        ProductionGraph.Plot plot = ProductionGraph.Plot.create(history, 3);
        assertEquals(4, plot.maximum);
        assertEquals(1f / 3, plot.knownFrom, .0001f);
        assertEquals(0f, plot.height[0], 0f);
        assertEquals(1f, plot.height[1], 0f);
        assertEquals(1f, plot.height[2], 0f);
        assertEquals(1f, plot.right[2], 0f);
    }
    @Test public void windowUsesTimeRatherThanStretchingShortHistory() {
        ProductionGraph.Plot plot = ProductionGraph.Plot.create(
                new ProductionHistory(new int[] {3}, 60000, 60000, 0), 15);
        assertEquals(14f / 15, plot.left[0], .0001f);
        assertEquals(1f, plot.right[0], 0f);
    }
    @Test public void zeroProductionIsValidHistoryWithNonzeroAxis() {
        ProductionGraph.Plot plot = ProductionGraph.Plot.create(
                new ProductionHistory(new int[] {0, 0}, 60000, 120000, 0), 1);
        assertTrue(plot.hasHistory);
        assertEquals(1, plot.maximum);
        assertEquals(0f, plot.height[1], 0f);
        assertEquals(0f, plot.right[0], 0f);
    }
    @Test public void plotsSmoothedValuesWithoutTruncatingFractionalCounts() {
        ProductionGraph.Plot plot = ProductionGraph.Plot.create(
                new ProductionHistory(new int[] {10, 20}, 14080, 28160, 0,
                        new double[] {.5, 1.5}), 1);
        assertEquals(2, plot.maximum);
        assertEquals(.25f, plot.height[0], 0f);
        assertEquals(.75f, plot.height[1], 0f);
    }
    @Test public void partialFirstBucketStartsAtRecordingBoundary() {
        ProductionGraph.Plot plot = ProductionGraph.Plot.create(
                new ProductionHistory(new int[] {2}, 60000, 60000, 30000), 1);
        assertEquals(.5f, plot.left[0], 0f);
        assertEquals(.5f, plot.knownFrom, 0f);
    }
}
