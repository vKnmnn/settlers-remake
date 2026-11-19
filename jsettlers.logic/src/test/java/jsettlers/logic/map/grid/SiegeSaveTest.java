package jsettlers.logic.map.grid;

import java.util.Arrays;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.mapobject.EMapObjectType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.movable.ESiegeWeaponType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.workers.SiegeWorkshopBuilding;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.military.SiegeWeaponMovable;
import jsettlers.logic.objects.siege.SiegeProjectileObject;
import jsettlers.logic.player.Player;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.network.synchronic.timer.NetworkTimer;
import jsettlers.testutils.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/** Real grid save/load regressions, exercising worker and weapon actions through their public APIs. */
public class SiegeSaveTest {
	private MainGrid original;
	private MainGrid restored;
	private final ShortPoint2D workshopPosition = new ShortPoint2D(32, 32);

	@Before public void setup() {
		MatchConstants.init(new NetworkTimer(true), 0L);
		original = new MainGrid("siege-save", "Siege save", (short) 64, (short) 64,
				new PlayerSetting[] { new PlayerSetting((byte) 0), new PlayerSetting((byte) 1) });
	}

	@After public void cleanup() {
		if (restored != null) restored.stopThreads();
		if (original != null) original.stopThreads();
		JSettlersGame.clearState();
	}

	@Test public void queuedOrdersAndPartiallyFetchedMaterialsSurviveSaving() throws Exception {
		SiegeWorkshopBuilding workshop = workshop();
		workshop.orderSiegeWeapon();
		workshop.orderSiegeWeapon();
		workshop.weaponMaterialTaken(EMaterialType.PLANK);
		restored = TestUtils.serializeAndDeserialize(original);
		SiegeWorkshopBuilding loaded = loadedWorkshop();
		assertEquals(2, loaded.getOrderedSiegeWeapons());

		// Supply only the materials not delivered before saving: lost progress would prevent completion.
		for (EMaterialType material : ESiegeWeaponType.CATAPULT.getWeaponCostMaterials()) {
			int alreadyTaken = material == EMaterialType.PLANK ? 1 : 0;
			for (int i = alreadyTaken; i < ESiegeWeaponType.CATAPULT.getWeaponCost(material); i++) loaded.weaponMaterialTaken(material);
		}
		for (int i = 0; i < ESiegeWeaponType.ASSEMBLY_STEPS; i++) loaded.workOnWeapon();
		assertEquals(1, loaded.getOrderedSiegeWeapons());
		assertEquals(1, weapons(restored));
		assertEquals(0, weapons(original));
		assertEquals(2, workshop.getOrderedSiegeWeapons());

		// The next order must require fresh materials rather than duplicating the first weapon.
		for (int i = 0; i < ESiegeWeaponType.ASSEMBLY_STEPS; i++) loaded.workOnWeapon();
		assertEquals(1, loaded.getOrderedSiegeWeapons());
		assertEquals(1, weapons(restored));
	}

	@Test public void partiallyAssembledWeaponFinishesAfterOnlyRemainingWork() throws Exception {
		SiegeWorkshopBuilding workshop = workshop();
		workshop.orderSiegeWeapon();
		for (EMaterialType material : ESiegeWeaponType.CATAPULT.getWeaponCostMaterials()) {
			for (int i = 0; i < ESiegeWeaponType.CATAPULT.getWeaponCost(material); i++) workshop.weaponMaterialTaken(material);
		}
		for (int i = 0; i < ESiegeWeaponType.ASSEMBLY_STEPS - 2; i++) workshop.workOnWeapon();
		assertEquals(0, weapons(original));
		restored = TestUtils.serializeAndDeserialize(original);
		SiegeWorkshopBuilding loaded = loadedWorkshop();
		loaded.workOnWeapon();
		assertEquals(1, loaded.getOrderedSiegeWeapons());
		assertEquals(0, weapons(restored));
		loaded.workOnWeapon();
		assertEquals(0, loaded.getOrderedSiegeWeapons());
		assertEquals(1, weapons(restored));
	}

	@Test public void ammunitionCooldownAndInFlightProjectileSurviveSaving() throws Exception {
		ShortPoint2D source = new ShortPoint2D(12, 12);
		ShortPoint2D target = new ShortPoint2D(24, 12);
		Player owner = original.getPartitionsGrid().getPlayer((byte) 0);
		Player enemy = original.getPartitionsGrid().getPlayer((byte) 1);
		SiegeWeaponMovable weapon = (SiegeWeaponMovable) Movable.createMovable(EMovableType.CATAPULT, owner, source, original.movablePathfinderGrid);
		Movable victim = Movable.createMovable(EMovableType.CATAPULT, enemy, target, original.movablePathfinderGrid);
		weapon.setAmmo(3);
		int shotTime = 0;
		for (; shotTime < 5000 && weapon.getAmmo() == 3; shotTime += 50) {
			MatchConstants.clock().setTime(shotTime);
			weapon.timerEvent();
		}
		shotTime = MatchConstants.clock().getTime();
		assertEquals("weapon must actually fire before the snapshot", 2, weapon.getAmmo());
		SiegeProjectileObject projectile = projectile(original, target);
		assertNotNull(projectile);
		MatchConstants.clock().setTime(shotTime + 50);
		float progress = projectile.getStateProgress();
		assertTrue(progress > 0f && progress < 1f);
		restored = TestUtils.serializeAndDeserialize(original);
		SiegeWeaponMovable loadedWeapon = (SiegeWeaponMovable) restored.getMovableGrid().getMovableAt(source.x, source.y);
		Movable loadedVictim = (Movable) restored.getMovableGrid().getMovableAt(target.x, target.y);
		SiegeProjectileObject loadedProjectile = projectile(restored, target);
		assertNotNull(loadedProjectile);
		assertEquals(2, loadedWeapon.getAmmo());
		assertEquals(projectile.getWeaponType(), loadedProjectile.getWeaponType());
		assertEquals(projectile.getHitStrength(), loadedProjectile.getHitStrength(), 0f);
		assertEquals(source, loadedProjectile.getSourcePos());
		assertEquals(target, loadedProjectile.getTargetPos());
		assertEquals(progress, loadedProjectile.getStateProgress(), 0f);
		assertSame(restored.getPartitionsGrid().getPlayer((byte) 0), loadedProjectile.getShooterPlayer());

		// Its scheduled impact must affect the restored grid exactly once, not the original grid.
		float healthBefore = loadedVictim.getHealth();
		MatchConstants.clock().setTime(shotTime + 2000);
		restored.mapObjectsManager.timerEvent();
		if (projectile.getHitStrength() > 0) {
			assertTrue(loadedVictim.getHealth() < healthBefore);
		} else {
			assertEquals("saved misses must remain harmless", healthBefore, loadedVictim.getHealth(), 0f);
		}
		assertEquals(healthBefore, victim.getHealth(), 0f);
		assertNull(projectile(restored, target));
		float healthAfter = loadedVictim.getHealth();
		restored.mapObjectsManager.timerEvent();
		assertEquals(healthAfter, loadedVictim.getHealth(), 0f);

		// A save/load must not make the already-fired weapon ready early.
		for (int elapsed = 2000; elapsed < ESiegeWeaponType.CATAPULT.cooldownMs; elapsed += 100) {
			MatchConstants.clock().setTime(shotTime + elapsed);
			loadedWeapon.timerEvent();
			assertEquals("cooldown was lost while loading", 2, loadedWeapon.getAmmo());
		}
		int deadline = shotTime + ESiegeWeaponType.CATAPULT.cooldownMs + 5000;
		for (int time = shotTime + ESiegeWeaponType.CATAPULT.cooldownMs; time < deadline && loadedWeapon.getAmmo() == 2; time += 100) {
			MatchConstants.clock().setTime(time);
			loadedWeapon.timerEvent();
		}
		assertEquals("weapon did not resume firing after its saved cooldown", 1, loadedWeapon.getAmmo());
	}

	@Test public void completedWeaponWaitsForAnUnprotectedSpawnPosition() {
		SiegeWorkshopBuilding workshop = workshop();
		workshop.orderSiegeWeapon();
		for (EMaterialType material : ESiegeWeaponType.CATAPULT.getWeaponCostMaterials()) {
			for (int i = 0; i < ESiegeWeaponType.CATAPULT.getWeaponCost(material); i++) workshop.weaponMaterialTaken(material);
		}
		ShortPoint2D center = workshop.getAmmoStackPosition();
		ShortPoint2D free = jsettlers.common.map.shapes.HexGridArea.stream(center.x, center.y, 1, SiegeWeaponMovable.RELOAD_RADIUS)
				.filter((x, y) -> original.movablePathfinderGrid.isFreePosition(x, y) && !original.flagsGrid.isProtected(x, y))
				.getFirst().get();
		jsettlers.common.map.shapes.HexGridArea.stream(center.x, center.y, 1, SiegeWeaponMovable.RELOAD_RADIUS)
				.forEach((x, y) -> original.flagsGrid.setProtected(x, y, true));
		for (int i = 0; i < ESiegeWeaponType.ASSEMBLY_STEPS; i++) workshop.workOnWeapon();
		assertEquals(0, weapons(original));
		assertEquals(1, workshop.getOrderedSiegeWeapons());
		original.flagsGrid.setProtected(free.x, free.y, false);
		workshop.workOnWeapon();
		assertEquals(0, workshop.getOrderedSiegeWeapons());
		assertEquals(EMovableType.CATAPULT, original.movableGrid.getMovableAt(free.x, free.y).getMovableType());
	}

	private SiegeWorkshopBuilding workshop() {
		return (SiegeWorkshopBuilding) original.constructBuildingAt(workshopPosition, EBuildingType.CATAPULT_WORKSHOP,
				original.getPartitionsGrid().getPlayer((byte) 0), true);
	}

	private SiegeWorkshopBuilding loadedWorkshop() {
		return (SiegeWorkshopBuilding) restored.getObjectsGrid().getBuildingAt(workshopPosition.x, workshopPosition.y);
	}

	private long weapons(MainGrid grid) {
		return Arrays.stream(grid.getMovableGrid().getMovableArray()).filter(movable -> movable != null && movable.getMovableType() == EMovableType.CATAPULT).count();
	}

	private SiegeProjectileObject projectile(MainGrid grid, ShortPoint2D target) {
		return (SiegeProjectileObject) grid.getObjectsGrid().getMapObjectAt(target.x, target.y, EMapObjectType.SIEGE_PROJECTILE);
	}
}
