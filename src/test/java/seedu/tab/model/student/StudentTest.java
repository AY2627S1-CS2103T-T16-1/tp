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

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import seedu.tab.model.tag.Flag;
import seedu.tab.testutil.StudentBuilder;

public class StudentTest {

    @Test
    public void constructor_withoutFlag_defaultsToUnflagged() {
        Student student = new Student(ALICE.getName(), ALICE.getPhone(),
                ALICE.getEmail(), Optional.empty(), ALICE.getTags());
        assertFalse(student.isFlagged());
        assertTrue(student.getFlag().isEmpty());
    }

    @Test
    public void constructor_withFlag_exposesTypedFlag() {
        Student student = new StudentBuilder(ALICE).withFlag(true).build();
        assertEquals(new Flag(), student.getFlag().orElseThrow());
        assertTrue(student.isFlagged());
    }

    @Test
    public void constructor_withoutNusStudentIdAndWithFlag_preservesBothStates() {
        Student student = new Student(ALICE.getName(), ALICE.getPhone(),
                ALICE.getEmail(), Optional.empty(), ALICE.getTags(), true);

        assertTrue(student.getNusStudentId().isEmpty());
        assertTrue(student.isFlagged());
    }

    @Test
    public void constructor_withNusStudentIdWithoutFlag_preservesBothStates() {
        NusStudentId nusStudentId = new NusStudentId("E0123456");
        Student student = new Student(ALICE.getName(), ALICE.getPhone(),
                ALICE.getEmail(), Optional.of(nusStudentId), ALICE.getTags());

        assertEquals(Optional.of(nusStudentId), student.getNusStudentId());
        assertFalse(student.isFlagged());
    }

    @Test
    public void constructor_nullOptionalDetail_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Student(ALICE.getName(), ALICE.getPhone(),
                null, ALICE.getNusStudentId(), ALICE.getTags()));
        assertThrows(NullPointerException.class, () -> new Student(ALICE.getName(), ALICE.getPhone(),
                ALICE.getEmail(), null, ALICE.getTags()));
        assertThrows(NullPointerException.class, () -> new Student(ALICE.getName(), ALICE.getPhone(),
                null, ALICE.getNusStudentId(), ALICE.getTags(), true));
        assertThrows(NullPointerException.class, () -> new Student(ALICE.getName(), ALICE.getPhone(),
                ALICE.getEmail(), null, ALICE.getTags(), true));
    }

    @Test
    public void constructor_emptyOptionalDetails_preservesAbsence() {
        Student student = new Student(ALICE.getName(), ALICE.getPhone(),
                Optional.empty(), Optional.empty(), ALICE.getTags());

        assertTrue(student.getEmail().isEmpty());
        assertTrue(student.getNusStudentId().isEmpty());
        assertTrue(student.getFlag().isEmpty());
        assertEquals(new StudentBuilder(ALICE).withoutEmail().withoutNusStudentId().build(), student);
    }

    @Test
    public void equalsAndHashCode_optionalDetailCombinations_preserveValueSemantics() {
        Student neither = new StudentBuilder(ALICE).withoutEmail().withoutNusStudentId().build();
        Student emailOnly = new StudentBuilder(ALICE).withoutNusStudentId().build();
        Student idOnly = new StudentBuilder(ALICE).withoutEmail().build();
        Student both = new StudentBuilder(ALICE).build();
        Set<Student> students = Set.of(neither, emailOnly, idOnly, both);

        assertEquals(4, students.size());
        for (Student student : students) {
            Student copy = new StudentBuilder(student).build();
            assertEquals(student, copy);
            assertEquals(student.hashCode(), copy.hashCode());
            assertTrue(students.contains(copy));
        }
    }

    @Test
    public void asObservableList_modifyList_throwsUnsupportedOperationException() {
        Student student = new StudentBuilder().build();
        assertThrows(UnsupportedOperationException.class, () -> student.getTags().remove(0));
    }

    @Test
    public void isSameStudent() {
        // same object with an NUS Student ID -> returns true
        assertTrue(ALICE.isSameStudent(ALICE));

        // null -> returns false
        assertFalse(ALICE.isSameStudent(null));

        // same NUS Student ID, all other attributes different -> returns true
        Student editedAlice = new StudentBuilder(ALICE).withName(VALID_NAME_BOB).withPhone(VALID_PHONE_BOB)
                .withEmail(VALID_EMAIL_BOB).withTags(VALID_TAG_HUSBAND).build();
        assertTrue(ALICE.isSameStudent(editedAlice));

        // different NUS Student ID, all other attributes same -> returns false
        editedAlice = new StudentBuilder(ALICE).withNusStudentId("A01234567X").build();
        assertFalse(ALICE.isSameStudent(editedAlice));

        // only one student has an NUS Student ID -> returns false
        assertFalse(ALICE.isSameStudent(BOB));

        // neither student has an NUS Student ID -> returns false
        Student bobCopy = new StudentBuilder(BOB).build();
        assertFalse(BOB.isSameStudent(bobCopy));
        assertFalse(BOB.isSameStudent(BOB));
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

        // a phone against none -> returns false, in either direction
        Student withoutPhone = new StudentBuilder(ALICE).withoutPhone().build();
        assertFalse(ALICE.equals(withoutPhone));
        assertFalse(withoutPhone.equals(ALICE));

        // neither holds a phone -> returns true
        assertTrue(withoutPhone.equals(new StudentBuilder(ALICE).withoutPhone().build()));
    }

    @Test
    public void hashCode_studentsWithoutAPhone_matchWhenTheyAreEqual() {
        Student withoutPhone = new StudentBuilder(ALICE).withoutPhone().build();
        assertEquals(withoutPhone.hashCode(), new StudentBuilder(ALICE).withoutPhone().build().hashCode());
    }

    @Test
    public void isSameStudent_oneHoldsNoPhone_stillTheSameStudent() {
        // the phone is not an identity either, so a roster entry without one is not a second record
        Student withoutPhone = new StudentBuilder(ALICE).withoutPhone().build();
        assertTrue(ALICE.isSameStudent(withoutPhone));
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
    public void isSameStudent_differentNusStudentId_returnsFalse() {
        Student withNusStudentId = new StudentBuilder(ALICE).withNusStudentId("A01234567X").build();
        Student withDifferentNusStudentId = new StudentBuilder(ALICE).withNusStudentId("A76543210Y").build();

        assertFalse(withNusStudentId.isSameStudent(withDifferentNusStudentId));
        assertFalse(ALICE.isSameStudent(withNusStudentId));
    }

    @Test
    public void isSameStudent_bothWithoutNusStudentIdDifferentNames_returnsFalse() {
        Student alice = new StudentBuilder(ALICE).withoutNusStudentId().build();
        Student bob = new StudentBuilder(BOB).withoutNusStudentId().build();

        assertFalse(alice.isSameStudent(bob));
        assertFalse(bob.isSameStudent(alice));
    }

    @Test
    public void isSameStudent_sameNusStudentIdDifferentName_returnsTrue() {
        Student alice = new StudentBuilder(ALICE).withNusStudentId("A01234567X").build();
        Student bob = new StudentBuilder(ALICE).withName(VALID_NAME_BOB).withNusStudentId("A01234567X").build();

        assertTrue(alice.isSameStudent(bob));
    }

    @Test
    public void isSameStudent_nusStudentIdCaseVariant_returnsTrue() {
        Student uppercase = new StudentBuilder(ALICE).withNusStudentId("A01234567X").build();
        Student lowercase = new StudentBuilder(BOB).withNusStudentId("a01234567x").build();

        assertTrue(uppercase.isSameStudent(lowercase));
    }

    @Test
    public void isSameStudent_sameNameWithoutNusStudentIds_returnsFalse() {
        Student first = new StudentBuilder().withName("Tan Wei Ming").withoutNusStudentId().build();
        Student second = new StudentBuilder(first).withoutNusStudentId().build();

        assertFalse(first.isSameStudent(second));
    }

    @Test
    public void toStringMethod() {
        String expected = Student.class.getCanonicalName() + "{name=" + ALICE.getName()
                + ", phone=" + ALICE.getPhone().orElse(null)
                + ", email=" + ALICE.getEmail().orElse(null) + ", nusStudentId=" + ALICE.getNusStudentId().orElse(null)
                + ", tags=" + ALICE.getTags()
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
        assertNotEquals(ALICE.hashCode(), new StudentBuilder(ALICE).withNusStudentId("A01234567X").build().hashCode());
    }

    @Test
    public void equals_differentNusStudentId_returnsFalse() {
        Student first = new StudentBuilder(ALICE).withNusStudentId("A01234567X").build();
        Student second = new StudentBuilder(ALICE).withNusStudentId("A76543210Y").build();
        assertFalse(first.equals(second));
        assertFalse(ALICE.equals(first));
    }
}
