/*******************************************************************************
 * Copyright (c) 2015 - 2017
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
package jsettlers.main.swing;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import go.graphics.area.Area;
import go.graphics.region.Region;
import go.graphics.sound.SoundPlayer;
import go.graphics.swing.AreaContainer;
import go.graphics.swing.sound.SwingSoundPlayer;

import jsettlers.common.CommitInfo;
import jsettlers.graphics.localization.Labels;
import jsettlers.common.menu.IJoinPhaseMultiplayerGameConnector;
import jsettlers.common.menu.IMapInterfaceConnector;
import jsettlers.common.menu.IMultiplayerConnector;
import jsettlers.common.menu.IStartedGame;
import jsettlers.common.menu.IStartingGame;
import jsettlers.graphics.map.ETextDrawPosition;
import jsettlers.graphics.map.MapContent;
import jsettlers.logic.map.loading.MapLoader;
import jsettlers.main.swing.menu.joinpanel.JoinGamePanel;
import jsettlers.main.swing.menu.mainmenu.MainMenuPanel;
import jsettlers.main.swing.menu.startinggamemenu.StartingGamePanel;
import jsettlers.main.swing.menu.statspanel.EndgameStatsPanel;
import jsettlers.main.swing.menu.campaign.CampaignCatalog;
import jsettlers.main.swing.menu.campaign.CampaignProgress;
import jsettlers.main.swing.menu.campaign.CampaignSession;
import jsettlers.common.resources.ResourceManager;
import java.io.IOException;
import javax.swing.JOptionPane;
import jsettlers.main.swing.settings.SettingsManager;

/**
 * @author codingberlin
 */
public class JSettlersFrame extends JFrame {
	private static final long serialVersionUID = 2607082717493797224L;

	private final MainMenuPanel mainPanel;
	private final EndgameStatsPanel endgameStatsPanel = new EndgameStatsPanel(this);
	private final StartingGamePanel startingGamePanel = new StartingGamePanel(this);
	private final JoinGamePanel joinGamePanel = new JoinGamePanel(this);
	private final SwingSoundPlayer soundPlayer = new SwingSoundPlayer(SettingsManager.getInstance());

	private Timer redrawTimer;
	private EndgameMonitor endgameMonitor;
	private final CampaignSession campaignSession = new CampaignSession(ResourceManager.getResourcesDirectory().toPath().resolve("campaign-progress.properties"));
	private JDialog resultDialog;
	private IStartedGame displayedGame;
	private boolean fullScreen = false;
	private AreaContainer areaContainer;

	JSettlersFrame() throws HeadlessException {
		setTitle("JSettlers - Version: " + CommitInfo.COMMIT_HASH_SHORT);
		setIconImage(JSettlersSwingUtil.APP_ICON);

		SettingsManager settingsManager = SettingsManager.getInstance();

		mainPanel = new MainMenuPanel(this);

		showMainMenu();
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setPreferredSize(new Dimension(1200, 800));
		pack();
		setLocationRelativeTo(null);

		fullScreen = settingsManager.getFullScreenMode();
		updateFullScreenMode();

		KeyboardFocusManager keyboardFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		keyboardFocusManager.addKeyEventDispatcher(e -> {
			if (e.getID() == KeyEvent.KEY_PRESSED) {
				if (e.isAltDown() && e.getKeyCode() == KeyEvent.VK_ENTER) {
					toogleFullScreenMode();
					return true; // consume this key event.
				}
			}
			return false;
		});
	}

	private void toogleFullScreenMode() {
		fullScreen = !fullScreen;
		SettingsManager.getInstance().setFullScreenMode(fullScreen);
		updateFullScreenMode();
	}

	private void updateFullScreenMode() {
		if(areaContainer != null) areaContainer.removeSurface();
		dispose();

		setResizable(!fullScreen);
		setUndecorated(fullScreen);

		pack();
		setVisible(true);

		GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice graphicsDevice = graphicsEnvironment.getDefaultScreenDevice();
		graphicsDevice.setFullScreenWindow(fullScreen ? this : null);
		if(areaContainer != null) areaContainer.notifyResize();
	}

	private void abortRedrawTimerIfPresent() {
		if (redrawTimer != null) {
			redrawTimer.stop();
			redrawTimer = null;
		}
	}

	public void showMainMenu() {
		campaignSession.cancel();
		setNewContentPane(mainPanel);
	}

	public CampaignProgress getCampaignProgress() throws IOException {
		return campaignSession.getProgress();
	}

	public void showStartingGamePanel(IStartingGame startingGame) {
		showStartingGamePanel(startingGame, null);
	}

	public void showStartingGamePanel(IStartingGame startingGame, CampaignCatalog.Mission mission) {
		campaignSession.prepare(mission);
		startingGamePanel.setStartingGame(startingGame);
		setNewContentPane(startingGamePanel);
	}

	private void setNewContentPane(Container newContent) {
		abortRedrawTimerIfPresent();
		setContentPane(newContent);
		revalidate();
		repaint();
	}

	public void exit() {
		soundPlayer.close();
		abortRedrawTimerIfPresent();
		System.exit(0);
	}

	public SoundPlayer getSoundPlayer() {
		return soundPlayer;
	}

	public void setContent(MapContent content) {
		Region region = new Region(500, 500);
		region.setContent(content);
		Area area = new Area();
		area.set(region);

		int fpsLimit = SettingsManager.getInstance().getFpsLimit();
		if(fpsLimit != 0) {
			redrawTimer = new Timer((int)(1000.0f/fpsLimit), e -> region.requestRedraw());
			redrawTimer.setInitialDelay(0);
			redrawTimer.start();
		}

		SwingUtilities.invokeLater(() -> {
			setContentPane(areaContainer = new AreaContainer(area, SettingsManager.getInstance().getBackend(), SettingsManager.getInstance().isGraphicsDebug(), SettingsManager.getInstance().getGuiScale()));
			areaContainer.updateFPSLimit(fpsLimit);
			revalidate();
			repaint();
		});
	}

	public void showNewSinglePlayerGameMenu(MapLoader mapLoader) {
		joinGamePanel.setSinglePlayerMap(mapLoader);
		setNewContentPane(joinGamePanel);
	}

	public void showNewMultiPlayerGameMenu(MapLoader mapLoader, IMultiplayerConnector connector) {
		joinGamePanel.setNewMultiPlayerMap(mapLoader, connector);
		setNewContentPane(joinGamePanel);
	}

	public void showJoinMultiplayerMenu(IJoinPhaseMultiplayerGameConnector joinPhaseMultiplayerGameConnector, MapLoader mapLoader, String playerUUID) {
		joinGamePanel.setJoinMultiPlayerMap(joinPhaseMultiplayerGameConnector, mapLoader, playerUUID);
		setNewContentPane(joinGamePanel);
	}

	private void closeResultDialog() {
		if (endgameMonitor != null) {
			endgameMonitor.stop();
			endgameMonitor = null;
		}
		if (resultDialog != null) {
			resultDialog.dispose();
			resultDialog = null;
		}
	}

	private void showGameResult(IStartedGame game) {
		if (game != displayedGame || game.isShutdownFinished()) return;
		recordCampaignResult(game);
		JDialog dialog = new JDialog(this, Labels.getString("stats-panel-title"), false);
		resultDialog = dialog;
		EndgameStatsPanel panel = new EndgameStatsPanel(dialog::dispose, () -> {
			dialog.dispose();
			game.stopGame();
		});
		panel.setGame(game);
		dialog.setContentPane(panel);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.pack();
		dialog.setSize(Math.min(dialog.getWidth(), getWidth()), Math.min(dialog.getHeight(), getHeight()));
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
	}

	public void showEndgameStatistics(IStartedGame game) {
		if (game != displayedGame) return;
		recordCampaignResult(game);
		closeResultDialog();
		displayedGame = null;
		if(areaContainer != null) {
			areaContainer.disposeAll();
			areaContainer = null;
		}

		endgameStatsPanel.setGame(game);
		setNewContentPane(endgameStatsPanel);
	}

	private void recordCampaignResult(IStartedGame game) {
		try {
			campaignSession.recordResult(game);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, Labels.getString("campaign-progress-failed") + "\n" + e.getMessage(), Labels.getString("campaign-title"), JOptionPane.ERROR_MESSAGE);
		}
	}

	public IMapInterfaceConnector showStartedGame(IStartedGame startedGame) {
		MapContent content = new MapContent(startedGame, soundPlayer, ETextDrawPosition.DESKTOP);
		SwingUtilities.invokeLater(() -> {
			closeResultDialog();
			displayedGame = startedGame;
			campaignSession.started(startedGame.isMultiplayerGame());
			setContent(content);
			endgameMonitor = new EndgameMonitor(startedGame,
					this::showGameResult);
			endgameMonitor.start();
		});
		startedGame.setGameExitListener(exitGame -> SwingUtilities.invokeLater(() -> showEndgameStatistics(exitGame)));
		return content.getInterfaceConnector();
	}
}
