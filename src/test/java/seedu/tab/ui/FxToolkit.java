package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.javafx.stage.WindowHelper;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * Starts the JavaFX toolkit for a test, and runs work on the thread it requires.
 */
final class FxToolkit {

    // Cold window initialization under Rosetta can exceed ten seconds on hosted macOS runners.
    private static final int TIMEOUT_SECONDS = 60;

    /**
     * A window whose activation is controlled by the test instead of the desktop window manager.
     * Scene focus and control key bindings still use JavaFX's normal focus machinery.
     */
    static final class FocusStage extends Stage {
        void activate() {
            WindowHelper.setFocused(this, true);
        }

        void deactivate() {
            WindowHelper.setFocused(this, false);
        }
    }

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

    /**
     * Retries assertions after successive pulses until they pass or the shared timeout expires.
     */
    static void awaitAssertion(Runnable assertion) throws Exception {
        awaitAssertion(assertion, TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Retries assertions with a caller-supplied timeout, preserving the last assertion failure.
     */
    static void awaitAssertion(Runnable assertion, long timeout, TimeUnit unit) throws Exception {
        CompletableFuture<Void> result = new CompletableFuture<>();
        AtomicReference<AssertionError> lastFailure = new AtomicReference<>();
        AnimationTimer timer = onFxThread(() -> startPolling(assertion, result, lastFailure));
        try {
            result.get(timeout, unit);
        } catch (TimeoutException timeoutFailure) {
            if (lastFailure.get() != null) {
                throw lastFailure.get();
            }
            throw timeoutFailure;
        } finally {
            result.cancel(false);
            onFxThread(() -> {
                timer.stop();
                return null;
            });
        }
    }

    private static AnimationTimer startPolling(Runnable assertion, CompletableFuture<Void> result,
            AtomicReference<AssertionError> lastFailure) {
        checkAssertion(assertion, result, lastFailure);
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Assertions run after the pulse's layout work rather than during animation processing.
                Platform.runLater(() -> checkAssertion(assertion, result, lastFailure));
            }
        };
        timer.start();
        return timer;
    }

    private static void checkAssertion(Runnable assertion, CompletableFuture<Void> result,
            AtomicReference<AssertionError> lastFailure) {
        if (result.isDone()) {
            return;
        }
        try {
            assertion.run();
            result.complete(null);
        } catch (AssertionError failure) {
            lastFailure.set(failure);
        } catch (RuntimeException failure) {
            result.completeExceptionally(failure);
        }
    }

    /**
     * Runs {@code work} on the thread JavaFX requires its nodes to be built on.
     */
    static <T> T onFxThread(Callable<T> work) throws Exception {
        FutureTask<T> task = new FutureTask<>(work);
        Platform.runLater(task);
        return task.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}
