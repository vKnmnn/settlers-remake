package jsettlers.main.swing.menu.statspanel;

import javax.swing.table.DefaultTableModel;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.util.concurrent.atomic.AtomicInteger;
import jsettlers.common.menu.FakeMapGame;
import jsettlers.graphics.localization.Labels;
import jsettlers.common.player.EWinState;
import jsettlers.common.player.IEndgameStatistic;
import jsettlers.common.player.IInGamePlayer;
import jsettlers.common.player.IPlayer;
import org.junit.Test;

import static org.junit.Assert.*;

public class EndgameStatsPanelTest {
	@Test
	public void skipsEmptySlotsAndUsesRealReadOnlyTableHeaders() {
		IInGamePlayer player = new IPlayer.DummyPlayer((byte) 2) {
			@Override public EWinState getWinState() { return EWinState.WON; }
			@Override public IEndgameStatistic getEndgameStatistic() {
				return statistics(true);
			}
		};
		DefaultTableModel model = EndgameStatsPanel.createModel(new IInGamePlayer[] { null, player });
		assertEquals(1, model.getRowCount());
		assertEquals("3: Player", model.getValueAt(0, 0));
		assertEquals(2, model.getValueAt(0, 1));
		assertEquals(12, model.getColumnCount());
		assertEquals(98765L, model.getValueAt(0, 3));
		assertEquals(55000L, model.getValueAt(0, 4));
		assertEquals(8L, model.getValueAt(0, 8));
		assertEquals(25L, model.getValueAt(0, 9));
		assertEquals(12L, model.getValueAt(0, 10));
		assertFalse(model.isCellEditable(0, 0));
	}
	@Test
	public void leaveButtonRequestsLocalShutdownOnceWithoutAnActionQueue() throws Exception {
		AtomicInteger stopped = new AtomicInteger();
		FakeMapGame game = new FakeMapGame(null) {
			@Override public void stopGame() { stopped.incrementAndGet(); }
		};
		SwingUtilities.invokeAndWait(() -> {
			EndgameStatsPanel panel = new EndgameStatsPanel(() -> {}, game::stopGame);
			JButton leave = findButton(panel, Labels.getString("game-menu-quit"));
			assertNotNull(leave);
			leave.doClick();
			leave.doClick();
		});
		assertEquals(1, stopped.get());
	}

	@Test
	public void reusedPanelCanReturnToMenuAfterEachGame() throws Exception {
		AtomicInteger returned = new AtomicInteger();
		SwingUtilities.invokeAndWait(() -> {
			EndgameStatsPanel panel = new EndgameStatsPanel(returned::incrementAndGet, null);
			for (int game = 0; game < 2; game++) {
				panel.setGame(new FakeMapGame(null));
				JButton next = findButton(panel, Labels.getString("stats-panel-exit"));
				assertTrue(next.isEnabled());
				next.doClick();
			}
		});
		assertEquals(2, returned.get());
	}

	@Test
	public void incompleteLegacyStatisticsDoNotInventComparableScores() {
		IInGamePlayer player = new IPlayer.DummyPlayer((byte) 0) {
			@Override public IEndgameStatistic getEndgameStatistic() { return statistics(false); }
		};
		DefaultTableModel model = EndgameStatsPanel.createModel(new IInGamePlayer[] { player });
		assertEquals("—", model.getValueAt(0, 3));
		assertEquals(25L, model.getValueAt(0, 9));
	}

	private static IEndgameStatistic statistics(boolean complete) {
		return new IEndgameStatistic() {
			public short getAmountOfProducedSoldiers() { return 12; }
			public short getAmountOfProducedMana() { return 25; }
			public short getAmountOfProducedGold() { return 8; }
			public String getName() { return "Player"; }
			public byte getTeam() { return 1; }
			public long getScore() { return 98765; }
			public long getSettlersCreated() { return 55000; }
			public long getBuildingMaterialsUsed() { return 16; }
			public long getFoodConsumed() { return 17; }
			public long getMinedMaterials() { return 18; }
			public long getGoldValue() { return 8; }
			public long getManaProduced() { return 25; }
			public long getSoldiersRecruited() { return 12; }
			public long getBattlePoints() { return 19; }
			public boolean isComplete() { return complete; }
		};
	}

	private static JButton findButton(Container root, String text) {
		for (Component child : root.getComponents()) {
			if (child instanceof JButton && text.equals(((JButton) child).getText())) return (JButton) child;
			if (child instanceof Container) {
				JButton found = findButton((Container) child, text);
				if (found != null) return found;
			}
		}
		return null;
	}

}
