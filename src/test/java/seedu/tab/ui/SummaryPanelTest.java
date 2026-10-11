package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import seedu.tab.model.student.Student;
import seedu.tab.testutil.StudentBuilder;

/**
 * Tests the counting the summary panel shows. The arithmetic is kept apart from the nodes, so
 * it can be checked without a toolkit.
 */
public class SummaryPanelTest {

    private static final int TIMEOUT_SECONDS = 10;

    @BeforeAll
    public static void startToolkit() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException alreadyRunning) {
            started.countDown();
        }
        assertTrue(started.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "the toolkit did not start");
    }

    private static <T> T onFxThread(Callable<T> work) throws Exception {
        FutureTask<T> task = new FutureTask<>(work);
        Platform.runLater(task);
        return task.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private static List<String> tagRowsOf(SummaryPanel panel) {
        VBox rows = (VBox) panel.getRoot().lookup("#tagRows");
        return rows.getChildren().stream()
                .map(row -> ((Label) ((javafx.scene.layout.HBox) row).getChildren().get(0)).getText())
                .toList();
    }

    private static String textOf(SummaryPanel panel, String id) {
        return ((Label) panel.getRoot().lookup(id)).getText();
    }

    private static Student tagged(String name, boolean isFlagged, String... tags) {
        return new StudentBuilder().withName(name).withTags(tags).withFlag(isFlagged).build();
    }

    @Test
    public void countByTag_mixedBook_countsEachTag() {
        List<Student> students = List.of(
                tagged("Alice", false, "CS2103T", "T1"),
                tagged("Bob", true, "CS2103T"),
                tagged("Cara", false, "CS2103T", "T1", "Lab 3"));

        assertEquals(Map.of("CS2103T", 3, "T1", 2, "Lab 3", 1), SummaryPanel.countByTag(students));
    }

    @Test
    public void countByTag_ties_areOrderedByCountThenAlphabetically() {
        List<Student> students = List.of(
                tagged("Alice", false, "T3", "T1", "CS2103T"),
                tagged("Bob", false, "T3", "T1", "CS2103T"),
                tagged("Cara", false, "CS2103T"));

        // a set would let equally used tags swap places between runs
        assertEquals(List.of("CS2103T", "T1", "T3"),
                List.copyOf(SummaryPanel.countByTag(students).keySet()));
    }

    @Test
    public void countByTag_noStudents_isEmpty() {
        assertTrue(SummaryPanel.countByTag(List.of()).isEmpty());
    }

    @Test
    public void countByTag_studentWithoutTags_contributesNothing() {
        List<Student> students = List.of(tagged("Alice", false), tagged("Bob", false, "T1"));

        assertEquals(Map.of("T1", 1), SummaryPanel.countByTag(students));
    }

    @Test
    public void countFlagged_countsOnlyTheFlagged() {
        List<Student> students = List.of(
                tagged("Alice", true), tagged("Bob", false), tagged("Cara", true));

        assertEquals(2, SummaryPanel.countFlagged(students));
    }

    @Test
    public void countFlagged_noStudents_isZero() {
        assertEquals(0, SummaryPanel.countFlagged(List.of()));
    }

    @Test
    public void describeTotal_oneStudent_usesTheSingular() {
        assertEquals("1 student", SummaryPanel.describeTotal(1));
    }

    @Test
    public void describeTotal_anyOtherCount_usesThePlural() {
        assertEquals("0 students", SummaryPanel.describeTotal(0));
        assertEquals("48 students", SummaryPanel.describeTotal(48));
    }

    @Test
    public void panel_mixedBook_showsTheCountsItHasWorkedOut() throws Exception {
        ObservableList<Student> students = FXCollections.observableArrayList(
                tagged("Alice", true, "CS2103T", "T1"),
                tagged("Bob", false, "CS2103T"));

        SummaryPanel panel = onFxThread(() -> new SummaryPanel(students));

        assertEquals("1", textOf(panel, "#flaggedCount"));
        assertEquals("2 students", textOf(panel, "#total"));
        assertEquals(List.of("CS2103T", "T1"), tagRowsOf(panel));
    }

    @Test
    public void panel_listChanges_followsIt() throws Exception {
        ObservableList<Student> students = FXCollections.observableArrayList(
                tagged("Alice", true, "T1"));
        SummaryPanel panel = onFxThread(() -> new SummaryPanel(students));

        // a command that narrows or grows the displayed list must take the summary with it
        onFxThread(() -> students.setAll(
                tagged("Bob", false, "T2"), tagged("Cara", false, "T2")));

        assertEquals("0", textOf(panel, "#flaggedCount"));
        assertEquals("2 students", textOf(panel, "#total"));
        assertEquals(List.of("T2"), tagRowsOf(panel));
    }

    @Test
    public void panel_emptyList_showsZeroesRatherThanNothing() throws Exception {
        SummaryPanel panel = onFxThread(() ->
                new SummaryPanel(FXCollections.observableArrayList()));

        assertEquals("0", textOf(panel, "#flaggedCount"));
        assertEquals("0 students", textOf(panel, "#total"));
        assertTrue(tagRowsOf(panel).isEmpty());
    }
}
