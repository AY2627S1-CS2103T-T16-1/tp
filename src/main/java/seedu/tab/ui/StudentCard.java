package seedu.tab.ui;

import java.util.Comparator;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import seedu.tab.model.student.NusStudentId;
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
     *
     * <p>The cut is taken from the middle. Students who share the front of a name are common
     * enough in a tutorial group, and a cut at the close puts two of them on the list as the
     * same text, which leaves a reader scanning it nothing to go by. Keeping the end of the
     * name as well tells the two cards apart on sight.
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
    private FlowPane contacts;
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
    private Label nusStudentId;
    @FXML
    private TagRow tags;

    /**
     * Creates a {@code StudentCard} with the given {@code Student} and index to display.
     */
    public StudentCard(Student student, int displayedIndex) {
        super(FXML);
        this.student = student;
        id.setText(String.valueOf(displayedIndex));
        UiText.showMiddleShortenedToFit(name, student.getName().fullName, MAX_NAME_CHARACTERS);
        UiText.showToFit(phone, student.getPhone().value);
        if (student.getEmail().isPresent()) {
            UiText.showToFit(email, student.getEmail().orElseThrow().toString());
        } else {
            show(email, null);
        }
        show(followUp, student.getFlag().map(flag -> flag.tagName).orElse(null));
        show(nusStudentId, student.getNusStudentId().map(NusStudentId::toString).orElse(null));
        for (Label contact : new Label[] {nusStudentId, phone, email}) {
            contact.maxWidthProperty().bind(contacts.widthProperty());
        }
        clipToItsOwnBox(details);
        tags.setTags(student.getTags().stream()
                .map(tag -> tag.tagName)
                .sorted(String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder()))
                .toList());
    }

    /**
     * Holds {@code details} to the space the card gives it. The lines inside give way on
     * their own, so clipping is a backstop against drawing outside the card.
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
        UiText.showToFit(label, value);
    }
}
