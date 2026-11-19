package jsettlers.main.swing.menu.campaign;

import java.io.IOException;
import java.nio.file.Path;
import jsettlers.common.menu.IStartedGame;
import jsettlers.common.player.IInGamePlayer;

/** Associates a launch/result pair. Normal games and multiplayer never inherit a previous campaign. */
public final class CampaignSession {
	private final Path progressFile;
	private CampaignProgress progress;
	private CampaignCatalog.Mission pending;
	private CampaignCatalog.Mission active;

	public CampaignSession(Path progressFile) { this.progressFile = progressFile; }

	public CampaignProgress getProgress() throws IOException {
		if (progress == null) progress = new CampaignProgress(progressFile);
		return progress;
	}

	public void prepare(CampaignCatalog.Mission mission) { pending = mission; }

	public void started(boolean multiplayer) {
		active = multiplayer ? null : pending;
		pending = null;
	}

	public void cancel() { pending = null; }

	public boolean recordResult(IStartedGame game) throws IOException {
		IInGamePlayer player = game.getInGamePlayer();
		return active != null && player != null && getProgress().recordResult(active, player.getWinState(), game.isMultiplayerGame(), player.getPlayerId());
	}
}
