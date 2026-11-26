package jsettlers.ai.highlevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jsettlers.common.ai.EPlayerType;
import jsettlers.common.player.ECivilisation;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.grid.MainGrid;
import jsettlers.logic.map.loading.MapLoader;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.network.client.task.packets.TaskPacket;
import jsettlers.network.synchronic.timer.NetworkTimer;
import jsettlers.testutils.TestUtils;
import jsettlers.testutils.map.MapUtils;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class AiExecutionOwnershipTest {
    private MainGrid grid;
    @After public void cleanup() {
        if (grid != null) grid.stopThreads();
        JSettlersGame.clearState();
    }

    @Test public void externalCommandsRetainProfileButOnlyHostSchedulesDecisions() throws Exception {
        TestUtils.setupTempResourceManager();
        MatchConstants.init(new NetworkTimer(true), 42L);
        MapLoader map = MapUtils.getMountainlake();
        PlayerSetting[] settings = new PlayerSetting[map.getMaxPlayers()];
        Arrays.fill(settings, new PlayerSetting());
        PlayerSetting host = new PlayerSetting(EPlayerType.AI_S3_EASY, ECivilisation.ROMAN, (byte)0);
        settings[0] = host.withoutAiExecution();
        grid = map.loadMainGrid(settings).getMainGrid();
        assertSame(EPlayerType.AI_S3_EASY, grid.getPartitionsGrid().getPlayer(0).getPlayerType());
        List<TaskPacket> commands = new ArrayList<>();
        new AiExecutor(settings, grid, commands::add).timerEvent();
        assertTrue("Clients/replays must not add AI orders", commands.isEmpty());
        settings[0] = host;
        new AiExecutor(settings, grid, commands::add).timerEvent();
        assertFalse("The same profile must issue decisions on the host", commands.isEmpty());
    }
}
