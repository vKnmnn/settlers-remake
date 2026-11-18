/*******************************************************************************
 * Copyright (c) 2026
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package jsettlers.logic.player;

import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.common.player.ProductionHistory;

import jsettlers.common.material.EMaterialType;
import jsettlers.common.player.IProductionStatistic;

/**
 * Counts the materials produced by the workers of a player.
 */
public class ProductionStatistic implements IProductionStatistic, Serializable {
	private static final long serialVersionUID = 1L;

	// Simulation-time buckets; bounded storage independent of match length.
	static final int BUCKET_MILLIS = 60_000;
	static final int MAX_HISTORY_MINUTES = 60;
	private final int[] produced = new int[EMaterialType.NUMBER_OF_MATERIALS];
	private int[][] history = new int[EMaterialType.NUMBER_OF_MATERIALS][MAX_HISTORY_MINUTES];
	private int[] bucketIds = emptyBucketIds();
	private int availableSinceMillis;
	private int lastEventTime;

	private static int[] emptyBucketIds() {
		int[] ids = new int[MAX_HISTORY_MINUTES];
		Arrays.fill(ids, -1);
		return ids;
	}

	private int currentTime() {
		return MatchConstants.clock() == null ? lastEventTime : Math.max(0, MatchConstants.clock().getTime());
	}

	public void materialProduced(EMaterialType materialType) {
		materialProduced(materialType, currentTime());
	}

	synchronized void materialProduced(EMaterialType materialType, int timeMillis) {
		if (materialType == null || !materialType.isDroppable() || materialType.ordinal >= produced.length) return;
		if (timeMillis < lastEventTime) throw new IllegalArgumentException("Production time moved backwards");
		lastEventTime = timeMillis;
		produced[materialType.ordinal]++;
		int bucket = timeMillis / BUCKET_MILLIS;
		int index = bucket % bucketIds.length;
		if (bucketIds[index] != bucket) {
			for (int[] material : history) material[index] = 0;
			bucketIds[index] = bucket;
		}
		history[materialType.ordinal][index]++;
	}

	@Override
	public synchronized int getAmountProduced(EMaterialType materialType) {
		if (materialType == null || materialType.ordinal >= produced.length) return 0;
		return produced[materialType.ordinal];
	}

	@Override
	public ProductionHistory getHistory(EMaterialType materialType, int windowMinutes) {
		return historyAt(materialType, windowMinutes, currentTime());
	}

	/** Read-only: opening the statistics cannot change saved simulation state. */
	synchronized ProductionHistory historyAt(EMaterialType materialType, int windowMinutes, int timeMillis) {
		if (windowMinutes <= 0 || windowMinutes > MAX_HISTORY_MINUTES) {
			throw new IllegalArgumentException("History window must be between 1 and " + MAX_HISTORY_MINUTES);
		}
		int lastBucket = Math.max(0, timeMillis) / BUCKET_MILLIS;
		int[] amounts = new int[windowMinutes];
		for (int i = 0; i < amounts.length; i++) {
			int bucket = lastBucket - amounts.length + 1 + i;
			if (bucket < 0 || materialType == null || materialType.ordinal >= history.length) continue;
			int index = bucket % bucketIds.length;
			if (bucketIds[index] == bucket) amounts[i] = history[materialType.ordinal][index];
		}
		return new ProductionHistory(amounts, BUCKET_MILLIS, (lastBucket + 1) * BUCKET_MILLIS,
				Math.max(availableSinceMillis, Math.max(0, lastBucket - bucketIds.length + 1) * BUCKET_MILLIS));
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		if (history == null || bucketIds == null) {
			// Old saves retain their totals, but cannot reconstruct past production times.
			history = new int[EMaterialType.NUMBER_OF_MATERIALS][MAX_HISTORY_MINUTES];
			bucketIds = emptyBucketIds();
			availableSinceMillis = currentTime();
			lastEventTime = availableSinceMillis;
		}
	}
}
