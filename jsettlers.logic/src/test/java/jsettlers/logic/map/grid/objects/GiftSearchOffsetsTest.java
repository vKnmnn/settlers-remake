package jsettlers.logic.map.grid.objects;

import java.util.HashSet;
import java.util.List;

import jsettlers.common.position.ShortPoint2D;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GiftSearchOffsetsTest {
	@Test
	public void searchContainsTenThousandDistinctPositions() {
		assertEquals(10_000, GiftSearchOffsets.OFFSETS.size());
		assertEquals(10_000, new HashSet<>(GiftSearchOffsets.OFFSETS).size());
	}

	@Test
	public void beginsAtCenterThenVisitsSixNeighboursAndIntermediateSecondRing() {
		int[][] expected = {
				{0, 0}, {1, 0}, {1, 1}, {0, 1}, {-1, 0}, {-1, -1}, {0, -1},
				{2, 1}, {1, 2}, {-1, 1}, {-2, -1}, {-1, -2}, {1, -1},
				{2, 0}, {2, 2}, {0, 2}, {-2, 0}, {-2, -2}, {0, -2}
		};
		for (int i = 0; i < expected.length; i++) {
			assertEquals("offset " + i, new ShortPoint2D(expected[i][0], expected[i][1]),
					GiftSearchOffsets.OFFSETS.get(i));
		}
	}

	@Test
	public void wedgeRepresentativesRemainDistanceOrderedWithStableTies() {
		List<ShortPoint2D> offsets = GiftSearchOffsets.OFFSETS;
		long previousMetric = -1;
		ShortPoint2D previous = new ShortPoint2D(0, 0);
		// The slightly anisotropic original metric orders wedges, not individual rotations.
		for (int i = 1; i < offsets.size(); i += 6) {
			ShortPoint2D point = offsets.get(i);
			long metric = 2_500L * (2 * point.x - point.y) * (2 * point.x - point.y)
					+ 7_569L * point.y * point.y;
			assertTrue(metric >= previousMetric);
			if (metric == previousMetric) {
				assertTrue(point.x > previous.x || point.x == previous.x && point.y > previous.y);
			}
			previousMetric = metric;
			previous = point;
		}
	}

	@Test(expected = UnsupportedOperationException.class)
	public void sharedOffsetsCannotBeModified() {
		GiftSearchOffsets.OFFSETS.clear();
	}
}
