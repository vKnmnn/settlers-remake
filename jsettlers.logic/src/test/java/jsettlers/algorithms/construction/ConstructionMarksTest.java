package jsettlers.algorithms.construction;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jsettlers.common.buildings.BuildingVariant;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.landscape.ELandscapeType;
import jsettlers.common.map.shapes.MapRectangle;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.position.RelativePoint;
import org.junit.Test;

import static org.junit.Assert.*;

public class ConstructionMarksTest {
	private static final MapRectangle AREA = new MapRectangle(0, 0, 2, 1);
	private static final BuildingVariant BUILDING = EBuildingType.SMALL_LIVINGHOUSE.getVariant(ECivilisation.ROMAN);

	@Test
	public void partiallyCalculatedMarksCanBeRemovedAfterFailure() {
		TestMap map = new TestMap();
		map.failSecondTile = true;
		NewConstructionMarksAlgorithm algorithm = new NewConstructionMarksAlgorithm(map, (byte) 0);
		try {
			algorithm.calculateConstructMarks(AREA, BUILDING);
			fail("Expected interrupted calculation");
		} catch (IllegalStateException expected) {
			assertEquals(1, map.markCount.get());
		}
		algorithm.removeConstructionMarks();
		assertEquals("A failed calculation must not leave orphaned marks", 0, map.markCount.get());
	}

	@Test
	public void cancelRemovesMarksEvenWhenWorkerIsWaiting() throws Exception {
		TestMap map = new TestMap();
		CountDownLatch readyToWait = new CountDownLatch(1);
		ConstructionMarksThread worker = new ConstructionMarksThread(map, () -> {
			if (map.markCount.get() == 2) {
				readyToWait.countDown();
				return true;
			}
			return false;
		}, (byte) 0);
		try {
			worker.setScreen(AREA);
			worker.setBuilding(BUILDING);
			assertTrue("Worker did not finish calculating marks", readyToWait.await(5, TimeUnit.SECONDS));
			worker.cancel();
			assertTrue("Cancel left marks on the map", map.allRemoved.await(5, TimeUnit.SECONDS));
			assertEquals(0, map.markCount.get());
		} finally {
			worker.cancel();
		}
	}

	@Test
	public void failedCalculationIsClearedWhenBuildingIsDeselected() throws Exception {
		TestMap map = new TestMap();
		map.failSecondTile = true;
		ConstructionMarksThread worker = new ConstructionMarksThread(map, () -> false, (byte) 0);
		map.failureAction = () -> worker.setBuilding(null);
		try {
			worker.setScreen(AREA);
			worker.setBuilding(BUILDING);
			assertTrue("Failed calculation left marks after deselection", map.allRemoved.await(5, TimeUnit.SECONDS));
			assertEquals(0, map.markCount.get());
		} finally {
			worker.cancel();
		}
	}

	private static class TestMap extends AbstractConstructionMarkableMap {
		private final boolean[] marked = new boolean[2];
		final AtomicInteger markCount = new AtomicInteger();
		final CountDownLatch allRemoved = new CountDownLatch(1);
		boolean failSecondTile;
		Runnable failureAction = () -> {};

		@Override
		public void setConstructMarking(int x, int y, boolean set, boolean binary, RelativePoint[] positions) {
			if (marked[x] != set) {
				marked[x] = set;
				if (markCount.addAndGet(set ? 1 : -1) == 0) allRemoved.countDown();
			}
		}
		@Override public short getWidth() { return 2; }
		@Override public short getHeight() { return 1; }
		@Override public boolean canConstructAt(int x, int y, EBuildingType type, byte playerId) {
			if (failSecondTile && x == 1) {
				failureAction.run();
				throw new IllegalStateException("Calculation interrupted");
			}
			return true;
		}
		@Override public boolean canUsePositionForConstruction(int x, int y, Set<ELandscapeType> types, short partition) { return true; }
		@Override public short getPartitionIdAt(int x, int y) { return 0; }
		@Override public boolean canPlayerConstructOnPartition(byte player, short partition) { return true; }
		@Override public boolean isInBounds(int x, int y) { return y == 0 && x >= 0 && x < 2; }
		@Override public byte calculateConstructionMarkValue(int x, int y, RelativePoint[] positions) { return 0; }
	}
}
