package jsettlers.logic.map.grid;

import static org.junit.Assert.*;
import java.util.Arrays;
import jsettlers.common.action.EMoveToType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.movable.interfaces.ILogicMovable;
import jsettlers.logic.movable.military.SiegeWeaponMovable;
import jsettlers.logic.movable.military.InfantryMovable;
import jsettlers.logic.movable.other.FerryMovable;
import jsettlers.logic.player.Player;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.network.synchronic.timer.NetworkTimer;
import jsettlers.testutils.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SiegeFootprintTest {
	private MainGrid grid;
	private MainGrid loaded;
	private Player player;
	private static final ShortPoint2D CENTER = new ShortPoint2D(16, 16);

	@Before public void setup() {
		MatchConstants.init(new NetworkTimer(true), 0L);
		grid = new MainGrid("footprint", "Footprint", (short) 40, (short) 40,
				new PlayerSetting[] { new PlayerSetting((byte) 0), new PlayerSetting((byte) 1) });
		grid.landscapeGrid.generateBlockedPartitions();
		player = grid.getPartitionsGrid().getPlayer((byte) 0);
	}

	@After public void cleanup() {
		if (grid != null) grid.stopThreads();
		if (loaded != null) loaded.stopThreads();
		JSettlersGame.clearState();
	}

	@Test public void oneEntityReservesSevenCellsAndMovesInAllDirections() {
		Movable weapon = weapon(CENTER);
		assertEquals(1, Arrays.stream(grid.movableGrid.getMovableArray()).filter(m -> m == weapon).count());
		assertEquals(1, MovableManager.getAllMovables().stream().filter(m -> m == weapon).count());
		assertFootprint(grid, weapon, CENTER);
		for (EDirection direction : EDirection.VALUES) {
			ShortPoint2D next = direction.getNextHexPoint(CENTER);
			weapon.setPosition(next);
			assertEquals(next, weapon.getPosition());
			assertFootprint(grid, weapon, next);
			assertNull(grid.movableGrid.getOccupantAt(direction.getInverseDirection().getNextHexPoint(CENTER).x,
					direction.getInverseDirection().getNextHexPoint(CENTER).y));
			weapon.setPosition(CENTER);
		}
	}

	@Test public void peripheralBlockerRejectsWholeStepWithoutOverwritingAnything() {
		Movable weapon = weapon(CENTER);
		ShortPoint2D next = new ShortPoint2D(17, 16);
		Movable blocker = Movable.createMovable(EMovableType.SWORDSMAN_L1, player, new ShortPoint2D(18, 16), grid.movablePathfinderGrid);
		assertFalse(grid.movablePathfinderGrid.canOccupy(weapon, next));
		assertFalse(grid.movablePathfinderGrid.tryMovePosition(weapon, CENTER, next));
		weapon.setPosition(next);
		assertEquals(CENTER, weapon.getPosition());
		assertFootprint(grid, weapon, CENTER);
		assertSame(blocker, grid.movableGrid.getMovableAt(18, 16));
	}

	@Test public void footprintsCannotOverlapAndSingleUnitsCannotEnterTheirRing() {
		Movable first = weapon(CENTER);
		Movable second = weapon(new ShortPoint2D(20, 16));
		second.setPosition(new ShortPoint2D(18, 16));
		assertEquals(new ShortPoint2D(20, 16), second.getPosition());
		Movable person = Movable.createMovable(EMovableType.SWORDSMAN_L1, player, new ShortPoint2D(16, 19), grid.movablePathfinderGrid);
		person.setPosition(new ShortPoint2D(16, 17));
		assertEquals(new ShortPoint2D(16, 19), person.getPosition());
		assertFalse(grid.movablePathfinderGrid.tryMovePosition(person, person.getPosition(), new ShortPoint2D(16, 17)));
		assertFootprint(grid, first, CENTER);
	}

	@Test public void edgeAndPeripheralTerrainRejectSpawningAndPaths() {
		assertFalse(grid.movablePathfinderGrid.canSpawnAt(EMovableType.CATAPULT, player, new ShortPoint2D(0, 4)));
		grid.flagsGrid.setBlockedAndProtected((short) 18, (short) 16, true, true);
		assertFalse(grid.movablePathfinderGrid.canSpawnAt(EMovableType.CATAPULT, player, new ShortPoint2D(17, 16)));
		Movable weapon = weapon(CENTER);
		assertFalse(grid.movablePathfinderGrid.isValidPosition(weapon, 17, 16));
		assertNull(grid.movablePathfinderGrid.calculatePathTo(weapon, new ShortPoint2D(17, 16)));
	}

	@Test public void hiddenDeadAndReplacedWeaponsReleaseOnlyTheirOwnReservation() {
		SiegeWeaponMovable weapon = (SiegeWeaponMovable) weapon(CENTER);
		weapon.setVisible(false);
		assertTrue(grid.movableGrid.hasNoMovableAt(17, 16));
		weapon.setVisible(true);
		assertFootprint(grid, weapon, CENTER);
		int id = weapon.getID();
		weapon.defectTo(grid.getPartitionsGrid().getPlayer((byte) 1));
		ILogicMovable replacement = MovableManager.getMovableByID(id);
		assertNotSame(weapon, replacement);
		assertFootprint(grid, replacement, CENTER);
		replacement.kill();
		assertTrue(grid.movableGrid.hasNoMovableAt(17, 16));
		assertNull(grid.movableGrid.getMovableAt(16, 16));
	}

	@Test public void saveLoadRebuildsReservationsWithoutDuplicateUnits() throws Exception {
		Movable weapon = weapon(CENTER);
		loaded = TestUtils.serializeAndDeserialize(grid);
		ILogicMovable restored = loaded.movableGrid.getMovableAt(16, 16);
		assertEquals(weapon.getID(), restored.getID());
		assertEquals(1, Arrays.stream(loaded.movableGrid.getMovableArray()).filter(m -> m != null).count());
		assertFootprint(loaded, restored, CENTER);
	}

	@Test public void legacyOneCellSaveRelocatesConflictsDeterministically() throws Exception {
		Movable weapon = weapon(CENTER);
		grid.movableGrid.clearFootprintReservations(); // emulate an old one-cell save
		Movable person = Movable.createMovable(EMovableType.SWORDSMAN_L1, player, new ShortPoint2D(17, 16), grid.movablePathfinderGrid);
		loaded = TestUtils.serializeAndDeserialize(grid);
		ILogicMovable restoredPerson = loaded.movableGrid.getMovableAt(17, 16);
		assertEquals(person.getID(), restoredPerson.getID());
		ILogicMovable restoredWeapon = Arrays.stream(loaded.movableGrid.getMovableArray()).filter(m -> m != null && m.getID() == weapon.getID()).findFirst().get();
		assertNotEquals(CENTER, restoredWeapon.getPosition());
		assertFootprint(loaded, restoredWeapon, restoredWeapon.getPosition());
		MainGrid second = TestUtils.serializeAndDeserialize(grid);
		try {
			ILogicMovable secondWeapon = Arrays.stream(second.movableGrid.getMovableArray()).filter(m -> m != null && m.getID() == weapon.getID()).findFirst().get();
			assertEquals(restoredWeapon.getPosition(), secondWeapon.getPosition());
		} finally { second.stopThreads(); }
	}

	@Test public void swordsmanCanAttackAtTheEdgeOfTheFootprint() {
		SiegeWeaponMovable weapon = (SiegeWeaponMovable) weapon(CENTER);
		TestInfantry infantry = new TestInfantry(grid.getPartitionsGrid().getPlayer((byte) 1), new ShortPoint2D(18, 16));
		assertTrue(infantry.canAttack(weapon));
		infantry.setPosition(new ShortPoint2D(19, 16));
		assertFalse(infantry.canAttack(weapon));
	}

	@Test public void ferryLoadingAndUnloadingRestoresTheCompleteFootprint() {
		SiegeWeaponMovable weapon = (SiegeWeaponMovable) weapon(CENTER);
		FerryMovable ferry = (FerryMovable) Movable.createMovable(EMovableType.FERRY, player, new ShortPoint2D(18, 16), grid.movablePathfinderGrid);
		weapon.moveToFerry(ferry, CENTER);
		weapon.timerEvent();
		assertTrue(weapon.isOnFerry());
		assertTrue(grid.movableGrid.hasNoMovableAt(16, 16));
		assertTrue(grid.movableGrid.hasNoMovableAt(17, 16));
		ferry.unloadFerry();
		assertFalse(weapon.isOnFerry());
		assertTrue(ferry.getPassengers().isEmpty());
		assertFootprint(grid, weapon, weapon.getPosition());
	}

	@Test public void realPathStepKeepsOwnOverlapAndSerializesDuringWalking() throws Exception {
		SiegeWeaponMovable weapon = (SiegeWeaponMovable) weapon(CENTER);
		ShortPoint2D next = new ShortPoint2D(17, 16);
		weapon.moveTo(next, EMoveToType.FORCED);
		weapon.timerEvent();
		assertEquals(next, weapon.getPosition());
		assertEquals(jsettlers.common.movable.EMovableAction.WALKING, weapon.getAction());
		loaded = TestUtils.serializeAndDeserialize(grid);
		assertFootprint(loaded, loaded.movableGrid.getMovableAt(17, 16), next);
	}

	@Test public void noSafeLegacyLandingFailsExplicitlyInsteadOfOverwriting() throws Exception {
		weapon(CENTER);
		grid.movableGrid.clearFootprintReservations();
		for (short y = 0; y < grid.height; y++) for (short x = 0; x < grid.width; x++) {
			grid.flagsGrid.setBlockedAndProtected(x, y, true, true);
		}
		try {
			loaded = TestUtils.serializeAndDeserialize(grid);
			fail("An impossible legacy footprint must not silently overwrite occupants");
		} catch (java.io.InvalidObjectException expected) {
			assertTrue(expected.getMessage().contains("No safe seven-cell position"));
		}
	}

	@Test public void ferryWaitsWhenOnlyAnIsolatedLandingCellIsFree() {
		SiegeWeaponMovable weapon = (SiegeWeaponMovable) weapon(CENTER);
		FerryMovable ferry = (FerryMovable) Movable.createMovable(EMovableType.FERRY, player, new ShortPoint2D(18, 16), grid.movablePathfinderGrid);
		weapon.moveToFerry(ferry, CENTER);
		weapon.timerEvent();
		assertTrue(weapon.isOnFerry());
		for (short y = 0; y < grid.height; y++) for (short x = 0; x < grid.width; x++) {
			grid.flagsGrid.setBlockedAndProtected(x, y, true, true);
		}
		grid.flagsGrid.setBlockedAndProtected((short) 20, (short) 16, false, false);
		ferry.unloadFerry();
		assertTrue(weapon.isOnFerry());
		assertEquals(1, ferry.getPassengers().size());
		assertNull(grid.movableGrid.getMovableAt(20, 16));
	}

	@Test public void teleportSearchRejectsReservedPeripheralTiles() {
		weapon(CENTER);
		Movable priest = Movable.createMovable(EMovableType.MAGE, player, new ShortPoint2D(24, 24), grid.movablePathfinderGrid);
		assertFalse(grid.movablePathfinderGrid.fitsSearchType(priest, 17, 16, jsettlers.common.material.ESearchType.NON_BLOCKED_OR_PROTECTED));
		assertTrue(grid.movablePathfinderGrid.fitsSearchType(priest, 19, 16, jsettlers.common.material.ESearchType.NON_BLOCKED_OR_PROTECTED));
	}

	@Test public void newReinforcementCannotReplaceASiegeFootprint() {
		Movable weapon = weapon(CENTER);
		Movable newcomer = Movable.createMovable(EMovableType.SWORDSMAN_L1, player, new ShortPoint2D(17, 16), grid.movablePathfinderGrid);
		assertTrue(newcomer.getPosition().getOnGridDistTo(CENTER) > 1);
		assertFootprint(grid, weapon, CENTER);
		assertSame(newcomer, grid.movableGrid.getMovableAt(newcomer.getPosition().x, newcomer.getPosition().y));
	}

	@Test public void blockedReinforcementWaitsInsteadOfOverwritingOrCrashing() {
		Movable weapon = weapon(CENTER);
		for (short y = 0; y < grid.height; y++) for (short x = 0; x < grid.width; x++) {
			grid.flagsGrid.setBlockedAndProtected(x, y, true, true);
		}
		Movable newcomer = Movable.createMovable(EMovableType.SWORDSMAN_L1, player, new ShortPoint2D(17, 16), grid.movablePathfinderGrid);
		newcomer.timerEvent();
		assertTrue(newcomer.isAlive());
		assertFootprint(grid, weapon, CENTER);
		grid.flagsGrid.setBlockedAndProtected((short) 19, (short) 16, false, false);
		newcomer.timerEvent();
		assertSame(newcomer, grid.movableGrid.getMovableAt(19, 16));
	}

	@Test public void aHiddenWorkerWaitsUntilTheSiegeRingHasMovedAway() {
		Movable worker = Movable.createMovable(EMovableType.SIEGE_ENGINEER, player, new ShortPoint2D(17, 16), grid.movablePathfinderGrid);
		worker.setVisible(false);
		Movable weapon = weapon(CENTER);
		worker.setVisible(true);
		assertFootprint(grid, weapon, CENTER);
		weapon.setPosition(new ShortPoint2D(12, 16));
		worker.setVisible(true);
		assertSame(worker, grid.movableGrid.getMovableAt(17, 16));
	}

	@Test public void pathfinderRejectsACorridorThatOnlyFitsSingleUnits() {
		for (short y = 0; y < grid.height; y++) {
			if (y != 16) grid.flagsGrid.setBlockedAndProtected((short) 20, y, true, true);
		}
		Movable weapon = weapon(CENTER);
		assertNull(grid.movablePathfinderGrid.calculatePathTo(weapon, new ShortPoint2D(25, 16)));
		Movable infantry = Movable.createMovable(EMovableType.SWORDSMAN_L1, player, new ShortPoint2D(16, 20), grid.movablePathfinderGrid);
		assertNotNull(grid.movablePathfinderGrid.calculatePathTo(infantry, new ShortPoint2D(25, 16)));
	}

	private Movable weapon(ShortPoint2D position) {
		return Movable.createMovable(EMovableType.CATAPULT, player, position, grid.movablePathfinderGrid);
	}

	private void assertFootprint(MainGrid targetGrid, ILogicMovable weapon, ShortPoint2D center) {
		assertSame(weapon, targetGrid.movableGrid.getMovableAt(center.x, center.y));
		for (EDirection direction : EDirection.VALUES) {
			ShortPoint2D point = direction.getNextHexPoint(center);
			assertNull(targetGrid.movableGrid.getMovableAt(point.x, point.y));
			assertSame(weapon, targetGrid.movableGrid.getOccupantAt(point.x, point.y));
			assertFalse(targetGrid.movablePathfinderGrid.isFreePosition(point.x, point.y));
		}
	}

	private class TestInfantry extends InfantryMovable {
		TestInfantry(Player player, ShortPoint2D position) { super(SiegeFootprintTest.this.grid.movablePathfinderGrid, EMovableType.SWORDSMAN_L1, position, player, null); }
		boolean canAttack(SiegeWeaponMovable target) { enemy = target; return isEnemyAttackable(); }
	}
}
