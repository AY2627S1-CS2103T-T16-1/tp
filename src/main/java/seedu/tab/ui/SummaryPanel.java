package seedu.tab.ui;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BinaryOperator;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import seedu.tab.commons.util.StringUtil;
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
        students.addListener((javafx.collections.ListChangeListener<Student>) change -> summarise(students));
        tagScroll.viewportBoundsProperty().addListener((unused, previous, viewport) ->
                reserveScrollBarWidth(viewport.getWidth()));
        summarise(students);
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
     * Returns how many students each tag is on, most-used first and alphabetically among ties,
     * so that the order does not shift between runs for tags used equally often.
     *
     * <p>Case variants of one tag are counted together, because one tag is what {@code tag}
     * makes of them: it refuses to add a tag a student already carries in another case. A row
     * therefore gives the size of the group a reader is looking for rather than the size of one
     * spelling of it.
     *
     * <p>Two spellings are of one tag when {@link String#CASE_INSENSITIVE_ORDER} calls them
     * equal, which is exactly when {@code String#equalsIgnoreCase} does, and so exactly when
     * {@code tag} refuses the second of them. Folding case by lowercasing would answer a
     * different question and split a group {@code tag} has already joined: a Greek capital
     * sigma lowercases to {@code \u03C3}, while the final sigma {@code \u03C2} a student may
     * carry the same tag under lowercases to itself.
     */
    public static Map<String, Integer> countByTag(List<Student> students) {
        Map<String, Integer> byVariant = new TreeMap<>();
        Map<String, Integer> counts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Student student : students) {
            student.getTags().forEach(tag -> byVariant.merge(tag.tagName, 1, Integer::sum));
            // a student carrying two spellings of one tag is still one student in its group
            tagsOf(student).forEach(tag -> counts.merge(tag, 1, Integer::sum));
        }
        return orderByUse(counts, nameEachTag(byVariant));
    }

    /**
     * Returns the tags {@code student} carries, with two spellings of one tag coming back once.
     */
    private static Set<String> tagsOf(Student student) {
        Set<String> tags = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        student.getTags().forEach(tag -> tags.add(tag.tagName));
        return tags;
    }

    /**
     * Returns the name to list each tag under, which any of its spellings looks up.
     *
     * <p>A group is named with the spelling most of its students carry, so that the panel reads
     * as the cohort was typed. Spellings carried equally often are settled by order, because a
     * name that moved with the list would read as a second tag appearing.
     */
    private static Map<String, String> nameEachTag(Map<String, Integer> byVariant) {
        BinaryOperator<String> moreUsed = (named, other) ->
                byVariant.get(other) > byVariant.get(named) ? other : named;
        Map<String, String> names = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        byVariant.keySet().forEach(variant -> names.merge(variant, variant, moreUsed));
        return names;
    }

    /**
     * Returns the tags in {@code counts} most-used first, each under the name {@code names}
     * gives it.
     *
     * <p>The keys of {@code counts} hold one spelling per tag, and no two of them are case
     * variants of each other, so ordering them without regard to case orders the rows
     * alphabetically and leaves no tie for the spelling a key happens to hold to settle.
     */
    private static Map<String, Integer> orderByUse(Map<String, Integer> counts, Map<String, String> names) {
        Map<String, Integer> ordered = new LinkedHashMap<>();
        counts.entrySet().stream()
                // two tags counted the same read as a list in alphabetical order, and a
                // reader does not read "Zoo" as coming before "apple"
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER))
                .forEach(entry -> ordered.put(names.get(entry.getKey()), entry.getValue()));
        return ordered;
    }

    /**
     * Returns how many of {@code students} need follow-up.
     */
    public static long countFlagged(List<Student> students) {
        return students.stream().filter(Student::isFlagged).count();
    }

    /**
     * Returns {@code count} with the word for it, so that one student is not "1 students".
     */
    public static String describeTotal(int count) {
        return String.format(Locale.ROOT, TOTAL_FORMAT, count, StringUtil.getPluralSuffix(count));
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
     * The count keeps its full width beside the name, and sits against the middle of it, so
     * that a name taking two lines does not leave its count level with the first of them.
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
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("summary-row");
        return row;
    }
}
