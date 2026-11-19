package jsettlers.main.swing.menu.campaign;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Mission names and order from the original S3.EXE campaign file tables. No original assets are included. */
public final class CampaignCatalog {
	public static final class Campaign {
		public final String id;
		public final String label;
		public final List<Mission> missions;

		private Campaign(String id, String path, String prefix, int count, boolean padded) {
			this.id = id;
			this.label = "campaign-" + id;
			List<Mission> entries = new ArrayList<>();
			for (int number = 1; number <= count; number++) {
				String filename = prefix + (padded && number < 10 ? "0" : "") + number + ".map";
				entries.add(new Mission(id, number, path + "/" + filename));
			}
			missions = Collections.unmodifiableList(entries);
		}
	}

	public static final class Mission {
		public final String campaignId;
		public final int number;
		public final String relativePath;

		private Mission(String campaignId, int number, String relativePath) {
			this.campaignId = campaignId;
			this.number = number;
			this.relativePath = relativePath;
		}

		public String getId() { return campaignId + "." + number; }

		/** Resolve complete paths, never just filenames: base and mission-CD maps share names. */
		public File findFile(File... roots) {
			for (File root : roots) {
				File result = resolve(root, relativePath);
				if (result != null && result.isFile()) return result;
				// The base game's CD "S3" directory can also be the installation root.
				if (relativePath.startsWith("S3/")) {
					result = resolve(root, relativePath.substring(3));
					if (result != null && result.isFile()) return result;
				}
			}
			return null;
		}
	}

	public static final List<Campaign> CAMPAIGNS = List.of(
			new Campaign("roman", "S3/Mis_M/R", "r", 8, false),
			new Campaign("egyptian", "S3/Mis_M/E", "e", 8, false),
			new Campaign("asian", "S3/Mis_M/A", "j", 8, false),
			new Campaign("mission-roman", "S3MCD/Map/Mis_M/R", "r", 8, false),
			new Campaign("mission-egyptian", "S3MCD/Map/Mis_M/E", "e", 8, false),
			new Campaign("mission-asian", "S3MCD/Map/Mis_M/A", "j", 8, false),
			new Campaign("amazon", "S3Qota/Mis_M/Z", "z", 12, true),
			new Campaign("gods", "S3Qota/Mis_M/G", "g", 12, true));

	public static Mission findMission(String id) {
		if (id == null) return null;
		for (Campaign campaign : CAMPAIGNS) {
			for (Mission mission : campaign.missions) if (mission.getId().equals(id)) return mission;
		}
		return null;
	}

	private static File resolve(File root, String relative) {
		File current = root;
		for (String component : relative.split("/")) {
			if (current == null) return null;
			File exact = new File(current, component);
			if (exact.exists()) { current = exact; continue; }
			File[] children = current.listFiles();
			current = null;
			if (children == null) return null;
			for (File child : children) {
				if (child.getName().equalsIgnoreCase(component)) { current = child; break; }
			}
		}
		return current;
	}

	private CampaignCatalog() { }
}
