package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import seedu.tab.model.student.NusStudentId;
import seedu.tab.model.student.Student;
import seedu.tab.testutil.StudentBuilder;

public class StudentCardTest {

    /** The window's minimum width of 490 leaves about this much room for a card. */
    private static final int NARROWEST_CARD_WIDTH = 298;
    /** What the card cuts a name to, as the card states it. */
    private static final int MAX_NAME_CHARACTERS_UNDER_TEST = 22;
    /** What a cut name keeps from its end, and what it keeps from its start, as UiText states it. */
    private static final int NAME_CLOSING_CHARACTERS_UNDER_TEST = 9;
    private static final int NAME_OPENING_CHARACTERS_UNDER_TEST =
            MAX_NAME_CHARACTERS_UNDER_TEST - NAME_CLOSING_CHARACTERS_UNDER_TEST;
    /**
     * How much of what a name asks for its line is given, where a test needs the line to clip
     * the name. Three quarters leaves the name short of what it asks for in any font, since
     * the name is never drawn wider than the line it sits on.
     */
    private static final double NAME_ROOM_SHARE = 0.75;
    /**
     * How narrow a sweep takes the line, as a share of what the name asks for, and how far it
     * moves between one width and the next. Where a line cuts a name is a matter of pixels,
     * and the cut is not the worse for every width in turn: a width that leaves the name
     * readable says nothing about the one a pixel below it. A sweep therefore visits every
     * width, so that it meets every cut the line can make.
     */
    private static final double NARROWEST_SHARE = 0.2;
    private static final double ONE_PIXEL = 1;
    /** A character Java stores in two, as the Developer Guide's own example of one does. */
    private static final String BEYOND_THE_BASIC_PLANE = "\uD842\uDFB7";

    @BeforeAll
    public static void startToolkit() throws Exception {
        FxToolkit.start();
    }

    /** Runs {@code work} on the thread JavaFX requires its nodes to be built on. */
    private static <T> T onFxThread(Callable<T> work) throws Exception {
        return FxToolkit.onFxThread(work);
    }

    /**
     * Returns a scene holding {@code root}, dressed in the stylesheet the window uses, so that
     * anything measured is measured in the font the product actually shows.
     */
    private static Scene styledScene(Region root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        dressAndLayOut(root, scene);
        return scene;
    }

    /** Returns a scene laid out at the width its dressed root asks for. */
    private static Scene roomyStyledScene(Region root) {
        Scene scene = new Scene(root);
        dressAndLayOut(root, scene);
        root.autosize();
        root.layout();
        return scene;
    }

    private static void dressAndLayOut(Region root, Scene scene) {
        scene.getStylesheets().add(
                UiPart.class.getResource("/view/DarkTheme.css").toExternalForm());
        root.applyCss();
        root.layout();
    }

    private static Label emailLabelOf(Student student) throws Exception {
        return onFxThread(() -> (Label) new StudentCard(student, 1).getRoot().lookup("#email"));
    }

    private static Label followUpLabelOf(Student student) throws Exception {
        return onFxThread(() -> (Label) new StudentCard(student, 1).getRoot().lookup("#followUp"));
    }

    private static Student studentNamed(String name) {
        return new StudentBuilder().withName(name).withTags("alpha", "bravo").withFlag(true).build();
    }

    private static Label nameLabelOf(String name) throws Exception {
        return onFxThread(() -> (Label) new StudentCard(
                new StudentBuilder().withName(name).build(), 1).getRoot().lookup("#name"));
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
    public void constructor_emailCutByWidth_showsWholeAddressUntilWidened() throws Exception {
        Student student = new StudentBuilder().withEmail("siti.nuraisyah.binte.ahmad.hassan@alumni.u.nus.edu")
                .withFlag(true).build();
        onFxThread(() -> {
            Region root = new StudentCard(student, 1).getRoot();
            assertNotNull(styledScene(root, NARROWEST_CARD_WIDTH, 120));
            Label email = (Label) root.lookup("#email");
            assertTrue(email.getWidth() < email.prefWidth(-1), "the card should cut the email");
            assertNotNull(email.getTooltip());
            assertEquals(student.getEmail().orElseThrow().toString(), email.getTooltip().getText());

            layOutAt(root, root.prefWidth(-1));
            assertTrue(email.getWidth() >= email.prefWidth(-1) - 0.5);
            assertNull(email.getTooltip(), "an address that fits needs no tooltip");
            return null;
        });
    }

    @Test
    public void constructor_longPhone_keepsBothContactsInsideCard() throws Exception {
        for (String number : List.of("1234 5678 (HP) 1111-3333 (Office)",
                "+65 6516 2727 ext 21 (office, Mon-Fri 9-5), +65 9123 4567 (HP, after 6pm)")) {
            for (boolean isFlagged : List.of(false, true)) {
                Student student = new StudentBuilder().withPhone(number).withEmail("e0923841@u.nus.edu")
                        .withFlag(isFlagged).withNusStudentId("A01234567X").build();
                double roomyWidth = onFxThread(() -> cardLaidOutRoomily(student, 1).getWidth());
                for (double width : List.of((double) NARROWEST_CARD_WIDTH, roomyWidth)) {
                    assertContactDetailsReadable(student, width);
                }
            }
        }
    }

    private static void assertContactDetailsReadable(Student student, double width) throws Exception {
        onFxThread(() -> {
            Region root = new StudentCard(student, 1).getRoot();
            assertNotNull(styledScene(root, width, 160));
            Region details = (Region) root.lookup("#details");
            Label phone = (Label) root.lookup("#phone");
            Label email = (Label) root.lookup("#email");
            for (Label label : List.of(phone, email)) {
                assertHorizontallyInside(details.localToScene(details.getBoundsInLocal()),
                        label.localToScene(label.getBoundsInLocal()), label == phone ? "phone" : "email");
                if (label.getWidth() < label.prefWidth(-1) - 0.5) {
                    assertNotNull(label.getTooltip());
                    assertEquals(label.getText(), label.getTooltip().getText());
                    assertTrue(label.lookupAll(".text").stream().filter(Text.class::isInstance)
                            .map(Text.class::cast)
                            .anyMatch(text -> text.getText().endsWith(label.getEllipsisString())));
                }
            }
            assertEquals(student.getPhone().value, phone.getText());
            assertEquals(student.getEmail().orElseThrow().toString(), email.getText());
            assertContactsDoNotOverlap(phone, email);
            layOutAt(root, widthForAllContacts(root));
            assertNull(phone.getTooltip(), "a phone that fits needs no tooltip");
            assertNull(email.getTooltip(), "an email that fits needs no tooltip");
            return null;
        });
    }

    private static void assertContactsDoNotOverlap(Label first, Label second) {
        Bounds before = first.getBoundsInParent();
        Bounds after = second.getBoundsInParent();
        assertTrue(before.getMaxY() <= after.getMinY() || before.getMaxX() <= after.getMinX(),
                "contact fields overlap");
    }

    private static double widthForAllContacts(Region root) {
        FlowPane contacts = (FlowPane) root.lookup("#contacts");
        return root.getWidth() - contacts.getWidth() + contacts.prefWidth(-1) + ONE_PIXEL;
    }

    @Test
    public void constructor_shortContacts_keepsOrdinaryCardsCompact() throws Exception {
        for (boolean isFlagged : List.of(false, true)) {
            Student shortContacts = new StudentBuilder().withPhone("91234567").withEmail("e0923841@u.nus.edu")
                    .withNusStudentId("A01234567X").withFlag(isFlagged).build();
            onFxThread(() -> {
                Region compact = cardLaidOutRoomily(shortContacts, 1);
                FlowPane contacts = (FlowPane) compact.lookup("#contacts");
                double singleRowHeight = contacts.getChildren().stream().filter(Node::isManaged)
                        .mapToDouble(field -> field.prefHeight(-1)).max().orElseThrow();
                assertTrue(contacts.getHeight() <= singleRowHeight + ONE_PIXEL,
                        "fitting details must not add rows to an ordinary card");
                return null;
            });
        }
    }

    @Test
    public void constructor_longContacts_exercisesClippingAndRestoresTooltipsAfterResizing() throws Exception {
        Student student = new StudentBuilder().withPhone(
                "+65 6516 2727 ext 21 (office, Mon-Fri 9-5), +65 9123 4567 (HP, after 6pm)")
                .withEmail("a.very.long.student.email.address.for.contact.testing@alumni.u.nus.edu")
                .withNusStudentId("W".repeat(NusStudentId.MAX_LENGTH)).withFlag(true).build();
        onFxThread(() -> {
            Region root = new StudentCard(student, 99).getRoot();
            assertNotNull(styledScene(root, NARROWEST_CARD_WIDTH, 160));
            List<Label> fields = List.of((Label) root.lookup("#nusStudentId"),
                    (Label) root.lookup("#phone"), (Label) root.lookup("#email"));
            assertContactsClipped(fields.subList(1, fields.size()));
            double narrowHeight = root.prefHeight(NARROWEST_CARD_WIDTH);
            layOutAt(root, widthForAllContacts(root));
            for (Label field : fields) {
                assertNull(field.getTooltip(), "a field that fits needs no tooltip");
                assertTrue(field.getWidth() >= field.prefWidth(-1) - 0.5);
            }
            assertTrue(root.prefHeight(root.getWidth()) < narrowHeight, "widening must remove wrapped rows");
            layOutAt(root, NARROWEST_CARD_WIDTH);
            assertContactsClipped(fields.subList(1, fields.size()));
            return null;
        });
    }

    @Test
    public void resize_contactRowAcrossWidths_keepsEveryFieldInsideWithoutOverlap() throws Exception {
        Student student = new StudentBuilder().withPhone("1234 5678 (HP) 1111-3333 (Office)")
                .withEmail("e0923841@u.nus.edu").withNusStudentId("W".repeat(NusStudentId.MAX_LENGTH))
                .withFlag(true).build();
        onFxThread(() -> {
            Region root = new StudentCard(student, 99).getRoot();
            assertNotNull(styledScene(root, NARROWEST_CARD_WIDTH, 160));
            List<Label> fields = List.of((Label) root.lookup("#nusStudentId"),
                    (Label) root.lookup("#phone"), (Label) root.lookup("#email"));
            double widest = root.prefWidth(-1);
            for (double width = NARROWEST_CARD_WIDTH; width <= widest; width += ONE_PIXEL) {
                layOutAt(root, width);
                Region contacts = (Region) root.lookup("#contacts");
                Bounds area = contacts.localToScene(contacts.getBoundsInLocal());
                for (Label field : fields) {
                    Bounds drawn = field.localToScene(field.getBoundsInLocal());
                    assertHorizontallyInside(area, drawn, field.getId());
                    assertTrue(drawn.getMaxY() <= area.getMaxY() + 0.5, "a wrapped field escapes its row");
                }
                assertContactsDoNotOverlap(fields.get(0), fields.get(1));
                assertContactsDoNotOverlap(fields.get(1), fields.get(2));
            }
            return null;
        });
    }

    private static void assertContactsClipped(List<Label> fields) {
        for (Label field : fields) {
            assertTrue(field.getWidth() < field.prefWidth(-1) - 0.5,
                    "the long " + field.getId() + " must exercise clipping");
            assertNotNull(field.getTooltip());
            assertEquals(field.getText(), field.getTooltip().getText());
            assertTrue(field.lookupAll(".text").stream().filter(Text.class::isInstance).map(Text.class::cast)
                    .anyMatch(text -> text.getText().endsWith(field.getEllipsisString())));
        }
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
    public void constructor_anyStudent_leavesTheNusStudentIdSlotOut() throws Exception {
        // the card lays the slot out so that the field can fill it without further UI work,
        // and must not show a placeholder in the meantime
        Label nusStudentId = onFxThread(() -> (Label) new StudentCard(new StudentBuilder().build(), 1)
                .getRoot().lookup("#nusStudentId"));

        assertFalse(nusStudentId.isManaged());
        assertFalse(nusStudentId.isVisible());
    }

    @Test
    public void constructor_studentWithNusStudentId_showsTheNusStudentId() throws Exception {
        Student student = new StudentBuilder().withNusStudentId("A01234567X").build();
        Label nusStudentId = onFxThread(() -> (Label) new StudentCard(student, 1)
                .getRoot().lookup("#nusStudentId"));

        assertEquals("A01234567X", nusStudentId.getText());
        assertTrue(nusStudentId.isManaged());
        assertTrue(nusStudentId.isVisible());
    }

    @Test
    public void constructor_narrowCard_keepsAllDetailsVisible() throws Exception {
        Student base = new StudentBuilder().withPhone("+65 9123 4567")
                .withEmail("a.very.long.address.indeed@u.nus.edu").build();
        Student student = new StudentBuilder(base)
                .withNusStudentId("A".repeat(NusStudentId.MAX_LENGTH)).build();

        // Neighboring widths exercise different rounding when JavaFX shrinks the detail row.
        for (int width = NARROWEST_CARD_WIDTH; width <= 320; width++) {
            assertDetailsInside(student, width);
        }
    }

    private static void assertDetailsInside(Student student, double width) throws Exception {
        Bounds[] measured = onFxThread(() -> {
            Region root = (Region) new StudentCard(student, 1).getRoot();
            assertNotNull(styledScene(root, width, 120));
            Region details = (Region) root.lookup("#details");
            Label nusStudentId = (Label) root.lookup("#nusStudentId");
            Label phone = (Label) root.lookup("#phone");
            Label email = (Label) root.lookup("#email");
            return new Bounds[] {
                details.localToScene(details.getBoundsInLocal()),
                nusStudentId.localToScene(nusStudentId.getBoundsInLocal()),
                phone.localToScene(phone.getBoundsInLocal()),
                email.localToScene(email.getBoundsInLocal())
            };
        });

        assertHorizontallyInside(measured[0], measured[1], "NUS Student ID");
        assertHorizontallyInside(measured[0], measured[2], "phone");
        assertHorizontallyInside(measured[0], measured[3], "email");
    }

    /** Verifies that a field has visible width and is drawn inside its details container. */
    private static void assertHorizontallyInside(Bounds container, Bounds field, String fieldName) {
        assertTrue(field.getWidth() > 0, "the " + fieldName + " has no visible width");
        assertTrue(field.getMinX() >= container.getMinX() - 0.5,
                "the " + fieldName + " starts before the details container");
        assertTrue(field.getMaxX() <= container.getMaxX() + 0.5,
                "the " + fieldName + " ends at " + field.getMaxX()
                        + ", past the details container's " + container.getMaxX());
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
    public void constructor_shortPhoneAndEmail_shareTheLine() throws Exception {
        Student student = new StudentBuilder().withPhone("91234567")
                .withEmail("alice@example.com").build();

        onFxThread(() -> {
            Region root = cardLaidOutRoomily(student, 1);
            Label phone = (Label) root.lookup("#phone");
            Label email = (Label) root.lookup("#email");
            assertEquals(phone.getLayoutY(), email.getLayoutY(), 0.5);
            assertContactsDoNotOverlap(phone, email);
            assertTrue(phone.isVisible());
            assertTrue(email.isVisible());
            return null;
        });
    }

    @Test
    public void constructor_longNameAndTag_doNotPushTheCardOutOfShape() throws Exception {
        Student student = new StudentBuilder()
                .withName("Nurul Izzah binte Hassan Al-Rashid Abdullah Mohamed")
                .withPhone("91112222")
                .withEmail("a.very.long.address.indeed@u.nus.edu")
                .withTags("AY2627 Sem 1 CS2103T T16").withFlag(true).build();

        Label name = nameLaidOutNarrowly(student, 48);
        double[] measured = onFxThread(() -> {
            Region root = (Region) name.getScene().getRoot();
            Label pill = (Label) root.lookup("#followUp");
            Label phone = (Label) root.lookup("#phone");
            return new double[] {root.getWidth(), pill.getBoundsInParent().getMaxX(),
                phone.getWidth(), phone.prefWidth(-1)};
        });

        // a card drawn wider than the list is what scrolls the list sideways, and the pill
        // at its trailing edge is the first thing to be carried off the side of it
        assertTrue(measured[1] <= measured[0] + 0.5,
                "the pill ends at " + measured[1] + ", past the card's " + measured[0]);
        // and half a phone number is of no use to anyone
        assertTrue(measured[2] >= measured[3] - 0.5,
                "the phone was cut to " + measured[2] + " of " + measured[3]);
    }

    @Test
    public void constructor_aLongNameAndTag_doNotPushTheFollowUpPillOutOfTheCard() throws Exception {
        Student student = new StudentBuilder()
                .withName("Nurul Izzah binte Hassan Al-Rashid Abdullah Mohamed")
                .withTags("AY2627 Sem 1 CS2103T T16").withFlag(true).build();

        Label name = nameLaidOutNarrowly(student, 48);
        boolean fits = onFxThread(() -> {
            Region root = (Region) name.getScene().getRoot();
            Label pill = (Label) root.lookup("#followUp");
            Bounds inCard = pill.localToScene(pill.getBoundsInLocal());
            return inCard.getMaxX() <= root.getWidth() && inCard.getWidth() > 0;
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

        Label name = nameLaidOutNarrowly(student, 14);
        List<String> shown = onFxThread(() -> {
            Region root = (Region) name.getScene().getRoot();
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
            Region root = cardLaidOutRoomily(student, 17);
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
        double[] cardEdges = new double[2];
        boolean[] flags = {true, false};
        for (int i = 0; i < flags.length; i++) {
            // a short name, which a width floor on the name would pad out into a gap
            Student student = new StudentBuilder().withName("Bo")
                    .withTags("T1").withFlag(flags[i]).build();
            double[] edges = onFxThread(() -> {
                Region root = cardLaidOutRoomily(student, 3);
                double needed = root.prefWidth(-1);
                layOutAt(root, needed * 2);
                assertTrue(root.getWidth() > needed + 0.5,
                        "the card needs spare room to test the pill's trailing position");
                Label name = (Label) root.lookup("#name");
                Region tags = (Region) root.lookup("#tags");
                Label pill = (Label) root.lookup("#followUp");
                // where the name's text ends, not where its box does: a name given the
                // whole line still reads as a gap between the name and the chips
                return new double[] {
                    name.localToScene(name.getBoundsInLocal()).getMinX() + name.prefWidth(-1),
                    tags.localToScene(tags.getBoundsInLocal()).getMinX(),
                    pill.localToScene(pill.getBoundsInLocal()).getMaxX(),
                    root.getWidth() - root.getInsets().getRight(),
                };
            });
            gaps[i] = edges[1] - edges[0];
            pillEdges[i] = edges[2];
            cardEdges[i] = edges[3];
        }

        // the chips follow the name rather than sitting out at the trailing edge, and they sit
        // in the same place whether or not the student carries a pill
        assertTrue(gaps[0] < 12, "the chips drifted away from the name by " + gaps[0]);
        assertEquals(gaps[0], gaps[1], 0.5, "the pill moved the chips");
        // the pill a flagged student does carry stays against the card's trailing edge
        assertEquals(cardEdges[0], pillEdges[0], 0.5, "the pill left the card's trailing edge");
    }

    @Test
    public void constructor_aNameLongerThanTheCard_isCutWithTheWholeOnHover() throws Exception {
        String whole = "Siti Nur-Aisyah Binte Test Test Test Test Test Test Test";
        Label shown = nameLabelOf(whole);

        assertEquals("Siti Nur-Aisy\u2026Test Test", shown.getText(), "both ends should be kept");
        assertNotNull(shown.getTooltip(), "the whole name should still be reachable");
        assertEquals(whole, shown.getTooltip().getText());
    }

    @Test
    public void constructor_namesSharingTheirStart_readDifferently() throws Exception {
        Label ismail = nameLabelOf("Mohamed Abdul Rahman bin Ismail");
        Label ahmad = nameLabelOf("Mohamed Abdul Rahman bin Ahmad");

        assertEquals("Mohamed Abdul\u2026in Ismail", ismail.getText());
        assertEquals("Mohamed Abdul\u2026bin Ahmad", ahmad.getText());
    }

    @Test
    public void constructor_namesSharingTheirStart_areDrawnApartAtEveryNarrowWidth() throws Exception {
        String ismail = "Mohamed Abdul Rahman bin Ismail";
        String ahmad = "Mohamed Abdul Rahman bin Ahmad";

        List<String[]> drawn = onFxThread(() -> {
            Label first = nameLaidOutRoomily(studentNamed(ismail));
            Label second = nameLaidOutRoomily(studentNamed(ahmad));

            // one line width for both, as a list gives every card the same width
            List<String[]> pairs = new ArrayList<>();
            for (double line : lineWidthsThatCut(first)) {
                pairs.add(new String[] {
                    drawnTextOf(onALineOf(first, line)),
                    drawnTextOf(onALineOf(second, line)),
                });
            }
            return pairs;
        });

        // the line cuts the name from its middle too, so what it leaves still holds the end
        // of the name. Cut at the end instead, both cards drew "Mohamed Abd..."
        for (String[] pair : drawn) {
            assertNotEquals(pair[0], pair[1], "both cards drew " + pair[0]);
            assertTrue(pair[0].endsWith("l"), "the end of the name is gone from " + pair[0]);
            assertTrue(pair[1].endsWith("d"), "the end of the name is gone from " + pair[1]);
        }
    }

    @Test
    public void constructor_aNameHoldingACharacterStoredInTwo_isNeverDrawnInHalf() throws Exception {
        // a cut by index between the two halves of such a character draws as a box. The line
        // cuts at both ends of the name, so one of these sits in front of each cut
        String pair = BEYOND_THE_BASIC_PLANE;
        List<String> names = List.of(
                "Alexander " + pair + "\u7530",
                "Alexander Bernard Chris" + pair + "topher",
                "Ale" + pair + "xander Bernard Christopher");

        for (String whole : names) {
            for (String drawn : drawnAcrossNarrowLines(studentNamed(whole))) {
                assertTrue(holdsOnlyWholeCharacters(drawn),
                        "half a character was left behind in " + drawn + " of " + whole);
            }
        }
    }

    @Test
    public void constructor_aNameHoldingACombiningAccent_neverStrandsItFromItsLetter() throws Exception {
        // an accent with no letter to compose with stays a character in its own right, and a
        // cut in front of it leaves it standing on the ellipsis
        String marked = "x\u0301";
        List<String> names = List.of(
                "Alexander " + marked + "e",
                "Alexander Bernard Chris" + marked + "topher",
                "Ale" + marked + "ander Bernard Christopher");

        for (String whole : names) {
            Student student = studentNamed(whole);
            assertTrue(student.getName().fullName.indexOf('\u0301') >= 0,
                    "the stored name holds no mark of its own, so this would prove nothing");
            for (String drawn : drawnAcrossNarrowLines(student)) {
                assertTrue(holdsNoStrandedMark(drawn),
                        "an accent was left standing on its own in " + drawn + " of " + whole);
            }
        }
    }

    @Test
    public void constructor_aNameAtOrOneOverTheCount_isShownWhole() throws Exception {
        Label atTheCount = nameLabelOf("Jonathan Chua Wei Ming");
        // one over, where the ellipsis would take back the one place the cut saves
        Label oneOver = nameLabelOf("Jonathan Chuah Wei Ming");

        assertEquals("Jonathan Chua Wei Ming", atTheCount.getText());
        assertNull(atTheCount.getTooltip(), "a name that fits needs no tooltip");
        assertEquals("Jonathan Chuah Wei Ming", oneOver.getText());
        assertNull(oneOver.getTooltip(), "a name that fits needs no tooltip");
    }

    /**
     * Returns the name on a card laid out at the width the card itself asks for, where nothing
     * on it has had to give way. The scene dresses the card in the stylesheet first, so every
     * width read off it afterwards is measured in the font the product draws.
     */
    private static Label nameLaidOutRoomily(Student student) {
        return nameLaidOutRoomily(student, 1);
    }

    private static Label nameLaidOutRoomily(Student student, int displayedIndex) {
        Region root = cardLaidOutRoomily(student, displayedIndex);

        Label name = (Label) root.lookup("#name");
        assertTrue(name.getWidth() >= name.prefWidth(-1) - 0.5,
                "the card should start out showing the whole of a name it asks "
                + name.prefWidth(-1) + " for");
        return name;
    }

    /** Returns a card laid out at the width its dressed content asks for. */
    private static Region cardLaidOutRoomily(Student student, int displayedIndex) {
        Region root = new StudentCard(student, displayedIndex).getRoot();
        assertNotNull(roomyStyledScene(root));
        layOutAt(root, root.prefWidth(-1));
        return root;
    }

    /**
     * Returns the name on a line that keeps the tags' minimum readable width and gives the
     * name only {@link #NAME_ROOM_SHARE} of what it asks for.
     */
    private static Label nameLaidOutNarrowly(Student student, int displayedIndex) throws Exception {
        return onFxThread(() -> {
            Label name = nameLaidOutRoomily(student, displayedIndex);
            Region line = (Region) name.getParent();
            Region tags = (Region) line.lookup("#tags");
            String roomyName = drawnTextOf(name);
            double gap = line.prefWidth(-1) - name.prefWidth(-1) - tags.prefWidth(-1);
            double narrowLineWidth = name.prefWidth(-1) * NAME_ROOM_SHARE
                    + gap + tags.minWidth(-1);

            onALineOf(name, narrowLineWidth);
            assertTrue(name.getWidth() < name.prefWidth(-1) - 0.5,
                    "the narrow line left the name whole, so nothing is clipped");
            assertNotEquals(roomyName, drawnTextOf(name),
                    "the narrow line should draw less of the name");
            return name;
        });
    }

    /**
     * Narrows the card holding {@code name} until its first line is {@code lineWidth} wide.
     *
     * <p>The index, the pill and the padding hold their own width, so the whole of a change in
     * the card's width reaches that line.
     */
    private static Label onALineOf(Label name, double lineWidth) {
        Region root = (Region) name.getScene().getRoot();
        double besideTheLine = root.getWidth() - ((Region) name.getParent()).getWidth();
        layOutAt(root, besideTheLine + lineWidth);
        return name;
    }

    /**
     * Returns the name on a card whose first line is given {@link #NAME_ROOM_SHARE} of what the
     * name by itself asks for, so that the line has too little for the name whatever the glyphs
     * of the platform measure. A width in pixels would clip the name where the font is wide and
     * leave it whole where the font is narrow.
     */
    private static Label clippedNameLabelOf(Student student) throws Exception {
        return onFxThread(() -> {
            Label name = nameLaidOutRoomily(student);
            double wanted = name.prefWidth(-1);

            // the name is drawn no wider than the line it sits on, so a line given a share of
            // what the name asks for leaves the name short of it however the line shares it
            // out with the tags beside it
            onALineOf(name, wanted * NAME_ROOM_SHARE);

            assertTrue(name.getWidth() < wanted - 0.5,
                    "the line left the name its whole " + wanted + ", so nothing is clipped");
            return name;
        });
    }

    /** Draws {@code root} again at {@code width}, as a narrowing window would. */
    private static void layOutAt(Region root, double width) {
        root.resize(width, root.getHeight());
        root.layout();
    }

    /** Returns what a card draws of {@code student}'s name at each width of the sweep. */
    private static List<String> drawnAcrossNarrowLines(Student student) throws Exception {
        return onFxThread(() -> {
            Label name = nameLaidOutRoomily(student);

            List<String> drawn = new ArrayList<>();
            for (double line : lineWidthsThatCut(name)) {
                drawn.add(drawnTextOf(onALineOf(name, line)));
            }
            return drawn;
        });
    }

    /**
     * Returns every width, a pixel apart, at which the line can take something off the name:
     * from a fifth of what the name asks for, up to the width at which the line draws the
     * name and the tags beside it whole. The tags take their room from the same line, so a
     * line wider than the name alone can still cut it.
     */
    private static List<Double> lineWidthsThatCut(Label name) {
        double narrowest = name.prefWidth(-1) * NARROWEST_SHARE;
        double widestThatCuts = ((Region) name.getParent()).prefWidth(-1);

        List<Double> widths = new ArrayList<>();
        for (double line = narrowest; line <= widestThatCuts; line += ONE_PIXEL) {
            widths.add(line);
        }
        return widths;
    }

    /** Returns true if no combining mark in {@code text} has been stranded from its letter. */
    private static boolean holdsNoStrandedMark(String text) {
        for (int at = 0; at < text.length(); at++) {
            boolean isStranded = Character.getType(text.charAt(at)) == Character.NON_SPACING_MARK
                    && (at == 0 || text.charAt(at - 1) == '\u2026');
            if (isStranded) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the glyphs {@code label} draws, which its own overrun has had its say on, rather
     * than the text it was given. A row that runs out of room takes more off the text, and
     * {@code getText} says nothing about that.
     */
    private static String drawnTextOf(Label label) {
        Text drawn = (Text) label.lookup(".text");
        assertNotNull(drawn, "the label has not been given a skin to draw with");
        return drawn.getText();
    }

    @Test
    public void constructor_shortNamesInNarrowCards_showTheirWholeNamesOnHover() throws Exception {
        for (String whole : List.of("Wong Wei Ming Jonathan", "Siti Nur-Aisyah Hassan", "Siti Nur-Aisyah Hamid")) {
            for (double width : List.of((double) NARROWEST_CARD_WIDTH, 368.0, 448.0)) {
                onFxThread(() -> {
                    Region root = new StudentCard(studentNamed(whole), 1).getRoot();
                    assertNotNull(styledScene(root, width, 120));
                    Label name = (Label) root.lookup("#name");
                    boolean isCut = !whole.equals(name.getText());
                    if (width == NARROWEST_CARD_WIDTH) {
                        assertTrue(isCut, "the minimum-width card should cut " + whole);
                    }
                    String expectedTooltip = isCut ? whole : null;
                    String tooltip = name.getTooltip() == null ? null : name.getTooltip().getText();
                    assertEquals(expectedTooltip, tooltip, "only a width-cut name needs a full-name tooltip");

                    layOutAt(root, root.prefWidth(-1));
                    assertEquals(whole, name.getText());
                    assertNull(name.getTooltip(), "widening the card should remove an unnecessary tooltip");
                    return null;
                });
            }
        }
    }

    @Test
    public void constructor_aNameTheRowClips_isNamedOnHover() throws Exception {
        // a name the count leaves whole, on a line too narrow to draw all of it beside the
        // tags and the pill. The count takes nothing off it and the line clips it anyway
        String whole = "Jonathan Chuah Wei Ming";

        Label shown = clippedNameLabelOf(studentNamed(whole));

        assertTrue(shown.getText().length() < whole.length(), "the line should have cut the name");
        assertNotNull(shown.getTooltip(), "a name the row clipped should still be readable");
        assertEquals(whole, shown.getTooltip().getText());
    }

    @Test
    public void constructor_aShorterNameTheRowClips_isNamedOnHover() throws Exception {
        // well inside the count, so this is the hole a cut by count never covered rather
        // than the one at the count's own boundary
        String whole = "Alexander Yeoh Boon";

        Label shown = clippedNameLabelOf(studentNamed(whole));

        assertTrue(shown.getText().length() < whole.length(), "the line should have cut the name");
        assertNotNull(shown.getTooltip(), "a shorter name the row clipped should be readable too");
        assertEquals(whole, shown.getTooltip().getText());
    }

    @Test
    public void constructor_aNameTwoOverTheCount_isCut() throws Exception {
        Label shown = nameLabelOf("Jonathan Cheong Wei Ming");

        assertEquals("Jonathan Cheo\u2026Wei Ming", shown.getText());
    }

    @Test
    public void constructor_aLongNameOfOneWord_isCutFromItsMiddle() throws Exception {
        Label shown = nameLabelOf("Thirunavukkarasusubramaniam");

        assertEquals("Thirunavukkar\u2026bramaniam", shown.getText());
    }

    @Test
    public void constructor_aLongNameInAnotherScript_isCutFromItsMiddle() throws Exception {
        Label shown = nameLabelOf("\u9648\u4F1F\u660E\u674E\u5C0F\u9F99\u738B\u5927\u9524"
                + "\u5F20\u4E09\u4E30\u8D75\u56DB\u4E5D\u5B59\u609F\u7A7A\u8BF8\u845B"
                + "\u4EAE\u5468\u6770\u502B");

        assertEquals("\u9648\u4F1F\u660E\u674E\u5C0F\u9F99\u738B\u5927\u9524"
                + "\u5F20\u4E09\u4E30\u8D75\u2026\u5B59\u609F\u7A7A\u8BF8\u845B"
                + "\u4EAE\u5468\u6770\u502B", shown.getText());
    }

    @Test
    public void constructor_aShortName_isShownWholeAndWithoutATooltip() throws Exception {
        Label shown = nameLabelOf("Alex Yeoh");

        assertEquals("Alex Yeoh", shown.getText());
        assertNull(shown.getTooltip(), "a name that fits needs no tooltip");
    }

    @Test
    public void constructor_aLongName_isCutAtTheSamePlaceOnEveryCard() throws Exception {
        Student student = new StudentBuilder()
                .withName("Siti Nur-Aisyah Binte Abdullah Rahman")
                .withTags("alpha", "bravo", "charlie").withFlag(true).build();

        // a name cut by the room the row has left breaks off wherever the tags and the pill
        // leave it, so the same name reads differently from one card to the next. Where the
        // line is too narrow for all three the name does give way, which is what the next
        // test covers; these are widths where it is not, in any font
        List<String> cut = onFxThread(() -> {
            Label name = nameLaidOutRoomily(student);
            Region line = (Region) name.getParent();
            double roomyLineWidth = line.prefWidth(-1);
            List<Double> lineWidths = List.of(roomyLineWidth,
                    roomyLineWidth + name.prefWidth(-1), roomyLineWidth * 2);
            List<String> drawn = new ArrayList<>();
            for (double lineWidth : lineWidths) {
                onALineOf(name, lineWidth);
                // an elided label still reports the whole text, so what the name reads as is
                // only fixed if the row gave it the width it asked for. A row rounds a width
                // to whole pixels, so the two are compared to within one.
                assertTrue(name.getWidth() >= name.prefWidth(-1) - 1,
                        "a line " + lineWidth + " wide cut the name again, to " + name.getWidth()
                        + " of the " + name.prefWidth(-1) + " it asked for");
                drawn.add(drawnTextOf(name));
            }
            return drawn;
        });

        assertEquals(List.of(cut.get(0), cut.get(0), cut.get(0)), cut);
        assertEquals("Siti Nur-Aisy\u2026ah Rahman", cut.get(0));
    }

    @Test
    public void constructor_aCardTooNarrowForTheName_cutsItFurtherAndKeepsATag() throws Exception {
        Student student = new StudentBuilder()
                .withName("Siti Nur-Aisyah Binte Abdullah Rahman")
                .withTags("alpha", "bravo").withFlag(true).build();

        Label name = nameLaidOutNarrowly(student, 1);
        double[] widths = onFxThread(() -> {
            Region root = (Region) name.getScene().getRoot();
            List<String> chips = visibleChipsOf(root);
            assertFalse(chips.isEmpty(),
                    "a card with tags should show the start of one however narrow it is");
            assertTrue(chips.get(0).startsWith("alpha"),
                    "the card should keep the first tag rather than only its count: " + chips);
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

        Label name = nameLaidOutRoomily(student);
        double[] widths = onFxThread(() -> {
            Region root = (Region) name.getScene().getRoot();
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
            Region root = cardLaidOutRoomily(student, 1);
            return visibleChipsOf(root);
        });

        // five chips squeezed into one row come out as single letters, which say nothing
        assertEquals(List.of("alpha", "bravo", "charlie", "2 more\u2026"), shown);
    }

    @Test
    public void constructor_severalTags_areShownInOrder() throws Exception {
        Student student = new StudentBuilder().withTags("T1", "Lab 3", "needs-follow").build();

        List<String> shown = onFxThread(() -> {
            Region root = cardLaidOutRoomily(student, 1);
            return visibleChipsOf(root);
        });

        // tags are ordered alphabetically ignoring case: "Lab 3" < "needs-follow" < "T1"
        assertEquals(List.of("Lab 3", "needs-follow", "T1"), shown);
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
        // of one behind, which is drawn as a broken glyph. One of these sits exactly where
        // the card opens its cut and the other exactly where it closes it
        String graduate = "\uD83C\uDF93";
        String whole = "A".repeat(NAME_OPENING_CHARACTERS_UNDER_TEST - 1) + graduate
                + " and more besides " + graduate
                + "B".repeat(NAME_CLOSING_CHARACTERS_UNDER_TEST - 1);
        Label shown = nameLabelOf(whole);

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

    @Test
    public void constructor_atEveryWidth_saysWhenItHasLeftTagsOut() throws Exception {
        // a card showing some of a student's tags must say that there are more, at whatever
        // width: a card that simply drops the count leaves the reader thinking that is all
        // a long name and a pill, which is what leaves the tags least room
        Student student = new StudentBuilder()
                .withName("Bartholomew Montgomery Fitzwilliam")
                .withTags("attendance-low", "needs-consult", "midterm").withFlag(true).build();

        List<String> silent = new ArrayList<>();
        for (int width = 300; width <= 760; width += 10) {
            int at = width;
            List<String> chips = onFxThread(() -> {
                Region root = (Region) new StudentCard(student, 3).getRoot();
                assertNotNull(styledScene(root, at, 120));
                return visibleChipsOf(root);
            });
            // the row counts what it left out while a chip is shown, and counts the tags
            // themselves once it has given up its last chip; either says how many there are
            boolean hasHiddenCount = chips.stream()
                    .anyMatch(chip -> chip.endsWith("more\u2026") || chip.equals("3 tags"));
            if (chips.size() < 3 && !hasHiddenCount) {
                silent.add(at + ": " + chips);
            }
        }

        assertTrue(silent.isEmpty(), "these widths left tags out without saying so: " + silent);
    }

    @Test
    public void constructor_aCardTooNarrowForBoth_showsTheTagRatherThanTheCount() throws Exception {
        Student student = new StudentBuilder().withName("Charlotte Oliveiro")
                .withTags("AY2627 Sem 1", "CS2103T", "T16", "needsconsultation").build();

        Label name = nameLaidOutNarrowly(student, 3);
        List<String> chips = onFxThread(() -> {
            Region root = (Region) name.getScene().getRoot();
            return visibleChipsOf(root);
        });

        // which tag a student carries says more than how many they have, so whatever else
        // the row has room for, it names a tag first. How many else fit depends on the
        // font the machine has, so that is not pinned here
        assertFalse(chips.isEmpty(), "the row should name a tag");
        assertTrue(chips.get(0).startsWith("AY2627"),
                "the row should lead with the tag, not a count: " + chips);
    }

    @Test
    public void constructor_narrowCards_keepNameLettersAndCompleteTagCounts() throws Exception {
        List<String> allTags = List.of("attendance-low", "consult", "follow-up", "lab", "midterm", "tutorial");
        for (int tagCount : List.of(1, 2, 4, 6)) {
            List<String> names = allTags.subList(0, tagCount);
            for (boolean isFlagged : new boolean[] {false, true}) {
                Student student = new StudentBuilder().withName("Bernice Yu")
                        .withTags(names.toArray(String[]::new)).withFlag(isFlagged).build();
                onFxThread(() -> {
                    Region root = new StudentCard(student, 2).getRoot();
                    assertNotNull(styledScene(root, NARROWEST_CARD_WIDTH, 120));
                    double widest = Math.max(NARROWEST_CARD_WIDTH, root.prefWidth(-1));
                    for (double width = NARROWEST_CARD_WIDTH; width <= widest; width += ONE_PIXEL) {
                        layOutAt(root, width);
                        Label name = (Label) root.lookup("#name");
                        assertTrue(drawnTextOf(name).startsWith("Be"),
                                "the name should keep its opening letters at width " + width);
                        assertHiddenTagsCounted((TagRow) root.lookup("#tags"), names);
                    }
                    return null;
                });
            }
        }
    }

    @Test
    public void constructor_countAtNameAllowanceBoundary_keepsCompleteCountBesideName() throws Exception {
        onFxThread(() -> {
            List<String> names = List.of("attendance-low", "consult");
            Student student = new StudentBuilder().withName("Bernice Yu")
                    .withTags(names.toArray(String[]::new)).withFlag(true).build();
            Region root = new StudentCard(student, 2).getRoot();
            assertNotNull(styledScene(root, NARROWEST_CARD_WIDTH, 120));
            Label count = (Label) root.lookup(".tag-overflow");
            // Reproduce the Linux boundary using the local font's count width: the
            // 64 px name allowance and 8 px gap leave the count one pixel short.
            count.setText("2 tags");
            Label name = (Label) root.lookup("#name");
            onALineOf(name, 64 + 8 + Math.floor(count.prefWidth(-1)) - ONE_PIXEL);
            assertHiddenTagsCounted((TagRow) root.lookup("#tags"), names);
            assertTrue(drawnTextOf((Label) root.lookup("#name")).startsWith("Be"));
            return null;
        });
    }

    private static void assertHiddenTagsCounted(TagRow tags, List<String> names) {
        Label overflow = (Label) tags.getChildrenUnmodifiable().stream()
                .filter(node -> node.getStyleClass().contains("tag-overflow")).findFirst().orElseThrow();
        int shown = (int) tags.getChildrenUnmodifiable().stream()
                .filter(Node::isVisible).filter(node -> node != overflow).count();
        if (shown == names.size()) {
            assertFalse(overflow.isVisible());
            return;
        }
        assertTrue(overflow.isVisible(), "hidden tags must be counted");
        String expectedCount = (names.size() - shown) + (shown == 0 ? " tags" : " more\u2026");
        assertEquals(expectedCount, overflow.getText());
        assertTrue(overflow.getWidth() >= overflow.prefWidth(-1) - 0.5,
                "the count must fit in full: " + overflow.getText() + " in " + overflow.getWidth());
        assertTrue(overflow.getBoundsInParent().getMaxX() <= tags.getWidth() + 0.5);
        assertEquals(String.join(", ", names.subList(shown, names.size())), overflow.getTooltip().getText());
    }

    @Test
    public void constructor_atEveryWidth_keepsSomeOfTheName() throws Exception {
        // a flagged student with tags on the narrowest window the product allows, where the
        // name had been squeezed down to an ellipsis and nothing else
        Student student = new StudentBuilder().withName("Bernice Yu")
                .withTags("attendance-low", "needs-consult").withFlag(true).build();

        List<String> unreadable = new ArrayList<>();
        for (int width = 280; width <= 760; width += 10) {
            int at = width;
            String shown = onFxThread(() -> {
                Region root = (Region) new StudentCard(student, 2).getRoot();
                assertNotNull(styledScene(root, at, 120));
                Label name = (Label) root.lookup("#name");
                return name.getWidth() + "|" + name.prefWidth(-1);
            });
            double drawn = Double.parseDouble(shown.split("\\|")[0]);
            if (drawn < 40) {
                unreadable.add(at + ": " + drawn);
            }
        }

        // a name a reader cannot make out at all says less about the student than a tag
        // count they cannot see
        assertTrue(unreadable.isEmpty(), "the name was squeezed away at: " + unreadable);
    }

    @Test
    public void setText_null_leavesTheNameHoldingNothingRatherThanFailing() throws Exception {
        // a Label takes a null text, and a name that kept one would carry it into the width
        // it reports and into the cut it makes from it
        double empty = onFxThread(() -> {
            CardName name = new CardName();
            name.setText("Alexander Bernard Christopher");
            name.setText(null);
            return name.prefWidth(-1);
        });

        double never = onFxThread(() -> new CardName().prefWidth(-1));
        assertEquals(never, empty, "a name given null should ask for the room an empty one does");
    }

    @Test
    public void constructor_aClippedNameGivenItsRoomBack_losesTheTooltip() throws Exception {
        // comfortably past the width the row keeps for a name, so the line is willing to cut
        // it on any platform's glyphs, and well inside the count, so the count takes nothing
        String whole = "Alexander Yeoh Boon";

        String hovers = onFxThread(() -> {
            Label name = nameLaidOutRoomily(studentNamed(whole));
            double wanted = name.prefWidth(-1);
            String roomy = name.getTooltip() == null ? "none" : "set";

            onALineOf(name, wanted * NAME_ROOM_SHARE);
            assertTrue(name.getText().length() < whole.length(), "the line should have cut the name");
            String narrowed = name.getTooltip() == null ? "none" : "set";

            // back to the width the card asks for, where the name and the tags both fit;
            // the name's own width is not enough, because the tags take from the same line
            Region root = (Region) name.getScene().getRoot();
            layOutAt(root, root.prefWidth(-1));
            assertEquals(whole, name.getText(), "the line should have given the name back");
            return roomy + "," + narrowed + "," + (name.getTooltip() == null ? "none" : "set");
        });

        // the whole name is on the card again, so naming it on hover says nothing further
        assertEquals("none,set,none", hovers);
    }

}
