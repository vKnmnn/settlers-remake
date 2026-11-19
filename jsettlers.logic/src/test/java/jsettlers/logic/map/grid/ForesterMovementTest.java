package jsettlers.logic.map.grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.function.BooleanSupplier;
import jsettlers.common.CommonConstants;
import jsettlers.common.ai.EPlayerType;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.map.shapes.HexGridArea;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.menu.IStartedGame;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.integration.ai.AiTestUtils;
import jsettlers.logic.buildings.Building;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.loading.MapLoader;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.player.Player;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.main.replay.ReplayUtils;
import jsettlers.main.replay.PausedGameStartup;
import jsettlers.network.synchronic.timer.NetworkTimer;
import jsettlers.testutils.TestUtils;
import jsettlers.testutils.map.MapUtils;
import org.junit.After;
import org.junit.Test;

public class ForesterMovementTest {
	private MainGrid grid;
	private IStartedGame startedGame;
	private int time;

	@After
	public void cleanup() {
		if (startedGame != null) ReplayUtils.awaitShutdown(startedGame);
		else {
			if (grid != null) grid.stopThreads();
			JSettlersGame.clearState();
		}
	}

	@Test
	public void fleeingForesterCanCrossNeutralGroundWithoutDyingInOneTick() {
		MatchConstants.init(new NetworkTimer(true), 0L);
		grid = new MainGrid("flee", "Flee", (short) 40, (short) 40,
				new PlayerSetting[] { new PlayerSetting((byte) 0), new PlayerSetting((byte) 1) });
		grid.landscapeGrid.generateBlockedPartitions();
		Player player = grid.getPartitionsGrid().getPlayer((byte) 1);
		ShortPoint2D start = new ShortPoint2D(16, 16);
		assertNotSame(player, grid.movablePathfinderGrid.getPlayerAt(start));
		Movable worker = Movable.createMovable(EMovableType.FORESTER, player, start, grid.movablePathfinderGrid);
		worker.timerEvent();
		assertTrue("Neutral ground must not exhaust all flee attempts synchronously", worker.isAlive());
		assertEquals(1, start.getOnGridDistTo(worker.getPosition()));
		assertSame(worker, grid.movableGrid.getMovableAt(worker.getPosition().x, worker.getPosition().y));
	}

	@Test
	public void foresterLeavesPlantsAndReturnsAliveForThreeWorkCycles() throws Exception {
		TestUtils.setupTempResourceManager();
		CommonConstants.ENABLE_CONSOLE_LOGGING = true;
		MapLoader map = MapUtils.getMountainlake();
		PlayerSetting[] settings = AiTestUtils.getDefaultPlayerSettings(map.getMaxPlayers());
		settings[0] = new PlayerSetting(EPlayerType.HUMAN, ECivilisation.ROMAN, (byte) 0);
		JSettlersGame.GameRunner game = AiTestUtils.createStartingGame(settings, map);
		startedGame = PausedGameStartup.await(game);
		time = 1000;
		MatchConstants.clock().fastForwardTo(time);
		grid = game.getMainGrid();
		Player player = grid.partitionsGrid.getPlayer((byte) 0);
		ShortPoint2D center = Building.getAllBuildings().stream().filter(b -> b.getPlayer() == player).findFirst().get().getPosition();
		ShortPoint2D site = HexGridArea.stream(center.x, center.y, 8, 40).filterBounds(grid.width, grid.height)
				.filter((x, y) -> grid.constructionMarksGrid.canConstructAt(x, y, EBuildingType.FORESTER, (byte) 0)).getFirst().get();
		Building house = grid.constructBuildingAt(site, EBuildingType.FORESTER, player, true);
		ShortPoint2D door = house.getDoor();
		ShortPoint2D spawn = HexGridArea.stream(door.x, door.y, 1, 11).filterBounds(grid.width, grid.height)
				.filter((x, y) -> grid.movablePathfinderGrid.isFreePosition(x, y)
						&& !grid.movablePathfinderGrid.isWater(x, y) && !grid.flagsGrid.isProtected(x, y)).getFirst().get();
		Movable worker = Movable.createMovable(EMovableType.FORESTER, player, spawn, grid.movablePathfinderGrid);
		for (int cycle = 0; cycle < 3; cycle++) {
			assertTrue("Worker must leave carrying a tree", runUntil(worker, () -> worker.getMaterial() == EMaterialType.TREE && !worker.getPosition().equals(door)));
			assertTrue("Worker must plant and return alive", runUntil(worker, () -> worker.getMaterial() == EMaterialType.NO_MATERIAL && worker.getPosition().equals(door)));
		}
	}

	private boolean runUntil(Movable worker, BooleanSupplier condition) {
		for (int elapsed = 0; elapsed < 120000; elapsed += 200) {
			MatchConstants.clock().fastForwardTo(time += 200);
			if (!worker.isAlive()) return false;
			if (condition.getAsBoolean()) return true;
		}
		return false;
	}
}
