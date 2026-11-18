package jsettlers.input;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import jsettlers.common.menu.UIState;
import jsettlers.input.tasks.EGuiAction;
import jsettlers.input.tasks.SimpleGuiTask;
import org.junit.Test;
import static org.junit.Assert.*;

public class SaveFeedbackTest {
    @Test public void successIsReportedOnlyAfterSaveReturnsAndEverySaveReports() {
        List<String> events = new ArrayList<>();
        GuiTaskExecutor executor = executor(events, null);
        save(executor);
        save(executor);
        assertEquals(List.of("save started", "save finished", "save_success",
                "save started", "save finished", "save_success"), events);
    }

    @Test public void defeatedPlayerCanStillSaveButCannotIssueGameplayCommands() {
        List<String> events = new ArrayList<>();
        GuiTaskExecutor executor = executor(events, null, true);
        save(executor);
        executor.executeTask(new SimpleGuiTask(EGuiAction.DESTROY_BUILDING, (byte) 0));
        assertEquals(List.of("save started", "save finished", "save_success"), events);
    }

    @Test public void ioFailureDoesNotReportSuccess() { assertFailure(new IOException("disk full")); }
    @Test public void runtimeFailureDoesNotReportSuccess() { assertFailure(new IllegalStateException("serialization failed")); }
    @Test public void interruptionReportsFailureAndPreservesInterrupt() {
        try {
            assertFailure(new InterruptedException("cancelled"));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally { Thread.interrupted(); }
    }

    private void assertFailure(Exception failure) {
        List<String> events = new ArrayList<>();
        save(executor(events, failure));
        assertEquals(List.of("save started", "save_failed"), events);
    }

    private void save(GuiTaskExecutor executor) {
        executor.executeTask(new SimpleGuiTask(EGuiAction.QUICK_SAVE, (byte) 0));
    }

    private GuiTaskExecutor executor(List<String> events, Exception failure) {
        return executor(events, failure, false);
    }

    private GuiTaskExecutor executor(List<String> events, Exception failure, boolean defeated) {
        IGuiInputGrid grid = (IGuiInputGrid) Proxy.newProxyInstance(IGuiInputGrid.class.getClassLoader(),
                new Class<?>[] {IGuiInputGrid.class}, (proxy, method, args) -> {
                    if (method.getName().equals("hasLost")) return defeated;
                    if (method.getName().equals("save")) {
                        events.add("save started");
                        assertEquals(Byte.valueOf((byte) 0), args[0]);
                        if (failure != null) throw failure;
                        events.add("save finished");
                    }
                    return null;
                });
        ITaskExecutorGuiInterface ui = new ITaskExecutorGuiInterface() {
            @Override public void renewSelection() {}
            @Override public UIState getUIState() { return null; }
            @Override public void showStatus(String label) { events.add(label); }
        };
        return new GuiTaskExecutor(grid, ui, (byte) 0);
    }
}
