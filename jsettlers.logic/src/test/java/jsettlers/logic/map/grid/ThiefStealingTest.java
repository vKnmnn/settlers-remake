package jsettlers.logic.map.grid;

import java.lang.reflect.Field;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.specialist.ThiefMovable;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.network.synchronic.timer.NetworkTimer;
import jsettlers.testutils.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ThiefStealingTest {
    private MainGrid grid;
    private MainGrid restored;
    private final ShortPoint2D target = new ShortPoint2D(16, 16);

    @Before
    public void setup() {
        MatchConstants.init(new NetworkTimer(true), 0L);
        grid = new MainGrid("thief-test", "Thief test", (short) 32, (short) 32,
                new PlayerSetting[] {new PlayerSetting((byte) 0), new PlayerSetting((byte) 1)});
        grid.getPartitionsGrid().changePlayerAt(target.x, target.y, (byte) 1);
        grid.movablePathfinderGrid.dropMaterial(target, EMaterialType.GOLD, false, true);
    }

    @After
    public void cleanup() {
        if (restored != null) restored.stopThreads();
        grid.stopThreads();
        JSettlersGame.clearState();
    }

    @Test
    public void goodsAreSecuredBeforeAnotherCarrierCanTakeThem() throws Exception {
        ThiefMovable thief = stealAtTarget();
        assertEquals(EMovableAction.ACTION1, thief.getAction());
        assertEquals(EMaterialType.GOLD, thief.getMaterial());
        assertNull(grid.movablePathfinderGrid.takeMaterial(target));
        thief.timerEvent();
        assertEquals(EMaterialType.GOLD, thief.getMaterial());
    }

    @Test
    public void savingDuringStealKeepsOneCopyOfTheGoods() throws Exception {
        ThiefMovable thief = stealAtTarget();
        Object[] state = TestUtils.serializeAndDeserialize(new Object[] {grid, thief});
        restored = (MainGrid) state[0];
        ThiefMovable loaded = (ThiefMovable) state[1];
        assertEquals(EMaterialType.GOLD, loaded.getMaterial());
        assertNull(restored.movablePathfinderGrid.takeMaterial(target));
        loaded.timerEvent();
        assertEquals(EMaterialType.GOLD, loaded.getMaterial());
    }

    @Test
    public void secondThiefCannotDuplicateTheLastItem() throws Exception {
        ThiefMovable first = stealAtTarget();
        ThiefMovable second = stealAtTarget();
        assertEquals(EMaterialType.GOLD, first.getMaterial());
        assertEquals(EMaterialType.NO_MATERIAL, second.getMaterial());
    }

    @Test
    public void ownGoodsAreNotStolen() throws Exception {
        grid.getPartitionsGrid().changePlayerAt(target.x, target.y, (byte) 0);
        ThiefMovable thief = stealAtTarget();
        assertEquals(EMaterialType.NO_MATERIAL, thief.getMaterial());
        assertEquals(EMaterialType.GOLD, grid.movablePathfinderGrid.takeMaterial(target));
    }

    private ThiefMovable stealAtTarget() throws Exception {
        ThiefMovable thief = new ThiefMovable(grid.movablePathfinderGrid, target,
                grid.getPartitionsGrid().getPlayer((byte) 0), null);
        // Place the thief at an already selected target, isolating arrival from pathfinding.
        Field currentTarget = ThiefMovable.class.getDeclaredField("currentTarget");
        currentTarget.setAccessible(true);
        currentTarget.set(thief, target);
        thief.timerEvent();
        return thief;
    }
}
