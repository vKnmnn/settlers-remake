package jsettlers.main.swing.menu.campaign;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.io.File;
import java.util.concurrent.ExecutionException;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import jsettlers.graphics.localization.Labels;
import jsettlers.logic.player.InitialGameState;
import jsettlers.main.JSettlersGame;
import jsettlers.main.swing.JSettlersFrame;
import jsettlers.main.swing.settings.SettingsManager;

/** Desktop campaign selection. Original map data must be supplied by the player. */
public final class CampaignPanel extends JPanel {
	private final JSettlersFrame frame;
	private final CampaignProgress progress;
	private final JComboBox<CampaignCatalog.Campaign> campaigns = new JComboBox<>(CampaignCatalog.CAMPAIGNS.toArray(new CampaignCatalog.Campaign[0]));
	private final DefaultListModel<CampaignCatalog.Mission> missions = new DefaultListModel<>();
	private final JList<CampaignCatalog.Mission> list = new JList<>(missions);
	private final JTextArea description = new JTextArea();
	private final JButton start = new JButton(Labels.getString("campaign-start"));
	private boolean loading;

	public CampaignPanel(JSettlersFrame frame, CampaignProgress progress) {
		super(new BorderLayout(8, 8));
		this.frame = frame;
		this.progress = progress;
		campaigns.setRenderer(new DefaultListCellRenderer() {
			@Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
				return super.getListCellRendererComponent(list, value instanceof CampaignCatalog.Campaign ? Labels.getString(((CampaignCatalog.Campaign) value).label) : "", index, selected, focus);
			}
		});
		list.setCellRenderer(new DefaultListCellRenderer() {
			@Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
				if (!(value instanceof CampaignCatalog.Mission)) return super.getListCellRendererComponent(list, "", index, selected, focus);
				CampaignCatalog.Mission mission = (CampaignCatalog.Mission) value;
				String status = progress.isCompleted(mission) ? "campaign-completed" : !progress.isUnlocked(mission) ? "campaign-locked" : findFile(mission) == null ? "campaign-missing" : "campaign-ready";
				return super.getListCellRendererComponent(list, Labels.getString("campaign-mission") + " " + mission.number + " — " + Labels.getString(status), index, selected, focus);
			}
		});
		description.setEditable(false);
		description.setLineWrap(true);
		description.setWrapStyleWord(true);
		description.setOpaque(false);
		JPanel body = new JPanel(new GridLayout(1, 2, 12, 0));
		body.add(new JScrollPane(list));
		body.add(new JScrollPane(description));
		add(campaigns, BorderLayout.NORTH);
		add(body, BorderLayout.CENTER);
		add(start, BorderLayout.SOUTH);
		campaigns.addActionListener(event -> refresh());
		list.addListSelectionListener(event -> updateSelection());
		start.addActionListener(event -> startSelected());
		refresh();
	}

	public void refresh() {
		CampaignCatalog.Campaign campaign = (CampaignCatalog.Campaign) campaigns.getSelectedItem();
		if (campaign == null) return;
		missions.clear();
		int next = 0;
		for (CampaignCatalog.Mission mission : campaign.missions) {
			missions.addElement(mission);
			if (progress.isUnlocked(mission) && !progress.isCompleted(mission)) next = mission.number - 1;
		}
		list.setSelectedIndex(next);
		updateSelection();
	}

	private File findFile(CampaignCatalog.Mission mission) {
		SettingsManager settings = SettingsManager.getInstance();
		String additional = settings.getAdditionalMapsDirectory();
		return mission.findFile(new File(settings.getSettlersFolder()), additional == null ? null : new File(additional));
	}

	private void updateSelection() {
		CampaignCatalog.Mission mission = list.getSelectedValue();
		boolean available = mission != null && findFile(mission) != null;
		start.setEnabled(!loading && available && progress.isUnlocked(mission));
		if (mission == null) { description.setText(""); return; }
		String text = Labels.getString("campaign-experimental") + "\n\n";
		if (!available) text += Labels.getString("campaign-missing-help") + "\n\n" + mission.relativePath;
		else if (!progress.isUnlocked(mission)) text += Labels.getString("campaign-locked-help");
		else text += Labels.getString("campaign-ready-help");
		description.setText(text);
		description.setCaretPosition(0);
	}

	private void startSelected() {
		CampaignCatalog.Mission mission = list.getSelectedValue();
		if (loading || mission == null || !progress.isUnlocked(mission)) return;
		File file = findFile(mission);
		if (file == null) { updateSelection(); return; }
		loading = true;
		campaigns.setEnabled(false);
		list.setEnabled(false);
		updateSelection();
		new SwingWorker<PreparedMission, Void>() {
			@Override protected PreparedMission doInBackground() throws Exception {
				CampaignMapLoader loader = new CampaignMapLoader(file);
				return new PreparedMission(loader, loader.initialState(System.currentTimeMillis()));
			}
			@Override protected void done() {
				loading = false;
				campaigns.setEnabled(true);
				list.setEnabled(true);
				updateSelection();
				// The player can leave this menu while the file is being read.
				if (!isShowing()) return;
				try {
					PreparedMission prepared = get();
					String briefing = prepared.loader.getDescription();
					if (!briefing.isBlank()) {
						JTextArea text = new JTextArea(briefing, 15, 55);
						text.setEditable(false);
						text.setLineWrap(true);
						text.setWrapStyleWord(true);
						if (JOptionPane.showConfirmDialog(CampaignPanel.this, new JScrollPane(text), Labels.getString("campaign-briefing"), JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
					}
					progress.associateMap(mission, prepared.loader.getMapId());
					frame.showStartingGamePanel(new JSettlersGame(prepared.loader, prepared.state).start(), mission);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} catch (Exception e) {
					Throwable cause = e instanceof ExecutionException ? e.getCause() : e;
					JOptionPane.showMessageDialog(CampaignPanel.this, Labels.getString("campaign-load-failed") + "\n" + cause.getMessage(), Labels.getString("campaign-title"), JOptionPane.ERROR_MESSAGE);
				}
			}
		}.execute();
	}

	private static final class PreparedMission {
		final CampaignMapLoader loader;
		final InitialGameState state;
		PreparedMission(CampaignMapLoader loader, InitialGameState state) { this.loader = loader; this.state = state; }
	}
}
