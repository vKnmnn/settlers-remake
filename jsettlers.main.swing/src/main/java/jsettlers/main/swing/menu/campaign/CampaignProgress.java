package jsettlers.main.swing.menu.campaign;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import jsettlers.common.player.EWinState;

/** Local campaign progress, separate from simulation state and multiplayer. */
public final class CampaignProgress {
	private final Path file;
	private Properties data = new Properties();

	public CampaignProgress(Path file) throws IOException {
		this.file = file;
		if (Files.exists(file)) {
			try (InputStream input = Files.newInputStream(file)) { data.load(input); }
		}
	}

	public boolean isCompleted(CampaignCatalog.Mission mission) {
		return Boolean.parseBoolean(data.getProperty("won." + mission.getId()));
	}

	public boolean isUnlocked(CampaignCatalog.Mission mission) {
		for (int i = 1; i < mission.number; i++) {
			if (!Boolean.parseBoolean(data.getProperty("won." + mission.campaignId + "." + i))) return false;
		}
		return true;
	}

	public void associateMap(CampaignCatalog.Mission mission, String mapId) throws IOException {
		if (mapId == null || mapId.isEmpty()) throw new IllegalArgumentException("Missing map identity");
		Properties updated = copy();
		updated.setProperty("map." + mapId, mission.getId());
		store(updated);
	}

	public CampaignCatalog.Mission missionForMap(String baseMapId) {
		return baseMapId == null ? null : CampaignCatalog.findMission(data.getProperty("map." + baseMapId));
	}

	public boolean recordResult(CampaignCatalog.Mission mission, EWinState result, boolean multiplayer, byte playerId) throws IOException {
		if (mission == null || multiplayer || playerId != 0 || result != EWinState.WON || !isUnlocked(mission) || isCompleted(mission)) return false;
		Properties updated = copy();
		updated.setProperty("won." + mission.getId(), "true");
		store(updated);
		return true;
	}

	private Properties copy() {
		Properties result = new Properties();
		result.putAll(data);
		return result;
	}

	private void store(Properties updated) throws IOException {
		Path absolute = file.toAbsolutePath();
		Files.createDirectories(absolute.getParent());
		Path temporary = Files.createTempFile(absolute.getParent(), ".campaign-", ".tmp");
		try {
			try (OutputStream output = Files.newOutputStream(temporary)) { updated.store(output, "JSettlers campaign progress"); }
			try {
				Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
			}
			data = updated;
		} finally { Files.deleteIfExists(temporary); }
	}
}
