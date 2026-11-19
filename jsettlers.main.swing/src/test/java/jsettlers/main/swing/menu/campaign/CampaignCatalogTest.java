package jsettlers.main.swing.menu.campaign;

import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class CampaignCatalogTest {
	@Rule public TemporaryFolder temporary = new TemporaryFolder();

	@Test public void originalCatalogHasEightIndependentCampaignsAnd72Missions() {
		assertEquals(8, CampaignCatalog.CAMPAIGNS.size());
		Set<String> ids = new HashSet<>();
		for (CampaignCatalog.Campaign campaign : CampaignCatalog.CAMPAIGNS) {
			for (CampaignCatalog.Mission mission : campaign.missions) assertTrue(ids.add(mission.getId()));
		}
		assertEquals(72, ids.size());
		assertEquals("S3/Mis_M/A/j8.map", CampaignCatalog.findMission("asian.8").relativePath);
		assertNull(CampaignCatalog.findMission("asian.9"));
		assertNull(CampaignCatalog.findMission(null));
	}

	@Test public void resolvesWindowsCaseWithoutConfusingBaseAndExpansionMaps() throws Exception {
		File root = temporary.getRoot();
		File base = create("s3/mis_m/r/R1.MAP");
		File expansion = create("S3MCD/MAP/MIS_M/R/r1.map");
		assertEquals(base, CampaignCatalog.findMission("roman.1").findFile(root));
		assertEquals(expansion, CampaignCatalog.findMission("mission-roman.1").findFile(root));
		assertNull(CampaignCatalog.findMission("roman.2").findFile(root));
		assertNull(CampaignCatalog.findMission("roman.1").findFile(new File(root, "S3MCD")));
	}

	@Test public void acceptsInstalledBaseDirectoryAndAdditionalMediaRoot() throws Exception {
		File installed = create("Mis_M/E/e1.map");
		File additional = temporary.newFolder("media");
		File mission = new File(additional, "S3/Mis_M/E/e2.map");
		Files.createDirectories(mission.toPath().getParent());
		Files.createFile(mission.toPath());
		assertEquals(installed, CampaignCatalog.findMission("egyptian.1").findFile(temporary.getRoot(), null));
		assertEquals(mission, CampaignCatalog.findMission("egyptian.2").findFile(temporary.getRoot(), additional));
	}

	@Test public void availabilityRequiresAKnownInstalledMissionAndTracksRemoval() throws Exception {
		File root = temporary.getRoot();
		assertFalse(CampaignCatalog.hasInstalledMissions(root, null));
		create("random.map");
		assertFalse(CampaignCatalog.hasInstalledMissions(root));
		File mission = create("mis_m/r/R1.MAP");
		assertTrue(CampaignCatalog.hasInstalledMissions(null, root));
		Files.delete(mission.toPath());
		assertFalse(CampaignCatalog.hasInstalledMissions(root));
	}

	private File create(String path) throws Exception {
		File file = new File(temporary.getRoot(), path);
		Files.createDirectories(file.toPath().getParent());
		Files.createFile(file.toPath());
		return file;
	}
}
