package jsettlers.integration.playthrough;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import jsettlers.common.ai.EPlayerType;
import jsettlers.common.menu.IStartedGame;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.player.EWinState;
import jsettlers.integration.ai.AiTestUtils;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.grid.MainGrid;
import jsettlers.logic.map.loading.MapLoader;
import jsettlers.logic.player.Player;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.main.replay.ReplayUtils;
import jsettlers.main.replay.PausedGameStartup;
import jsettlers.testutils.TestUtils;
import jsettlers.testutils.map.MapUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

/** Opt-in real AI matches, deliberately excluded from ordinary test and unitTest tasks. */
@RunWith(Parameterized.class)
public class FourCivilisationsIT {
	private static final int MINUTE = 60000;
	private static final byte STRONG = 9, WEAK = 7;
	private final ECivilisation civilisation;

	@Parameterized.Parameters(name = "{0}") public static Object[] civilisations() {
		String selected = System.getProperty("playthrough.civilisation");
		return selected == null ? ECivilisation.VALUES : new Object[] { ECivilisation.valueOf(selected) };
	}
	public FourCivilisationsIT(ECivilisation civilisation) { this.civilisation = civilisation; }

	@Test(timeout = 2700000) public void completeMatchWithSaveLoad() throws Exception {
		jsettlers.common.CommonConstants.ENABLE_CONSOLE_LOGGING = false;
		TestUtils.setupTempResourceManager();
		int maximumMinutes = Integer.getInteger("playthrough.minutes", 120);
		int checkpointMinutes = Integer.getInteger("playthrough.checkpoint", 10);
		assertTrue("checkpoint must precede match limit", checkpointMinutes > 0 && checkpointMinutes < maximumMinutes);
		PlayerSetting[] settings = AiTestUtils.getDefaultPlayerSettings(12);
		settings[STRONG] = new PlayerSetting(EPlayerType.AI_HARD, civilisation, (byte) 0);
		settings[WEAK] = new PlayerSetting(EPlayerType.AI_EASY, civilisation, (byte) 1);
		JSettlersGame.GameRunner runner = AiTestUtils.createStartingGame(settings, MapUtils.getSpezialSumpf());
		IStartedGame started = PausedGameStartup.await(runner);
		boolean resumed = false;
		try {
			for (int minute = 1; minute <= maximumMinutes; minute++) {
				MatchConstants.clock().fastForwardTo(minute * MINUTE);
				MainGrid grid = runner.getMainGrid();
				Player strong = grid.getPartitionsGrid().getPlayer(STRONG);
				Player weak = grid.getPartitionsGrid().getPlayer(WEAK);
				report(minute, strong, weak, resumed);
				if (minute == checkpointMinutes) {
					long score = strong.getEndgameStatistic().getScore();
					long soldiers = strong.getEndgameStatistic().getSoldiersRecruited();
					MapLoader save = MapUtils.saveMainGrid(grid, STRONG, null);
					assertNotNull("checkpoint save failed", save);
					System.out.println("PLAYTHROUGH checkpoint " + save.getListedMap().getFile());
					ReplayUtils.awaitShutdown(started);
					started = null;
					runner = AiTestUtils.createStartingGame(settings, save);
					started = PausedGameStartup.await(runner);
					MatchConstants.clock().setPausing(true);
					Player loaded = runner.getMainGrid().getPartitionsGrid().getPlayer(STRONG);
					assertEquals(civilisation, loaded.getCivilisation());
					assertEquals("score at checkpoint", score, loaded.getEndgameStatistic().getScore());
					assertEquals("recruitment at checkpoint", soldiers, loaded.getEndgameStatistic().getSoldiersRecruited());
					resumed = true;
				}
				if (strong.getWinState() != EWinState.UNDECIDED || weak.getWinState() != EWinState.UNDECIDED) {
					assertTrue("match must exercise save/load before finishing", resumed);
					assertEquals("strong AI should win established fixture", EWinState.WON, strong.getWinState());
					assertEquals(EWinState.LOST, weak.getWinState());
					assertTrue(strong.getEndgameStatistic().isComplete());
					assertTrue("real economic activity required", strong.getEndgameStatistic().getBuildingMaterialsUsed() > 0);
					assertTrue("real recruitment required", strong.getEndgameStatistic().getSoldiersRecruited() > 0);
					assertTrue("nonempty score required", strong.getEndgameStatistic().getScore() > 0);
					return;
				}
			}
			MapLoader diagnostic = MapUtils.saveMainGrid(runner.getMainGrid(), STRONG, null);
			fail(civilisation + " did not finish by " + maximumMinutes + " game minutes; diagnostic save: "
					+ (diagnostic == null ? "FAILED" : diagnostic.getListedMap().getFile()));
		} finally {
			if (started != null) ReplayUtils.awaitShutdown(started);
		}
	}

	private void report(int minute, Player strong, Player weak, boolean resumed) throws Exception {
		String row = civilisation + "\t" + minute + "\t" + resumed + "\t" + strong.getWinState() + "\t" + weak.getWinState()
				+ "\t" + strong.getEndgameStatistic().getSoldiersRecruited() + "\t" + weak.getEndgameStatistic().getSoldiersRecruited()
				+ "\t" + strong.getEndgameStatistic().getScore() + "\n";
		System.out.print("PLAYTHROUGH " + row);
		Path file = Paths.get(System.getProperty("playthrough.report", "build/reports/playthrough/progress.tsv"));
		Files.createDirectories(file.toAbsolutePath().getParent());
		Files.write(file, row.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
	}
}
