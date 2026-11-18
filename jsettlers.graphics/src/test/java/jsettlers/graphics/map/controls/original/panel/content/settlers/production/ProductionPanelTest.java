package jsettlers.graphics.map.controls.original.panel.content.settlers.production;

import java.lang.reflect.Proxy;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.player.*;
import jsettlers.graphics.action.ExecutableAction;
import jsettlers.graphics.ui.UIPanel;
import org.junit.Test;
import static org.junit.Assert.*;

public class ProductionPanelTest {
    private static final class Statistics implements IProductionStatistic {
        EMaterialType material;
        int minutes;
        public int getAmountProduced(EMaterialType type) { return 42; }
        public ProductionHistory getHistory(EMaterialType type, int window) {
            material = type; minutes = window;
            return new ProductionHistory(new int[] {2, 4}, 14080, 28160, 0);
        }
    }
    @Test public void materialAndWindowSelectionStayLocalAndPlayerSwitchUsesNewStatistics() {
        Statistics first = new Statistics();
        ProductionPanel panel = new ProductionPanel();
        panel.setPlayer(player(first));
        UIPanel view = panel.getPanel();
        ((ExecutableAction) view.getAction(.26f, .82f).orElseThrow()).execute(); // stone in overview
        assertEquals(EMaterialType.STONE, first.material);
        ((ExecutableAction) view.getAction(.85f, .12f).orElseThrow()).execute(); // longest window
        assertEquals(60, first.minutes);
        Statistics second = new Statistics();
        panel.setPlayer(player(second));
        assertEquals(EMaterialType.STONE, second.material);
        assertEquals(60, second.minutes);
        ((ExecutableAction) view.getAction(.5f, .04f).orElseThrow()).execute(); // overview
        ((ExecutableAction) view.getAction(.10f, .82f).orElseThrow()).execute(); // planks
        assertEquals(EMaterialType.PLANK, second.material);
        assertEquals(EMaterialType.STONE, first.material);
    }
    private static IInGamePlayer player(Statistics statistics) {
        return (IInGamePlayer) Proxy.newProxyInstance(IInGamePlayer.class.getClassLoader(),
                new Class<?>[] {IInGamePlayer.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getProductionStatistic")) return statistics;
                    throw new AssertionError("Unexpected player access: " + method.getName());
                });
    }
}
