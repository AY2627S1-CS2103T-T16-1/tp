package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    /**
     * Returns the text of the chips the row is showing, in the order it draws them.
     *
     * <p>A chip drawn outside the row counts, and fails here: skipping it quietly let a row
     * that drew a count half off its edge read as a row that drew no count at all.
     */
    private static List<String> chipsOf(TagRow row) {
        assertTrue(drawnInside(row), "a chip was drawn outside the row");
        return row.getChildrenUnmodifiable().stream()
                .filter(Node::isVisible)
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
    public void setTags_aVeryNarrowRow_namesATagAndCountsTheRest() throws Exception {
        TagRow row = rowOf(70, List.of("needsconsultation", "urgent"));

        List<String> chips = chipsOf(row);
        // a bare "1 more…" names no tag at all, and a lone chip says nothing about the
        // tags left out, so a narrow row carries a cut tag and a count of the others
        assertTrue(chips.get(0).startsWith("needsconsult"), chips.get(0));
        assertEquals("1 more\u2026", chips.get(chips.size() - 1), "chips were " + chips);
        // and no chip is drawn wider than its own text, however much room it is given
        for (Node chip : row.getChildrenUnmodifiable()) {
            Label label = (Label) chip;
            assertTrue(label.getWidth() <= label.prefWidth(-1) + 0.5,
                    "\"" + label.getText() + "\" was stretched to " + label.getWidth());
        }
    }

    @Test
    public void setTags_roomForOneChipButNotBothWhole_stillSaysHowManyMore() throws Exception {
        // wide enough for one chip and a count, but not for both chips. Dropping the count
        // here leaves nothing to say the other tag is there at all
        TagRow row = rowOf(160, List.of("needsconsultation", "urgentfollowup"));

        List<String> chips = chipsOf(row);
        assertEquals("1 more\u2026", chips.get(chips.size() - 1),
                "the row should still say how many it left out: " + chips);
        assertTrue(drawnInside(row), "a chip was drawn outside the row: " + chips);
    }

    @Test
    public void setTags_oneTagInAWideRow_drawsItAtItsOwnWidth() throws Exception {
        TagRow row = rowOf(400, List.of("alpha"));

        Label chip = (Label) row.getChildrenUnmodifiable().get(0);
        // a chip stretched to the row reads as a pill across the card rather than as a tag
        assertEquals(chip.prefWidth(-1), chip.getWidth(), 0.5,
                "the chip was stretched to " + chip.getWidth());
    }

    /** Returns true if every chip the row shows is drawn within the row. */
    private static boolean drawnInside(TagRow row) {
        return row.getChildrenUnmodifiable().stream()
                .filter(Node::isVisible)
                .allMatch(chip -> chip.getBoundsInParent().getMinX() >= -0.5
                        && chip.getBoundsInParent().getMaxX() <= row.getWidth() + 0.5);
    }

    @Test
    public void setTags_tagsOfTheLengthACourseUses_areShownWhole() throws Exception {
        // the tags this product is for: a count that cuts these cuts most of what is typed
        List<String> ordinary = List.of("attendance-low", "needs-consult", "midterm", "Lab 3");
        for (String tag : ordinary) {
            TagRow row = rowOf(400, List.of(tag));

            assertEquals(List.of(tag), chipsOf(row), "\"" + tag + "\" should be shown whole");
        }
    }

    @Test
    public void setTags_aShortTagInARowWithNoRoomForACount_isNotStretched() throws Exception {
        // narrow enough that the count has to go, and a first tag far shorter than what is
        // then left over: a chip drawn to the room reads as a pill across the card
        TagRow row = rowOf(400, List.of("T1", "needs-consult"));
        // below the row's own minimum, which is what a card beside a very long name gives it
        onFxThread(() -> {
            row.resize(60, row.getHeight());
            row.layout();
            return null;
        });

        Label chip = (Label) row.getChildrenUnmodifiable().get(0);
        assertEquals(chip.prefWidth(-1), chip.getWidth(), 0.5,
                "the chip was stretched to " + chip.getWidth() + " of a 60 wide row");
    }

    @Test
    public void setTags_aTagTheRowCuts_namesTheWholeOfItOnHover() throws Exception {
        // under the sixteen characters a chip is cut to, but too wide for the room the row
        // has, which is what happens beside a long name or a follow-up pill
        // wide enough for a chip and a count, but not for the whole of the chip
        TagRow row = rowOf(400, List.of("attendance-low", "needs-consult"));
        onFxThread(() -> {
            row.resize(140, row.getHeight());
            row.layout();
            return null;
        });

        Label chip = (Label) row.getChildrenUnmodifiable().get(0);
        assertEquals("attendance-low", chip.getText(), "the text is under the cut length");
        assertTrue(chip.getWidth() < chip.prefWidth(-1), "the row should have cut the chip");
        assertNotNull(chip.getTooltip(), "a chip the row cut should still be readable");
        assertEquals("attendance-low", chip.getTooltip().getText());
    }

    @Test
    public void setTags_oneTagInARowTooNarrowForIt_cutsItToTheRow() throws Exception {
        // the one tag a student has, on a row with no space for it: there is nothing to
        // count, so the whole row goes to the chip whatever is left of it
        TagRow row = rowOf(400, List.of("needsconsultation"));
        onFxThread(() -> {
            row.resize(30, row.getHeight());
            row.layout();
            return null;
        });

        Label chip = (Label) row.getChildrenUnmodifiable().get(0);
        assertTrue(chip.isVisible(), "the row should still name the tag it has");
        assertEquals(30, chip.getWidth(), 0.5, "the chip should be cut to the row");
        assertTrue(drawnInside(row), "the chip was drawn outside the row");
    }
}
