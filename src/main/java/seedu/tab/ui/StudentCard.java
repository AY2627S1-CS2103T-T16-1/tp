package seedu.tab.ui;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import seedu.tab.model.student.Email;
import seedu.tab.model.student.NusStudentId;
import seedu.tab.model.student.Phone;
import seedu.tab.model.student.Student;
import seedu.tab.model.tag.Tag;

/**
 * A UI component that displays information of a {@code Student}.
 */
public class StudentCard extends UiPart<Region> {

    private static final String FXML = "StudentListCard.fxml";

    private final Student student;

    // JavaFX reserves names such as location and resources for injected fields.
    // See https://github.com/se-edu/addressbook-level4/issues/336.
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
    private Label indexWidth;
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
     * Creates a {@code StudentCard} with the given {@code Student} and index to display,
     * reserving room for an index of its own width.
     */
    public StudentCard(Student student, int displayedIndex) {
        this(student, displayedIndex, displayedIndex);
    }

    /**
     * Creates a {@code StudentCard} with the given {@code Student} and index to display,
     * reserving room for the widest index a list of {@code listSize} students can show so
     * that the text beside the index starts at the same place on every card.
     */
    public StudentCard(Student student, int displayedIndex, int listSize) {
        super(FXML);
        this.student = student;
        id.setText(String.valueOf(displayedIndex));
        indexWidth.setText(widestIndex(listSize));
        UiText.showToFit(name, student.getName().getFullName());
        show(phone, student.getPhone().map(Phone::toString).orElse(null));
        show(email, student.getEmail().map(Email::toString).orElse(null));
        show(followUp, student.getFlag().map(flag -> flag.getTagName()).orElse(null));
        show(nusStudentId, student.getNusStudentId().map(NusStudentId::toString).orElse(null));
        for (Label contact : new Label[] {nusStudentId, phone, email}) {
            capToRowWidth(contact, contacts);
        }
        clipToItsOwnBox(details);
        tags.setTags(student.getTags().stream()
                .sorted(Tag.DISPLAY_ORDER)
                .map(tag -> tag.getTagName())
                .toList());
    }

    /**
     * Returns a stand-in for the widest index a list of {@code listSize} students can show.
     * The digits of the fonts TAB runs with share one width, so only how many of them an
     * index has decides the column; an eight stands in for a font whose digits differ.
     */
    static String widestIndex(int listSize) {
        int digits = String.valueOf(Math.max(1, listSize)).length();
        return "8".repeat(digits);
    }

    /**
     * Holds {@code contact} to the width of the row it sits in, so a long detail is shortened
     * rather than pushing the card sideways. The cap waits for the row to have a width.
     *
     * <p>A card is measured before it is ever laid out, and until then the row reports a width
     * of zero. Capping at that width makes every detail measure as nothing wide, so the row
     * reports that they all fit on one line and the card asks for the height of an unwrapped
     * one. A list cell that keeps such a measurement shows a card too short for what is in it,
     * and the clip above hides the line that does not fit.
     */
    private static void capToRowWidth(Label contact, FlowPane row) {
        contact.maxWidthProperty().bind(Bindings.createDoubleBinding(() ->
                row.getWidth() > 0 ? row.getWidth() : Double.MAX_VALUE, row.widthProperty()));
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

    public Student getStudent() {
        return student;
    }
}
