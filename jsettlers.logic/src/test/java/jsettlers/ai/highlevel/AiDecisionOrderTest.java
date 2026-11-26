package jsettlers.ai.highlevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import org.junit.Test;

import static org.junit.Assert.*;

public class AiDecisionOrderTest {
    @Test
    public void sharedRandomDrawsAndOrdersFollowPlayerOrderAcrossRounds() {
        Thread simulationThread = Thread.currentThread();
        Random sharedRandom = new Random(42);
        Random expectedRandom = new Random(42);
        List<String> orders = new ArrayList<>();
        List<Callable<Void>> players = new ArrayList<>();
        for (int id = 0; id < 4; id++) {
            final int player = id;
            players.add(() -> {
                assertSame("Decisions must finish on the simulation thread", simulationThread, Thread.currentThread());
                orders.add(player + ":" + sharedRandom.nextInt());
                return null;
            });
        }
        for (int round = 0; round < 20; round++) {
            AiExecutor.applyRulesInPlayerOrder(players);
            for (int player = 0; player < 4; player++) {
                assertEquals(player + ":" + expectedRandom.nextInt(), orders.get(round * 4 + player));
            }
        }
    }

    @Test
    public void interruptionStopsRemainingDecisionsAndPreservesInterruptFlag() {
        List<Callable<Void>> players = List.of(
                () -> { throw new InterruptedException("shutdown"); },
                () -> { fail("No decisions should run after interruption"); return null; });
        try {
            AiExecutor.applyRulesInPlayerOrder(players);
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
