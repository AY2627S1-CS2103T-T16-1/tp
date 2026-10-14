package seedu.tab.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import seedu.tab.model.student.Email;
import seedu.tab.model.student.Student;

/**
 * A UI component that displays information of a {@code Student}.
 */
public class StudentCard extends UiPart<Region> {

    private static final String FXML = "StudentListCard.fxml";

    /**
     * How much of a name a card shows. A card is a summary and the name shares its row with
     * the tags and the pill, so a name longer than this is cut rather than taking the row.
     * The whole of it is still a hover away. The count is what a name is cut to, whatever
     * else the card holds, so every long name on the list breaks off at the same place.
     */
    private static final int MAX_NAME_CHARACTERS = 22;

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
    private VBox details;
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
    private TagRow tags;

    /**
     * Creates a {@code StudentCard} with the given {@code Student} and index to display.
     */
    public StudentCard(Student student, int displayedIndex) {
        super(FXML);
        this.student = student;
        id.setText(String.valueOf(displayedIndex));
        UiText.showShortened(name, student.getName().fullName, MAX_NAME_CHARACTERS);
        phone.setText(student.getPhone().value);
        show(email, student.getEmail().map(Email::toString).orElse(null));
        show(followUp, student.getFlag().map(flag -> flag.tagName).orElse(null));
        // a student carries no NUS ID yet, so the slot the card lays out for it stays empty
        show(nusId, null);
        clipToItsOwnBox(details);
        tags.setTags(student.getTags().stream()
                .map(tag -> tag.tagName)
                .sorted()
                .toList());
    }

    /**
     * Holds {@code details} to the space the card gives it, so that nothing in it is ever
     * drawn over the pill at the card's trailing edge. The lines inside give way on their
     * own, so this is the backstop rather than the rule.
     */
    private static void clipToItsOwnBox(Region details) {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(details.widthProperty());
        clip.heightProperty().bind(details.heightProperty());
        details.setClip(clip);
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
