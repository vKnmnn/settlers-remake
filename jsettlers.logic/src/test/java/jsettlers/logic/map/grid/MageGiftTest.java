package jsettlers.logic.map.grid;

import jsettlers.common.ai.EPlayerType;
import jsettlers.common.map.shapes.MapCircle;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.ESpellType;
import jsettlers.common.movable.EMovableType;
import jsettlers.logic.movable.Movable;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.military.MageMovable;
import jsettlers.logic.player.Player;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.network.synchronic.timer.NetworkTimer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Exercises the real priest command and material stacks. Placement deliberately tests
 * the current radius-three free-cell policy, not the original offset search/stack merging.
 */
public class MageGiftTest {
    private static final ShortPoint2D TARGET = new ShortPoint2D(13, 12);
    private MainGrid grid;
    private MageMovable priest;

    @Before public void setup() {
        MatchConstants.init(new NetworkTimer(true), 0L);
        grid = new MainGrid("mage-gift-test", "Mage gift test", (short) 64, (short) 64,
                new PlayerSetting[] {new PlayerSetting(EPlayerType.HUMAN, ECivilisation.ROMAN, (byte) 0)});
        Player player = grid.getPartitionsGrid().getPlayer((byte) 0);
        for (int i = 0; i < 100; i++) player.getMannaInformation().increaseManna();
        priest = (MageMovable) Movable.createMovable(EMovableType.MAGE, player,
                new ShortPoint2D(12, 12), grid.movablePathfinderGrid);
    }

    @After public void cleanup() {
        if (grid != null) grid.stopThreads();
        JSettlersGame.clearState();
    }

    @Test public void freeLandReceivesFourPositiveMaterialStacks() {
        cast();
        int[] stacksAndGoods = new int[2];
        region().stream().forEach((x, y) -> {
            int goods = amountAt(x, y);
            if (goods > 0) {
                stacksAndGoods[0]++;
                stacksAndGoods[1] += goods;
                assertTrue(goods <= 7);
            }
        });
        assertEquals(4, stacksAndGoods[0]);
        assertTrue(stacksAndGoods[1] >= 4);
    }

    @Test public void blockedRegionReceivesNothingAndExistingStackIsUnchanged() {
        assertTrue(grid.movablePathfinderGrid.dropMaterial(TARGET, EMaterialType.PLANK, false, false));
        region().stream().forEach((x, y) -> grid.flagsGrid.setBlockedAndProtected(x, y, true));
        cast();
        region().stream().forEach((x, y) -> assertEquals(
                x == TARGET.x && y == TARGET.y ? 1 : 0, amountAt(x, y)));
        assertEquals(1, grid.mapObjectsManager.getStackSize(TARGET.x, TARGET.y, EMaterialType.PLANK));
    }

    @Test public void oneFreeCellReceivesOnlyOnePackageWithoutOverflowIntoBlockedCells() {
        region().stream().forEach((x, y) -> grid.flagsGrid.setBlockedAndProtected(x, y,
                x != TARGET.x || y != TARGET.y));
        cast();
        assertTrue(amountAt(TARGET.x, TARGET.y) > 0);
        region().stream().forEach((x, y) -> {
            if (x != TARGET.x || y != TARGET.y) assertEquals(0, amountAt(x, y));
        });
    }

    private void cast() {
        priest.moveToCast(TARGET, ESpellType.GIFTS);
        priest.timerEvent();
    }

    private MapCircle region() { return new MapCircle(TARGET, ESpellType.GIFTS_RADIUS); }

    private int amountAt(int x, int y) {
        int amount = 0;
        for (EMaterialType material : EMaterialType.values()) {
            amount += grid.mapObjectsManager.getStackSize((short) x, (short) y, material);
        }
        return amount;
    }
}
