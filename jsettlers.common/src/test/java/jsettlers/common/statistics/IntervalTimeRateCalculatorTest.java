package jsettlers.common.statistics;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class IntervalTimeRateCalculatorTest {
	private long realTime;
	private long gameTime;
	private final IntervalTimeRateCalculator calculator =
			new IntervalTimeRateCalculator(250, () -> gameTime, () -> realTime);

	private void advance(long realDelta, long gameDelta) {
		realTime += realDelta;
		gameTime += gameDelta;
		calculator.tick();
	}

	@Test public void fastForwardDoesNotLingerInSubsequentMeasurements() {
		advance(250, 250);
		assertEquals(1, calculator.getRate(), 0);
		advance(250, 60000);
		assertEquals(240, calculator.getRate(), 0);
		advance(250, 250);
		assertEquals(1, calculator.getRate(), 0);
		advance(250, 0);
		assertEquals(0, calculator.getRate(), 0);
		advance(250, 500);
		assertEquals(2, calculator.getRate(), 0);
	}

	@Test public void quantizedSimulationTicksAreMeasuredOverWholeInterval() {
		for (int i = 0; i < 24; i++) {
			advance(10, (i + 1) % 5 == 0 ? 50 : 0);
			assertEquals(0, calculator.getRate(), 0);
		}
		advance(10, 50);
		assertEquals(1, calculator.getRate(), 0);
		calculator.tick();
		assertEquals(1, calculator.getRate(), 0);
	}

	@Test public void slowRenderingUsesActualElapsedTimeAndRecoversAfterClockReset() {
		advance(2000, 60000);
		assertEquals(30, calculator.getRate(), 0);
		advance(250, 250);
		assertEquals(1, calculator.getRate(), 0);
		gameTime = 0;
		advance(250, 0);
		assertEquals(0, calculator.getRate(), 0);
		advance(250, 250);
		assertEquals(1, calculator.getRate(), 0);
	}
}
