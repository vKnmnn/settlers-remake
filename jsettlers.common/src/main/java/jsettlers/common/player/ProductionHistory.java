package jsettlers.common.player;

/** Immutable production counts for consecutive simulation-time intervals, oldest first. */
public final class ProductionHistory {
    private final int[] amounts;
    private final int bucketMillis;
    private final int endTimeMillis;
    private final int availableSinceMillis;

    public ProductionHistory(int[] amounts, int bucketMillis, int endTimeMillis, int availableSinceMillis) {
        if (bucketMillis <= 0) throw new IllegalArgumentException("Non-positive history interval");
        this.amounts = amounts.clone();
        this.bucketMillis = bucketMillis;
        this.endTimeMillis = endTimeMillis;
        this.availableSinceMillis = availableSinceMillis;
    }

    public int[] getAmounts() { return amounts.clone(); }
    public int getBucketMillis() { return bucketMillis; }
    /** Exclusive end of the last interval, which may still be in progress. */
    public int getEndTimeMillis() { return endTimeMillis; }
    /** Times before this point were not recorded (e.g. a save from an older version). */
    public int getAvailableSinceMillis() { return availableSinceMillis; }
}
