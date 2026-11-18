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
import jsettlers.logic.map.loading.original.data.EOriginalMapStackType;
import jsettlers.logic.objects.stack.StackMapObject;
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
                new PlayerSetting[] { new PlayerSetting((byte) 0), new PlayerSetting((byte) 1) });
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
    public void requiresEveryStockGoalBeforeWinning() {
        condition.timerEvent();
        assertEquals(EWinState.UNDECIDED, player.getWinState());
        putStack(8, EMaterialType.PLANK, 2, (byte) 0);
        condition.timerEvent();
        assertEquals(EWinState.UNDECIDED, player.getWinState());
        putStack(10, EMaterialType.STONE, 1, (byte) 0);
        condition.timerEvent();
        assertEquals(EWinState.WON, player.getWinState());
    }
    @Test
    public void requiresImportedAlcoholGoalsBeforeWinning() {
        condition.produceGoodsToWin(Set.of(
                new OriginalProduceGoodsWinCondition(
                        EOriginalMapStackType.getTypeByInt(33).value, 2),
                new OriginalProduceGoodsWinCondition(
                        EOriginalMapStackType.getTypeByInt(34).value, 1)));
        condition.timerEvent();
        assertEquals(EWinState.UNDECIDED, player.getWinState());
        putStack(8, EMaterialType.LIQUOR, 2, (byte) 0);
        condition.timerEvent();
        assertEquals(EWinState.UNDECIDED, player.getWinState());
        putStack(10, EMaterialType.MEAD, 1, (byte) 0);
        condition.timerEvent();
        assertEquals(EWinState.WON, player.getWinState());
    }
    @Test
    public void pastProductionAndEnemyStocksDoNotSatisfyGoals() {
        player.getProductionStatistic().materialProduced(EMaterialType.PLANK);
        player.getProductionStatistic().materialProduced(EMaterialType.PLANK);
        player.getProductionStatistic().materialProduced(EMaterialType.STONE);
        putStack(8, EMaterialType.PLANK, 2, (byte) 1);
        putStack(10, EMaterialType.STONE, 1, (byte) 1);
        condition.timerEvent();
        assertEquals(EWinState.UNDECIDED, player.getWinState());
    }

    @Test
    public void consumedGoodsMustBeReplacedBeforeWinning() {
        StackMapObject planks = putStack(8, EMaterialType.PLANK, 2, (byte) 0);
        condition.timerEvent();
        planks.decrement();
        putStack(10, EMaterialType.STONE, 1, (byte) 0);
        condition.timerEvent();
        assertEquals(EWinState.UNDECIDED, player.getWinState());
        planks.increment();
        condition.timerEvent();
        assertEquals(EWinState.WON, player.getWinState());
    }

    @Test
    public void unmappedLegacyGoalCannotCauseAutomaticVictory() {
        condition.produceGoodsToWin(Set.of(new OriginalProduceGoodsWinCondition(null, 1)));
        condition.timerEvent();
        assertEquals(EWinState.UNDECIDED, player.getWinState());
    }

    private StackMapObject putStack(int x, EMaterialType type, int count, byte owner) {
        grid.getPartitionsGrid().changePlayerAt(x, 8, owner);
        StackMapObject stack = new StackMapObject(type, (byte) count);
        grid.getObjectsGrid().addMapObjectAt(x, 8, stack);
        return stack;
    }
}
