package jsettlers.main.swing;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import jsettlers.common.menu.IStartedGame;
import jsettlers.common.player.EWinState;
import jsettlers.common.player.IInGamePlayer;
import jsettlers.common.player.IPlayer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EndgameMonitorTest {
	@Test
	public void victoryAndDefeatNotifyOnceWithoutStoppingMultiplayer() {
		for (EWinState outcome : new EWinState[] { EWinState.WON, EWinState.LOST }) {
			AtomicReference<EWinState> state = new AtomicReference<>(EWinState.UNDECIDED);
			IInGamePlayer player = new IPlayer.DummyPlayer((byte) 0) {
				@Override public EWinState getWinState() { return state.get(); }
			};
			AtomicInteger shown = new AtomicInteger();
			EndgameMonitor monitor = new EndgameMonitor(game(player, false), ignored -> shown.incrementAndGet());
			monitor.poll();
			assertEquals(0, shown.get());
			state.set(outcome);
			monitor.poll();
			monitor.poll();
			assertEquals(1, shown.get());
		}
	}

	@Test
	public void shutdownAndCancelledGamesDoNotOpenAResultWindow() {
		IInGamePlayer loser = new IPlayer.DummyPlayer((byte) 0) {
			@Override public EWinState getWinState() { return EWinState.LOST; }
		};
		AtomicInteger shown = new AtomicInteger();
		new EndgameMonitor(game(loser, true), ignored -> shown.incrementAndGet()).poll();
		EndgameMonitor cancelled = new EndgameMonitor(game(loser, false), ignored -> shown.incrementAndGet());
		cancelled.stop();
		cancelled.poll();
		new EndgameMonitor(game(null, false), ignored -> shown.incrementAndGet()).poll();
		assertEquals(0, shown.get());
	}

	private static IStartedGame game(IInGamePlayer player, boolean stopped) {
		return (IStartedGame) Proxy.newProxyInstance(IStartedGame.class.getClassLoader(), new Class<?>[] { IStartedGame.class },
				(proxy, method, args) -> {
					switch (method.getName()) {
					case "getInGamePlayer": return player;
					case "isShutdownFinished": return stopped;
					case "isMultiplayerGame": return true;
					default: throw new AssertionError("Unexpected game mutation: " + method.getName());
					}
				});
	}
}
