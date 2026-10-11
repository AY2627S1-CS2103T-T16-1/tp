package seedu.tab.ui;

import java.util.Comparator;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import seedu.tab.model.student.Email;
import seedu.tab.model.student.Student;

/**
 * A UI component that displays information of a {@code Student}.
 */
public class StudentCard extends UiPart<Region> {

    private static final String FXML = "StudentListCard.fxml";

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
    private FlowPane tags;

    /**
     * Creates a {@code StudentCard} with the given {@code Student} and index to display.
     */
    public StudentCard(Student student, int displayedIndex) {
        super(FXML);
        this.student = student;
        id.setText(String.valueOf(displayedIndex));
        name.setText(student.getName().fullName);
        phone.setText(student.getPhone().value);
        show(email, student.getEmail().map(Email::toString).orElse(null));
        show(followUp, student.getFlag().map(flag -> flag.tagName).orElse(null));
        // a student carries no NUS ID yet, so the slot the card lays out for it stays empty
        show(nusId, null);
        student.getTags().stream()
                .sorted(Comparator.comparing(tag -> tag.tagName))
                .forEach(tag -> tags.getChildren().add(new Label(tag.tagName)));
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
