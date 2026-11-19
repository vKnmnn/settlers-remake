package jsettlers.logic.map.grid;

import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.map.shapes.HexGridArea;
import jsettlers.common.movable.ESiegeWeaponType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.Building;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.IAttackable;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.network.synchronic.timer.NetworkTimer;
import jsettlers.testutils.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SiegeBuildingDamageTest {
    private MainGrid original;
    private MainGrid restored;
    private final ShortPoint2D source = new ShortPoint2D(12, 12);
    private final ShortPoint2D position = new ShortPoint2D(30, 12);

    @Before public void setup() {
        MatchConstants.init(new NetworkTimer(true), 0L);
        original = new MainGrid("siege-damage", "Siege damage", (short) 64, (short) 64,
                new PlayerSetting[] { new PlayerSetting((byte) 0), new PlayerSetting((byte) 1) });
    }

    @After public void cleanup() {
        if (restored != null) restored.stopThreads();
        original.stopThreads();
        JSettlersGame.clearState();
    }

    @Test public void structuralDamageSurvivesSavingAndEventuallyDestroysUnoccupiedTower() throws Exception {
        Building tower = original.constructBuildingAt(position, EBuildingType.TOWER, original.getPartitionsGrid().getPlayer((byte) 1), true);
        tower.receiveSiegeHit(60, source, original.getPartitionsGrid().getPlayer((byte) 0));
        assertEquals(190, tower.getSiegeHealth(), 0f);
        restored = TestUtils.serializeAndDeserialize(original);
        Building loaded = restored.getObjectsGrid().getBuildingAt(position.x, position.y);
        assertEquals(190, loaded.getSiegeHealth(), 0f);
        loaded.receiveSiegeHit(190, source, restored.getPartitionsGrid().getPlayer((byte) 0));
        assertTrue(loaded.isDestroyed());
        assertEquals(1, loaded.getPlayer().getPlayerId());
    }

    @Test public void targetSearchIgnoresInfantryButFindsEmptyTower() {
        Movable.createMovable(EMovableType.SWORDSMAN_L1, original.getPartitionsGrid().getPlayer((byte) 1),
                new ShortPoint2D(24, 12), original.movablePathfinderGrid);
        assertNull(original.movablePathfinderGrid.getSiegeTarget(source, original.getPartitionsGrid().getPlayer((byte) 0), ESiegeWeaponType.CATAPULT, (short) 10, (short) 26));
        original.constructBuildingAt(position, EBuildingType.TOWER, original.getPartitionsGrid().getPlayer((byte) 1), true);
        IAttackable target = original.movablePathfinderGrid.getSiegeTarget(source, original.getPartitionsGrid().getPlayer((byte) 0), ESiegeWeaponType.CATAPULT, (short) 10, (short) 26);
        assertNotNull(target);
        assertEquals(position, target.getPosition());
    }

    @Test public void alliedStructuresAreImmuneToImpacts() {
        Building tower = original.constructBuildingAt(position, EBuildingType.TOWER, original.getPartitionsGrid().getPlayer((byte) 0), true);
        original.movablePathfinderGrid.applySiegeDamage(position, ESiegeWeaponType.GONG, 1000,
                original.getPartitionsGrid().getPlayer((byte) 0), source);
        assertEquals(250, tower.getSiegeHealth(), 0f);
        assertFalse(tower.isDestroyed());
    }

    @Test public void gongDestroysItsCivilianTargetWithoutHurtingNearbySettlers() {
        original.landscapeGrid.generateBlockedPartitions();
        ShortPoint2D targetPosition = new ShortPoint2D(16, 12);
        Building target = original.constructBuildingAt(targetPosition, EBuildingType.LUMBERJACK,
                original.getPartitionsGrid().getPlayer((byte) 1), true);
        ShortPoint2D gongPosition = HexGridArea.stream(targetPosition.x, targetPosition.y, 3, 5)
                .filterBounds(original.width, original.height)
                .filter((x, y) -> !original.movablePathfinderGrid.isBlockedOrProtected(x, y))
                .filter((x, y) -> original.movablePathfinderGrid.canSpawnAt(EMovableType.GONG, original.getPartitionsGrid().getPlayer((byte) 0), new ShortPoint2D(x, y)))
                .getFirst().get();
        Movable gong = Movable.createMovable(EMovableType.GONG, original.getPartitionsGrid().getPlayer((byte) 0), gongPosition,
                original.movablePathfinderGrid);
        Movable settler = Movable.createMovable(EMovableType.BEARER, original.getPartitionsGrid().getPlayer((byte) 1),
                new ShortPoint2D(12, 16), original.movablePathfinderGrid);
        float settlerHealth = settler.getHealth();
        for (int time = 0; time < 30000 && !target.isDestroyed(); time += 50) {
            MatchConstants.clock().setTime(time);
            gong.timerEvent();
        }
        assertTrue("gong must actually strike and destroy its selected building", target.isDestroyed());
        assertEquals(settlerHealth, settler.getHealth(), 0f);
    }

    @Test public void originalHitThresholdsAndDamageBoundsArePreserved() {
        assertFalse(ESiegeWeaponType.CATAPULT.isHit(32000));
        assertTrue(ESiegeWeaponType.CATAPULT.isHit(32001));
        assertFalse(ESiegeWeaponType.BALLISTA.isHit(20000));
        assertTrue(ESiegeWeaponType.BALLISTA.isHit(20001));
        assertFalse(ESiegeWeaponType.CANNON.isHit(15000));
        assertTrue(ESiegeWeaponType.CANNON.isHit(15001));
        assertFalse(ESiegeWeaponType.GONG.isHit(20000));
        assertTrue(ESiegeWeaponType.GONG.isHit(20001));
        assertEquals(60, ESiegeWeaponType.CATAPULT.getHitDamage(0));
        assertEquals(119, ESiegeWeaponType.CATAPULT.getHitDamage(65535));
        assertEquals(80, ESiegeWeaponType.BALLISTA.getHitDamage(0));
        assertEquals(159, ESiegeWeaponType.BALLISTA.getHitDamage(65535));
        assertEquals(100, ESiegeWeaponType.CANNON.getHitDamage(0));
        assertEquals(199, ESiegeWeaponType.CANNON.getHitDamage(65535));
    }
}
