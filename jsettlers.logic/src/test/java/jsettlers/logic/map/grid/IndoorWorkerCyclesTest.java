package jsettlers.logic.map.grid;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import jsettlers.common.ai.EPlayerType;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.buildings.stacks.RelativeStack;
import jsettlers.common.landscape.EResourceType;
import jsettlers.common.map.shapes.HexGridArea;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.material.EPriority;
import jsettlers.common.menu.IStartedGame;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.integration.ai.AiTestUtils;
import jsettlers.logic.buildings.Building;
import jsettlers.logic.buildings.workers.WorkerBuilding;
import jsettlers.logic.buildings.workers.MineBuilding;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.loading.MapLoader;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.movable.civilian.BuildingWorkerMovable;
import jsettlers.logic.player.Player;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.main.replay.PausedGameStartup;
import jsettlers.main.replay.ReplayUtils;
import jsettlers.testutils.TestUtils;
import jsettlers.testutils.map.MapUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Real input pickup, work, output delivery and return cycles; no direct production-method calls. */
@RunWith(Parameterized.class)
public class IndoorWorkerCyclesTest {
	@Parameterized.Parameters(name = "{0}") public static Object[] workplaces() {
		return new EBuildingType[] { EBuildingType.TOOLSMITH, EBuildingType.WEAPONSMITH,
			EBuildingType.SAWMILL, EBuildingType.IRONMELT, EBuildingType.GOLDMELT,
			EBuildingType.COALMINE, EBuildingType.IRONMINE, EBuildingType.GOLDMINE,
			EBuildingType.SULFURMINE, EBuildingType.GEMSMINE, EBuildingType.MILL,
			EBuildingType.BAKER, EBuildingType.PIG_FARM, EBuildingType.DONKEY_FARM,
			EBuildingType.SLAUGHTERHOUSE, EBuildingType.CHARCOAL_BURNER,
			EBuildingType.BREWERY, EBuildingType.DISTILLERY, EBuildingType.LABORATORY,
			EBuildingType.MEAD_BREWERY, EBuildingType.POWDER_MAKER };
	}
	private final EBuildingType type;
	private IStartedGame game;
	private MainGrid grid;
	private Player player;
	private WorkerBuilding building;
	private Movable worker;
	private int time = 1000;
	private boolean leftDoor;
	private boolean inputDecreased;
	private int initialInputs;
	private final Set<EMaterialType> outputs = EnumSet.noneOf(EMaterialType.class);

	public IndoorWorkerCyclesTest(EBuildingType type) { this.type = type; }

	@After public void cleanup() { if (game != null) ReplayUtils.awaitShutdown(game); }

	@Test public void workerCompletesTwoCyclesAndReturnsAlive() throws Exception {
		TestUtils.setupTempResourceManager();
		ECivilisation civilisation = Arrays.stream(ECivilisation.VALUES)
			.filter(c -> type.getVariant(c) != null).findFirst().get();
		MapLoader map = MapUtils.getMountainlake();
		PlayerSetting[] settings = AiTestUtils.getDefaultPlayerSettings(map.getMaxPlayers());
		settings[0] = new PlayerSetting(EPlayerType.HUMAN, civilisation, (byte) 0);
		JSettlersGame.GameRunner runner = AiTestUtils.createStartingGame(settings, map);
		game = PausedGameStartup.await(runner);
		MatchConstants.clock().fastForwardTo(time);
		grid = runner.getMainGrid();
		player = grid.partitionsGrid.getPlayer((byte) 0);
		ShortPoint2D center = Building.getAllBuildings().stream().filter(b -> b.getPlayer() == player).findFirst().get().getPosition();
		// Isolate production statistics from pre-existing producers on the fixture map.
		Building.getAllBuildings().stream().filter(b -> b.getPlayer() == player && b instanceof WorkerBuilding)
			.forEach(b -> ((WorkerBuilding) b).setPriority(EPriority.STOPPED));
		// Mine terrain placement is not under test; use a legal clear footprint and seed its deposit.
		EBuildingType placement = isMine() ? EBuildingType.SAWMILL : type;
		ShortPoint2D site = HexGridArea.stream(center.x, center.y, 8, 45).filterBounds(grid.width, grid.height)
			.filter((x, y) -> grid.constructionMarksGrid.canConstructAt(x, y, placement, (byte) 0))
			.filter((x, y) -> Arrays.stream(type.getVariant(civilisation).getProtectedTiles())
				.map(p -> p.calculatePoint(new ShortPoint2D(x,y))).allMatch(p -> grid.isInBounds(p.x,p.y)
					&& !grid.movablePathfinderGrid.isBlockedOrProtected(p.x,p.y))).getFirst()
			.orElseThrow(() -> new AssertionError("no site for " + type));
		building = (WorkerBuilding) grid.constructBuildingAt(site, type, player, true);
		for (RelativeStack stack : building.getBuildingVariant().getOfferStacks()) outputs.add(stack.getMaterialType());
		if (type == EBuildingType.TOOLSMITH) {
			building.getMaterialProduction().setAbsoluteProductionRequest(EMaterialType.HAMMER, 4);
			outputs.add(EMaterialType.HAMMER);
		} else if (type == EBuildingType.WEAPONSMITH) {
			outputs.addAll(Arrays.asList(EMaterialType.SWORD, EMaterialType.BOW, EMaterialType.SPEAR));
		}
		if (isMine()) {
			EResourceType resource = type == EBuildingType.COALMINE ? EResourceType.COAL
				: type == EBuildingType.IRONMINE ? EResourceType.IRONORE
				: type == EBuildingType.GOLDMINE ? EResourceType.GOLDORE
				: type == EBuildingType.SULFURMINE ? EResourceType.BRIMSTONE : EResourceType.GEMSTONE;
			Arrays.stream(building.getBuildingVariant().getBlockedTiles()).map(p -> p.calculatePoint(site))
				.forEach(p -> grid.landscapeGrid.setResourceAt(p.x, p.y, resource, (byte) 20));
		}
		supplyInputs();
		initialInputs = inputAmount();
		ShortPoint2D door = building.getDoor();
		ShortPoint2D spawn = HexGridArea.stream(door.x, door.y, 1, 12).filterBounds(grid.width, grid.height)
			.filter((x,y) -> grid.movablePathfinderGrid.isFreePosition(x,y) && !grid.movablePathfinderGrid.isWater(x,y)
				&& !grid.flagsGrid.isProtected(x,y)).getFirst().get();
		worker = Movable.createMovable(building.getBuildingVariant().getWorkerType(), player, spawn, grid.movablePathfinderGrid);
		assertTrue("a worker must be assigned", runUntil(() -> assignedWorker().isPresent(), 60000));
		// The map may already contain a closer idle smith/melter. Observe the worker actually assigned by the manager.
		worker = assignedWorker().get();
		assertSame(building, ((BuildingWorkerMovable) worker).getGarrisonedBuilding());
		int initialDeposit = isMine() ? ((MineBuilding) building).getRemainingResourceAmount() : 0;
		// Miners start with ten food credits: eleven deliveries force a real food pickup as well.
		int cycles = isMine() ? 11 : 2;
		for (int cycle = 0; cycle < cycles; cycle++) {
			long before = produced();
			assertTrue(type + " failed output cycle " + cycle + " at " + worker.getPosition(),
				runUntil(() -> produced() > before, type == EBuildingType.DONKEY_FARM ? 12 * 60000 : 4 * 60000));
			// Busy workers intentionally remain outside while more inputs are available.
			building.setPriority(EPriority.STOPPED);
			boolean returned = runUntil(() -> worker.getPosition().equals(door), 60000);
			assertTrue(type + " failed to return: alive=" + worker.isAlive() + " position=" + worker.getPosition() + " door=" + door + " material=" + worker.getMaterial(), returned);
			clearOutputStacks();
			building.setPriority(EPriority.DEFAULT);
		}
		assertTrue("worker must survive", worker.isAlive());
		assertTrue("worker must leave the door to handle materials", leftDoor);
		assertTrue("worker must consume supplied input", inputDecreased);
		if (isMine()) assertTrue("mining must deplete the actual deposit", ((MineBuilding) building).getRemainingResourceAmount() < initialDeposit);
	}

	private Optional<Movable> assignedWorker() {
		return MovableManager.getAllMovables().stream().filter(m -> m instanceof BuildingWorkerMovable
			&& ((BuildingWorkerMovable) m).getGarrisonedBuilding() == building).map(m -> (Movable) m).findFirst();
	}
	private boolean isMine() { return type.name().endsWith("MINE"); }
	private long produced() {
		if (type == EBuildingType.DONKEY_FARM) return MovableManager.getAllMovables().stream()
			.filter(m -> m.isAlive() && m.getPlayer() == player && m.getMovableType() == EMovableType.DONKEY).count();
		return outputs.stream().mapToLong(m -> player.getProductionStatistic().getAmountProduced(m)).sum();
	}
	private boolean runUntil(BooleanSupplier condition, int budget) {
		for (int elapsed = 0; elapsed < budget; elapsed += 200) {
			MatchConstants.clock().fastForwardTo(time += 200);
			if (!worker.isAlive()) return false;
			leftDoor |= !worker.getPosition().equals(building.getDoor()) && time > 10000;
			inputDecreased |= inputAmount() < initialInputs;
			if (condition.getAsBoolean()) return true;
			if (type == EBuildingType.DONKEY_FARM && inputAmount() < 4) supplyInputs();
		}
		return false;
	}
	private void supplyInputs() {
		for (RelativeStack stack : building.getBuildingVariant().getRequestStacks()) {
			ShortPoint2D p = stack.calculatePoint(building.getPosition());
			while (grid.mapObjectsManager.getStackSize(p.x,p.y,stack.getMaterialType()) < 8)
				assertTrue("supply " + stack.getMaterialType(), grid.movablePathfinderGrid.dropMaterial(p,stack.getMaterialType(),false,false));
		}
	}
	private int inputAmount() {
		return Arrays.stream(building.getBuildingVariant().getRequestStacks()).mapToInt(s -> {
			ShortPoint2D p = s.calculatePoint(building.getPosition());
			return grid.mapObjectsManager.getStackSize(p.x,p.y,s.getMaterialType());
		}).sum();
	}
	private void clearOutputStacks() {
		for (RelativeStack stack : building.getBuildingVariant().getOfferStacks()) {
			ShortPoint2D p = stack.calculatePoint(building.getPosition());
			while (grid.movablePathfinderGrid.takeMaterial(p,stack.getMaterialType())) { }
		}
	}
}
