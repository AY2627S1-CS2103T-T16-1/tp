package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import seedu.tab.model.student.Student;
import seedu.tab.testutil.StudentBuilder;

public class StudentCardTest {

    private static final int TIMEOUT_SECONDS = 10;
    private static final double CARD_WIDTH = 420;
    /** What the card cuts a name to, as the card states it. */
    private static final int MAX_NAME_CHARACTERS_UNDER_TEST = 22;
    /** About what the list gives a card when the window opens at its default size. */
    private static final double DEFAULT_CARD_WIDTH = 490;

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
     * Returns a scene holding {@code root}, dressed in the stylesheet the window uses, so that
     * anything measured is measured in the font the product actually shows.
     */
    private static Scene styledScene(Region root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(
                UiPart.class.getResource("/view/DarkTheme.css").toExternalForm());
        root.applyCss();
        root.layout();
        return scene;
    }

    private static Label emailLabelOf(Student student) throws Exception {
        return onFxThread(() -> (Label) new StudentCard(student, 1).getRoot().lookup("#email"));
    }

    private static Label followUpLabelOf(Student student) throws Exception {
        return onFxThread(() -> (Label) new StudentCard(student, 1).getRoot().lookup("#followUp"));
    }

    @Test
    public void constructor_studentWithEmail_showsTheEmail() throws Exception {
        Student student = new StudentBuilder().withEmail("alice@example.com").build();
        Label email = emailLabelOf(student);

        assertEquals("alice@example.com", email.getText());
        assertTrue(email.isManaged());
        assertTrue(email.isVisible());
    }

    @Test
    public void constructor_studentWithoutEmail_leavesTheRowOut() throws Exception {
        Student student = new StudentBuilder().withoutEmail().build();
        Label email = emailLabelOf(student);

        // the row is unmanaged as well as hidden, so the card does not keep a blank line for it
        assertFalse(email.isManaged());
        assertFalse(email.isVisible());
    }

    @Test
    public void constructor_flaggedStudent_showsFollowUpStatus() throws Exception {
        Label followUp = followUpLabelOf(new StudentBuilder().withFlag(true).build());

        assertEquals("Needs follow-up", followUp.getText());
        assertTrue(followUp.isManaged());
        assertTrue(followUp.isVisible());
    }

    @Test
    public void constructor_unflaggedStudent_leavesFollowUpRowOut() throws Exception {
        Label followUp = followUpLabelOf(new StudentBuilder().withFlag(false).build());

        assertFalse(followUp.isManaged());
        assertFalse(followUp.isVisible());
    }

    @Test
    public void constructor_withoutEmailOrFollowUp_leavesBothRowsOut() throws Exception {
        Student student = new StudentBuilder().withoutEmail().withFlag(false).build();
        StudentCard card = onFxThread(() -> new StudentCard(student, 1));
        Label email = (Label) card.getRoot().lookup("#email");
        Label followUp = (Label) card.getRoot().lookup("#followUp");

        assertFalse(email.isManaged());
        assertFalse(email.isVisible());
        assertFalse(followUp.isManaged());
        assertFalse(followUp.isVisible());
    }

    @Test
    public void constructor_nameAndPhoneHoldingSymbols_areShownAsGiven() throws Exception {
        Student student = new StudentBuilder().withName("Ravi s/o Kumaran").withPhone("+65 9123 4567")
                .withoutEmail().build();
        StudentCard card = onFxThread(() -> new StudentCard(student, 7));

        assertEquals("Ravi s/o Kumaran", ((Label) card.getRoot().lookup("#name")).getText());
        assertEquals("+65 9123 4567", ((Label) card.getRoot().lookup("#phone")).getText());
        assertEquals("7", ((Label) card.getRoot().lookup("#id")).getText());
    }

    @Test
    public void constructor_anyStudent_leavesTheNusIdSlotOut() throws Exception {
        // the card lays the slot out so that the field can fill it without further UI work,
        // and must not show a placeholder in the meantime
        Label nusId = onFxThread(() -> (Label) new StudentCard(new StudentBuilder().build(), 1)
                .getRoot().lookup("#nusId"));

        assertFalse(nusId.isManaged());
        assertFalse(nusId.isVisible());
    }

    @Test
    public void constructor_flaggedStudent_putsTheMarkerInTheTrailingPill() throws Exception {
        StudentCard card = onFxThread(() -> new StudentCard(
                new StudentBuilder().withFlag(true).build(), 1));
        Label followUp = (Label) card.getRoot().lookup("#followUp");

        assertTrue(followUp.getStyleClass().contains("card-pill"),
                "the follow-up marker should be the trailing pill the mockup draws");
    }

    @Test
    public void constructor_phoneAndEmail_shareTheSecondaryLine() throws Exception {
        Student student = new StudentBuilder().withPhone("91234567")
                .withEmail("alice@example.com").build();
        StudentCard card = onFxThread(() -> new StudentCard(student, 1));
        Label phone = (Label) card.getRoot().lookup("#phone");
        Label email = (Label) card.getRoot().lookup("#email");

        // the mockup drops the phone, but it is the one required contact field, so it stays
        assertEquals(phone.getParent(), email.getParent());
        assertTrue(phone.isVisible());
        assertTrue(email.isVisible());
    }

    @Test
    public void constructor_longNameAndTag_doNotPushTheCardOutOfShape() throws Exception {
        Student student = new StudentBuilder()
                .withName("Nurul Izzah binte Hassan Al-Rashid Abdullah Mohamed")
                .withPhone("91112222")
                .withEmail("a.very.long.address.indeed@u.nus.edu")
                .withTags("AY2627 Sem 1 CS2103T T16").withFlag(true).build();

        double[] measured = onFxThread(() -> {
            Region root = (Region) new StudentCard(student, 48).getRoot();
            assertNotNull(styledScene(root, CARD_WIDTH, 120));
            Label pill = (Label) root.lookup("#followUp");
            Label phone = (Label) root.lookup("#phone");
            return new double[] {root.prefWidth(-1), root.getWidth(),
                pill.getBoundsInParent().getMaxX(), phone.getWidth(), phone.prefWidth(-1)};
        });

        // a card drawn wider than the list is what scrolls the list sideways, and the pill
        // at its trailing edge is the first thing to be carried off the side of it
        assertTrue(measured[1] <= CARD_WIDTH + 0.5,
                "the card was drawn " + measured[1] + " wide in a " + CARD_WIDTH + " list");
        assertTrue(measured[2] <= measured[1] + 0.5,
                "the pill ends at " + measured[2] + ", past the card's " + measured[1]);
        // and half a phone number is of no use to anyone
        assertTrue(measured[3] >= measured[4] - 0.5,
                "the phone was cut to " + measured[3] + " of " + measured[4]);
    }

    @Test
    public void constructor_aLongNameAndTag_doNotPushTheFollowUpPillOutOfTheCard() throws Exception {
        Student student = new StudentBuilder()
                .withName("Nurul Izzah binte Hassan Al-Rashid Abdullah Mohamed")
                .withTags("AY2627 Sem 1 CS2103T T16").withFlag(true).build();

        boolean fits = onFxThread(() -> {
            StudentCard card = new StudentCard(student, 48);
            Region root = (Region) card.getRoot();
            assertNotNull(styledScene(root, CARD_WIDTH, 120));

            Label pill = (Label) root.lookup("#followUp");
            Bounds inCard = pill.localToScene(pill.getBoundsInLocal());
            return inCard.getMaxX() <= CARD_WIDTH && inCard.getWidth() > 0;
        });

        // the pill carries the follow-up marker, and an age once one is recorded, so it has to
        // stay in the card however long the name beside it is
        assertTrue(fits, "the follow-up pill was pushed outside the card");
    }

    @Test
    public void constructor_aLongName_leavesTheTagsReadable() throws Exception {
        Student student = new StudentBuilder()
                .withName("Siti Nur-Aisyah Binte Test Test Test Test Test Test Test")
                .withTags("T1").withFlag(true).build();

        List<String> shown = onFxThread(() -> {
            Region root = (Region) new StudentCard(student, 14).getRoot();
            assertNotNull(styledScene(root, CARD_WIDTH, 120));

            assertTrue(((Label) root.lookup("#followUp")).getWidth() > 0,
                    "the follow-up pill should still be laid out");
            return visibleChipsOf(root);
        });

        // a chip cut down to an ellipsis says nothing, so the name gives way before the chip
        assertEquals(List.of("T1"), shown);
    }

    @Test
    public void constructor_aVeryLongTag_leavesTheNameReadable() throws Exception {
        Student student = new StudentBuilder().withName("Siti Nur-Aisyah")
                .withTags("test".repeat(20)).withFlag(true).build();

        double[] widths = onFxThread(() -> {
            StudentCard card = new StudentCard(student, 17);
            Region root = (Region) card.getRoot();
            assertNotNull(styledScene(root, CARD_WIDTH, 120));

            Label name = (Label) root.lookup("#name");
            Label pill = (Label) root.lookup("#followUp");
            Region tags = (Region) root.lookup("#tags");
            return new double[] {name.getWidth(), tags.getWidth(), pill.getWidth(),
                name.prefWidth(-1)};
        });

        // one enormous tag used to take the whole row, crush the name and run under the pill
        assertTrue(widths[0] >= widths[3] - 0.5,
                "the chip crushed the name to " + widths[0] + " of the " + widths[3] + " it asked for");
        assertTrue(widths[1] <= 150, "the chip should be capped, but was " + widths[1]);
        assertTrue(widths[2] > 0, "the follow-up pill should still be laid out");
    }

    @Test
    public void constructor_anyStudent_putsTheChipsBesideTheNameAndThePillAtTheEdge() throws Exception {
        double[] gaps = new double[2];
        double[] pillEdges = new double[2];
        boolean[] flags = {true, false};
        for (int i = 0; i < flags.length; i++) {
            // a short name, which a width floor on the name would pad out into a gap
            Student student = new StudentBuilder().withName("Bo")
                    .withTags("T1").withFlag(flags[i]).build();
            double[] edges = onFxThread(() -> {
                Region root = (Region) new StudentCard(student, 3).getRoot();
                assertNotNull(styledScene(root, CARD_WIDTH, 120));

                Label name = (Label) root.lookup("#name");
                Region tags = (Region) root.lookup("#tags");
                Label pill = (Label) root.lookup("#followUp");
                // where the name's text ends, not where its box does: a name given the
                // whole line still reads as a gap between the name and the chips
                return new double[] {
                    name.localToScene(name.getBoundsInLocal()).getMinX() + name.prefWidth(-1),
                    tags.localToScene(tags.getBoundsInLocal()).getMinX(),
                    pill.localToScene(pill.getBoundsInLocal()).getMaxX(),
                };
            });
            gaps[i] = edges[1] - edges[0];
            pillEdges[i] = edges[2];
        }

        // the chips follow the name rather than sitting out at the trailing edge, and they sit
        // in the same place whether or not the student carries a pill
        assertTrue(gaps[0] < 12, "the chips drifted away from the name by " + gaps[0]);
        assertEquals(gaps[0], gaps[1], 0.5, "the pill moved the chips");
        // the pill a flagged student does carry stays against the card's trailing edge
        assertTrue(pillEdges[0] > CARD_WIDTH - 40, "the pill left the edge at " + pillEdges[0]);
    }

    @Test
    public void constructor_aNameLongerThanTheCard_isCutWithTheWholeOnHover() throws Exception {
        String whole = "Siti Nur-Aisyah Binte Test Test Test Test Test Test Test";
        Label shown = onFxThread(() -> (Label) new StudentCard(
                new StudentBuilder().withName(whole).build(), 1).getRoot().lookup("#name"));

        assertTrue(shown.getText().length() < whole.length(), "the name should be cut");
        assertTrue(shown.getText().endsWith("\u2026"), "and marked as cut");
        assertNotNull(shown.getTooltip(), "the whole name should still be reachable");
        assertEquals(whole, shown.getTooltip().getText());
    }

    @Test
    public void constructor_aShortName_isShownWholeAndWithoutATooltip() throws Exception {
        Label shown = onFxThread(() -> (Label) new StudentCard(
                new StudentBuilder().withName("Alex Yeoh").build(), 1).getRoot().lookup("#name"));

        assertEquals("Alex Yeoh", shown.getText());
        assertNull(shown.getTooltip(), "a name that fits needs no tooltip");
    }

    @Test
    public void constructor_aLongName_isCutAtTheSamePlaceOnEveryCard() throws Exception {
        Student student = new StudentBuilder()
                .withName("Siti Nur-Aisyah Binte Abdullah Rahman")
                .withTags("alpha", "bravo", "charlie").withFlag(true).build();

        // a name cut by the room the row has left breaks off wherever the tags and the pill
        // leave it, so the same name reads differently from one card to the next
        List<String> cut = new ArrayList<>();
        for (double cardWidth : new double[] {DEFAULT_CARD_WIDTH, 600, 760}) {
            cut.add(onFxThread(() -> {
                Region root = (Region) new StudentCard(student, 1).getRoot();
                assertNotNull(styledScene(root, cardWidth, 120));
                Label name = (Label) root.lookup("#name");
                // an elided label still reports the whole text, so what the name reads as is
                // only fixed if the row gave it the width it asked for. A row rounds a width
                // to whole pixels, so the two are compared to within one.
                assertTrue(name.getWidth() >= name.prefWidth(-1) - 1,
                        "a card " + cardWidth + " wide cut the name again, to " + name.getWidth()
                        + " of the " + name.prefWidth(-1) + " it asked for");
                return name.getText();
            }));
        }

        assertEquals(List.of(cut.get(0), cut.get(0), cut.get(0)), cut);
        assertEquals("Siti Nur-Aisyah Binte\u2026", cut.get(0));
    }

    @Test
    public void constructor_aCardTooNarrowForTheName_cutsItFurtherAndKeepsATag() throws Exception {
        Student student = new StudentBuilder()
                .withName("Siti Nur-Aisyah Binte Abdullah Rahman")
                .withTags("alpha", "bravo").withFlag(true).build();

        double[] widths = onFxThread(() -> {
            Region root = (Region) new StudentCard(student, 1).getRoot();
            assertNotNull(styledScene(root, 300, 120));
            Label name = (Label) root.lookup("#name");
            assertTrue(visibleChipsOf(root).size() > 0,
                    "a card with tags should show the start of one however narrow it is");
            return new double[] {name.getWidth(), name.prefWidth(-1)};
        });

        // below the width the name is cut to, it gives way to the tags beside it, and the
        // label draws the ellipsis that says it has
        assertTrue(widths[0] < widths[1],
                "the name should give way at " + widths[0] + " rather than hold " + widths[1]);
        assertTrue(widths[0] > 0, "the name should not vanish");
    }

    @Test
    public void constructor_aStudentWithNoTags_givesTheWholeLineToTheName() throws Exception {
        Student student = new StudentBuilder()
                .withName("Siti Nur-Aisyah Binte Abdullah Rahman").withTags().build();

        double[] widths = onFxThread(() -> {
            Region root = (Region) new StudentCard(student, 1).getRoot();
            assertNotNull(styledScene(root, DEFAULT_CARD_WIDTH, 120));
            Label name = (Label) root.lookup("#name");
            return new double[] {name.getWidth(), name.prefWidth(-1),
                root.lookup("#tags").getBoundsInParent().getWidth()};
        });

        // no chips means no gap where chips would have been
        assertEquals(0, widths[2], "a row with no tags should take no width");
        assertTrue(widths[0] >= widths[1] - 1, "the name should have the line to itself");
    }

    @Test
    public void constructor_moreTagsThanFit_countsTheRest() throws Exception {
        Student student = new StudentBuilder()
                .withTags("alpha", "bravo", "charlie", "delta", "echo").build();

        List<String> shown = onFxThread(() -> {
            Region root = (Region) new StudentCard(student, 1).getRoot();
            assertNotNull(styledScene(root, CARD_WIDTH, 120));
            return visibleChipsOf(root);
        });

        // five chips squeezed into one row come out as single letters, which say nothing
        assertEquals(List.of("alpha", "bravo", "charlie", "2 more\u2026"), shown);
    }

    @Test
    public void constructor_severalTags_areShownInOrder() throws Exception {
        Student student = new StudentBuilder().withTags("T1", "Lab 3", "needs-follow").build();

        List<String> shown = onFxThread(() -> {
            Region root = (Region) new StudentCard(student, 1).getRoot();
            assertNotNull(styledScene(root, CARD_WIDTH, 120));
            return visibleChipsOf(root);
        });

        assertEquals(List.of("Lab 3", "T1", "needs-follow"), shown);
    }

    /**
     * Returns the text of the chips the laid-out {@code card} is showing, in the order the
     * row draws them.
     */
    private static List<String> visibleChipsOf(Region card) {
        return card.lookup("#tags").getParent().lookupAll("#tags .label").stream()
                .filter(Node::isVisible)
                .map(node -> (Label) node)
                .sorted(Comparator.comparingDouble(Label::getLayoutX))
                .map(Label::getText)
                .collect(Collectors.toList());
    }

    @Test
    public void constructor_aNameHoldingAnEmoji_doesNotCutItInHalf() throws Exception {
        // an emoji is two characters as Java stores it, so cutting at an index can leave half
        // of one behind, which is drawn as a broken glyph. This one sits exactly where the
        // card cuts the name
        String whole = "A".repeat(MAX_NAME_CHARACTERS_UNDER_TEST - 1)
                + "\uD83C\uDF93 and more besides";
        Label shown = onFxThread(() -> (Label) new StudentCard(
                new StudentBuilder().withName(whole).build(), 1).getRoot().lookup("#name"));

        String cut = shown.getText();
        assertTrue(holdsOnlyWholeCharacters(cut), "half an emoji was left behind in " + cut);
        assertEquals(whole, shown.getTooltip().getText());
    }

    /** Returns true if {@code text} holds no half of a character whose other half is gone. */
    private static boolean holdsOnlyWholeCharacters(String text) {
        for (int at = 0; at < text.length(); at++) {
            char character = text.charAt(at);
            if (Character.isHighSurrogate(character)) {
                if (at + 1 >= text.length() || !Character.isLowSurrogate(text.charAt(at + 1))) {
                    return false;
                }
                at++;
            } else if (Character.isLowSurrogate(character)) {
                return false;
            }
        }
        return true;
    }

    @Test
    public void constructor_aNameLongInBytesButShortToRead_isNotMarkedAsCut() throws Exception {
        // seventeen characters to a reader, twenty-nine to Java, against a cap of
        // twenty-two: the name fits, so nothing should be taken off it and nothing should
        // say otherwise
        String whole = "Siti " + "\uD83C\uDF93".repeat(12);
        Label shown = onFxThread(() -> (Label) new StudentCard(
                new StudentBuilder().withName(whole).build(), 1).getRoot().lookup("#name"));

        assertEquals(whole, shown.getText());
        assertNull(shown.getTooltip(), "a name that fits needs no tooltip");
    }

    @Test
    public void layout_aFirstLineMissingItsName_saysSoRatherThanGuessing() throws Exception {
        // the row is laid out by hand, so a layout file that left the name out would
        // otherwise draw the tags in its place and look merely odd
        Exception thrown = onFxThread(() -> {
            CardHeadline row = new CardHeadline();
            row.getChildren().add(new TagRow());
            return assertThrows(IllegalStateException.class, () -> {
                row.applyCss();
                row.layout();
            });
        });

        assertTrue(thrown.getMessage().contains("Label"), thrown.getMessage());
    }
}
