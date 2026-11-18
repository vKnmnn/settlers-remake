/*******************************************************************************
 * Copyright (c) 2019
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package jsettlers.main.swing.menu.statspanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import jsettlers.common.menu.IStartedGame;
import jsettlers.common.player.IEndgameStatistic;
import jsettlers.common.player.EWinState;
import jsettlers.common.player.IInGamePlayer;
import jsettlers.graphics.localization.Labels;
import jsettlers.main.swing.JSettlersFrame;
import jsettlers.main.swing.lookandfeel.ELFStyle;
import jsettlers.main.swing.lookandfeel.components.BackgroundPanel;

public class EndgameStatsPanel extends BackgroundPanel {
	private final JTable statsTable = new JTable();
	private final JLabel result = new JLabel();
	private final JLabel completeness = new JLabel();
	private final List<JButton> actionButtons = new ArrayList<>();

	public EndgameStatsPanel(JSettlersFrame settlersFrame) {
		this(settlersFrame::showMainMenu, null);
	}

	/** When leaveGame is supplied, the game is still running behind this result window. */
	public EndgameStatsPanel(Runnable continueAction, Runnable leaveGame) {
		setPreferredSize(new Dimension(1200, 480));
		JPanel mainPane = new JPanel(new BorderLayout(8, 8));
		JPanel heading = new JPanel();
		heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
		heading.add(result);
		heading.add(completeness);
		mainPane.add(heading, BorderLayout.NORTH);
		statsTable.setRowHeight(26);
		result.putClientProperty(ELFStyle.KEY, ELFStyle.LABEL_HEADER);
		completeness.putClientProperty(ELFStyle.KEY, ELFStyle.LABEL_LONG);
		statsTable.setPreferredScrollableViewportSize(new Dimension(1100, 250));
		statsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		statsTable.setTableHeader(new javax.swing.table.JTableHeader(statsTable.getColumnModel()) {
			@Override
			public String getToolTipText(java.awt.event.MouseEvent event) {
				int column = columnAtPoint(event.getPoint());
				if (column < 0) return null;
				switch (getColumnModel().getColumn(column).getModelIndex()) {
				case 5: return Labels.getString("stats-panel-buildings-tooltip");
				case 6: return Labels.getString("stats-panel-food-tooltip");
				case 7: return Labels.getString("stats-panel-mines-tooltip");
				case 11: return Labels.getString("stats-panel-battles-tooltip");
				default: return null;
				}
			}
		});
		JScrollPane scroll = new JScrollPane(statsTable);
		scroll.setColumnHeaderView(statsTable.getTableHeader());
		mainPane.add(scroll, BorderLayout.CENTER);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(button(leaveGame == null ? "stats-panel-exit" : "stats-panel-continue", continueAction));
		if (leaveGame != null) buttons.add(button("game-menu-quit", leaveGame));
		mainPane.add(buttons, BorderLayout.SOUTH);
		add(mainPane, BorderLayout.CENTER);
		SwingUtilities.updateComponentTreeUI(this);
		statsTable.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
	}

	private JButton button(String label, Runnable action) {
		JButton button = new JButton(Labels.getString(label));
		actionButtons.add(button);
		button.putClientProperty(ELFStyle.KEY, ELFStyle.BUTTON_STONE);
		button.addActionListener(event -> {
			button.setEnabled(false);
			action.run();
		});
		return button;
	}

	public void setGame(IStartedGame game) {
		for (JButton button : actionButtons) button.setEnabled(true);
		IInGamePlayer local = game.getInGamePlayer();
		String outcome = local == null || local.getWinState() == EWinState.UNDECIDED ? Labels.getString("stats-panel-title")
				: Labels.getString("winstate_" + local.getWinState());
		int seconds = Math.max(0, game.getGameTimeProvider().getGameTime() / 1000);
		result.setText(outcome + " — " + Labels.getString("stats-panel-time") + " "
				+ String.format("%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60));
		statsTable.setModel(createModel(game.getAllInGamePlayers()));
		int[] widths = { 220, 45, 95, 80, 75, 80, 75, 75, 75, 75, 80, 80 };
		for (int i = 0; i < widths.length; i++) statsTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
		boolean complete = true;
		IInGamePlayer[] players = game.getAllInGamePlayers();
		if (players != null) for (IInGamePlayer player : players) {
			if (player != null && !player.getEndgameStatistic().isComplete()) complete = false;
		}
		completeness.setText(complete ? " " : Labels.getString("stats-panel-incomplete"));
	}

	static DefaultTableModel createModel(IInGamePlayer[] players) {
		String[] keys = { "name", "team", "result", "score", "settlers", "buildings", "food", "mines", "gold", "manna", "warriors", "battles" };
		String[] columns = new String[keys.length];
		for (int i = 0; i < keys.length; i++) columns[i] = Labels.getString("stats-panel-" + keys[i]);
		List<Object[]> rows = new ArrayList<>();
		if (players != null) for (IInGamePlayer player : players) {
			if (player == null) continue;
			IEndgameStatistic stats = player.getEndgameStatistic();
			rows.add(new Object[] {
				(player.getPlayerId() + 1) + ": " + stats.getName(),
				stats.getTeam() + 1,
				Labels.getString("stats-panel-result-" + player.getWinState()),
				stats.isComplete() ? stats.getScore() : "—",
				stats.getSettlersCreated(), stats.getBuildingMaterialsUsed(), stats.getFoodConsumed(),
				stats.getMinedMaterials(), stats.getGoldValue(), stats.getManaProduced(),
				stats.getSoldiersRecruited(), stats.getBattlePoints()
			});
		}
		return new DefaultTableModel(rows.toArray(new Object[0][]), columns) {
			@Override
			public boolean isCellEditable(int row, int column) { return false; }
		};
	}
}
