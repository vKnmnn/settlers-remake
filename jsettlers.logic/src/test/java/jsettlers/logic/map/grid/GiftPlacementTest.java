package jsettlers.logic.map.grid;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jsettlers.common.map.shapes.MapCircle;
import jsettlers.common.map.shapes.FreeMapArea;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.logic.buildings.Building;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EMovableType;
import jsettlers.logic.movable.Movable;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.grid.partition.manager.materials.offers.EOfferPriority;
import jsettlers.logic.map.grid.partition.manager.materials.offers.MaterialOffer;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.network.synchronic.timer.NetworkTimer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class GiftPlacementTest {
    private static final ShortPoint2D TARGET = new ShortPoint2D(24, 24);
    private MainGrid grid;

    @Before public void setup() {
        MatchConstants.init(new NetworkTimer(true), 0L);
        grid = new MainGrid("gift-placement", "Gift placement", (short) 64, (short) 64,
                new PlayerSetting[] {new PlayerSetting((byte) 0)});
    }
    @After public void cleanup() { grid.stopThreads(); JSettlersGame.clearState(); }

    @Test public void nearbyMatchingStackHasPriorityOverEmptyTarget() {
        ShortPoint2D existing = new ShortPoint2D(25, 24);
        seed(existing, EMaterialType.GOLD, 2);
        List<ShortPoint2D> placed = drop(TARGET, 3);
        assertEquals(List.of(existing, existing, existing), placed);
        assertEquals(5, size(existing, EMaterialType.GOLD));
        assertEquals(0, size(TARGET, EMaterialType.GOLD));
    }

    @Test public void packageOverflowFillsExistingStackToEightThenCreatesAnother() {
        seed(TARGET, EMaterialType.GOLD, 6);
        List<ShortPoint2D> placed = drop(TARGET, 5);
        assertEquals(5, placed.size());
        assertEquals(8, size(TARGET, EMaterialType.GOLD));
        Set<ShortPoint2D> positions = new HashSet<>(placed);
        assertEquals(2, positions.size());
        positions.remove(TARGET);
        assertEquals(3, size(positions.iterator().next(), EMaterialType.GOLD));
    }

    @Test public void incompatibleFullBlockedAndProtectedEmptyCellsRemainUnchanged() {
        ShortPoint2D full = new ShortPoint2D(25, 24);
        ShortPoint2D blocked = new ShortPoint2D(24, 25);
        ShortPoint2D protectedEmpty = new ShortPoint2D(23, 24);
        seed(TARGET, EMaterialType.PLANK, 3);
        seed(full, EMaterialType.GOLD, 8);
        seed(blocked, EMaterialType.GOLD, 2);
        grid.flagsGrid.setBlockedAndProtected(blocked.x, blocked.y, true);
        grid.flagsGrid.setProtected(protectedEmpty.x, protectedEmpty.y, true);
        List<ShortPoint2D> placed = drop(TARGET, 4);
        assertEquals(4, placed.size());
        for (ShortPoint2D point : placed) {
            assertFalse(Set.of(TARGET, full, blocked, protectedEmpty).contains(point));
        }
        assertEquals(3, size(TARGET, EMaterialType.PLANK));
        assertEquals(0, size(TARGET, EMaterialType.GOLD));
        assertEquals(8, size(full, EMaterialType.GOLD));
        assertEquals(2, size(blocked, EMaterialType.GOLD));
        assertEquals(0, size(protectedEmpty, EMaterialType.GOLD));
    }

    @Test public void matchingStackInBuildingAreaIsNotFilledEvenWhenUnblocked() {
        Building building = grid.constructBuildingAt(new ShortPoint2D(40, 40), EBuildingType.SAWMILL,
                grid.getPartitionsGrid().getPlayer((byte) 0), true);
        // Associate an accessible storage cell with the real building, isolating the area guard
        // from collision flags: ordinary material insertion must remain possible here.
        grid.objectsGrid.setBuildingArea(new FreeMapArea(List.of(TARGET)), building);
        seed(TARGET, EMaterialType.GOLD, 2);
        assertFalse(grid.flagsGrid.isBlocked(TARGET.x, TARGET.y));
        List<ShortPoint2D> placed = drop(TARGET, 3);
        assertEquals(3, placed.size());
        assertFalse(placed.contains(TARGET));
        assertEquals(2, size(TARGET, EMaterialType.GOLD));
    }

    @Test public void blockedRadiusThreeSearchesFurtherOut() {
        new MapCircle(TARGET, 3).stream().forEach((x, y) -> grid.flagsGrid.setBlockedAndProtected(x, y, true));
        List<ShortPoint2D> placed = drop(TARGET, 4);
        assertEquals(4, placed.size());
        for (ShortPoint2D point : placed) assertTrue(TARGET.getOnGridDistTo(point) > 3);
    }

    @Test public void occupiedTargetIsSkippedEvenWithoutExistingStacks() {
        Movable unit = Movable.createMovable(EMovableType.SWORDSMAN_L1,
                grid.getPartitionsGrid().getPlayer((byte) 0), TARGET, grid.movablePathfinderGrid);
        List<ShortPoint2D> placed = drop(TARGET, 3);
        assertEquals(3, placed.size());
        assertFalse(placed.contains(TARGET));
        assertSame(unit, grid.movablePathfinderGrid.getMovableAt(TARGET.x, TARGET.y));
    }

    @Test public void newStacksAvoidUnitsAndNeighboringStacks() {
        Movable unit = Movable.createMovable(
                EMovableType.SWORDSMAN_L1,
                grid.getPartitionsGrid().getPlayer((byte) 0), TARGET, grid.movablePathfinderGrid);
        ShortPoint2D existing = new ShortPoint2D(25, 24);
        seed(existing, EMaterialType.PLANK, 2);
        List<ShortPoint2D> placed = drop(TARGET, 3);
        assertEquals(3, placed.size());
        for (ShortPoint2D point : placed) {
            assertNotEquals(TARGET, point);
            assertTrue(existing.getOnGridDistTo(point) > 1);
        }
        assertSame(unit, grid.movablePathfinderGrid.getMovableAt(TARGET.x, TARGET.y));
        assertEquals(2, size(existing, EMaterialType.PLANK));
    }

    @Test public void fullyBlockedMapReturnsNoPlacements() {
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) {
            grid.flagsGrid.setBlockedAndProtected(x, y, true);
        }
        assertTrue(drop(TARGET, 4).isEmpty());
        for (short y = 0; y < 64; y++) for (short x = 0; x < 64; x++) {
            assertEquals(0, grid.mapObjectsManager.getStackSize(x, y, EMaterialType.GOLD));
        }
    }

    @Test public void mapCornersSkipOutOfBoundsOffsetsAndPreserveAllGoods() {
        for (ShortPoint2D corner : List.of(new ShortPoint2D(0, 0), new ShortPoint2D(63, 0),
                new ShortPoint2D(0, 63), new ShortPoint2D(63, 63))) {
            List<ShortPoint2D> placed = drop(corner, 11);
            assertEquals(11, placed.size());
            int sum = 0;
            for (ShortPoint2D point : new HashSet<>(placed)) {
                assertTrue(point.x >= 0 && point.x < 64 && point.y >= 0 && point.y < 64);
                int amount = size(point, EMaterialType.GOLD);
                assertTrue(amount <= 8);
                sum += amount;
            }
            assertEquals(11, sum);
        }
    }

    @Test public void eachPlacedUnitHasAMaterialOfferAtItsActualPosition() {
        List<ShortPoint2D> placed = drop(TARGET, 11);
        assertEquals(11, placed.size());
        for (ShortPoint2D point : new HashSet<>(placed)) {
            MaterialOffer offer = grid.getPartitionsGrid().getPartitionAt(point.x, point.y)
                    .getMaterialOfferAt(point, EMaterialType.GOLD, EOfferPriority.OFFER_TO_ALL);
            assertNotNull(offer);
            assertEquals(size(point, EMaterialType.GOLD), offer.getAmount());
            assertEquals(EMaterialType.GOLD, grid.movablePathfinderGrid.takeMaterial(point));
        }
    }

    private List<ShortPoint2D> drop(ShortPoint2D target, int amount) {
        return grid.movablePathfinderGrid.dropGift(target, EMaterialType.GOLD, amount);
    }
    private void seed(ShortPoint2D point, EMaterialType material, int amount) {
        for (int i = 0; i < amount; i++) assertTrue(grid.movablePathfinderGrid.dropMaterial(point, material, false, false));
    }
    private int size(ShortPoint2D point, EMaterialType material) {
        return grid.mapObjectsManager.getStackSize(point.x, point.y, material);
    }
}
