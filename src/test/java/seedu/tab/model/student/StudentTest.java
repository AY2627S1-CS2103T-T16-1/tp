package seedu.tab.model.student;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_EMAIL_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_NAME_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_PHONE_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
import static seedu.tab.testutil.Assert.assertThrows;
import static seedu.tab.testutil.TypicalStudents.ALICE;
import static seedu.tab.testutil.TypicalStudents.BOB;

import org.junit.jupiter.api.Test;

import seedu.tab.testutil.StudentBuilder;

public class StudentTest {

    @Test
    public void constructor_withoutFlag_defaultsToUnflagged() {
        Student student = new Student(ALICE.getName(), ALICE.getPhone(),
                ALICE.getEmail().orElse(null), ALICE.getTags());
        assertFalse(student.isFlagged());
    }

    @Test
    public void asObservableList_modifyList_throwsUnsupportedOperationException() {
        Student student = new StudentBuilder().build();
        assertThrows(UnsupportedOperationException.class, () -> student.getTags().remove(0));
    }

    @Test
    public void isSameStudent() {
        // same object -> returns true
        assertTrue(ALICE.isSameStudent(ALICE));

        // null -> returns false
        assertFalse(ALICE.isSameStudent(null));

        // same name, all other attributes different -> returns true
        Student editedAlice = new StudentBuilder(ALICE).withPhone(VALID_PHONE_BOB).withEmail(VALID_EMAIL_BOB)
                .withTags(VALID_TAG_HUSBAND).build();
        assertTrue(ALICE.isSameStudent(editedAlice));

        // different name, all other attributes same -> returns false
        editedAlice = new StudentBuilder(ALICE).withName(VALID_NAME_BOB).build();
        assertFalse(ALICE.isSameStudent(editedAlice));

        // name differs in case, all other attributes same -> returns false
        Student editedBob = new StudentBuilder(BOB).withName(VALID_NAME_BOB.toLowerCase()).build();
        assertFalse(BOB.isSameStudent(editedBob));

        // name differs only by surrounding whitespace -> returns true, because Name stores a
        // normalized form and an accidental extra space does not make a different student
        String nameWithTrailingSpaces = VALID_NAME_BOB + " ";
        editedBob = new StudentBuilder(BOB).withName(nameWithTrailingSpaces).build();
        assertTrue(BOB.isSameStudent(editedBob));
    }

    @Test
    public void equals() {
        // same values -> returns true
        Student aliceCopy = new StudentBuilder(ALICE).build();
        assertTrue(ALICE.equals(aliceCopy));

        // same object -> returns true
        assertTrue(ALICE.equals(ALICE));

        // null -> returns false
        assertFalse(ALICE.equals(null));

        // different type -> returns false
        assertFalse(ALICE.equals(5));

        // different student -> returns false
        assertFalse(ALICE.equals(BOB));

        // different name -> returns false
        Student editedAlice = new StudentBuilder(ALICE).withName(VALID_NAME_BOB).build();
        assertFalse(ALICE.equals(editedAlice));

        // different phone -> returns false
        editedAlice = new StudentBuilder(ALICE).withPhone(VALID_PHONE_BOB).build();
        assertFalse(ALICE.equals(editedAlice));

        // different email -> returns false
        editedAlice = new StudentBuilder(ALICE).withEmail(VALID_EMAIL_BOB).build();
        assertFalse(ALICE.equals(editedAlice));

        // different tags -> returns false
        editedAlice = new StudentBuilder(ALICE).withTags(VALID_TAG_HUSBAND).build();
        assertFalse(ALICE.equals(editedAlice));

        // different follow-up flag -> returns false
        editedAlice = new StudentBuilder(ALICE).withFlag(!ALICE.isFlagged()).build();
        assertFalse(ALICE.equals(editedAlice));

        // an email against none -> returns false, in either direction
        Student withoutEmail = new StudentBuilder(ALICE).withoutEmail().build();
        assertFalse(ALICE.equals(withoutEmail));
        assertFalse(withoutEmail.equals(ALICE));

        // neither holds an email -> returns true
        assertTrue(withoutEmail.equals(new StudentBuilder(ALICE).withoutEmail().build()));
    }

    @Test
    public void hashCode_studentsWithoutAnEmail_matchWhenTheyAreEqual() {
        Student withoutEmail = new StudentBuilder(ALICE).withoutEmail().build();
        assertEquals(withoutEmail.hashCode(), new StudentBuilder(ALICE).withoutEmail().build().hashCode());
    }

    @Test
    public void isSameStudent_oneHoldsNoEmail_stillTheSameStudent() {
        // the email is not an identity, so leaving it out does not make a second record of one student
        Student withoutEmail = new StudentBuilder(ALICE).withoutEmail().build();
        assertTrue(ALICE.isSameStudent(withoutEmail));
    }

    @Test
    public void isSameStudent_differentFollowUpFlag_stillTheSameStudent() {
        Student flaggedAlice = new StudentBuilder(ALICE).withFlag(!ALICE.isFlagged()).build();
        assertTrue(ALICE.isSameStudent(flaggedAlice));
    }

    @Test
    public void toStringMethod() {
        String expected = Student.class.getCanonicalName() + "{name=" + ALICE.getName() + ", phone=" + ALICE.getPhone()
                + ", email=" + ALICE.getEmail().orElse(null) + ", tags=" + ALICE.getTags()
                + ", isFlagged=" + ALICE.isFlagged() + "}";
        assertEquals(expected, ALICE.toString());
    }

    @Test
    public void hashCode_equalStudents_returnsSameHashCode() {
        // the contract that matters: two students that compare equal must hash alike, or one
        // of them could go missing from a hashed collection
        assertEquals(ALICE.hashCode(), new StudentBuilder(ALICE).build().hashCode());
    }

    @Test
    public void hashCode_studentsDifferingInOneField_returnDifferentHashCodes() {
        // not required by the contract, which permits collisions, but every field should
        // reach the hash or students would cluster needlessly
        assertNotEquals(ALICE.hashCode(), new StudentBuilder(ALICE).withName(VALID_NAME_BOB)
                .build().hashCode());
        assertNotEquals(ALICE.hashCode(), new StudentBuilder(ALICE).withPhone(VALID_PHONE_BOB)
                .build().hashCode());
        assertNotEquals(ALICE.hashCode(), new StudentBuilder(ALICE).withEmail(VALID_EMAIL_BOB)
                .build().hashCode());
        assertNotEquals(ALICE.hashCode(), new StudentBuilder(ALICE).withTags(VALID_TAG_HUSBAND)
                .build().hashCode());
        assertNotEquals(ALICE.hashCode(), new StudentBuilder(ALICE).withFlag(!ALICE.isFlagged())
                .build().hashCode());
    }
}
