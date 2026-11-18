package jsettlers.common.statistics;

import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Measures elapsed target time per real time over non-overlapping intervals. */
public class IntervalTimeRateCalculator {
	private final long interval;
	private final Supplier<Long> targetTime;
	private final LongSupplier realTime;
	private long previousRealTime;
	private long previousTargetTime;
	private float rate;

	public IntervalTimeRateCalculator(long interval, Supplier<Long> targetTime) {
		this(interval, targetTime, System::nanoTime);
	}

	IntervalTimeRateCalculator(long interval, Supplier<Long> targetTime, LongSupplier realTime) {
		if (interval <= 0) throw new IllegalArgumentException("Non-positive measurement interval");
		this.interval = interval;
		this.targetTime = targetTime;
		this.realTime = realTime;
		previousRealTime = realTime.getAsLong();
		previousTargetTime = targetTime.get();
	}

	public void tick() {
		long now = realTime.getAsLong();
		long elapsed = now - previousRealTime;
		if (elapsed < interval) return;
		long target = targetTime.get();
		// A restored or reset clock must not produce a negative speed.
		rate = (float) (Math.max(0L, target - previousTargetTime) / (double) elapsed);
		previousRealTime = now;
		previousTargetTime = target;
	}

	public float getRate() {
		return rate;
	}
}
