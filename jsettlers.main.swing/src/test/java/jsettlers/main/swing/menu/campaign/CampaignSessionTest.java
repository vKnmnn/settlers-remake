package jsettlers.main.swing.menu.campaign;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import jsettlers.common.menu.IStartedGame;
import jsettlers.common.player.EWinState;
import jsettlers.common.player.IInGamePlayer;
import jsettlers.common.player.IPlayer;
import static org.junit.Assert.*;

public class CampaignSessionTest {
	@Rule public TemporaryFolder temporary = new TemporaryFolder();

	@Test public void unrelatedGameAndCancelledLaunchCannotInheritCampaign() throws Exception {
		CampaignSession session = session();
		CampaignCatalog.Mission first = CampaignCatalog.findMission("roman.1");
		session.prepare(first);
		session.cancel();
		session.started(false);
		assertFalse(session.recordResult(winningGame(false)));
		session.prepare(first);
		session.started(true);
		assertFalse(session.recordResult(winningGame(true)));
		session.prepare(first);
		session.started(false);
		session.prepare(null);
		session.started(false);
		assertFalse(session.recordResult(winningGame(false)));
		assertFalse(session.getProgress().isCompleted(first));
	}

	@Test public void loadedCampaignVictoryUnlocksOnce() throws Exception {
		Path file = temporary.getRoot().toPath().resolve("progress.properties");
		CampaignSession previous = new CampaignSession(file);
		CampaignCatalog.Mission first = CampaignCatalog.findMission("roman.1");
		previous.getProgress().associateMap(first, "saved-map-base-id");
		CampaignSession restored = new CampaignSession(file);
		restored.prepare(restored.getProgress().missionForMap("saved-map-base-id"));
		restored.started(false);
		assertTrue(restored.recordResult(winningGame(false)));
		assertFalse(restored.recordResult(winningGame(false)));
		assertTrue(restored.getProgress().isUnlocked(CampaignCatalog.findMission("roman.2")));
	}

	private CampaignSession session() { return new CampaignSession(temporary.getRoot().toPath().resolve("progress.properties")); }

	private IStartedGame winningGame(boolean multiplayer) {
		IInGamePlayer player = new IPlayer.DummyPlayer((byte) 0) {
			@Override public EWinState getWinState() { return EWinState.WON; }
		};
		return (IStartedGame) Proxy.newProxyInstance(IStartedGame.class.getClassLoader(), new Class<?>[] { IStartedGame.class }, (proxy, method, args) -> {
			switch (method.getName()) {
			case "getInGamePlayer": return player;
			case "isMultiplayerGame": return multiplayer;
			default: throw new AssertionError("Unexpected game mutation: " + method.getName());
			}
		});
	}
}
