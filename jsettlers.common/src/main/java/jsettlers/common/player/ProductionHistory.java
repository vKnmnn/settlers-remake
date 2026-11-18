package jsettlers.common.player;

/** Immutable production counts for consecutive simulation-time intervals, oldest first. */
public final class ProductionHistory {
    private final int[] amounts;
    private final int bucketMillis;
    private final double[] smoothedAmounts;
    private static final int[] WEIGHTS = {3, 6, 8, 9, 9, 10, 10, 10, 9, 9, 8, 6, 3};
    private final int endTimeMillis;
    private final int availableSinceMillis;

    public ProductionHistory(int[] amounts, int bucketMillis, int endTimeMillis, int availableSinceMillis) {
        this(amounts, bucketMillis, endTimeMillis, availableSinceMillis, null);
    }

    public ProductionHistory(int[] amounts, int bucketMillis, int endTimeMillis, int availableSinceMillis,
                             double[] smoothedAmounts) {
        if (smoothedAmounts != null && smoothedAmounts.length != amounts.length) throw new IllegalArgumentException("History length mismatch");
        if (bucketMillis <= 0) throw new IllegalArgumentException("Non-positive history interval");
        this.amounts = amounts.clone();
        this.bucketMillis = bucketMillis;
        this.endTimeMillis = endTimeMillis;
        this.availableSinceMillis = availableSinceMillis;
        this.smoothedAmounts = smoothedAmounts == null ? smooth() : smoothedAmounts.clone();
    }

    private double[] smooth() {
        double[] result = new double[amounts.length];
        for (int i = 0; i < amounts.length; i++) {
            long weighted = 0;
            int weight = 0;
            for (int delta = -6; delta <= 6; delta++) {
                int sample = i + delta;
                if (sample < 0 || sample >= amounts.length) continue;
                long sampleEnd = (long) endTimeMillis - (amounts.length - 1L - sample) * bucketMillis;
                if (sampleEnd <= availableSinceMillis) continue;
                weighted += (long) amounts[sample] * WEIGHTS[delta + 6];
                weight += WEIGHTS[delta + 6];
            }
            result[i] = weight == 0 ? 0 : weighted / (double) weight;
        }
        return result;
    }

    /** Original 13-point kernel, normalized by actual available weights (sum 110). */
    public double[] getSmoothedAmounts() { return smoothedAmounts.clone(); }
    public int[] getAmounts() { return amounts.clone(); }
    public int getBucketMillis() { return bucketMillis; }
    /** Exclusive end of the last interval, which may still be in progress. */
    public int getEndTimeMillis() { return endTimeMillis; }
    /** Times before this point were not recorded (e.g. a save from an older version). */
    public int getAvailableSinceMillis() { return availableSinceMillis; }
}
