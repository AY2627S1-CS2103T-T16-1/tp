package seedu.tab.ui;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import seedu.tab.model.student.Student;

/**
 * A panel summarising the students on display: how many need following up, which tags are in
 * use and how many students carry each, and how many students there are in all.
 *
 * <p>It counts the list the window is showing rather than the whole student book, so a search
 * narrows the summary along with the list.
 */
public class SummaryPanel extends UiPart<Region> {

    private static final String FXML = "SummaryPanel.fxml";
    private static final String SINGULAR = "%d student";
    private static final String PLURAL = "%d students";

    @FXML
    private Label flaggedCount;
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
        students.addListener((javafx.collections.ListChangeListener<Student>) change -> summarise(students));
        summarise(students);
    }

    /**
     * Returns how many students each tag is on, most-used first and alphabetically among ties,
     * so that the order does not shift between runs for tags used equally often.
     */
    public static Map<String, Integer> countByTag(List<Student> students) {
        Map<String, Integer> counts = new TreeMap<>();
        for (Student student : students) {
            student.getTags().forEach(tag -> counts.merge(tag.tagName, 1, Integer::sum));
        }

        Map<String, Integer> ordered = new LinkedHashMap<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .forEach(entry -> ordered.put(entry.getKey(), entry.getValue()));
        return ordered;
    }

    /**
     * Returns how many of {@code students} are waiting on a reply.
     */
    public static long countFlagged(List<Student> students) {
        return students.stream().filter(Student::isFlagged).count();
    }

    /**
     * Returns {@code count} with the word for it, so that one student is not "1 students".
     */
    public static String describeTotal(int count) {
        return String.format(count == 1 ? SINGULAR : PLURAL, count);
    }

    private void summarise(List<Student> students) {
        flaggedCount.setText(String.valueOf(countFlagged(students)));
        total.setText(describeTotal(students.size()));

        tagRows.getChildren().clear();
        countByTag(students).forEach((tag, count) -> tagRows.getChildren().add(rowFor(tag, count)));
    }

    /**
     * Returns a row naming {@code tag} with its count pushed to the far edge.
     */
    private static HBox rowFor(String tag, int count) {
        Label name = new Label(tag);
        name.getStyleClass().add("summary-name");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label number = new Label(String.valueOf(count));
        number.getStyleClass().add("summary-count");

        HBox row = new HBox(name, spacer, number);
        row.getStyleClass().add("summary-row");
        return row;
    }
}
