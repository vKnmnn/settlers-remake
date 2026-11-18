package jsettlers.main.swing;

import java.util.function.Consumer;
import javax.swing.Timer;

import jsettlers.common.menu.IStartedGame;
import jsettlers.common.player.EWinState;
import jsettlers.common.player.IInGamePlayer;

/** Observes the local result without stopping the simulation or other players. Runs on the EDT. */
final class EndgameMonitor {
	private final IStartedGame game;
	private final Consumer<IStartedGame> finished;
	private final Timer timer;
	private boolean stopped;

	EndgameMonitor(IStartedGame game, Consumer<IStartedGame> finished) {
		this.game = game;
		this.finished = finished;
		timer = new Timer(500, event -> poll());
	}

	void start() {
		if (!stopped) timer.start();
	}

	void poll() {
		if (stopped) return;
		if (game.isShutdownFinished()) {
			stop();
			return;
		}
		IInGamePlayer player = game.getInGamePlayer();
		if (player != null && player.getWinState() != EWinState.UNDECIDED) {
			stop();
			finished.accept(game);
		}
	}

	void stop() {
		stopped = true;
		timer.stop();
	}
}
