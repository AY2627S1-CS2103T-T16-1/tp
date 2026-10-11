package seedu.tab.ui;

import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import seedu.tab.model.student.Email;
import seedu.tab.model.student.Student;

/**
 * A UI component that displays information of a {@code Student}.
 */
public class StudentCard extends UiPart<Region> {

    private static final String FXML = "StudentListCard.fxml";
    /** Enough of a chip to read the start of a tag, kept even beside a very long name. */
    private static final double READABLE_CHIP_WIDTH = 70;

    /**
     * How much of a value a card shows. A card is a summary, and one enormous value should
     * not crowd out the rest of the record, so these are generous rather than tight: a name
     * longer than this is rare, and the whole of it is still a hover away.
     */
    private static final int MAX_NAME_CHARACTERS = 42;
    private static final int MAX_TAG_CHARACTERS = 16;
    private static final int MAX_TAGS_SHOWN = 3;
    private static final String ELLIPSIS = "\u2026";

    /**
     * Note: Certain keywords such as "location" and "resources" are reserved keywords in JavaFX.
     * As a consequence, UI elements' variable names cannot be set to such keywords
     * or an exception will be thrown by JavaFX during runtime.
     *
     * @see <a href="https://github.com/se-edu/addressbook-level4/issues/336">The issue on AddressBook level 4</a>
     */

    public final Student student;

    @FXML
    private HBox cardPane;
    @FXML
    private Label name;
    @FXML
    private Label id;
    @FXML
    private Label phone;
    @FXML
    private Label email;
    @FXML
    private Label followUp;
    @FXML
    private Label nusId;
    @FXML
    private HBox tags;

    /**
     * Creates a {@code StudentCard} with the given {@code Student} and index to display.
     */
    public StudentCard(Student student, int displayedIndex) {
        super(FXML);
        this.student = student;
        id.setText(String.valueOf(displayedIndex));
        showShortened(name, student.getName().fullName, MAX_NAME_CHARACTERS);
        phone.setText(student.getPhone().value);
        show(email, student.getEmail().map(Email::toString).orElse(null));
        show(followUp, student.getFlag().map(flag -> flag.tagName).orElse(null));
        // a student carries no NUS ID yet, so the slot the card lays out for it stays empty
        show(nusId, null);
        List<String> tagNames = student.getTags().stream()
                .map(tag -> tag.tagName)
                .sorted()
                .toList();
        tagNames.stream().limit(MAX_TAGS_SHOWN).forEach(this::addChip);
        if (tagNames.size() > MAX_TAGS_SHOWN) {
            // a row of chips cut down to single letters tells the User nothing, so the ones
            // that do not fit are counted instead of being squeezed in beside the rest
            Label more = new Label("+" + (tagNames.size() - MAX_TAGS_SHOWN));
            more.setTooltip(new Tooltip(String.join(", ", tagNames)));
            tags.getChildren().add(more);
        }
        // a student with tags keeps enough of the row for one chip to stay readable, while a
        // student with none leaves the whole row to the name
        tags.setMinWidth(tags.getChildren().isEmpty() ? 0 : READABLE_CHIP_WIDTH);
    }

    /**
     * Adds a chip for {@code tagName}, shortened if it is longer than a chip should be.
     */
    private void addChip(String tagName) {
        Label chip = new Label();
        showShortened(chip, tagName, MAX_TAG_CHARACTERS);
        tags.getChildren().add(chip);
    }

    /**
     * Puts {@code value} on {@code label}, cut to {@code limit} characters with an ellipsis if
     * it runs longer. What was cut is still readable: the label carries the whole value as its
     * tooltip, so a card summarises without the record becoming unreachable.
     */
    private static void showShortened(Label label, String value, int limit) {
        if (value.length() <= limit) {
            label.setText(value);
            return;
        }
        label.setText(value.substring(0, limit) + ELLIPSIS);
        label.setTooltip(new Tooltip(value));
    }

    /**
     * Puts {@code value} on {@code label}, or takes the label out of the card when there is no
     * value. An unmanaged label takes no space, so the card closes up rather than keeping a
     * blank line where the detail would have been.
     */
    private static void show(Label label, String value) {
        if (value == null) {
            label.setManaged(false);
            label.setVisible(false);
            return;
        }
        label.setText(value);
    }
}
