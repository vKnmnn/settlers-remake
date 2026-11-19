package jsettlers.logic.map.grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.function.BooleanSupplier;

import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.buildings.stacks.RelativeStack;
import jsettlers.common.landscape.ELandscapeType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.movable.EShipType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.workers.DockyardBuilding;
import jsettlers.logic.buildings.workers.WorkerBuilding;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.movable.civilian.BuildingWorkerMovable;
import jsettlers.logic.movable.other.AttackableHumanMovable;
import jsettlers.logic.player.Player;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.network.synchronic.timer.NetworkTimer;
import jsettlers.testutils.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Real worker behavior on a deterministic owned land/coast fixture; assignment is explicit. */
public class WorkerServicesTest {
	private MainGrid grid;
	private Player player;
	private int time;

	@Before
	public void setup() {
		TestUtils.setupTempResourceManager();
		MatchConstants.init(new NetworkTimer(true), 0L);
		grid = new MainGrid("services", "Services", (short) 64, (short) 64,
				new PlayerSetting[] { new PlayerSetting((byte) 0), new PlayerSetting((byte) 1) });
		player = grid.getPartitionsGrid().getPlayer((byte) 0);
		for (int y = 0; y < grid.height; y++) for (int x = 40; x < grid.width; x++) {
			grid.landscapeGrid.setLandscapeTypeAt(x, y, ELandscapeType.WATER8, false);
		}
		grid.landscapeGrid.generateBlockedPartitions();
		for (int y = 1; y < grid.height - 1; y++) for (int x = 1; x < 40; x++) {
			grid.partitionsGrid.changePlayerAt(x, y, (byte) 0);
		}
	}

	@After
	public void cleanup() {
		if (grid != null) grid.stopThreads();
		JSettlersGame.clearState();
	}

	@Test
	public void healerTreatsAWoundedSoldierThroughItsRealWorkCycle() {
		WorkerBuilding hospital = (WorkerBuilding) grid.constructBuildingAt(new ShortPoint2D(20, 20), EBuildingType.HOSPITAL, player, true);
		BuildingWorkerMovable healer = assign(EMovableType.HEALER, hospital);
		ShortPoint2D healSpot = hospital.getBuildingVariant().getHealSpot().calculatePoint(hospital.getPosition());
		AttackableHumanMovable patient = (AttackableHumanMovable) Movable.createMovable(EMovableType.SWORDSMAN_L1, player,
				new ShortPoint2D(healSpot.x + 3, healSpot.y), grid.movablePathfinderGrid);
		patient.receiveHit(50, new ShortPoint2D(50, 50), grid.getPartitionsGrid().getPlayer((byte) 1));
		assertTrue(patient.needsTreatment());
		assertTrue("Healer must find, summon and treat the injured soldier", runUntil(
				() -> !patient.needsTreatment(), 60000, healer, patient));
		assertTrue(healer.isAlive());
		assertTrue(patient.isAlive());
		assertEquals(EMovableType.SWORDSMAN_L1.getHealth(), patient.getHealth(), 0.001f);
		assertTrue(patient.getPosition().getOnGridDistTo(healSpot) <= 5);
	}

	@Test
	public void dockworkerConsumesMaterialsAndBuildsAFerry() {
		buildShip(EShipType.FERRY);
	}

	@Test
	public void dockworkerConsumesMaterialsAndBuildsACargoShip() {
		buildShip(EShipType.CARGO_SHIP);
	}

	private void buildShip(EShipType type) {
		DockyardBuilding dockyard = (DockyardBuilding) grid.constructBuildingAt(new ShortPoint2D(30, 20), EBuildingType.DOCKYARD, player, true);
		BuildingWorkerMovable worker = assign(EMovableType.DOCKWORKER, dockyard);
		dockyard.setDock(new ShortPoint2D(42, 24));
		assertNotNull("Fixture must have an accessible actual dock", dockyard.getDock());
		dockyard.orderShipType(type);
		assertEquals(type, dockyard.getOrderedShipType());
		for (RelativeStack stack : dockyard.getBuildingVariant().getRequestStacks()) {
			for (int i = 0; i < type.getRequiredMaterial(stack.getMaterialType()); i++) {
				assertTrue(grid.movablePathfinderGrid.dropMaterial(stack.calculatePoint(dockyard.getPosition()), stack.getMaterialType(), false, false));
			}
		}
		assertTrue("Dockworker must complete its actual carrying and building cycles", runUntil(
				() -> countShips(type.movableType) == 1, 240000, worker));
		assertTrue(worker.isAlive());
		assertEquals(1, countShips(type.movableType));
		assertNull(dockyard.getOrderedShipType());
		for (RelativeStack stack : dockyard.getBuildingVariant().getRequestStacks()) {
			ShortPoint2D position = stack.calculatePoint(dockyard.getPosition());
			assertEquals("Ordered construction materials must be consumed", 0,
					grid.mapObjectsManager.getStackSize(position.x, position.y, stack.getMaterialType()));
		}
		MovableManager.getAllMovables().stream().filter(m -> m.getMovableType() == type.movableType).forEach(ship -> {
			assertTrue(ship.isAlive());
			assertTrue(grid.movablePathfinderGrid.isWater(ship.getPosition().x, ship.getPosition().y));
		});
	}

	private long countShips(EMovableType type) {
		return MovableManager.getAllMovables().stream().filter(m -> m.getMovableType() == type && m.isAlive()).count();
	}

	private BuildingWorkerMovable assign(EMovableType type, WorkerBuilding building) {
		BuildingWorkerMovable worker = (BuildingWorkerMovable) Movable.createMovable(type, player, building.getDoor(), grid.movablePathfinderGrid);
		worker.setWorkerJob(building);
		assertTrue(building.isOccupied());
		return worker;
	}

	private boolean runUntil(BooleanSupplier condition, int duration, Movable... units) {
		for (int elapsed = 0; elapsed < duration; elapsed += 200) {
			MatchConstants.clock().fastForwardTo(time += 200);
			for (Movable unit : units) {
				assertTrue("Worker/patient must survive", unit.isAlive());
				unit.timerEvent();
			}
			if (condition.getAsBoolean()) return true;
		}
		return false;
	}
}
