package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;

public class FxToolkitTest {

    @BeforeAll
    public static void startToolkit() throws Exception {
        FxToolkit.start();
    }

    @Test
    public void awaitAssertion_conditionNeedsMorePulses_retriesOnFxThread() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        FxToolkit.awaitAssertion(() -> {
            assertTrue(Platform.isFxApplicationThread());
            assertTrue(attempts.incrementAndGet() > 5, "the condition is not ready yet");
        });
        assertEquals(6, attempts.get());
        awaitMorePulses();
        assertEquals(6, attempts.get(), "assertions should stop after success");
    }

    @Test
    public void awaitAssertion_conditionNeverPasses_throwsLastFailureAndStops() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        AssertionError expected = new AssertionError("the scene is 764 wide for content needing 981");
        AssertionError actual = assertThrows(AssertionError.class, () ->
                FxToolkit.awaitAssertion(() -> {
                    attempts.incrementAndGet();
                    throw expected;
                }, 0, TimeUnit.SECONDS));
        assertSame(expected, actual);
        int attemptsAtTimeout = attempts.get();
        awaitMorePulses();
        assertEquals(attemptsAtTimeout, attempts.get(), "assertions should stop after timeout");
    }

    @Test
    public void awaitAssertion_unexpectedException_propagatesWithoutRetrying() {
        AtomicInteger attempts = new AtomicInteger();
        IllegalStateException expected = new IllegalStateException("unexpected failure");
        ExecutionException actual = assertThrows(ExecutionException.class, () ->
                FxToolkit.awaitAssertion(() -> {
                    attempts.incrementAndGet();
                    throw expected;
                }));
        assertSame(expected, actual.getCause());
        assertEquals(1, attempts.get());
    }

    private static void awaitMorePulses() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        FxToolkit.awaitAssertion(() -> assertTrue(attempts.incrementAndGet() > 3));
    }
}
