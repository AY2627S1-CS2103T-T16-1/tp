package seedu.tab.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.tab.model.student.Student;
import seedu.tab.testutil.StudentBuilder;

public class LikelyDuplicateWarningTest {
    private static final String SUCCESS_MESSAGE = "Student saved.";

    @Test
    public void appendTo_equivalentNames_warnsWithoutChangingSpelling() {
        String[][] cases = {
            {"Sean O’Brien", "Sean O'Brien"},
            {"Mary O'Neil", "Mary O’Neil"},
            {"D\u02bcSouza", "D'Souza"},
            {"D'Souza", "D\u02bcSouza"},
            {"Hawai\u02bbi Lee", "Hawai'i Lee"},
            {"Tat\u02b9yana Ivanova", "Tat'yana Ivanova"},
            {"D\uff07Souza", "D\u2032Souza"},
            {"José Rivera", "JOSE RIVERA"},
            {"Nguyễn Văn An", "nguyen van an"},
            {"Đặng Thị Mai", "Dang Thi Mai"},
            {"Dang Thi Mai", "Đặng Thị Mai"},
            {"Łukasz Nowak", "Lukasz Nowak"},
            {"Lukasz Nowak", "Łukasz Nowak"},
            {"Søren Kierkegaard", "Soren Kierkegaard"},
            {"Soren Kierkegaard", "Søren Kierkegaard"},
            {"ΝΙΚΟΣ", "νικοσ"},
            {"νικοσ", "νικος"},
            {"Charlotte Oliveiro", "Charlotte-Oliveiro"},
            {"Mary-Jane Tan", "Mary Jane Tan"},
            {"Mary–Jane Tan", "Mary Jane Tan"},
            {"Mary Jane Tan", "Mary–Jane Tan"},
            {"Mary—Jane Tan", "Mary-Jane Tan"},
            {"Mary−Jane Tan", "Mary Jane Tan"},
            {"Mary－Jane Tan", "Mary–Jane Tan"}
        };
        for (String[] pair : cases) {
            String storedName = pair[0];
            String typedName = pair[1];
            Student existing = new StudentBuilder().withName(storedName).withPhone("91234567").withoutEmail().build();
            Student changed = new StudentBuilder().withName(typedName).withPhone("92345678").withoutEmail().build();
            String expected = SUCCESS_MESSAGE + "\n\nWARNING: Possible duplicate student: " + storedName
                    + " has the same name."
                    + "\nReview the records and delete duplicates only if they are the same person.";
            assertEquals(expected, LikelyDuplicateWarning.appendTo(SUCCESS_MESSAGE, changed, List.of(existing)));
            assertEquals(storedName, existing.getName().getFullName());
            assertEquals(typedName, changed.getName().getFullName());
        }
    }

    @Test
    public void appendTo_dashShapedLetterInName_doesNotWarn() {
        // the prolonged sound mark is a letter of the name, not a dash standing for a space
        Student existing = new StudentBuilder().withName("Mari Jane Tan").withoutPhone().withoutEmail().build();
        Student changed = new StudentBuilder().withName("MariーJane Tan").withoutPhone().withoutEmail().build();

        assertEquals(SUCCESS_MESSAGE, LikelyDuplicateWarning.appendTo(
                SUCCESS_MESSAGE, changed, List.of(existing)));
    }

    @Test
    public void appendTo_equivalentPhoneDigits_warnsWithoutChangingFormatting() {
        Student existing = new StudentBuilder().withName("Alice Tan").withPhone("9123-4567")
                .withoutEmail().build();
        for (String phone : List.of("91234567", "(9123) 4567", "9123.4567", "+9123-4567",
                "９１２３４５６７", "٩١٢٣٤٥٦٧", "9123\u00a04567")) {
            Student changed = new StudentBuilder().withName("Alicia Tan").withPhone(phone).withoutEmail().build();

            assertEquals(SUCCESS_MESSAGE + "\n\nWARNING: Possible duplicate student: Alice Tan "
                    + "has the same phone.\n"
                    + "Review the records and delete duplicates only if they are the same person.",
                    LikelyDuplicateWarning.appendTo(SUCCESS_MESSAGE, changed, List.of(existing)));
            assertEquals("9123-4567", existing.getPhone().orElseThrow().getValue());
        }
    }

    @Test
    public void appendTo_differentFullPhoneDigits_doesNotWarn() {
        Student existing = new StudentBuilder().withName("Alice Tan").withPhone("9123-4567")
                .withoutEmail().build();
        for (String phone : List.of("+65 91234567", "91234567 ext 21", "91234568", "9123456")) {
            Student changed = new StudentBuilder().withName("Alicia Tan").withPhone(phone).withoutEmail().build();

            assertEquals(SUCCESS_MESSAGE, LikelyDuplicateWarning.appendTo(
                    SUCCESS_MESSAGE, changed, List.of(existing)));
        }
    }

    @Test
    public void appendTo_absentPhoneOnEitherSide_doesNotWarn() {
        // two records that both lack a phone share no number, so neither is evidence of a duplicate
        Student withPhone = new StudentBuilder().withName("Alice Tan").withPhone("91234567")
                .withoutEmail().build();
        Student alsoWithoutPhone = new StudentBuilder().withName("Alicia Tan").withoutPhone()
                .withoutEmail().build();
        Student changed = new StudentBuilder().withName("Casey Low").withoutPhone().withoutEmail().build();

        assertEquals(SUCCESS_MESSAGE, LikelyDuplicateWarning.appendTo(
                SUCCESS_MESSAGE, changed, List.of(withPhone, alsoWithoutPhone)));
    }

    @Test
    public void appendTo_normalizedFieldsMatch_appendsEveryWarning() {
        Student existing = new StudentBuilder().withName("Alex Yeoh")
                .withPhone("+65 9123 4567").withEmail("Alex@Example.com").build();
        Student changed = new StudentBuilder().withName("  aLeX   yEoH  ")
                .withPhone("+6591234567").withEmail("alex@example.com").build();

        String expected = SUCCESS_MESSAGE
                + "\n\nWARNING: Possible duplicate student: Alex Yeoh has the same name."
                + "\nWARNING: Possible duplicate student: Alex Yeoh has the same phone."
                + "\nWARNING: Possible duplicate student: Alex Yeoh has the same email."
                + "\nReview the records and delete duplicates only if they are the same person.";

        assertEquals(expected, LikelyDuplicateWarning.appendTo(
                SUCCESS_MESSAGE, changed, List.of(existing)));
    }

    @Test
    public void appendChangedFieldsTo_multipleNameAndEmailMatches_omitsAmbiguousValues() {
        Student original = new StudentBuilder().withName("Other Name").withoutPhone().withoutEmail().build();
        Student changed = new StudentBuilder(original).withName("Jose Ng").withEmail("upper@x.com").build();
        Student first = new StudentBuilder().withName("José Ng").withoutPhone().withEmail("UPPER@X.COM").build();
        Student second = new StudentBuilder().withName("JOSE NG").withoutPhone().withEmail("Upper@x.com").build();
        String expected = SUCCESS_MESSAGE
                + "\n\nWARNING: Possible duplicate students: José Ng and JOSE NG have the same name."
                + "\nWARNING: Possible duplicate students: José Ng and JOSE NG have the same email."
                + "\nReview the records and delete duplicates only if they are the same person.";

        assertEquals(expected, LikelyDuplicateWarning.appendChangedFieldsTo(
                SUCCESS_MESSAGE, original, changed, List.of(original, first, second)));
        assertEquals("José Ng", first.getName().getFullName());
        assertEquals("UPPER@X.COM", first.getEmail().orElseThrow().getValue());
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
    public void appendChangedFieldsTo_unchangedMatches_returnsSuccessMessage() {
        Student original = new StudentBuilder().build();
        Student changed = new StudentBuilder(original).withTags("tutorial").build();
        Student matching = new StudentBuilder(original).withNusStudentId("A01234567X").build();

        assertEquals(SUCCESS_MESSAGE, LikelyDuplicateWarning.appendChangedFieldsTo(
                SUCCESS_MESSAGE, original, changed, List.of(original, matching)));
    }

    @Test
    public void appendChangedFieldsTo_changedPhoneMatches_warnsOnlyForPhone() {
        Student original = new StudentBuilder().withName("Tan Wei Ming")
                .withPhone("91234567").withEmail("shared@example.com").build();
        Student changed = new StudentBuilder(original).withPhone("92345678").build();
        Student matching = new StudentBuilder(original).withPhone("9234 5678").build();

        String expected = SUCCESS_MESSAGE
                + "\n\nWARNING: Possible duplicate student: Tan Wei Ming has the same phone."
                + "\nReview the records and delete duplicates only if they are the same person.";

        assertEquals(expected, LikelyDuplicateWarning.appendChangedFieldsTo(
                SUCCESS_MESSAGE, original, changed, List.of(original, matching)));
    }

    @Test
    public void appendTo_twoPhoneMatches_namesBothStudents() {
        Student first = new StudentBuilder().withName("Student One").withPhone("000").withoutEmail().build();
        Student second = new StudentBuilder().withName("Student Two").withPhone("0 0 0").withoutEmail().build();
        Student changed = new StudentBuilder().withName("Student Three").withPhone("000").withoutEmail().build();

        String expected = SUCCESS_MESSAGE
                + "\n\nWARNING: Possible duplicate students: Student One and Student Two "
                + "have the same phone."
                + "\nReview the records and delete duplicates only if they are the same person.";

        assertEquals(expected, LikelyDuplicateWarning.appendTo(
                SUCCESS_MESSAGE, changed, List.of(first, second)));
    }

    @Test
    public void appendTo_threePhoneMatches_summarizesOneOtherStudent() {
        Student first = new StudentBuilder().withName("Student One").withPhone("000").withoutEmail().build();
        Student second = new StudentBuilder().withName("Student Two").withPhone("0 0 0").withoutEmail().build();
        Student third = new StudentBuilder().withName("Student Three").withPhone("000").withoutEmail().build();
        Student changed = new StudentBuilder().withName("Student Four").withPhone("000").withoutEmail().build();

        String expected = SUCCESS_MESSAGE
                + "\n\nWARNING: Possible duplicate students: Student One, Student Two and 1 other "
                + "have the same phone."
                + "\nReview the records and delete duplicates only if they are the same person.";

        assertEquals(expected, LikelyDuplicateWarning.appendTo(
                SUCCESS_MESSAGE, changed, List.of(first, second, third)));
    }

    @Test
    public void appendTo_manyPhoneMatches_groupsStudentNames() {
        Student first = new StudentBuilder().withName("Student One").withPhone("000").withoutEmail().build();
        Student second = new StudentBuilder().withName("Student Two").withPhone("0 0 0").withoutEmail().build();
        Student third = new StudentBuilder().withName("Student Three").withPhone("000").withoutEmail().build();
        Student fourth = new StudentBuilder().withName("Student Four").withPhone("000").withoutEmail().build();
        Student changed = new StudentBuilder().withName("Student Five").withPhone("000").withoutEmail().build();

        String expected = SUCCESS_MESSAGE
                + "\n\nWARNING: Possible duplicate students: Student One, Student Two and 2 others "
                + "have the same phone."
                + "\nReview the records and delete duplicates only if they are the same person.";

        assertEquals(expected, LikelyDuplicateWarning.appendTo(
                SUCCESS_MESSAGE, changed, List.of(first, second, third, fourth)));
    }
}
