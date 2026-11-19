package jsettlers.main.swing.menu.campaign;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import jsettlers.common.player.EWinState;
import static org.junit.Assert.*;

public class CampaignProgressTest {
	@Rule public TemporaryFolder temporary = new TemporaryFolder();
	private final CampaignCatalog.Mission first = CampaignCatalog.findMission("roman.1");
	private final CampaignCatalog.Mission second = CampaignCatalog.findMission("roman.2");

	@Test public void onlyLocalVictoriesUnlockNextMissionAndRemainAfterRestart() throws Exception {
		Path file = temporary.getRoot().toPath().resolve("progress.properties");
		CampaignProgress progress = new CampaignProgress(file);
		assertTrue(progress.isUnlocked(first));
		assertFalse(progress.isUnlocked(second));
		assertFalse(progress.recordResult(second, EWinState.WON, false, (byte) 0));
		assertFalse(progress.recordResult(first, EWinState.LOST, false, (byte) 0));
		assertFalse(progress.recordResult(first, EWinState.UNDECIDED, false, (byte) 0));
		assertFalse(progress.recordResult(first, EWinState.WON, true, (byte) 0));
		assertFalse(progress.recordResult(first, EWinState.WON, false, (byte) 1));
		assertTrue(progress.recordResult(first, EWinState.WON, false, (byte) 0));
		assertFalse(progress.recordResult(first, EWinState.WON, false, (byte) 0));
		CampaignProgress reloaded = new CampaignProgress(file);
		assertTrue(reloaded.isCompleted(first));
		assertTrue(reloaded.isUnlocked(second));
		assertFalse(reloaded.isUnlocked(CampaignCatalog.findMission("mission-roman.2")));
		assertFalse(reloaded.isUnlocked(CampaignCatalog.findMission("roman.3")));
	}

	@Test public void savegameBaseMapIdsKeepDistinctCampaignsAcrossRestart() throws Exception {
		Path file = temporary.getRoot().toPath().resolve("progress.properties");
		CampaignProgress progress = new CampaignProgress(file);
		progress.associateMap(first, "123r1");
		progress.associateMap(CampaignCatalog.findMission("mission-roman.1"), "456r1");
		CampaignProgress reloaded = new CampaignProgress(file);
		assertSame(first, reloaded.missionForMap("123r1"));
		assertEquals("mission-roman.1", reloaded.missionForMap("456r1").getId());
		assertNull(reloaded.missionForMap("unknown"));
		assertNull(reloaded.missionForMap(null));
	}

	@Test public void failedWriteDoesNotReportCompletedOrLosePriorProgress() throws Exception {
		Path file = temporary.getRoot().toPath().resolve("progress.properties");
		CampaignProgress progress = new CampaignProgress(file);
		Files.createDirectory(file);
		Files.createFile(file.resolve("cannot-replace-nonempty-directory"));
		try {
			progress.recordResult(first, EWinState.WON, false, (byte) 0);
			fail("Expected write failure");
		} catch (IOException expected) { }
		assertFalse(progress.isCompleted(first));
		assertFalse(progress.isUnlocked(second));
	}
}
