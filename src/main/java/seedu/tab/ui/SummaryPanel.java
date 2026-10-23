package seedu.tab.ui;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

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

    private static final double TAG_ROW_SPACING = 4;

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
     *
     * <p>Case variants of one tag are counted together, because one tag is what the rest of TAB
     * makes of them: {@code tag} refuses to add a tag a student already carries in another case,
     * and {@code find} matches every variant. A row therefore gives the size of the group a
     * reader is looking for rather than the size of one spelling of it.
     */
    public static Map<String, Integer> countByTag(List<Student> students) {
        Map<String, Integer> byVariant = new TreeMap<>();
        Map<String, Integer> counts = new TreeMap<>();
        for (Student student : students) {
            student.getTags().forEach(tag -> byVariant.merge(tag.tagName, 1, Integer::sum));
            // a student carrying two spellings of one tag is still one student in its group
            tagsOf(student).forEach(tag -> counts.merge(tag, 1, Integer::sum));
        }
        return orderByUse(counts, nameEachTag(byVariant));
    }

    /**
     * Returns the tags {@code student} carries, with their case folded away so that two
     * spellings of one tag come back as one tag.
     */
    private static Set<String> tagsOf(Student student) {
        return student.getTags().stream()
                .map(tag -> tag.tagName.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    /**
     * Returns the name to list each tag in {@code byVariant} under, keyed by the tag with its
     * case folded away.
     *
     * <p>A group is named with the spelling most of its students carry, so that the panel reads
     * as the cohort was typed. Spellings carried equally often are settled by order, because a
     * name that moved with the list would read as a second tag appearing.
     */
    private static Map<String, String> nameEachTag(Map<String, Integer> byVariant) {
        BinaryOperator<String> moreUsed = (named, other) ->
                byVariant.get(other) > byVariant.get(named) ? other : named;
        Map<String, String> names = new TreeMap<>();
        byVariant.keySet().forEach(variant ->
                names.merge(variant.toLowerCase(Locale.ROOT), variant, moreUsed));
        return names;
    }

    /**
     * Returns the tags in {@code counts} most-used first, each under the name {@code names}
     * gives it.
     *
     * <p>The keys of {@code counts} are tags with their case folded away, so ordering by them
     * orders the rows alphabetically without regard to case, and no two of them are equal.
     */
    private static Map<String, Integer> orderByUse(Map<String, Integer> counts, Map<String, String> names) {
        Map<String, Integer> ordered = new LinkedHashMap<>();
        counts.entrySet().stream()
                // two tags counted the same read as a list in alphabetical order, and a
                // reader does not read "Zoo" as coming before "apple"
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .forEach(entry -> ordered.put(names.get(entry.getKey()), entry.getValue()));
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
     *
     * <p>Long names wrap so that tags with a shared prefix remain distinguishable.
     * The count keeps its full width beside the name.
     */
    private static HBox rowFor(String tag, int count) {
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
        row.getStyleClass().add("summary-row");
        return row;
    }
}
