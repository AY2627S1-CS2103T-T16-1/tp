package seedu.tab.ui;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
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

    /**
     * Creates a {@code StudentListPanel} with the given {@code ObservableList}.
     */
    public StudentListPanel(ObservableList<Student> studentList) {
        super(FXML);
        studentListView.setItems(studentList);
        studentListView.setCellFactory(listView -> new StudentListViewCell());
    }

    /**
     * Scrolls the student's card into view without moving keyboard focus.
     */
    public void scrollTo(Student student) {
        Scene scene = studentListView.getScene();
        Runnable reveal = new Runnable() {
            private boolean hasScrolled;

            @Override
            public void run() {
                if (hasScrolled) {
                    scene.removePostLayoutPulseListener(this);
                }
                int index = studentListView.getItems().indexOf(student);
                if (index >= 0) {
                    VirtualFlow<?> flow = (VirtualFlow<?>) studentListView.lookup(".virtual-flow");
                    flow.scrollTo(index);
                }
                hasScrolled = true;
                Platform.requestNextPulse();
            }
        };
        // The first scroll materializes cards; the next pulse corrects their measured heights.
        scene.addPostLayoutPulseListener(reveal);
        Platform.requestNextPulse();
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
    private Region cardFor(Student student, int displayedIndex) {
        Region card = new StudentCard(student, displayedIndex).getRoot();
        card.maxWidthProperty().bind(studentListView.widthProperty().subtract(SCROLL_BAR_ALLOWANCE));
        return card;
    }

    /**
     * Custom {@code ListCell} that displays the graphics of a {@code Student} using a {@code StudentCard}.
     */
    class StudentListViewCell extends ListCell<Student> {
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

        @Override
        protected void updateItem(Student student, boolean empty) {
            super.updateItem(student, empty);

            if (empty || student == null) {
                setGraphic(null);
                setText(null);
            } else {
                setGraphic(cardFor(student, getIndex() + 1));
            }
        }
    }

}
