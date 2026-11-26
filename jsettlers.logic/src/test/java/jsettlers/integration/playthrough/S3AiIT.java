package jsettlers.integration.playthrough;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jsettlers.ai.highlevel.AiStatistics;
import jsettlers.common.ai.EPlayerType;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.menu.IStartedGame;
import jsettlers.common.player.ECivilisation;
import jsettlers.integration.ai.AiTestUtils;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.loading.MapLoader;
import jsettlers.logic.player.Player;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.main.replay.PausedGameStartup;
import jsettlers.main.replay.ReplayUtils;
import jsettlers.testutils.TestUtils;
import jsettlers.testutils.map.MapUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

/** Both S3 profiles build a real settlement and resume with the same profile after loading. */
@RunWith(Parameterized.class)
public class S3AiIT {
    @Parameterized.Parameters(name = "{0}") public static Object[] civilisations() { return ECivilisation.VALUES; }
    private final ECivilisation civilisation;
    public S3AiIT(ECivilisation civilisation) { this.civilisation = civilisation; }

    @Test(timeout = 600000) public void buildRecruitAndResumeBothProfiles() throws Exception {
        jsettlers.common.CommonConstants.ENABLE_CONSOLE_LOGGING = false;
        jsettlers.logic.constants.Constants.FOG_OF_WAR_DEFAULT_ENABLED = false;
        TestUtils.setupTempResourceManager();
        PlayerSetting[] settings = AiTestUtils.getDefaultPlayerSettings(12);
        settings[9] = new PlayerSetting(EPlayerType.AI_S3_NORMAL, civilisation, (byte)0);
        settings[7] = new PlayerSetting(EPlayerType.AI_S3_EASY, civilisation, (byte)0);
        // An unchanged JSettlers player in the same match also exercises mixed-profile scheduling.
        settings[0] = new PlayerSetting(EPlayerType.AI_EASY, civilisation, (byte)0);
        JSettlersGame.GameRunner runner = AiTestUtils.createStartingGame(settings, MapUtils.getSpezialSumpf());
        IStartedGame game = PausedGameStartup.await(runner);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            MatchConstants.clock().fastForwardTo(10 * 60000);
            MapLoader save = MapUtils.saveMainGrid(runner.getMainGrid(), (byte)9, null);
            assertNotNull(save);
            ReplayUtils.awaitShutdown(game);
            game = null;
            runner = AiTestUtils.createStartingGame(settings, save);
            game = PausedGameStartup.await(runner);
            assertSame(EPlayerType.AI_S3_NORMAL, runner.getMainGrid().getPartitionsGrid().getPlayer(9).getPlayerType());
            assertSame(EPlayerType.AI_S3_EASY, runner.getMainGrid().getPartitionsGrid().getPlayer(7).getPlayerType());
            MatchConstants.clock().fastForwardTo(60 * 60000);
            AiStatistics statistics = new AiStatistics(runner.getMainGrid(), pool);
            statistics.updateStatistics();
            jsettlers.common.CommonConstants.ENABLE_CONSOLE_LOGGING = true;
            for (byte id : new byte[]{9,7}) {
                Player player = runner.getMainGrid().getPartitionsGrid().getPlayer(id);
                String context = civilisation + " " + player.getPlayerType();
                for (EBuildingType type : EBuildingType.values()) {
                    int count = statistics.getTotalNumberOfBuildingTypeForPlayer(type, id);
                    if (count > 0) System.out.println(context + " " + type + "=" + statistics.getNumberOfBuildingTypeForPlayer(type, id) + "/" + count);
                }
                System.out.println(context + " recruited=" + player.getEndgameStatistic().getSoldiersRecruited());
                assertTrue(context + " must finish a sawmill", statistics.getNumberOfBuildingTypeForPlayer(EBuildingType.SAWMILL, id) > 0);
                assertTrue(context + " must finish lumberjacks", statistics.getNumberOfBuildingTypeForPlayer(EBuildingType.LUMBERJACK, id) > 0);
                assertTrue(context + " must recruit", player.getEndgameStatistic().getSoldiersRecruited() > 0);
                assertTrue(context + " must use materials", player.getEndgameStatistic().getBuildingMaterialsUsed() > 0);
                System.out.println("S3_AI " + context + " soldiers=" + player.getEndgameStatistic().getSoldiersRecruited()
                        + " materials=" + player.getEndgameStatistic().getBuildingMaterialsUsed());
            }
        } finally {
            if (game != null) ReplayUtils.awaitShutdown(game);
            pool.shutdownNow();
        }
    }
}
