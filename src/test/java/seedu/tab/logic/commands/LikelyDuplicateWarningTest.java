package seedu.tab.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.tab.model.student.Student;
import seedu.tab.testutil.StudentBuilder;

public class LikelyDuplicateWarningTest {
    private static final String SUCCESS_MESSAGE = "Student saved.";

    @Test
    public void appendTo_normalizedFieldsMatch_appendsEveryWarning() {
        Student existing = new StudentBuilder().withName("Alex Yeoh")
                .withPhone("+65 9123 4567").withEmail("Alex@Example.com").build();
        Student changed = new StudentBuilder().withName("  aLeX   yEoH  ")
                .withPhone("+6591234567").withEmail("alex@example.com").build();

        String expected = SUCCESS_MESSAGE
                + "\n\nWARNING: Possible duplicate student: Alex Yeoh has the same name (aLeX yEoH). "
                + "Delete one if they are the same person."
                + "\nWARNING: Possible duplicate student: Alex Yeoh has the same phone (+6591234567). "
                + "Delete one if they are the same person."
                + "\nWARNING: Possible duplicate student: Alex Yeoh has the same email (alex@example.com). "
                + "Delete one if they are the same person.";

        assertEquals(expected, LikelyDuplicateWarning.appendTo(
                SUCCESS_MESSAGE, changed, List.of(existing)));
    }

    @Test
    public void appendTo_noMatchingFields_returnsSuccessMessage() {
        Student existing = new StudentBuilder().withName("Alex Yeoh")
                .withPhone("91234567").withoutEmail().build();
        Student changed = new StudentBuilder().withName("Bernice Yu")
                .withPhone("92345678").withoutEmail().build();

        assertEquals(SUCCESS_MESSAGE, LikelyDuplicateWarning.appendTo(
                SUCCESS_MESSAGE, changed, List.of(existing)));
    }

    @Test
    public void appendTo_excludedStudentMatchesItself_returnsSuccessMessage() {
        Student changed = new StudentBuilder().build();

        assertEquals(SUCCESS_MESSAGE, LikelyDuplicateWarning.appendToExcluding(
                SUCCESS_MESSAGE, changed, List.of(changed), changed));
    }
}
