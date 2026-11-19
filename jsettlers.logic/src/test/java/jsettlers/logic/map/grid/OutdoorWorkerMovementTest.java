package jsettlers.logic.map.grid;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import jsettlers.common.ai.EPlayerType;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.landscape.ELandscapeType;
import jsettlers.common.landscape.EResourceType;
import jsettlers.common.map.shapes.HexGridArea;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.material.ESearchType;
import jsettlers.common.menu.IStartedGame;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.integration.ai.AiTestUtils;
import jsettlers.logic.buildings.Building;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.loading.MapLoader;
import jsettlers.logic.movable.Movable;
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

/** Real timed worker movement/production; resource fixtures isolate work from terrain scarcity. */
@RunWith(Parameterized.class)
public class OutdoorWorkerMovementTest {
	@Parameterized.Parameters(name = "{0}") public static Collection<Object[]> workers() {
		return Arrays.asList(new Object[][] {
			{ EMovableType.LUMBERJACK, EBuildingType.LUMBERJACK, EMaterialType.TRUNK, ECivilisation.ROMAN },
			{ EMovableType.STONECUTTER, EBuildingType.STONECUTTER, EMaterialType.STONE, ECivilisation.ROMAN },
			{ EMovableType.FISHERMAN, EBuildingType.FISHER, EMaterialType.FISH, ECivilisation.ROMAN },
			{ EMovableType.FARMER, EBuildingType.FARM, EMaterialType.CROP, ECivilisation.ROMAN },
			{ EMovableType.WATERWORKER, EBuildingType.WATERWORKS, EMaterialType.WATER, ECivilisation.ROMAN },
			{ EMovableType.WINEGROWER, EBuildingType.WINEGROWER, EMaterialType.WINE, ECivilisation.ROMAN },
			{ EMovableType.RICE_FARMER, EBuildingType.RICE_FARM, EMaterialType.RICE, ECivilisation.ASIAN },
			{ EMovableType.BEEKEEPER, EBuildingType.BEEKEEPING, EMaterialType.HONEY, ECivilisation.AMAZON }
		});
	}
	private final EMovableType profession;
	private final EBuildingType buildingType;
	private final EMaterialType product;
	private final ECivilisation civilisation;
	private IStartedGame started;
	private MainGrid grid;
	private int time;

	public OutdoorWorkerMovementTest(EMovableType profession, EBuildingType buildingType, EMaterialType product, ECivilisation civilisation) {
		this.profession = profession;
		this.buildingType = buildingType;
		this.product = product;
		this.civilisation = civilisation;
	}

	@After public void shutdown() {
		if (started != null) ReplayUtils.awaitShutdown(started);
	}

	@Test public void workerLeavesWorksProducesAndReturnsForTwoCycles() throws Exception {
		TestUtils.setupTempResourceManager();
		MapLoader map = MapUtils.getMountainlake();
		PlayerSetting[] settings = AiTestUtils.getDefaultPlayerSettings(map.getMaxPlayers());
		settings[0] = new PlayerSetting(EPlayerType.HUMAN, civilisation, (byte) 0);
		JSettlersGame.GameRunner runner = AiTestUtils.createStartingGame(settings, map);
		started = PausedGameStartup.await(runner);
		grid = runner.getMainGrid();
		Player player = grid.partitionsGrid.getPlayer((byte) 0);
		ShortPoint2D center = Building.getAllBuildings().stream().filter(b -> b.getPlayer() == player).findFirst().get().getPosition();
		ShortPoint2D site = HexGridArea.stream(center.x, center.y, 8, 40).filterBounds(grid.width, grid.height)
				.filter((x, y) -> grid.constructionMarksGrid.canConstructAt(x, y, buildingType, (byte) 0)).getFirst()
				.orElseThrow(() -> new AssertionError("no construction site for " + profession));
		Building house = grid.constructBuildingAt(site, buildingType, player, true);
		ShortPoint2D door = house.getDoor();
		List<ShortPoint2D> resources = HexGridArea.stream(door.x, door.y, 6, 11).filterBounds(grid.width - 2, grid.height - 2)
				.filter((x, y) -> x > 2 && y > 2 && grid.movablePathfinderGrid.isFreePosition(x, y)
						&& !grid.movablePathfinderGrid.isWater(x, y) && !grid.flagsGrid.isProtected(x, y)
						&& grid.partitionsGrid.getPlayerAt(x, y) == player)
				.map(ShortPoint2D::new).collect(Collectors.toList());
		assertTrue("fixture needs resource locations", resources.size() >= 8);
		for (int i = 0; i < 8; i++) prepareResource(resources.get(i));
		ShortPoint2D spawn = HexGridArea.stream(door.x, door.y, 1, 11).filterBounds(grid.width, grid.height)
				.filter((x, y) -> grid.movablePathfinderGrid.isFreePosition(x, y)
						&& !grid.movablePathfinderGrid.isWater(x, y) && !grid.flagsGrid.isProtected(x, y)).getFirst().get();
		Movable worker = Movable.createMovable(profession, player, spawn, grid.movablePathfinderGrid);
		await(worker, "initial entry", () -> worker.getPosition().equals(door));
		for (int cycle = 0; cycle < 2; cycle++) {
			int productionBefore = player.getProductionStatistic().getAmountProduced(product);
			await(worker, "leave for cycle " + cycle, () -> worker.getPosition().getOnGridDistTo(door) > 1);
			await(worker, "work outside for cycle " + cycle, () -> worker.getPosition().getOnGridDistTo(door) > 1
					&& (worker.getAction() == EMovableAction.ACTION1 || worker.getAction() == EMovableAction.ACTION3
						|| worker.getAction() == EMovableAction.BEND_DOWN));
			await(worker, "produce for cycle " + cycle, () -> player.getProductionStatistic().getAmountProduced(product) > productionBefore);
			await(worker, "return for cycle " + cycle, () -> worker.getPosition().equals(door)
					&& worker.getMaterial() == EMaterialType.NO_MATERIAL);
		}
		assertTrue(worker.isAlive());
	}

	private void prepareResource(ShortPoint2D position) {
		switch (profession) {
		case LUMBERJACK: grid.mapObjectsManager.plantAdultTree(position); break;
		case STONECUTTER: grid.mapObjectsManager.addStone(position, 10); break;
		case FISHERMAN:
			// Direct fixture terrain: interactive terrain editing rejects isolated water next to grass.
			grid.landscapeGrid.setLandscapeTypeAt(position.x, position.y, ELandscapeType.WATER1, false);
			grid.flagsGrid.setBlockedAndProtected(position.x, position.y, true);
			grid.landscapeGrid.setResourceAt(position.x, position.y, EResourceType.FISH, (byte) 20); break;
		case WATERWORKER: grid.setLandscapeTypeAt(position.x, position.y, ELandscapeType.RIVER1); break;
		case FARMER: grid.mapObjectsManager.executeSearchType(position, ESearchType.PLANTABLE_CORN, 0f); break;
		case WINEGROWER: grid.mapObjectsManager.executeSearchType(position, ESearchType.PLANTABLE_WINE, 0f); break;
		case RICE_FARMER: grid.mapObjectsManager.executeSearchType(position, ESearchType.PLANTABLE_RICE, 0f); break;
		case BEEKEEPER: grid.mapObjectsManager.executeSearchType(position, ESearchType.PLANTABLE_HIVE, 0f); break;
		default: throw new AssertionError(profession);
		}
	}

	private void await(Movable worker, String phase, BooleanSupplier condition) {
		for (int elapsed = 0; elapsed < 12 * 60000; elapsed += 200) {
			MatchConstants.clock().fastForwardTo(time += 200);
			assertTrue(profession + " died during " + phase, worker.isAlive());
			if (condition.getAsBoolean()) return;
		}
		fail(profession + " timed out during " + phase + "; position=" + worker.getPosition() + ", material=" + worker.getMaterial());
	}
}
