package seedu.tab.ui;

import static seedu.tab.commons.util.CollectionUtil.NOT_FOUND;
import static seedu.tab.commons.util.CollectionUtil.indexOfSame;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.layout.Region;
import seedu.tab.model.student.Student;

/**
 * Panel containing the list of students.
 */
public class StudentListPanel extends UiPart<Region> {
    private static final String FXML = "StudentListPanel.fxml";
    /** Room left beside a card for the list's own scroll bar and insets. */
    private static final double SCROLL_BAR_ALLOWANCE = 18;

    @FXML
    private ListView<Student> studentListView;

    /** The index width the cards on screen were built for, so a rebuild is rare. */
    private String builtForWidestIndex = "";

    /**
     * Creates a {@code StudentListPanel} with the given {@code ObservableList}.
     */
    public StudentListPanel(ObservableList<Student> studentList) {
        super(FXML);
        studentListView.setItems(studentList);
        studentListView.setCellFactory(listView -> new StudentListViewCell());
        studentList.addListener((ListChangeListener<Student>) change -> rebuildOnWiderIndex());
    }

    /**
     * Rebuilds the cards when the list's largest index gains or loses a digit, so the room
     * every card holds for its index follows the list it sits in. A cell keeps its card while
     * its own student is unchanged, so the cards already on screen would otherwise keep the
     * column the list had when they were built, and the list would read ragged until the
     * student under each of them changed.
     */
    private void rebuildOnWiderIndex() {
        String widestIndex = StudentCard.widestIndex(studentListView.getItems().size());
        if (widestIndex.equals(builtForWidestIndex)) {
            return;
        }
        builtForWidestIndex = widestIndex;
        studentListView.refresh();
    }

    /**
     * Scrolls the student's card into view without moving keyboard focus.
     */
    public void scrollTo(Student student) {
        Scene scene = studentListView.getScene();
        scene.addPostLayoutPulseListener(new Reveal(scene, student));
        Platform.requestNextPulse();
    }

    /** Scrolls the list so that {@code student}'s card is in view, if the list still holds it. */
    private void scrollListTo(Student student) {
        int index = indexOfSame(studentListView.getItems(), student);
        if (index == NOT_FOUND) {
            return;
        }
        VirtualFlow<?> flow = (VirtualFlow<?>) studentListView.lookup(".virtual-flow");
        flow.scrollTo(index);
    }

    /** Returns whether the whole of {@code student}'s card sits inside the list. */
    private boolean isFullyInView(Student student) {
        Bounds list = studentListView.localToScene(studentListView.getLayoutBounds());
        return studentListView.lookupAll(".list-cell").stream()
                .filter(node -> node instanceof ListCell<?> cell && cell.getItem() == student)
                .anyMatch(node -> {
                    Bounds card = node.localToScene(node.getLayoutBounds());
                    return card.getMinY() >= list.getMinY() && card.getMaxY() <= list.getMaxY();
                });
    }

    /**
     * Scrolls a student's card into view once per layout pass, until the whole card is in view
     * or the passes run out, and then takes itself off the scene.
     *
     * <p>Each pass aims at the heights the list knows so far, and those keep changing under it:
     * the first materializes the cards, and a card whose contact details wrap grows once it has
     * a width to wrap against, after the scroll that revealed it. Stopping on a count rather
     * than on the card leaves such a card part way off the bottom for good, because nothing
     * scrolls the list again. The count remains as a stop for a card taller than the list
     * itself, which no amount of scrolling can bring wholly into view.
     */
    private final class Reveal implements Runnable {
        private static final int PASSES = 8;

        private final Scene scene;
        private final Student student;
        private int passesLeft = PASSES;

        private Reveal(Scene scene, Student student) {
            this.scene = scene;
            this.student = student;
        }

        @Override
        public void run() {
            scrollListTo(student);
            passesLeft--;
            if (passesLeft > 0 && !isFullyInView(student)) {
                Platform.requestNextPulse();
                return;
            }
            scene.removePostLayoutPulseListener(this);
        }
    }

    /**
     * Returns the card for {@code student}, held to the width the list has.
     *
     * <p>A card is bounded rather than starved. Asking the cell for no width at all does stop
     * a long name pushing the list sideways, but it also leaves the card laying itself out in
     * no room, so every name is elided to its minimum and stays that way once the list has
     * been resized. A maximum leaves each part of the card its preferred width until the card
     * really has run out of room.
     */
    private Region createCard(Student student, int displayedIndex) {
        Region card = new StudentCard(student, displayedIndex, studentListView.getItems().size()).getRoot();
        card.maxWidthProperty().bind(studentListView.widthProperty().subtract(SCROLL_BAR_ALLOWANCE));
        return card;
    }

    /**
     * Custom {@code ListCell} that displays the graphics of a {@code Student} using a {@code StudentCard}.
     */
    class StudentListViewCell extends ListCell<Student> {
        /**
         * Measures the graphic at its capped width so wrapped contact details retain their height.
         */
        @Override
        protected double computePrefHeight(double width) {
            if (!(getGraphic() instanceof Region card) || width < 0) {
                return super.computePrefHeight(width);
            }
            // The skin caps the graphic at its maximum width during layout. Measure at
            // that same width so a wrapped contact row receives all the height it needs.
            double contentWidth = Math.max(0, width - snappedLeftInset() - snappedRightInset());
            double cardWidth = Math.max(0, Math.min(contentWidth, card.maxWidth(-1)));
            return snappedTopInset() + card.prefHeight(cardWidth) + snappedBottomInset();
        }

        /**
         * Replaces the graphic for a displayed student and clears graphics on recycled empty cells.
         */
        @Override
        protected void updateItem(Student student, boolean isEmpty) {
            super.updateItem(student, isEmpty);

            if (isEmpty || student == null) {
                setGraphic(null);
                setText(null);
            } else {
                setGraphic(createCard(student, getIndex() + 1));
            }
        }
    }

}
