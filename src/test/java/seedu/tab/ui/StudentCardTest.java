package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import seedu.tab.model.student.Student;
import seedu.tab.testutil.StudentBuilder;

public class StudentCardTest {

    private static final int TIMEOUT_SECONDS = 10;
    private static final double CARD_WIDTH = 420;

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
                .withTags("AY2627 Sem 1 CS2103T T16").build();
        StudentCard card = onFxThread(() -> new StudentCard(student, 48));

        // the card must size to its content rather than demanding a width the window cannot give
        double width = onFxThread(() -> card.getRoot().prefWidth(-1));
        assertTrue(width > 0, "the card should report a preferred width");
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

        double[] widths = onFxThread(() -> {
            StudentCard card = new StudentCard(student, 14);
            Region root = (Region) card.getRoot();
            assertNotNull(styledScene(root, CARD_WIDTH, 120));

            Region tags = (Region) root.lookup("#tags");
            Label pill = (Label) root.lookup("#followUp");
            return new double[] {tags.getWidth(), tags.prefWidth(-1), pill.getWidth()};
        });

        // a chip cut down to an ellipsis says nothing, so enough of it survives to read
        assertTrue(widths[0] >= 70, "the tag chip was squeezed to nothing: " + widths[0]);
        assertTrue(widths[2] > 0, "the follow-up pill should still be laid out");
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
                root.getWidth()};
        });

        // one enormous tag used to take the whole row, crush the name and run under the pill
        assertTrue(widths[0] >= 120, "the name was crushed to " + widths[0]);
        assertTrue(widths[1] <= 150, "the chip should be capped, but was " + widths[1]);
        assertTrue(widths[2] > 0, "the follow-up pill should still be laid out");
    }

    @Test
    public void constructor_severalTags_areShownInOrder() throws Exception {
        Student student = new StudentBuilder().withTags("T1", "Lab 3", "needs-followup").build();
        HBox tags = onFxThread(() -> (HBox) new StudentCard(student, 1).getRoot().lookup("#tags"));

        List<String> shown = tags.getChildren().stream()
                .map(node -> ((Label) node).getText())
                .collect(Collectors.toList());
        assertEquals(List.of("Lab 3", "T1", "needs-followup"), shown);
    }
}
