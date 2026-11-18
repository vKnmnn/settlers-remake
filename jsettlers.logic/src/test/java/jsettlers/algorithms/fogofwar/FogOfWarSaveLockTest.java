package jsettlers.algorithms.fogofwar;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.*;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.grid.MainGrid;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.network.synchronic.timer.NetworkTimer;

public class FogOfWarSaveLockTest {
    @Test(timeout = 10000)
    public void updatesWaitForSaveAndResumeAfterwards() throws Exception {
        MatchConstants.init(new NetworkTimer(true), 0L);
        MainGrid grid = new MainGrid("fog-test", "Fog test", (short) 32, (short) 32,
                new PlayerSetting[] { new PlayerSetting((byte) 0) });
        FogOfWar fog = new FogOfWar(grid, (byte) 0);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch updated = new CountDownLatch(1);
        Thread worker = fog.new FoWThread("save-lock-test") {
            { framerate = 60; }
            @Override public void init() { started.countDown(); }
            @Override public void taskProcessor() {
                updated.countDown();
                fog.cancel();
            }
        };
        try {
            fog.pauseThreads();
            try {
                worker.start();
                assertTrue(started.await(2, TimeUnit.SECONDS));
                assertFalse("Update ran during save", updated.await(100, TimeUnit.MILLISECONDS));
            } finally {
                fog.resumeThreads();
            }
            assertTrue("Update did not resume", updated.await(2, TimeUnit.SECONDS));
        } finally {
            fog.cancel();
            worker.join(2000);
            grid.stopThreads();
            JSettlersGame.clearState();
        }
        assertFalse(worker.isAlive());
    }
}
