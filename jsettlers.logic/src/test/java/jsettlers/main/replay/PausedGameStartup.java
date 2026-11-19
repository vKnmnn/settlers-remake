package jsettlers.main.replay;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import jsettlers.common.menu.EGameError;
import jsettlers.common.menu.IMapInterfaceConnector;
import jsettlers.common.menu.IStartedGame;
import jsettlers.common.menu.IStartingGame;
import jsettlers.logic.constants.MatchConstants;

/** Starts integration simulations without an uncontrolled real-time tick before fast-forward. */
public final class PausedGameStartup extends DummyStartingGameListener {
	private final CountDownLatch finished = new CountDownLatch(1);
	private IStartedGame started;
	private Exception failure;

	private PausedGameStartup() { }

	public static IStartedGame await(IStartingGame game) throws InterruptedException {
		PausedGameStartup listener = new PausedGameStartup();
		game.setListener(listener);
		if (!listener.finished.await(60, TimeUnit.SECONDS)) {
			if (game instanceof IStartedGame) ((IStartedGame) game).stopGame();
			throw new AssertionError("game startup exceeded 60 seconds");
		}
		if (listener.failure != null) throw new AssertionError("game startup failed", listener.failure);
		return listener.started;
	}

	@Override public IMapInterfaceConnector preLoadFinished(IStartedGame game) {
		// JSettlersGame calls this before scheduling the AI and starting the clock.
		MatchConstants.clock().setPausing(true);
		started = game;
		return new DummyMapInterfaceConnector();
	}

	@Override public void startFinished() { finished.countDown(); }

	@Override public void startFailed(EGameError errorType, Exception exception) {
		failure = exception == null ? new IllegalStateException(errorType.toString()) : exception;
		finished.countDown();
	}
}
