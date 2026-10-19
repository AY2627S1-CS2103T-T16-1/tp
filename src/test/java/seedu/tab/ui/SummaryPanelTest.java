package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import seedu.tab.model.student.Student;
import seedu.tab.testutil.StudentBuilder;

/**
 * Tests what the summary panel counts and the order it lists it in.
 */
public class SummaryPanelTest {

    private static final double SHORT_WINDOW = 600;

    @BeforeAll
    public static void startToolkit() throws Exception {
        FxToolkit.start();
    }

    private static <T> T onFxThread(Callable<T> work) throws Exception {
        return FxToolkit.onFxThread(work);
    }

    /**
     * Returns a scene holding {@code root}, dressed in the stylesheet the window uses, so
     * that a row is measured in the font and the padding the product gives it.
     */
    private static Scene styledScene(Region root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(
                UiPart.class.getResource("/view/DarkTheme.css").toExternalForm());
        root.applyCss();
        root.layout();
        return scene;
    }

    @Test
    public void panel_manyTags_keepsTheTotalInSightAndScrollsTheTags() throws Exception {
        List<Student> many = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            many.add(tagged("Student " + i, false, "tag-" + i));
        }
        ObservableList<Student> students = FXCollections.observableArrayList(many);

        boolean[] state = onFxThread(() -> {
            SummaryPanel panel = new SummaryPanel(students);
            Region root = (Region) panel.getRoot();
            assertNotNull(styledScene(root, 160, SHORT_WINDOW));

            Label total = (Label) root.lookup("#total");
            Bounds inPanel = total.localToScene(total.getBoundsInLocal());
            return new boolean[] {
                root.lookup("#tagScroll") != null,
                inPanel.getMaxY() <= SHORT_WINDOW,
            };
        });

        // forty tags are taller than a short window, and the total belongs at the foot of the
        // sidebar rather than somewhere below the bottom of it
        assertTrue(state[0], "the tag list should sit in something that can scroll");
        assertTrue(state[1], "the student total was pushed off the bottom of the window");
    }

    private static List<String> tagRowsOf(SummaryPanel panel) {
        // the rows live inside a scroll pane, whose content a lookup cannot reach until the
        // skin has built a viewport for it
        ScrollPane scroll = (ScrollPane) panel.getRoot().lookup("#tagScroll");
        VBox rows = (VBox) scroll.getContent();
        return rows.getChildren().stream()
                .map(row -> ((Label) ((HBox) row).getChildren().get(0)).getText())
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

    @Test
    public void tagCounts_tagsCountedTheSame_areListedWithoutRegardToCase() {
        List<Student> students = List.of(
                new StudentBuilder().withName("A").withTags("Zoo").build(),
                new StudentBuilder().withName("B").withTags("apple").build(),
                new StudentBuilder().withName("C").withTags("Mango").build());

        assertEquals(List.of("apple", "Mango", "Zoo"),
                List.copyOf(SummaryPanel.countByTag(students).keySet()));
    }

    @Test
    public void rows_aTagTooLongForThePanel_keepItsCountWhole() throws Exception {
        String longTag = "attendance-and-participation-term-two";
        ObservableList<Student> students = FXCollections.observableArrayList();
        for (int i = 0; i < 12; i++) {
            students.add(tagged("Student " + i, false, longTag));
        }

        String[] shown = onFxThread(() -> {
            SummaryPanel panel = new SummaryPanel(students);
            Region root = (Region) panel.getRoot();
            assertNotNull(styledScene(root, 150, 400));
            assertEquals(150, root.getWidth());

            ScrollPane scroll = (ScrollPane) root.lookup("#tagScroll");
            VBox rows = (VBox) scroll.getContent();
            HBox row = (HBox) rows.getChildren().getFirst();
            Label name = (Label) row.getChildren().getFirst();
            Label count = (Label) row.getChildren().getLast();
            assertTrue(name.prefWidth(-1) + count.prefWidth(-1) > row.getWidth(),
                    "the row should be short of space");
            assertTrue(row.getWidth() <= scroll.getViewportBounds().getWidth(),
                    "the row should fit the visible tag list");
            assertEquals("12", ((Text) count.lookup(".text")).getText(),
                    "the drawn count should contain both digits");
            assertTrue(count.getWidth() >= count.prefWidth(-1) - 0.5,
                    "the count was squeezed to " + count.getWidth()
                    + " of the " + count.prefWidth(-1) + " it needs");
            return new String[] {count.getText(), name.getText(),
                name.getTooltip() == null ? null : name.getTooltip().getText()};
        });

        // the count is the one part of the row a reader cannot work out for themselves, so
        // it is the tag that gives way
        assertEquals("12", shown[0]);
        assertTrue(shown[1].length() < longTag.length(), "the tag should be cut: " + shown[1]);
        assertEquals(longTag, shown[2], "the whole tag should be a hover away");
    }

    @Test
    public void rows_aTagTheRowCuts_nameTheWholeOfItOnHover() throws Exception {
        // shorter than the count of characters a tag is cut to, so only the width cuts it
        String tag = "CS2103T-TUTORIAL";
        ObservableList<Student> students = FXCollections.observableArrayList(
                new StudentBuilder().withName("A").withTags(tag).build());

        String tooltip = onFxThread(() -> {
            SummaryPanel panel = new SummaryPanel(students);
            Region root = (Region) panel.getRoot();
            assertNotNull(styledScene(root, 190, 400));

            Label name = root.lookupAll(".summary-row .label").stream()
                    .map(node -> (Label) node)
                    .filter(label -> tag.equals(label.getText()))
                    .findFirst().orElseThrow(() -> new AssertionError("no row names the tag"));
            // squeezed as a narrow panel squeezes it
            HBox row = (HBox) name.getParent();
            row.resize(60, row.getHeight());
            row.layout();
            return name.getTooltip() == null ? null : name.getTooltip().getText();
        });

        assertEquals(tag, tooltip, "a tag the row cut should still be readable on hover");
    }
}
