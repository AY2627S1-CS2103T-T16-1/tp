package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class TagRowTest {

    private static final int TIMEOUT_SECONDS = 10;
    private static final String ELLIPSIS = "…";

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

    /** Runs {@code work} on the thread JavaFX requires its nodes to be built on. */
    private static <T> T onFxThread(Callable<T> work) throws Exception {
        FutureTask<T> task = new FutureTask<>(work);
        Platform.runLater(task);
        return task.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Returns a row holding {@code names}, laid out at {@code width} in the stylesheet the
     * window uses, so that what it shows is measured in the font the product draws.
     */
    private static TagRow rowOf(double width, List<String> names) throws Exception {
        return onFxThread(() -> {
            TagRow row = new TagRow();
            row.setId("tags");
            row.setTags(names);
            Region holder = new StackPane(row);
            Scene scene = new Scene(holder, width, 40);
            scene.getStylesheets().add(
                    UiPart.class.getResource("/view/DarkTheme.css").toExternalForm());
            holder.applyCss();
            holder.layout();
            return row;
        });
    }

    /** Returns the text of the chips the row is showing, in the order it draws them. */
    private static List<String> chipsOf(TagRow row) {
        return row.getChildrenUnmodifiable().stream()
                .filter(Node::isVisible)
                .filter(chip -> chip.getBoundsInParent().getMaxX() <= row.getWidth() + 0.5)
                .map(node -> (Label) node)
                .sorted(Comparator.comparingDouble(Label::getLayoutX))
                .map(Label::getText)
                .toList();
    }

    @Test
    public void setTags_roomForEveryChip_showsThemAll() throws Exception {
        TagRow row = rowOf(400, List.of("Lab 3", "T1"));

        assertEquals(List.of("Lab 3", "T1"), chipsOf(row));
    }

    @Test
    public void setTags_noTags_takesNoSpace() throws Exception {
        TagRow row = rowOf(400, List.of());

        assertEquals(List.of(), chipsOf(row));
        assertEquals(0, row.prefWidth(-1), "an empty row should ask for nothing");
        assertEquals(0, row.minWidth(-1), "an empty row should leave the name the whole row");
    }

    @Test
    public void setTags_moreTagsThanTheCapAllows_countsTheRest() throws Exception {
        TagRow row = rowOf(400, List.of("alpha", "bravo", "charlie", "delta", "echo"));

        // a card is a summary, so the chips beyond the cap are counted rather than shown
        assertEquals(List.of("alpha", "bravo", "charlie", "2 more" + ELLIPSIS), chipsOf(row));
    }

    @Test
    public void setTags_aNarrowRow_dropsChipsRatherThanSqueezingThem() throws Exception {
        TagRow row = rowOf(130, List.of("alpha", "bravo", "charlie"));

        List<String> chips = chipsOf(row);
        assertTrue(chips.size() < 3, "the row should drop chips it has no space for: " + chips);
        assertEquals("2 more" + ELLIPSIS, chips.get(chips.size() - 1));
        // what survives is drawn whole, rather than every chip being cut to a letter or two
        assertEquals("alpha", chips.get(0));
    }

    @Test
    public void setTags_theTrailingChip_namesWhatItLeftOut() throws Exception {
        TagRow row = rowOf(400, List.of("alpha", "bravo", "charlie", "delta", "echo"));

        Label trailing = (Label) row.getChildrenUnmodifiable().stream()
                .filter(node -> node.getStyleClass().contains("tag-overflow"))
                .findFirst().orElseThrow();
        assertEquals("delta, echo", trailing.getTooltip().getText());
    }

    @Test
    public void setTags_aTagLongerThanAChip_isCutWithTheWholeOnHover() throws Exception {
        String whole = "test".repeat(20);
        TagRow row = rowOf(400, List.of(whole));

        Label chip = (Label) row.getChildrenUnmodifiable().get(0);
        assertTrue(chip.getText().length() < whole.length(), "the chip should be cut");
        assertEquals(whole, chip.getTooltip().getText());
    }

    @Test
    public void prefWidth_manyTags_asksOnlyForWhatItDraws() throws Exception {
        List<String> many = IntStream.range(0, 20).mapToObj(i -> "tag" + i).toList();
        double wide = onFxThread(() -> rowOfWidth(many));
        double few = onFxThread(() -> rowOfWidth(List.of("tag0", "tag1", "tag2")));

        // a row that asked for all twenty chips left the name beside it nothing to shrink into
        assertTrue(wide < few * 2,
                "twenty tags asked for " + wide + " against " + few + " for three");
    }

    private static double rowOfWidth(List<String> names) {
        TagRow row = new TagRow();
        row.setTags(names);
        return row.prefWidth(-1);
    }

    @Test
    public void setTags_aRowTooNarrowForAChipAndACount_showsTheFirstTag() throws Exception {
        TagRow row = rowOf(70, List.of("needsconsultation", "urgent"));

        List<String> chips = chipsOf(row);
        // a bare "2 more…" names no tag at all, where the first tag cut short still says
        // which tag the student carries
        assertEquals(1, chips.size(), "the row should show one tag, not " + chips);
        assertEquals("needsconsult\u2026", chips.get(0));
    }

    @Test
    public void setTags_roomForOneChipButNotACount_leavesTheCountOut() throws Exception {
        // wide enough for "alpha" but not for "alpha" and a count beside it. Drawing the
        // count anyway put it half outside the row, where it read as "1 m"
        TagRow row = rowOf(58, List.of("alpha", "bravo"));

        List<String> chips = chipsOf(row);
        assertEquals(List.of("alpha"), chips);
        assertTrue(drawnInside(row), "a chip was drawn outside the row: " + chips);
    }

    /** Returns true if every chip the row shows is drawn within the row. */
    private static boolean drawnInside(TagRow row) {
        return row.getChildrenUnmodifiable().stream()
                .filter(Node::isVisible)
                .allMatch(chip -> chip.getBoundsInParent().getMinX() >= -0.5
                        && chip.getBoundsInParent().getMaxX() <= row.getWidth() + 0.5);
    }
}
