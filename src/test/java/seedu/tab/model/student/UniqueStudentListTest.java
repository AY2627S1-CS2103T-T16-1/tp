package seedu.tab.model.student;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
import static seedu.tab.testutil.Assert.assertThrows;
import static seedu.tab.testutil.TypicalStudents.ALICE;
import static seedu.tab.testutil.TypicalStudents.BOB;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.tab.model.student.exceptions.DuplicateStudentException;
import seedu.tab.model.student.exceptions.StudentNotFoundException;
import seedu.tab.testutil.StudentBuilder;

public class UniqueStudentListTest {

    private final UniqueStudentList uniqueStudentList = new UniqueStudentList();

    @Test
    public void contains_nullStudent_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> uniqueStudentList.contains(null));
    }

    @Test
    public void contains_studentNotInList_returnsFalse() {
        assertFalse(uniqueStudentList.contains(ALICE));
    }

    @Test
    public void contains_studentInList_returnsTrue() {
        uniqueStudentList.add(ALICE);
        assertTrue(uniqueStudentList.contains(ALICE));
    }

    @Test
    public void contains_studentWithSameIdentityFieldsInList_returnsTrue() {
        uniqueStudentList.add(ALICE);
        Student editedAlice = new StudentBuilder(ALICE).withTags(VALID_TAG_HUSBAND)
                .build();
        assertTrue(uniqueStudentList.contains(editedAlice));
    }

    @Test
    public void add_nullStudent_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> uniqueStudentList.add(null));
    }

    @Test
    public void add_duplicateStudent_throwsDuplicateStudentException() {
        uniqueStudentList.add(ALICE);
        assertThrows(DuplicateStudentException.class, () -> uniqueStudentList.add(ALICE));
    }

    @Test
    public void add_sameNameDifferentNusStudentIds_addsBothStudents() {
        Student first = new StudentBuilder().withName("Tan Wei Ming").withNusStudentId("A01234567X").build();
        Student second = new StudentBuilder(first).withNusStudentId("A76543210Y").build();

        uniqueStudentList.add(first);
        uniqueStudentList.add(second);

        assertEquals(List.of(first, second), uniqueStudentList.asUnmodifiableObservableList());
    }

    @Test
    public void add_sameNameWithoutNusStudentIds_addsBothStudents() {
        Student first = new StudentBuilder().withName("Tan Wei Ming").withoutNusStudentId().build();
        Student second = new StudentBuilder(first).withoutNusStudentId().build();

        uniqueStudentList.add(first);
        uniqueStudentList.add(second);

        assertEquals(List.of(first, second), uniqueStudentList.asUnmodifiableObservableList());
    }

    @Test
    public void add_sameNusStudentIdDifferentNames_throwsDuplicateStudentException() {
        Student first = new StudentBuilder().withName("Tan Wei Ming").withNusStudentId("A01234567X").build();
        Student second = new StudentBuilder(first).withName("Lee Wei Ming").build();
        uniqueStudentList.add(first);

        assertThrows(DuplicateStudentException.class, () -> uniqueStudentList.add(second));
    }

    @Test
    public void setStudent_nullTargetStudent_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> uniqueStudentList.setStudent(null, ALICE));
    }

    @Test
    public void setStudent_nullEditedStudent_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> uniqueStudentList.setStudent(ALICE, null));
    }

    @Test
    public void setStudent_targetStudentNotInList_throwsStudentNotFoundException() {
        assertThrows(StudentNotFoundException.class, () -> uniqueStudentList.setStudent(ALICE, ALICE));
    }

    @Test
    public void setStudent_editedStudentIsSameStudent_success() {
        uniqueStudentList.add(ALICE);
        uniqueStudentList.setStudent(ALICE, ALICE);
        UniqueStudentList expectedUniqueStudentList = new UniqueStudentList();
        expectedUniqueStudentList.add(ALICE);
        assertEquals(expectedUniqueStudentList, uniqueStudentList);
    }

    @Test
    public void setStudent_editedStudentHasSameIdentity_success() {
        uniqueStudentList.add(ALICE);
        Student editedAlice = new StudentBuilder(ALICE).withTags(VALID_TAG_HUSBAND)
                .build();
        uniqueStudentList.setStudent(ALICE, editedAlice);
        UniqueStudentList expectedUniqueStudentList = new UniqueStudentList();
        expectedUniqueStudentList.add(editedAlice);
        assertEquals(expectedUniqueStudentList, uniqueStudentList);
    }

    @Test
    public void setStudent_editedStudentHasDifferentIdentity_success() {
        uniqueStudentList.add(ALICE);
        uniqueStudentList.setStudent(ALICE, BOB);
        UniqueStudentList expectedUniqueStudentList = new UniqueStudentList();
        expectedUniqueStudentList.add(BOB);
        assertEquals(expectedUniqueStudentList, uniqueStudentList);
    }

    @Test
    public void setStudent_editedStudentHasNonUniqueIdentity_throwsDuplicateStudentException() {
        uniqueStudentList.add(ALICE);
        uniqueStudentList.add(BOB);
        Student editedBob = new StudentBuilder(BOB)
                .withNusStudentId(ALICE.getNusStudentId().orElseThrow().value).build();

        assertThrows(DuplicateStudentException.class, () -> uniqueStudentList.setStudent(BOB, editedBob));
    }

    @Test
    public void setStudent_equalStudentsWithoutNusStudentIds_replacesTargetReference() {
        Student first = new StudentBuilder().withoutNusStudentId().build();
        Student second = new StudentBuilder(first).build();
        Student editedSecond = new StudentBuilder(second).withPhone("91234567").build();
        uniqueStudentList.add(first);
        uniqueStudentList.add(second);

        uniqueStudentList.setStudent(second, editedSecond);

        assertSame(first, uniqueStudentList.asUnmodifiableObservableList().get(0));
        assertSame(editedSecond, uniqueStudentList.asUnmodifiableObservableList().get(1));
    }

    @Test
    public void remove_nullStudent_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> uniqueStudentList.remove(null));
    }

    @Test
    public void remove_studentDoesNotExist_throwsStudentNotFoundException() {
        assertThrows(StudentNotFoundException.class, () -> uniqueStudentList.remove(ALICE));
    }

    @Test
    public void remove_existingStudent_removesStudent() {
        uniqueStudentList.add(ALICE);
        uniqueStudentList.remove(ALICE);
        UniqueStudentList expectedUniqueStudentList = new UniqueStudentList();
        assertEquals(expectedUniqueStudentList, uniqueStudentList);
    }

    @Test
    public void remove_equalStudentsWithoutNusStudentIds_removesTargetReference() {
        Student first = new StudentBuilder().withoutNusStudentId().build();
        Student middle = new StudentBuilder().withName("Alice Tan").withoutNusStudentId().build();
        Student second = new StudentBuilder(first).build();
        uniqueStudentList.add(first);
        uniqueStudentList.add(middle);
        uniqueStudentList.add(second);

        uniqueStudentList.remove(second);

        assertEquals(2, uniqueStudentList.asUnmodifiableObservableList().size());
        assertSame(first, uniqueStudentList.asUnmodifiableObservableList().get(0));
        assertSame(middle, uniqueStudentList.asUnmodifiableObservableList().get(1));
    }

    @Test
    public void setStudents_nullUniqueStudentList_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> uniqueStudentList.setStudents((UniqueStudentList) null));
    }

    @Test
    public void setStudents_uniqueStudentList_replacesOwnListWithProvidedUniqueStudentList() {
        uniqueStudentList.add(ALICE);
        UniqueStudentList expectedUniqueStudentList = new UniqueStudentList();
        expectedUniqueStudentList.add(BOB);
        uniqueStudentList.setStudents(expectedUniqueStudentList);
        assertEquals(expectedUniqueStudentList, uniqueStudentList);
    }

    @Test
    public void setStudents_nullList_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> uniqueStudentList.setStudents((List<Student>) null));
    }

    @Test
    public void setStudents_list_replacesOwnListWithProvidedList() {
        uniqueStudentList.add(ALICE);
        List<Student> studentList = List.of(BOB);
        uniqueStudentList.setStudents(studentList);
        UniqueStudentList expectedUniqueStudentList = new UniqueStudentList();
        expectedUniqueStudentList.add(BOB);
        assertEquals(expectedUniqueStudentList, uniqueStudentList);
    }

    @Test
    public void setStudents_listWithDuplicateStudents_throwsDuplicateStudentException() {
        List<Student> listWithDuplicateStudents = List.of(ALICE, ALICE);
        assertThrows(DuplicateStudentException.class, () -> uniqueStudentList.setStudents(listWithDuplicateStudents));
    }

    @Test
    public void asUnmodifiableObservableList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, ()
            -> uniqueStudentList.asUnmodifiableObservableList().remove(0));
    }

    @Test
    public void toStringMethod() {
        assertEquals("[]", uniqueStudentList.toString());
        uniqueStudentList.add(ALICE);
        assertEquals("[" + ALICE + "]", uniqueStudentList.toString());
    }
}
