package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.ui.FxToolkit.onFxThread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import seedu.tab.model.student.Student;
import seedu.tab.testutil.StudentBuilder;

/**
 * Tests summary rendering and keyboard navigation.
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
    /** Where {@link #settledEdges} reports the width the tag list gives up to its scroll bar. */
    private static final int RESERVED = 2;

    @BeforeAll
    public static void startToolkit() throws Exception {
        FxToolkit.start();
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

    /**
     * Returns a student carrying {@code tagCount} tags, enough of them to overflow the panel.
     */
    private static Student carrying(int tagCount) {
        List<String> tags = new ArrayList<>();
        for (int i = 1; i <= tagCount; i++) {
            tags.add(String.format("tutorial-group-%02d", i));
        }
        return tagged("Ann Lee", true, tags.toArray(String[]::new));
    }

    /**
     * Returns the right edge of the flagged count, the right edge of the first tag count, and
     * the width the tag list is giving up to its scroll bar, once {@code root} has settled.
     *
     * <p>The scroll bar appears during layout, and keeping room for it beside the flagged count
     * moves that row, which lays the panel out again. The edges are therefore only worth
     * reading once the passes stop changing them.
     */
    private static double[] settledEdges(Region root) {
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
            scroll.getWidth() - scroll.getViewportBounds().getWidth(),
        };
    }

    @Test
    public void keyboardNavigation_overflowingTags_scrollsByLinePageAndToEitherEnd() throws Exception {
        ObservableList<Student> students = FXCollections.observableArrayList(carrying(30));
        Stage stage = onFxThread(() -> tagWindow(students));
        try {
            assertNavigationAcrossLayouts(stage, students);
        } finally {
            onFxThread(() -> {
                stage.hide();
                return null;
            });
        }
    }

    private static FxToolkit.FocusStage tagWindow(ObservableList<Student> students) {
        SummaryPanel panel = new SummaryPanel(students);
        FxToolkit.FocusStage window = new FxToolkit.FocusStage();
        window.setScene(styledScene(panel.getRoot(), 150, 400));
        window.activate();
        ScrollPane tags = (ScrollPane) panel.getRoot().lookup("#tagScroll");
        assertTrue(tags.isFocusTraversable());
        tags.requestFocus();
        return window;
    }

    private static void assertNavigationAcrossLayouts(Stage stage, ObservableList<Student> students) throws Exception {
        FxToolkit.awaitAssertion(() -> {
            ScrollPane tags = (ScrollPane) stage.getScene().lookup("#tagScroll");
            assertTrue(tags.isFocused(), "JavaFX scroll keys require actual keyboard focus");
            assertTrue(tags.getViewportBounds().getHeight() > 0);
        });
        onFxThread(() -> {
            assertTagNavigation((ScrollPane) stage.getScene().lookup("#tagScroll"));
            stage.getScene().getRoot().resize(150, 300);
            stage.getScene().getRoot().layout();
            return null;
        });
        FxToolkit.awaitAssertion(() -> {
            ScrollPane tags = (ScrollPane) stage.getScene().lookup("#tagScroll");
            assertTrue(tags.getViewportBounds().getHeight() < 200);
        });
        onFxThread(() -> {
            ScrollPane tags = (ScrollPane) stage.getScene().lookup("#tagScroll");
            assertTagNavigation(tags);
            students.clear();
            stage.getScene().getRoot().layout();
            press(tags, KeyCode.PAGE_DOWN);
            press(tags, KeyCode.PAGE_UP);
            assertEquals(tags.getVmin(), tags.getVvalue());
            assertTrue(tags.isFocused());
            return null;
        });
    }

    private static void assertTagNavigation(ScrollPane tags) {
        assertTrue(tags.getContent().getBoundsInLocal().getHeight() > tags.getViewportBounds().getHeight());
        press(tags, KeyCode.HOME);
        press(tags, KeyCode.DOWN);
        double afterLine = tags.getVvalue();
        assertTrue(afterLine > tags.getVmin());
        press(tags, KeyCode.UP);
        assertEquals(tags.getVmin(), tags.getVvalue());
        press(tags, KeyCode.PAGE_DOWN);
        assertTrue(tags.getVvalue() > afterLine, "page=" + tags.getVvalue() + ", line=" + afterLine
                + ", viewport=" + tags.getViewportBounds() + ", content=" + tags.getContent().getBoundsInLocal());
        press(tags, KeyCode.PAGE_UP);
        assertEquals(tags.getVmin(), tags.getVvalue());
        press(tags, KeyCode.END);
        assertEquals(tags.getVmax(), tags.getVvalue());
        press(tags, KeyCode.DOWN);
        assertEquals(tags.getVmax(), tags.getVvalue());
        press(tags, KeyCode.HOME);
        press(tags, KeyCode.UP);
        assertEquals(tags.getVmin(), tags.getVvalue());
    }

    private static void press(ScrollPane tags, KeyCode key) {
        tags.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", key,
                false, false, false, false));
    }

    @Test
    public void keyboardNavigation_modifiedPageKeysAndInactiveWindow_preservesScrollPosition() throws Exception {
        onFxThread(() -> {
            FxToolkit.FocusStage stage = tagWindow(
                    FXCollections.observableArrayList(carrying(30)));
            ScrollPane tags = (ScrollPane) stage.getScene().lookup("#tagScroll");
            tags.setVvalue(0.5);
            for (int modifier = 0; modifier < 4; modifier++) {
                assertPageKeyIgnored(tags, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.PAGE_DOWN,
                        modifier == 0, modifier == 1, modifier == 2, modifier == 3));
            }
            stage.deactivate();
            assertFalse(tags.isFocused());
            assertPageKeyIgnored(tags, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.PAGE_UP,
                    false, false, false, false));
            return null;
        });
    }

    /**
     * Observes the application handler before JavaFX applies its own platform-dependent bindings.
     */
    private static void assertPageKeyIgnored(ScrollPane tags, KeyEvent event) {
        double before = tags.getVvalue();
        AtomicBoolean delivered = new AtomicBoolean();
        EventHandler<KeyEvent> observer = received -> {
            delivered.set(true);
            assertEquals(before, tags.getVvalue());
            received.consume();
        };
        tags.addEventFilter(KeyEvent.KEY_PRESSED, observer);
        try {
            tags.fireEvent(event);
            assertTrue(delivered.get(), "ignored page keys must reach JavaFX");
            assertEquals(before, tags.getVvalue());
        } finally {
            tags.removeEventFilter(KeyEvent.KEY_PRESSED, observer);
        }
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
            ObservableList<Student> students = FXCollections.observableArrayList(carrying(tagCount));

            double[] edges = onFxThread(() -> {
                SummaryPanel panel = new SummaryPanel(students);
                Region root = (Region) panel.getRoot();
                styledScene(root, 150, 320);
                return settledEdges(root);
            });

            assertEquals(edges[0], edges[1], ALIGNMENT_TOLERANCE,
                    "the flagged count and the tag counts should share one right edge with "
                            + tagCount + " tags");
        }
    }

    @Test
    public void panel_aTagListThatOverflowsThenShrinks_keepsOneColumnOfNumbers() throws Exception {
        ObservableList<Student> students = FXCollections.observableArrayList(carrying(3));

        double[][] stages = onFxThread(() -> {
            SummaryPanel panel = new SummaryPanel(students);
            Region root = (Region) panel.getRoot();
            styledScene(root, 150, 320);

            double[] shortList = settledEdges(root);
            students.setAll(carrying(30));
            double[] overflowing = settledEdges(root);
            students.setAll(carrying(3));
            return new double[][] {shortList, overflowing, settledEdges(root)};
        });

        assertEquals(0, stages[0][RESERVED], 0.5, "three tags should fit without a scroll bar");
        assertTrue(stages[1][RESERVED] > 1, "thirty tags should bring out the scroll bar");
        assertEquals(0, stages[2][RESERVED], 0.5,
                "the scroll bar should go once the list is short again");

        assertEquals(stages[1][0], stages[1][1], ALIGNMENT_TOLERANCE,
                "the columns should line up once a search widens the list past the panel");
        assertEquals(stages[2][0], stages[2][1], ALIGNMENT_TOLERANCE,
                "the room kept for the scroll bar should go with it, so the columns line up again");
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
