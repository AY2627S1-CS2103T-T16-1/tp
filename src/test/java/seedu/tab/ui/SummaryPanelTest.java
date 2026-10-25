package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    /**
     * How far apart the two number columns may sit before they read as misaligned.
     *
     * <p>The flagged row and the tag rows are built from different nodes, so their right edges
     * round apart by up to a pixel where font metrics differ between platforms; a run on Linux
     * reported 137 against 136 for a panel that is aligned to the eye. The defect this guards
     * against moved them sixteen pixels apart, so a pixel of slack costs the check nothing.
     */
    private static final double ALIGNMENT_TOLERANCE = 1.5;
    /** A pair of spellings {@code equalsIgnoreCase} joins but lowercasing them does not. */
    private static final String CAPITAL_SIGMA = "\u03A3";
    private static final String FINAL_SIGMA = "\u03C2";

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
            styledScene(root, 160, SHORT_WINDOW);

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

    private static List<String> tagCountsOf(SummaryPanel panel) {
        ScrollPane scroll = (ScrollPane) panel.getRoot().lookup("#tagScroll");
        VBox rows = (VBox) scroll.getContent();
        return rows.getChildren().stream()
                .map(row -> ((Label) ((HBox) row).getChildren().getLast()).getText())
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
    public void countByTag_caseVariantsOfOneTag_countTogether() {
        List<Student> students = List.of(
                tagged("Ann Lee", false, "T1"),
                tagged("Ben Tan", false, "t1"),
                tagged("Cai Wei", false, "T1"));

        // tag and find make T1 and t1 one tag, so the panel should give the size of that group
        assertEquals(Map.of("T1", 3), SummaryPanel.countByTag(students));
    }

    @Test
    public void countByTag_variantMostStudentsCarry_namesTheGroup() {
        List<Student> students = List.of(
                tagged("Ann Lee", false, "T1"),
                tagged("Ben Tan", false, "t1"),
                tagged("Cai Wei", false, "t1"));

        // the row reads as most of the group typed the tag, whichever spelling that is
        assertEquals(Map.of("t1", 3), SummaryPanel.countByTag(students));
    }

    @Test
    public void countByTag_variantsUsedEqually_listTheGroupUnderOneOfThem() {
        List<Student> students = List.of(
                tagged("Ann Lee", false, "lab 3"),
                tagged("Ben Tan", false, "Lab 3"));

        assertEquals(Map.of("Lab 3", 2), SummaryPanel.countByTag(students));
    }

    @Test
    public void countByTag_variantsOfSeveralTags_giveEachGroupOneRow() {
        List<Student> students = List.of(
                tagged("Ann Lee", false, "T1", "Lab 3"),
                tagged("Ben Tan", false, "t1", "lab 3"),
                tagged("Cai Wei", false, "T1"));

        assertEquals(Map.of("T1", 3, "Lab 3", 2), SummaryPanel.countByTag(students));
        assertEquals(List.of("T1", "Lab 3"),
                List.copyOf(SummaryPanel.countByTag(students).keySet()));
    }

    @Test
    public void countByTag_oneStudentCarryingTwoVariants_countsThatStudentOnce() {
        List<Student> students = List.of(
                tagged("Ann Lee", false, "T1", "t1"),
                tagged("Ben Tan", false, "T1"));

        assertEquals(Map.of("T1", 2), SummaryPanel.countByTag(students));
    }

    @Test
    public void countByTag_variantsThatTagRefusesAsDuplicates_countTogether() {
        // tag reads a capital sigma and a final sigma as one tag, as equalsIgnoreCase does.
        // Folding case by lowercasing would split them: the capital lowercases to a medial
        // sigma, while the final sigma lowercases to itself
        assertTrue(CAPITAL_SIGMA.equalsIgnoreCase(FINAL_SIGMA));
        List<Student> students = List.of(
                tagged("Ann Lee", false, CAPITAL_SIGMA),
                tagged("Ben Tan", false, FINAL_SIGMA));

        assertEquals(Map.of(CAPITAL_SIGMA, 2), SummaryPanel.countByTag(students));
    }

    @Test
    public void countByTag_oneStudentCarryingTwoSuchVariants_countsThatStudentOnce() {
        List<Student> students = List.of(tagged("Ann Lee", false, CAPITAL_SIGMA, FINAL_SIGMA));

        assertEquals(Map.of(CAPITAL_SIGMA, 1), SummaryPanel.countByTag(students));
    }

    @Test
    public void panel_caseVariantsOfOneTag_showOneRowWithTheWholeGroup() throws Exception {
        ObservableList<Student> students = FXCollections.observableArrayList(
                tagged("Ann Lee", false, "T1"),
                tagged("Ben Tan", false, "t1"),
                tagged("Cai Wei", false, "T1"));

        SummaryPanel panel = onFxThread(() -> new SummaryPanel(students));

        assertEquals(List.of("T1"), tagRowsOf(panel));
        assertEquals(List.of("3"), tagCountsOf(panel));
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
            styledScene(root, 150, 400);
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
            Text drawnName = (Text) name.lookup(".text");
            return new String[] {count.getText(), drawnName.getText()};
        });

        assertEquals("12", shown[0]);
        assertEquals(longTag, shown[1], "the whole tag should be drawn across wrapped lines");
    }

    @Test
    public void panel_tagListWithAndWithoutAScrollBar_keepsOneColumnOfNumbers() throws Exception {
        for (int tagCount : new int[] {3, 30}) {
            List<String> tags = new ArrayList<>();
            for (int i = 1; i <= tagCount; i++) {
                tags.add(String.format("tutorial-group-%02d", i));
            }
            ObservableList<Student> students = FXCollections.observableArrayList(
                    tagged("Ann Lee", true, tags.toArray(String[]::new)));

            double[] edges = onFxThread(() -> {
                SummaryPanel panel = new SummaryPanel(students);
                Region root = (Region) panel.getRoot();
                styledScene(root, 150, 320);
                // the scroll bar appears during layout, and reserving room for it moves the
                // flagged row, so the pane needs to settle before the edges are read
                for (int pass = 0; pass < 5; pass++) {
                    root.applyCss();
                    root.layout();
                }
                ScrollPane scroll = (ScrollPane) root.lookup("#tagScroll");
                VBox rows = (VBox) scroll.getContent();
                Label flagged = (Label) root.lookup("#flaggedCount");
                Label firstCount = (Label) ((HBox) rows.getChildren().getFirst()).getChildren().getLast();
                return new double[] {
                    flagged.localToScene(flagged.getBoundsInLocal()).getMaxX(),
                    firstCount.localToScene(firstCount.getBoundsInLocal()).getMaxX(),
                };
            });

            assertEquals(edges[0], edges[1], ALIGNMENT_TOLERANCE,
                    "the flagged count and the tag counts should share one right edge with "
                            + tagCount + " tags");
        }
    }

    @Test
    public void rows_aWrappedTagName_centresItsCountAgainstTheWholeName() throws Exception {
        ObservableList<Student> students = FXCollections.observableArrayList(
                tagged("Ann Lee", false, "consultation-week-14"));

        double[] centres = onFxThread(() -> {
            SummaryPanel panel = new SummaryPanel(students);
            Region root = (Region) panel.getRoot();
            styledScene(root, 150, 400);
            ScrollPane scroll = (ScrollPane) root.lookup("#tagScroll");
            VBox rows = (VBox) scroll.getContent();
            HBox row = (HBox) rows.getChildren().getFirst();
            Label name = (Label) row.getChildren().getFirst();
            Label count = (Label) row.getChildren().getLast();
            assertTrue(name.getHeight() > count.getHeight() + 2,
                    "the tag name should wrap onto more than one line for this check to mean anything");
            return new double[] {
                name.getBoundsInParent().getMinY() + name.getHeight() / 2,
                count.getBoundsInParent().getMinY() + count.getHeight() / 2,
            };
        });

        // a count level with the first line reads as belonging to that line rather than the tag
        assertEquals(centres[0], centres[1], 0.5,
                "the count should sit against the middle of the whole wrapped name");
    }

    @Test
    public void rows_tagsWithSharedPrefixes_drawWholeNamesWithoutOverlap() throws Exception {
        List<String> tags = List.of("consultation-week-3", "consultation-week-4",
                "CS2103T Tutorial W09", "CS2103T Tutorial W10", "LongUnbrokenTag".repeat(10),
                "咨询课第三周");
        onFxThread(() -> {
            SummaryPanel panel = new SummaryPanel(FXCollections.observableArrayList(
                    tagged("Alice", false, tags.toArray(String[]::new))));
            Region root = (Region) panel.getRoot();
            styledScene(root, 150, 600);
            ScrollPane scroll = (ScrollPane) root.lookup("#tagScroll");
            VBox rows = (VBox) scroll.getContent();
            assertEquals(tags.size(), rows.getChildren().size());
            double previousBottom = 0;
            for (var node : rows.getChildren()) {
                HBox row = (HBox) node;
                Label name = (Label) row.getChildren().getFirst();
                Label count = (Label) row.getChildren().getLast();
                var drawnName = (Text) name.lookup(".text");
                assertTrue(tags.contains(name.getText()));
                assertEquals(name.getText(), drawnName.getText());
                assertTrue(drawnName.getLayoutBounds().getWidth() <= name.getWidth() + 0.5,
                        "the wrapped name should fit within the sidebar width");
                assertTrue(drawnName.getLayoutBounds().getHeight() <= name.getHeight() + 0.5,
                        "the complete wrapped name should fit inside its label");
                assertTrue(name.getBoundsInParent().getMaxX() <= count.getBoundsInParent().getMinX());
                assertTrue(row.getLayoutY() >= previousBottom, "wrapped rows should not overlap");
                previousBottom = row.getBoundsInParent().getMaxY();
            }
            return null;
        });
    }
}
