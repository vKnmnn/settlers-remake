package jsettlers.logic.map.loading.original;

import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.player.EWinState;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.grid.MainGrid;
import jsettlers.logic.map.loading.original.data.OriginalProduceGoodsWinCondition;
import jsettlers.logic.player.Player;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.network.synchronic.timer.NetworkTimer;

public class ProductionWinConditionTest {
    private MainGrid grid;
    private Player player;
    private OriginalSinglePlayerWinCondition condition;

    @Before
    public void setUp() {
        MatchConstants.init(new NetworkTimer(true), 0L);
        grid = new MainGrid("production-test", "Production test", (short) 32, (short) 32,
                new PlayerSetting[] { new PlayerSetting((byte) 0) });
        player = grid.getPartitionsGrid().getPlayer((byte) 0);
        condition = new OriginalSinglePlayerWinCondition(grid) {
            @Override
            protected boolean isPlayerDead(Player candidate) {
                return false; // Isolate production goals from the separate tower-loss rule.
            }
        };
        condition.produceGoodsToWin(Set.of(
                new OriginalProduceGoodsWinCondition(EMaterialType.PLANK, 2),
                new OriginalProduceGoodsWinCondition(EMaterialType.STONE, 1)));
    }

    @After
    public void tearDown() {
        grid.stopThreads();
        JSettlersGame.clearState();
    }

    @Test
    public void requiresEveryProductionGoalBeforeWinning() {
        condition.timerEvent();
        assertEquals(EWinState.UNDECIDED, player.getWinState());
        player.getProductionStatistic().materialProduced(EMaterialType.PLANK);
        player.getProductionStatistic().materialProduced(EMaterialType.PLANK);
        condition.timerEvent();
        assertEquals(EWinState.UNDECIDED, player.getWinState());
        player.getProductionStatistic().materialProduced(EMaterialType.STONE);
        condition.timerEvent();
        assertEquals(EWinState.WON, player.getWinState());
    }
}
