package jsettlers.logic.map.grid;

import jsettlers.logic.movable.military.MageMovable;

import jsettlers.algorithms.terraform.LandscapeEditor;
import jsettlers.common.map.shapes.MapCircle;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import jsettlers.common.landscape.ELandscapeType;
import jsettlers.common.movable.ESpellType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.network.synchronic.timer.NetworkTimer;
import jsettlers.testutils.TestUtils;

public class MageSaveTest {
    private MainGrid original;
    private MainGrid restored;

    @After
    public void tearDown() {
        if (original != null) original.stopThreads();
        if (restored != null) restored.stopThreads();
        JSettlersGame.clearState();
    }

    @Test
    public void queuedIrrigationStillChangesLandscapeAfterLoading() throws Exception {
        MatchConstants.init(new NetworkTimer(true), 0L);
        original = new MainGrid("mage-test", "Mage test", (short) 32, (short) 32,
                new PlayerSetting[] { new PlayerSetting((byte) 0) });
        AbstractMovableGrid terrain = original.movablePathfinderGrid;
        ShortPoint2D target = new ShortPoint2D(16, 16);
        new LandscapeEditor(
                pt -> terrain.getLandscapeTypeAt(pt.x, pt.y),
                (pt, type) -> terrain.setLandscape(pt.x, pt.y, type))
                .fill(ELandscapeType.DESERT, new MapCircle(target, 5).stream().toList());
        Player player = original.getPartitionsGrid().getPlayer((byte) 0);
        for (int i = 0; i < 100; i++) player.getMannaInformation().increaseManna();
        MageMovable mage = new MageMovable(terrain, target, player, null);
        mage.moveToCast(target, ESpellType.IRRIGATE);

        Object[] loaded = TestUtils.serializeAndDeserialize(new Object[] { original, mage });
        restored = (MainGrid) loaded[0];
        MageMovable loadedMage = (MageMovable) loaded[1];
        AbstractMovableGrid loadedTerrain = restored.movablePathfinderGrid;
        assertEquals(ELandscapeType.DESERT, loadedTerrain.getLandscapeTypeAt(target.x, target.y));
        loadedMage.timerEvent();
        assertEquals(ELandscapeType.GRASS, loadedTerrain.getLandscapeTypeAt(target.x, target.y));
        assertEquals(ELandscapeType.DESERT, terrain.getLandscapeTypeAt(target.x, target.y));
    }
}
