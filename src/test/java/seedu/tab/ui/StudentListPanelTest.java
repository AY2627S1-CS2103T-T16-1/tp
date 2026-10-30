package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.ui.FxToolkit.onFxThread;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Region;
import seedu.tab.model.student.Student;
import seedu.tab.testutil.StudentBuilder;

public class StudentListPanelTest {

    /**
     * A list narrow enough that a card of its own size would outgrow it. At a comfortable
     * width the card fits whether or not it is held to the list, so the test passed with the
     * binding taken out and guarded nothing.
     */
    private static final double LIST_WIDTH = 300;
    /**
     * How far apart two cards' names may start and still read as one column. Positions snap
     * to the device pixel grid, so a scaled display can place two aligned names a fraction of
     * a logical pixel apart. Half a pixel stays far below the width of a digit, which is what
     * a ragged column costs.
     */
    private static final double ALIGNMENT_ALLOWANCE = 0.5;
    /** Tall enough that a list of ten cards lays every one of them out. */
    private static final double TALL_ENOUGH_FOR_TEN = 1200;

    @BeforeAll
    public static void startToolkit() throws Exception {
        FxToolkit.start();
    }


    @Test
    public void cells_reusedAfterStudent_clearDetailsAndSupportUnspecifiedWidth() throws Exception {
        onFxThread(() -> {
            Student student = new StudentBuilder().withEmail("e0923841@u.nus.edu").build();
            StudentListPanel panel = new StudentListPanel(FXCollections.observableArrayList(student));
            StudentListPanel.StudentListViewCell cell = panel.new StudentListViewCell();
            new Scene(cell);
            cell.applyCss();
            cell.updateItem(student, false);
            // JavaFX also queries preferred size before it assigns a concrete width.
            assertTrue(cell.prefHeight(-1) > 0);
            assertTrue(cell.prefHeight(LIST_WIDTH) > 0);
            Label email = (Label) cell.getGraphic().lookup("#email");
            assertEquals("e0923841@u.nus.edu", email.getText());

            cell.updateItem(null, true);
            assertNull(cell.getGraphic(), "an empty reused cell must not retain student details");
            assertNull(cell.getText());
            assertTrue(Double.isFinite(cell.prefHeight(LIST_WIDTH)));
            cell.updateItem(student, false);
            cell.updateItem(null, false);
            assertNull(cell.getGraphic(), "a null item must not retain student details");
            assertNull(cell.getText());
            return null;
        });
    }

    @Test
    public void resize_wrappingContacts_keepsHeadlineAndEveryDetailInsideEachCell() throws Exception {
        for (String email : List.of("e0923841@u.nus.edu",
                "muhammad.haziq.bin.abdul.rahman.shah@alumni.u.nus.edu",
                "a".repeat(41) + "@u.nus.edu")) {
            StudentBuilder builder = new StudentBuilder().withName("Siti Aminah")
                    .withPhone("98765432").withEmail(email);
            ObservableList<Student> students = FXCollections.observableArrayList(
                    builder.withFlag(true).build(), builder.withFlag(false).build(),
                    builder.withNusStudentId("A0234567X").withTags("T01", "Lab 3").withFlag(true).build(),
                    builder.withFlag(false).build());
            onFxThread(() -> {
                Region root = new StudentListPanel(students).getRoot();
                Scene scene = new Scene(root, 800, 400);
                scene.getStylesheets().add(UiPart.class.getResource("/view/DarkTheme.css").toExternalForm());
                root.applyCss();
                // Visit each pixel in both directions, including scrollbar appearance and
                // disappearance, rather than assuming a font-specific wrap threshold.
                for (int width = 800; width >= 298; width--) {
                    assertCardsFit(root, width);
                }
                for (int width = 298; width <= 800; width++) {
                    assertCardsFit(root, width);
                }
                return null;
            });
        }
    }

    private static void assertCardsFit(Region root, double width) {
        root.resize(width, 400);
        root.layout();
        for (Node node : root.lookupAll(".list-cell")) {
            if (!(node instanceof ListCell<?> cell) || cell.getGraphic() == null) {
                continue;
            }
            Region card = (Region) cell.getGraphic();
            Label name = (Label) card.lookup("#name");
            Region contacts = (Region) card.lookup("#contacts");
            Bounds headline = name.localToScene(name.getLayoutBounds());
            Bounds details = contacts.localToScene(contacts.getLayoutBounds());
            Bounds cardBounds = card.localToScene(card.getLayoutBounds());
            assertTrue(name.getHeight() >= name.prefHeight(-1) - 0.5, "the name is squeezed");
            assertTrue(headline.getMinY() >= cardBounds.getMinY(), "the name is clipped at the top");
            assertTrue(headline.getMaxY() <= details.getMinY(), "the headline overlaps contacts");
            for (Node field : contacts.getChildrenUnmodifiable().stream().filter(Node::isManaged).toList()) {
                Bounds bounds = field.localToScene(field.getLayoutBounds());
                assertTrue(bounds.getMaxY() <= cardBounds.getMaxY(), "a contact escapes the card");
                assertTrue(bounds.getMaxY() <= details.getMaxY() + 0.5, "a contact is clipped");
            }
            // Index glyph widths vary between cards and fonts. Compare each contact row
            // with its own card edge rather than requiring different cards to match.
            // JavaFX can round the allocated edge by one pixel at native display scales.
            assertEquals(cardBounds.getMaxX() - card.getInsets().getRight(), details.getMaxX(), 1.0,
                    "contacts must extend to the card's trailing padding, including beneath the flag");
        }
        assertFalse(root.lookupAll("#contacts").isEmpty(), "the list must contain populated cards");
    }

    @Test
    public void cells_longContent_doNotOutgrowTheList() throws Exception {
        Student student = new StudentBuilder()
                .withName("Nurul Izzah binte Hassan Al-Rashid Abdullah Mohamed")
                .withPhone("91112222")
                .withEmail("a.very.long.address.indeed@u.nus.edu")
                .withTags("AY2627 Sem 1 CS2103T T16")
                .withFlag(true).build();
        ObservableList<Student> students = FXCollections.observableArrayList(student);

        boolean fits = onFxThread(() -> {
            StudentListPanel panel = new StudentListPanel(students);
            Region root = panel.getRoot();
            new Scene(root, LIST_WIDTH, 400);
            root.applyCss();
            root.layout();

            Set<Node> cells = root.lookupAll(".list-cell");
            assertFalse(cells.isEmpty(), "the list should have laid out at least one cell");
            // a cell wider than the list is what puts a horizontal scroll bar under it, which
            // is what appeared as soon as the sidebar narrowed the list
            boolean cellsFit = cells.stream()
                    .allMatch(cell -> cell.getLayoutBounds().getWidth() <= LIST_WIDTH);
            // and the pill at the trailing edge is the first thing a card that outgrew the
            // list carries off the side of it
            Node pill = root.lookup("#followUp");
            assertNotNull(pill, "a flagged student should carry a pill to be checked");
            assertTrue(pill.isVisible(), "the pill should be drawn on a flagged student");
            boolean pillShows = pill.localToScene(pill.getBoundsInLocal()).getMaxX() <= LIST_WIDTH;
            // a phone number cut in half is of no use, so it keeps its width whatever else
            // the card has to give up
            Label phone = (Label) root.lookup("#phone");
            boolean isPhoneWhole = phone.getWidth() >= phone.prefWidth(-1) - 0.5;
            assertTrue(isPhoneWhole, "the phone was cut to " + phone.getWidth()
                    + " of the " + phone.prefWidth(-1) + " it needs");
            return cellsFit && pillShows;
        });

        assertTrue(fits, "a card outgrew the list and would scroll it sideways");
    }

    @Test
    public void resize_listGrowingToATwoDigitIndex_keepsEveryNameStartingAtTheSameX() throws Exception {
        onFxThread(() -> {
            ObservableList<Student> students = FXCollections.observableArrayList();
            for (int index = 1; index <= 9; index++) {
                students.add(new StudentBuilder().withName("Student " + index).build());
            }
            StudentListPanel panel = new StudentListPanel(students);
            Region root = panel.getRoot();
            Scene scene = new Scene(root, 900, TALL_ENOUGH_FOR_TEN);
            scene.getStylesheets().add(UiPart.class.getResource("/view/DarkTheme.css").toExternalForm());
            root.applyCss();
            root.layout();

            List<Double> single = nameStartPositions(root);
            assertEquals(9, single.size(), "every card of the nine should have laid out a name");
            assertAligned(single, "nine single-digit indices");

            // the tenth student widens the column, and the nine cards already on screen keep
            // their own students, so nothing rebuilds them unless the panel asks for it
            students.add(new StudentBuilder().withName("Student 10").build());
            root.applyCss();
            root.layout();

            List<Double> withTen = nameStartPositions(root);
            assertEquals(10, withTen.size(), "every card of the ten should have laid out a name");
            assertAligned(withTen, "nine single-digit indices and one double-digit index");
            assertTrue(min(withTen) > min(single),
                    "a two-digit index should widen the column, from " + min(single)
                            + " to " + min(withTen));
            return null;
        });
    }

    @Test
    public void widestIndex_listSizes_reserveOneDigitPerDigitOfTheSize() {
        assertEquals("8", StudentCard.widestIndex(0));
        assertEquals("8", StudentCard.widestIndex(1));
        assertEquals("8", StudentCard.widestIndex(9));
        assertEquals("88", StudentCard.widestIndex(10));
        assertEquals("88", StudentCard.widestIndex(99));
        assertEquals("888", StudentCard.widestIndex(100));
        assertEquals("8888", StudentCard.widestIndex(1000));
    }

    /** Returns where each laid-out card starts its name, in scene coordinates. */
    private static List<Double> nameStartPositions(Region root) {
        return root.lookupAll(".list-cell").stream()
                .filter(node -> node instanceof ListCell<?> cell && cell.getIndex() >= 0
                        && cell.getGraphic() != null)
                .map(node -> ((ListCell<?>) node).getGraphic().lookup("#name"))
                .filter(name -> name != null)
                .map(name -> name.localToScene(name.getLayoutBounds()).getMinX())
                .toList();
    }

    private static double min(List<Double> positions) {
        return positions.stream().mapToDouble(Double::doubleValue).min().orElseThrow();
    }

    private static void assertAligned(List<Double> positions, String listDescription) {
        double spread = positions.stream().mapToDouble(Double::doubleValue).max().orElseThrow()
                - min(positions);
        assertTrue(spread <= ALIGNMENT_ALLOWANCE,
                "names start " + spread + " apart with " + listDescription + ", at " + positions);
    }
}
