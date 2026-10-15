package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;

/**
 * Starts the JavaFX toolkit for a test, and runs work on the thread it requires.
 */
final class FxToolkit {

    // Cold window initialization under Rosetta can exceed ten seconds on hosted macOS runners.
    private static final int TIMEOUT_SECONDS = 60;

    private FxToolkit() {} // this class only starts the toolkit

    /**
     * Starts the toolkit if it is not already running, and keeps it running.
     *
     * <p>JavaFX shuts itself down when the last window closes, and runs nothing more after
     * that, so one test that shows a window would leave every test after it waiting on a
     * thread that has gone.
     */
    static void start() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException alreadyRunning) {
            started.countDown();
        }
        assertTrue(started.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "the toolkit did not start");
        Platform.runLater(() -> Platform.setImplicitExit(false));
    }

    /** Runs {@code work} on the thread JavaFX requires its nodes to be built on. */
    static <T> T onFxThread(Callable<T> work) throws Exception {
        FutureTask<T> task = new FutureTask<>(work);
        Platform.runLater(task);
        return task.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}
