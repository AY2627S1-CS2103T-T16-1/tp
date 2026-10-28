package seedu.tab.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.tab.model.student.Student;
import seedu.tab.testutil.StudentBuilder;

public class DotlessEmailDomainWarningTest {
    private static final String SUCCESS_MESSAGE = "Student saved.";

    private static String expectedWarningFor(String address) {
        return SUCCESS_MESSAGE + "\n\nWARNING: Possibly incomplete email: " + address
                + " has no dot in its domain.\n"
                + "Check whether a part such as .com or .nus.edu is missing; the address is saved as entered.";
    }

    @Test
    public void appendTo_dotlessDomain_warnsWithoutChangingTheAddress() {
        for (String address : List.of("john@gmail", "e1234567@nus", "JOHN@GMAIL")) {
            Student added = new StudentBuilder().withName("Tan").withEmail(address).build();

            assertEquals(expectedWarningFor(address), DotlessEmailDomainWarning.appendTo(SUCCESS_MESSAGE, added));
            assertEquals(address, added.getEmail().orElseThrow().getValue());
        }
    }

    @Test
    public void appendTo_dottedDomain_doesNotWarn() {
        for (String address : List.of("john@gmail.com", "e1234567@u.nus.edu", "john.tan@x.co")) {
            Student added = new StudentBuilder().withName("Tan").withEmail(address).build();

            assertEquals(SUCCESS_MESSAGE, DotlessEmailDomainWarning.appendTo(SUCCESS_MESSAGE, added));
        }
    }

    @Test
    public void appendTo_absentEmail_doesNotWarn() {
        Student added = new StudentBuilder().withName("Tan").withoutEmail().build();

        assertEquals(SUCCESS_MESSAGE, DotlessEmailDomainWarning.appendTo(SUCCESS_MESSAGE, added));
    }

    @Test
    public void appendTo_dotlessDomainAlsoHeldByAnother_warnsAfterTheDuplicateWarning() {
        Student existing = new StudentBuilder().withName("Lee").withoutPhone().withEmail("john@gmail").build();
        Student added = new StudentBuilder().withName("Tan").withoutPhone().withEmail("john@gmail").build();

        String feedback = LikelyDuplicateWarning.appendTo(SUCCESS_MESSAGE, added, List.of(existing));

        assertEquals(SUCCESS_MESSAGE + "\n\nWARNING: Possible duplicate student: Lee has the same email "
                + "(john@gmail).\nReview the records and delete duplicates only if they are the same person."
                + "\n\nWARNING: Possibly incomplete email: john@gmail has no dot in its domain.\n"
                + "Check whether a part such as .com or .nus.edu is missing; the address is saved as entered.",
                DotlessEmailDomainWarning.appendTo(feedback, added));
    }

    @Test
    public void appendChangedEmailTo_newDotlessDomain_warns() {
        Student original = new StudentBuilder().withName("Tan").withEmail("john@gmail.com").build();
        Student edited = new StudentBuilder().withName("Tan").withEmail("john@gmail").build();

        assertEquals(expectedWarningFor("john@gmail"),
                DotlessEmailDomainWarning.appendChangedEmailTo(SUCCESS_MESSAGE, original, edited));
    }

    @Test
    public void appendChangedEmailTo_unchangedDotlessDomain_doesNotWarn() {
        // an edit of another field must not repeat a warning the User has already seen
        Student original = new StudentBuilder().withName("Tan").withEmail("john@gmail").build();
        Student edited = new StudentBuilder().withName("Tan").withEmail("john@gmail").withTags("T1").build();

        assertEquals(SUCCESS_MESSAGE,
                DotlessEmailDomainWarning.appendChangedEmailTo(SUCCESS_MESSAGE, original, edited));
    }

    @Test
    public void appendChangedEmailTo_dotlessDomainCleared_doesNotWarn() {
        Student original = new StudentBuilder().withName("Tan").withEmail("john@gmail").build();
        Student edited = new StudentBuilder().withName("Tan").withoutEmail().build();

        assertEquals(SUCCESS_MESSAGE,
                DotlessEmailDomainWarning.appendChangedEmailTo(SUCCESS_MESSAGE, original, edited));
    }
}
