package jsettlers.logic.map.grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.player.EWinState;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.loading.newmap.FreshMapLoader;
import jsettlers.logic.map.loading.original.OriginalMultiPlayerWinLoseHandler;
import jsettlers.logic.map.loading.savegame.SavegameLoader;
import jsettlers.logic.player.MatchRules;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.network.synchronic.timer.NetworkTimer;
import jsettlers.testutils.TestUtils;

public class FreeplayRulesTest {
	private MainGrid grid;
	private MainGrid restored;

	@Before public void setup() {
		MatchConstants.init(new NetworkTimer(true), 0L);
		grid = new MainGrid("rules", "Rules", (short) 64, (short) 64,
			new PlayerSetting[] { new PlayerSetting((byte) 0), new PlayerSetting((byte) 1) });
		grid.constructBuildingAt(new ShortPoint2D(32,32), EBuildingType.TOWER, grid.partitionsGrid.getPlayer((byte) 0), true);
	}

	@After public void cleanup() {
		if (grid != null) grid.stopThreads();
		if (restored != null) restored.stopThreads();
		JSettlersGame.clearState();
	}

	@Test public void freeplayStillDefeatsPlayersWithoutAwardingLastSurvivorVictory() {
		grid.setMatchRules(new MatchRules(MatchRules.Mode.FREEPLAY));
		new OriginalMultiPlayerWinLoseHandler(grid).timerEvent();
		assertEquals(EWinState.LOST, grid.partitionsGrid.getPlayer((byte) 1).getWinState());
		assertEquals(EWinState.UNDECIDED, grid.partitionsGrid.getPlayer((byte) 0).getWinState());
		grid.setMatchRules(MatchRules.DEFAULT);
		new OriginalMultiPlayerWinLoseHandler(grid).timerEvent();
		assertEquals(EWinState.WON, grid.partitionsGrid.getPlayer((byte) 0).getWinState());
	}

	@Test public void runningGridPersistsRulesAndLegacyGridDefaultsToMilitary() throws Exception {
		assertEquals(MatchRules.DEFAULT, grid.getMatchRules());
		MatchRules rules = new MatchRules(MatchRules.Mode.FREEPLAY);
		grid.setMatchRules(rules);
		restored = TestUtils.serializeAndDeserialize(grid);
		assertEquals(rules, restored.getMatchRules());
	}

	@Test public void savesDoNotAcceptNewGameDefaults() {
		assertFalse(new SavegameLoader(null, null).supportsCustomMatchRules());
		assertTrue(new FreshMapLoader(null, null).supportsCustomMatchRules());
	}
}
