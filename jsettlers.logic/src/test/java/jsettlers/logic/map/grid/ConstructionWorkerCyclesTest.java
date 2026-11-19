package jsettlers.logic.map.grid;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import jsettlers.common.ai.EPlayerType;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.map.shapes.HexGridArea;
import jsettlers.common.material.EMaterialType;
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
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.player.Player;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.main.replay.PausedGameStartup;
import jsettlers.main.replay.ReplayUtils;
import jsettlers.testutils.TestUtils;
import jsettlers.testutils.map.MapUtils;
import org.junit.After;
import org.junit.Test;

/** Construction through the real job queues, transport and worker behaviour trees. */
public class ConstructionWorkerCyclesTest {
	private IStartedGame started;
	private MainGrid grid;
	private Player player;
	private int time;

	@After public void shutdown() { if (started != null) ReplayUtils.awaitShutdown(started); }

	@Test public void workersFlattenSupplyAndFinishTwoSuccessiveBuildings() throws Exception {
		start();
		for (int job = 0; job < 2; job++) build(false);
	}

	@Test public void workersCompleteTwoBuildingsThroughNarrowPassagesBetweenStones() throws Exception {
		start();
		for (int job = 0; job < 2; job++) build(true);
	}

	private void start() throws Exception {
		TestUtils.setupTempResourceManager();
		MapLoader map = MapUtils.getMountainlake();
		PlayerSetting[] settings = AiTestUtils.getDefaultPlayerSettings(map.getMaxPlayers());
		settings[0] = new PlayerSetting(EPlayerType.HUMAN, ECivilisation.ROMAN, (byte) 0);
		JSettlersGame.GameRunner runner = AiTestUtils.createStartingGame(settings, map);
		started = PausedGameStartup.await(runner);
		grid = runner.getMainGrid();
		player = grid.partitionsGrid.getPlayer((byte) 0);
	}

	private void build(boolean stones) {
		ShortPoint2D center = Building.getAllBuildings().stream().filter(b -> b.getPlayer() == player).findFirst().get().getPosition();
		ShortPoint2D site = HexGridArea.stream(center.x, center.y, 25, 55).filterBounds(grid.width - 10, grid.height - 10)
			.filter((x,y) -> x > 10 && y > 10 && grid.constructionMarksGrid.canConstructAt(x,y,EBuildingType.FORESTER,(byte)0))
			.filter((x,y) -> !stones || HexGridArea.stream(x,y,10,10).filter((rx,ry) ->
				grid.movablePathfinderGrid.isFreePosition(rx,ry) && !grid.flagsGrid.isProtected(rx,ry)
					&& !grid.movablePathfinderGrid.isWater(rx,ry) && grid.partitionsGrid.getPlayerAt(rx,ry) == player).count() == 60)
			.getFirst().orElseThrow(() -> new AssertionError("No unobstructed construction fixture"));
		// Real stones occupy seven tiles. Rejected overlapping placements leave narrow
		// passages; keep one wider opening as well so the fixture is never a sealed trap.
		ShortPoint2D exit = new ShortPoint2D(site.x - 10, site.y);
		if (stones) HexGridArea.stream(site.x,site.y,10,10).forEach((x,y) -> {
			if (exit.getOnGridDistTo(new ShortPoint2D(x,y)) > 2) grid.mapObjectsManager.addStone(new ShortPoint2D(x,y),10);
		});
		if (stones) assertTrue("Fixture must obstruct most direct approaches",
			HexGridArea.stream(site.x,site.y,10,10).filter((x,y) -> grid.flagsGrid.isBlocked(x,y)).count() > 30);
		Building building = grid.constructBuildingAt(site, EBuildingType.FORESTER, player, false);
		assertFalse(building.isConstructionFinished());
		// Actual uneven ground, not a direct call to the building's material/progress methods.
		Arrays.stream(building.getFlattenTiles()).forEach(p -> {
			ShortPoint2D pos = p.calculatePoint(site);
			grid.landscapeGrid.setHeightAt(pos.x,pos.y,(byte)(30 + (pos.x & 1)));
		});
		Map<Movable, EMaterialType> carrying = new IdentityHashMap<>();
		Set<Movable> participants = Collections.newSetFromMap(new IdentityHashMap<>());
		boolean dug = false, laid = false, delivered = false, passedExit = false;
		for (int elapsed = 0; elapsed < 12 * 60 * 1000 && !building.isConstructionFinished(); elapsed += 200) {
			MatchConstants.clock().fastForwardTo(time += 200);
			for (var entry : MovableManager.getAllMovables()) {
				Movable worker = (Movable) entry;
				if (worker.getPlayer() != player) continue;
				ShortPoint2D pos = worker.getPosition();
				boolean near = pos.getOnGridDistTo(site) < 6;
				if (near && worker.getAction() == EMovableAction.ACTION1) {
					if (worker.getMovableType() == EMovableType.DIGGER) { dug = true; participants.add(worker); }
					if (worker.getMovableType() == EMovableType.BRICKLAYER) { laid = true; participants.add(worker); }
				}
				if (worker.getMovableType() == EMovableType.BEARER) {
					EMaterialType previous = carrying.put(worker,worker.getMaterial());
					if (near && (previous == EMaterialType.PLANK || previous == EMaterialType.STONE)
							&& worker.getMaterial() == EMaterialType.NO_MATERIAL) {
						delivered = true; participants.add(worker);
					}
					if (stones && pos.getOnGridDistTo(site) >= 9 && pos.getOnGridDistTo(site) <= 11
							&& (worker.getMaterial() == EMaterialType.PLANK || worker.getMaterial() == EMaterialType.STONE)
							&& HexGridArea.stream(pos.x,pos.y,1,1).filter((x,y) -> grid.flagsGrid.isBlocked(x,y)).count() >= 2) {
						assertFalse("Bearer stepped onto a stone",grid.flagsGrid.isBlocked(pos.x,pos.y));
						passedExit = true;
					}
				}
			}
			// Job changes replace the Java object while preserving the settler ID.
			for (Movable worker : participants) assertTrue("Construction participant died: " + worker,
				worker.isAlive() || MovableManager.getAllMovables().stream().anyMatch(current -> current.getID() == worker.getID() && current.isAlive()));
		}
		assertTrue("Building did not finish; dug="+dug+", laid="+laid+", delivered="+delivered,building.isConstructionFinished());
		assertTrue("No real digger action",dug);
		assertTrue("No real bricklayer action",laid);
		assertTrue("No bearer delivered construction materials",delivered);
		if (stones) assertTrue("Loaded bearer never traversed a narrow passage between stones",passedExit);
	}
}
