package jsettlers.logic.map.grid.objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jsettlers.common.position.ShortPoint2D;

/** The original game's distance-ordered search for placing loose goods. */
final class GiftSearchOffsets {
	private static final int SEARCH_LIMIT = 10_000;
	static final List<ShortPoint2D> OFFSETS = createOffsets();

	private GiftSearchOffsets() {
	}

	private static List<ShortPoint2D> createOffsets() {
		List<ShortPoint2D> wedge = new ArrayList<>(5_000);
		for (int x = 1; wedge.size() < 5_000; x++) {
			for (int y = 0; y < x && wedge.size() < 5_000; y++) {
				wedge.add(new ShortPoint2D(x, y));
			}
		}
		// List.sort is stable: ties retain the original x-then-y enumeration.
		wedge.sort(Comparator.comparingLong(point ->
				2_500L * (2 * point.x - point.y) * (2 * point.x - point.y)
						+ 7_569L * point.y * point.y));
		List<ShortPoint2D> offsets = new ArrayList<>(SEARCH_LIMIT);
		offsets.add(new ShortPoint2D(0, 0));
		for (ShortPoint2D point : wedge) {
			int x = point.x;
			int y = point.y;
			ShortPoint2D[] rotations = {
					new ShortPoint2D(x, y), new ShortPoint2D(x - y, x),
					new ShortPoint2D(-y, x - y), new ShortPoint2D(-x, -y),
					new ShortPoint2D(y - x, -x), new ShortPoint2D(y, y - x)
			};
			for (ShortPoint2D rotated : rotations) {
				offsets.add(rotated);
				if (offsets.size() == SEARCH_LIMIT) {
					return Collections.unmodifiableList(offsets);
				}
			}
		}
		throw new IllegalStateException("Not enough gift search offsets");
	}
}
