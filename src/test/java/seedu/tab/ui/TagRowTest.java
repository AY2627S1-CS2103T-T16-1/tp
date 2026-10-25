package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

public class TagRowTest {

    private static final String ELLIPSIS = "…";

    @BeforeAll
    public static void startToolkit() throws Exception {
        FxToolkit.start();
    }

    /** Runs {@code work} on the thread JavaFX requires its nodes to be built on. */
    private static <T> T onFxThread(Callable<T> work) throws Exception {
        return FxToolkit.onFxThread(work);
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
    public void setTags_longSimilarLabelsWithRoom_remainDistinct() throws Exception {
        List<String> names = List.of("Tutorial Group 01", "Tutorial Group 02",
                "CS2103T Tutorial W09", "CS2103T Tutorial W10");
        TagRow row = rowOf(1000, names);

        assertEquals(names, drawnChipsOf(row));
        onFxThread(() -> {
            row.resize(100, row.getHeight());
            row.layout();
            row.resize(1000, row.getHeight());
            row.layout();
            assertEquals(names, drawnChipsOf(row));
            return null;
        });
    }

    @Test
    public void setTags_noTags_takesNoSpace() throws Exception {
        TagRow row = rowOf(400, List.of());

        assertEquals(List.of(), chipsOf(row));
        assertEquals(0, row.prefWidth(-1), "an empty row should ask for nothing");
        assertEquals(0, row.minWidth(-1), "an empty row should leave the name the whole row");
    }

    @Test
    public void setTags_moreThanThreeTags_showsEveryChipThatFits() throws Exception {
        TagRow row = rowOf(400, List.of("alpha", "bravo", "charlie", "delta", "echo"));

        // A wide row uses its room for complete chips.
        assertEquals(List.of("alpha", "bravo", "charlie", "delta", "echo"), chipsOf(row));
    }

    @Test
    public void setTags_manyTags_showsAllWhenTheyFit() throws Exception {
        List<String> names = IntStream.range(0, 20).mapToObj(i -> "tag" + i).toList();
        TagRow row = rowOf(2000, names);

        assertEquals(names, chipsOf(row));
        assertEquals(names.size() + 1, row.getChildrenUnmodifiable().size());
    }

    @Test
    public void setTags_manyTagsInANarrowRow_countsAndNamesEveryHiddenTag() throws Exception {
        List<String> names = IntStream.range(0, 20).mapToObj(i -> "tag" + i).toList();
        TagRow row = rowOf(400, names);
        onFxThread(() -> {
            row.resize(60, row.getHeight());
            row.layout();
            return null;
        });

        List<String> visibleChips = chipsOf(row);
        assertEquals(1, visibleChips.size());
        // Every hidden tag must be included in the total.
        assertEquals(20, Integer.parseInt(visibleChips.getFirst().split(" ")[0]));
        Label overflow = (Label) row.getChildrenUnmodifiable().getLast();
        assertEquals(String.join(", ", names), overflow.getTooltip().getText());
    }

    @Test
    public void setTags_replacingManyTags_replacesLabelsAndOverflow() throws Exception {
        List<String> originalTags = List.of("alpha", "bravo", "charlie", "delta", "echo");
        TagRow row = rowOf(1000, originalTags);
        onFxThread(() -> {
            row.resize(60, row.getHeight());
            row.layout();
            return null;
        });
        Label originalOverflow = (Label) row.getChildrenUnmodifiable().getLast();
        assertTrue(originalOverflow.isVisible(), "the replacement must start with populated overflow");
        assertEquals(String.join(", ", originalTags), originalOverflow.getTooltip().getText());
        for (List<String> names : List.of(List.of("Lab 3"), List.<String>of(),
                List.of("T1", "T2", "T3", "T4"))) {
            onFxThread(() -> {
                row.setTags(names);
                row.getParent().applyCss();
                row.getParent().requestLayout();
                row.getParent().layout();
                return null;
            });

            assertEquals(names.size() + 1, row.getChildrenUnmodifiable().size());
            List<String> expected = names;
            assertEquals(expected, chipsOf(row));
            Label overflow = (Label) row.getChildrenUnmodifiable().getLast();
            assertFalse(overflow.isVisible(), "replacement tags fit, so the previous overflow must disappear");
            assertEquals("", overflow.getTooltip().getText());
        }
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
    public void setTags_aRowTooNarrowForEvenACutChip_countsTheTagsInstead() throws Exception {
        // what is left of a chip here would be an ellipsis and little else, which names no
        // tag, so the row says how many there are instead. The width is found rather than
        // picked: a label holds its text whatever room it has, so the only widths that say
        // anything are the ones where what is drawn is the whole of it
        List<String> readable = rowsDrawingOnlyTheCount(List.of("attendance-low", "bravo")).stream()
                .map(row -> drawnChipsOf(row).get(0))
                .distinct()
                .toList();

        assertEquals(List.of("2 tags"), readable, "the row drew: " + readable);
    }

    @Test
    public void setTags_threeTagsWithNoRoomForAChip_showsExactlyThreeTags() throws Exception {
        List<TagRow> rows = rowsDrawingOnlyTheCount(List.of("attendance-low", "bravo", "charlie"));

        for (TagRow row : rows) {
            assertEquals(List.of("3 tags"), drawnChipsOf(row));
        }
    }

    @Test
    public void setTags_aRowShowingNoChip_doesNotSayTheTagsAreMore() throws Exception {
        // nothing precedes the count there, so "more" would be more than what. Swept
        // rather than probed at one width, because which branch the row takes moves with
        // the glyphs the platform draws
        List<String> saidMore = new ArrayList<>();
        for (double width = 20; width <= 110; width += 2) {
            List<String> chips = drawnChipsOf(narrowRowOf(width, List.of("attendance-low", "bravo")));
            if (chips.size() == 1 && chips.get(0).contains("more")) {
                saidMore.add(width + ": " + chips);
            }
        }

        assertTrue(saidMore.isEmpty(), "the row counted what it left out at: " + saidMore);
    }

    @Test
    public void setTags_aRowShowingNoChip_namesTheTagsOnHover() throws Exception {
        TagRow row = rowsDrawingOnlyTheCount(List.of("attendance-low", "bravo")).get(0);

        Label trailing = (Label) row.getChildrenUnmodifiable().stream()
                .filter(node -> node.getStyleClass().contains("tag-overflow"))
                .findFirst().orElseThrow();
        // the chips are gone, so the hover is the only way left to read the tags
        assertEquals("attendance-low, bravo", trailing.getTooltip().getText());
    }

    /**
     * Every row, across the narrow widths, that has given up its last chip and draws the
     * whole of the count it put there instead.
     *
     * <p>The widths are found rather than picked. A label holds all of its text however
     * little room it has, so a row drawn at a width named in pixels may be showing an
     * ellipsis on one platform and the whole count on another, and a check made there
     * reads the same either way. Only a width whose drawn text is the whole of the held
     * text says anything, and this fails rather than returning none.
     */
    private static List<TagRow> rowsDrawingOnlyTheCount(List<String> names) throws Exception {
        List<TagRow> rows = new ArrayList<>();
        for (double width = 20; width <= 110; width += 2) {
            TagRow row = narrowRowOf(width, names);
            List<String> drawn = drawnChipsOf(row);
            if (drawn.size() == 1 && drawn.equals(chipsOf(row))) {
                rows.add(row);
            }
        }

        assertFalse(rows.isEmpty(), "no width drew the count in full, so nothing was checked");
        return rows;
    }

    /**
     * The text the row's visible chips are drawn with, which is what a reader sees. A
     * label holds its whole text however little of it the label is given room to draw,
     * so asking the label says nothing about what reached the window.
     */
    private static List<String> drawnChipsOf(TagRow row) {
        assertTrue(drawnInside(row), "a chip was drawn outside the row");
        return row.getChildrenUnmodifiable().stream()
                .filter(Node::isVisible)
                .map(node -> (Label) node)
                .sorted(Comparator.comparingDouble(Label::getLayoutX))
                .map(TagRowTest::drawnTextOf)
                .toList();
    }

    /** What {@code label} actually draws, read from the text node inside it. */
    private static String drawnTextOf(Label label) {
        return label.lookupAll(".text").stream()
                .filter(Text.class::isInstance)
                .map(node -> ((Text) node).getText())
                .findFirst()
                .orElse("");
    }

    /**
     * A row laid out at {@code width} whatever it asks for, as the card's first line lays
     * it out. A holder would not go below the row's own minimum, so the widths at which
     * the row gives up its last chip are only reachable this way.
     */
    private static TagRow narrowRowOf(double width, List<String> names) throws Exception {
        return onFxThread(() -> {
            TagRow row = new TagRow();
            // the stylesheet dresses the chips through #tags, so a row without the id is
            // measured in a font and padding the product never uses
            row.setId("tags");
            row.setTags(names);
            Region holder = new StackPane(row);
            Scene scene = new Scene(holder, Math.max(width, 400), 40);
            scene.getStylesheets().add(
                    UiPart.class.getResource("/view/DarkTheme.css").toExternalForm());
            holder.applyCss();
            holder.layout();
            row.resizeRelocate(0, 0, width, row.getHeight());
            row.layout();
            return row;
        });
    }

    @Test
    public void setTags_theTrailingChip_namesWhatItLeftOut() throws Exception {
        TagRow row = narrowRowOf(180, List.of("alpha", "bravo", "charlie", "delta", "echo"));

        Label trailing = (Label) row.getChildrenUnmodifiable().stream()
                .filter(node -> node.getStyleClass().contains("tag-overflow"))
                .findFirst().orElseThrow();
        int shown = chipsOf(row).size() - 1;
        assertEquals(String.join(", ", List.of("alpha", "bravo", "charlie", "delta", "echo")
                .subList(shown, 5)), trailing.getTooltip().getText());
    }

    @Test
    public void setTags_aTagLongerThanAChip_isCutWithTheWholeOnHover() throws Exception {
        String whole = "test".repeat(20);
        TagRow row = narrowRowOf(100, List.of(whole));

        Label chip = (Label) row.getChildrenUnmodifiable().get(0);
        assertTrue(chip.getWidth() < chip.prefWidth(-1), "the chip should be cut");
        assertEquals(whole, chip.getTooltip().getText());
    }

    @Test
    public void prefWidth_manyTags_asksForEveryCompleteChip() throws Exception {
        List<String> many = IntStream.range(0, 20).mapToObj(i -> "tag" + i).toList();
        double wide = onFxThread(() -> rowOfWidth(many));
        double few = onFxThread(() -> rowOfWidth(List.of("tag0", "tag1", "tag2")));

        // Headline allocation protects the name; the row reports all its content.
        assertTrue(wide > few * 2,
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
        // This allocation fits a partial chip and a count, but not the complete tag.
        TagRow row = rowOf(400, List.of("attendance-low", "needs-consult"));
        onFxThread(() -> {
            row.resize(140, row.getHeight());
            row.layout();
            return null;
        });

        Label chip = (Label) row.getChildrenUnmodifiable().get(0);
        assertEquals("attendance-low", chip.getText(), "the held text should remain complete");
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
