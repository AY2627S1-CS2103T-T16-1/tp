package seedu.tab.ui;

import static seedu.tab.model.util.StudentSummary.countByTag;
import static seedu.tab.model.util.StudentSummary.countFlagged;

import java.util.List;
import java.util.Locale;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import seedu.tab.commons.util.StringUtil;
import seedu.tab.model.student.Student;

/**
 * A panel summarizing the students on display: how many need following up, which tags are in
 * use and how many students carry each, and how many students there are in all.
 *
 * <p>It counts the list the window is showing rather than the whole student book, so a search
 * narrows the summary along with the list.
 */
public class SummaryPanel extends UiPart<Region> {

    private static final double TAG_ROW_SPACING = 4;

    private static final String FXML = "SummaryPanel.fxml";
    private static final String TOTAL_FORMAT = "%d student%s";

    @FXML
    private HBox flaggedRow;
    @FXML
    private Label flaggedCount;
    @FXML
    private ScrollPane tagScroll;
    @FXML
    private VBox tagRows;
    @FXML
    private Label total;

    /**
     * Creates a {@code SummaryPanel} over {@code students}, which it follows as commands change
     * what is on display.
     */
    public SummaryPanel(ObservableList<Student> students) {
        super(FXML);
        tagScroll.addEventFilter(KeyEvent.KEY_PRESSED, this::handlePageKeyPressed);
        students.addListener((ListChangeListener<Student>) change -> summarize(students));
        tagScroll.viewportBoundsProperty().addListener((unused, previous, viewport) ->
                reserveScrollBarWidth(viewport.getWidth()));
        summarize(students);
    }

    /** Scrolls a viewport per page key; JavaFX's ScrollPane otherwise uses the arrow-key increment. */
    private void handlePageKeyPressed(KeyEvent event) {
        boolean isPageKey = event.getCode() == KeyCode.PAGE_UP || event.getCode() == KeyCode.PAGE_DOWN;
        if (!isPageKey || !tagScroll.isFocused() || event.isControlDown() || event.isAltDown() || event.isMetaDown()
                || event.isShiftDown()) {
            return;
        }
        double viewportHeight = tagScroll.getViewportBounds().getHeight();
        double overflowHeight = tagRows.getHeight() - viewportHeight;
        if (overflowHeight > 0) {
            int direction = event.getCode() == KeyCode.PAGE_DOWN ? 1 : -1;
            double range = tagScroll.getVmax() - tagScroll.getVmin();
            double next = tagScroll.getVvalue() + direction * viewportHeight / overflowHeight * range;
            tagScroll.setVvalue(Math.clamp(next, tagScroll.getVmin(), tagScroll.getVmax()));
        }
        event.consume();
    }

    /**
     * Keeps the flagged count in the same column as the tag counts.
     *
     * <p>The tag list scrolls inside its own pane, so once there are more tags than fit, the
     * scroll bar takes its width out of the rows within it while the flagged row above is left
     * where it was. Reserving the same width beside the flagged count means the panel keeps one
     * column of numbers rather than splitting into two.
     */
    private void reserveScrollBarWidth(double viewportWidth) {
        double reserved = Math.max(0, tagScroll.getWidth() - viewportWidth);
        flaggedRow.setPadding(new Insets(0, reserved, 0, 0));
    }

    /**
     * Returns {@code count} with the word for it, so that one student is not "1 students".
     */
    public static String describeTotal(int count) {
        return String.format(Locale.ROOT, TOTAL_FORMAT, count, StringUtil.getPluralSuffix(count));
    }

    private void summarize(List<Student> students) {
        flaggedCount.setText(String.valueOf(countFlagged(students)));
        total.setText(describeTotal(students.size()));

        tagRows.getChildren().clear();
        countByTag(students).forEach((tag, count) -> tagRows.getChildren().add(createTagRow(tag, count)));
    }

    /**
     * Returns a row naming {@code tag} with its count pushed to the far edge.
     *
     * <p>Long names wrap so that tags with a shared prefix remain distinguishable.
     * The count keeps its full width beside the name, and sits against the middle of it, so
     * that a name taking two lines does not leave its count level with the first of them.
     */
    private static HBox createTagRow(String tag, int count) {
        Label name = new Label(tag);
        name.setWrapText(true);
        name.setMinWidth(0);
        name.getStyleClass().add("summary-name");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label number = new Label(String.valueOf(count));
        number.getStyleClass().add("summary-count");
        number.setMinWidth(Region.USE_PREF_SIZE);

        HBox row = new HBox(TAG_ROW_SPACING, name, spacer, number);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("summary-row");
        return row;
    }
}
