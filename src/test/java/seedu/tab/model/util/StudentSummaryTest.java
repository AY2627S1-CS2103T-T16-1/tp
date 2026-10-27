package seedu.tab.model.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import seedu.tab.model.student.Student;
import seedu.tab.testutil.StudentBuilder;

public class StudentSummaryTest {
    private static final String CAPITAL_SIGMA = "\u03A3";
    private static final String FINAL_SIGMA = "\u03C2";

    private static Student tagged(String name, boolean isFlagged, String... tags) {
        return new StudentBuilder().withName(name).withTags(tags).withFlag(isFlagged).build();
    }

    @Test
    public void countByTag_mixedBook_countsEachTag() {
        List<Student> students = List.of(
                tagged("Alice", false, "CS2103T", "T1"),
                tagged("Bob", true, "CS2103T"),
                tagged("Cara", false, "CS2103T", "T1", "Lab 3"));

        assertEquals(Map.of("CS2103T", 3, "T1", 2, "Lab 3", 1), StudentSummary.countByTag(students));
    }

    @Test
    public void countByTag_ties_areOrderedByCountThenAlphabetically() {
        List<Student> students = List.of(
                tagged("Alice", false, "T3", "T1", "CS2103T"),
                tagged("Bob", false, "T3", "T1", "CS2103T"),
                tagged("Cara", false, "CS2103T"));

        // a set would let equally used tags swap places between runs
        assertEquals(List.of("CS2103T", "T1", "T3"),
                List.copyOf(StudentSummary.countByTag(students).keySet()));
    }

    @Test
    public void countByTag_noStudents_isEmpty() {
        assertTrue(StudentSummary.countByTag(List.of()).isEmpty());
    }

    @Test
    public void countByTag_studentWithoutTags_contributesNothing() {
        List<Student> students = List.of(tagged("Alice", false), tagged("Bob", false, "T1"));

        assertEquals(Map.of("T1", 1), StudentSummary.countByTag(students));
    }

    @Test
    public void countFlagged_countsOnlyTheFlagged() {
        List<Student> students = List.of(
                tagged("Alice", true), tagged("Bob", false), tagged("Cara", true));

        assertEquals(2, StudentSummary.countFlagged(students));
    }

    @Test
    public void countFlagged_noStudents_isZero() {
        assertEquals(0, StudentSummary.countFlagged(List.of()));
    }

    @Test
    public void countByTag_tiedCounts_ordersWithoutRegardToCase() {
        List<Student> students = List.of(
                new StudentBuilder().withName("A").withTags("Zoo").build(),
                new StudentBuilder().withName("B").withTags("apple").build(),
                new StudentBuilder().withName("C").withTags("Mango").build());

        assertEquals(List.of("apple", "Mango", "Zoo"),
                List.copyOf(StudentSummary.countByTag(students).keySet()));
    }

    @Test
    public void countByTag_caseVariantsOfOneTag_countTogether() {
        List<Student> students = List.of(
                tagged("Ann Lee", false, "T1"),
                tagged("Ben Tan", false, "t1"),
                tagged("Cai Wei", false, "T1"));

        // tag and find make T1 and t1 one tag, so the panel should give the size of that group
        assertEquals(Map.of("T1", 3), StudentSummary.countByTag(students));
    }

    @Test
    public void countByTag_variantMostStudentsCarry_namesTheGroup() {
        List<Student> students = List.of(
                tagged("Ann Lee", false, "T1"),
                tagged("Ben Tan", false, "t1"),
                tagged("Cai Wei", false, "t1"));

        // the row reads as most of the group typed the tag, whichever spelling that is
        assertEquals(Map.of("t1", 3), StudentSummary.countByTag(students));
    }

    @Test
    public void countByTag_variantsUsedEqually_listTheGroupUnderOneOfThem() {
        List<Student> students = List.of(
                tagged("Ann Lee", false, "lab 3"),
                tagged("Ben Tan", false, "Lab 3"));

        assertEquals(Map.of("Lab 3", 2), StudentSummary.countByTag(students));
    }

    @Test
    public void countByTag_variantsOfSeveralTags_giveEachGroupOneRow() {
        List<Student> students = List.of(
                tagged("Ann Lee", false, "T1", "Lab 3"),
                tagged("Ben Tan", false, "t1", "lab 3"),
                tagged("Cai Wei", false, "T1"));

        assertEquals(Map.of("T1", 3, "Lab 3", 2), StudentSummary.countByTag(students));
        assertEquals(List.of("T1", "Lab 3"),
                List.copyOf(StudentSummary.countByTag(students).keySet()));
    }

    @Test
    public void countByTag_oneStudentCarryingTwoVariants_countsThatStudentOnce() {
        List<Student> students = List.of(
                tagged("Ann Lee", false, "T1", "t1"),
                tagged("Ben Tan", false, "T1"));

        assertEquals(Map.of("T1", 2), StudentSummary.countByTag(students));
    }

    @Test
    public void countByTag_variantsThatTagRefusesAsDuplicates_countTogether() {
        // tag reads a capital sigma and a final sigma as one tag, as equalsIgnoreCase does.
        // Folding case by lowercasing would split them: the capital lowercases to a medial
        // sigma, while the final sigma lowercases to itself
        assertTrue(CAPITAL_SIGMA.equalsIgnoreCase(FINAL_SIGMA));
        List<Student> students = List.of(
                tagged("Ann Lee", false, CAPITAL_SIGMA),
                tagged("Ben Tan", false, FINAL_SIGMA));

        assertEquals(Map.of(CAPITAL_SIGMA, 2), StudentSummary.countByTag(students));
    }

    @Test
    public void countByTag_oneStudentCarryingTwoSuchVariants_countsThatStudentOnce() {
        List<Student> students = List.of(tagged("Ann Lee", false, CAPITAL_SIGMA, FINAL_SIGMA));

        assertEquals(Map.of(CAPITAL_SIGMA, 1), StudentSummary.countByTag(students));
    }
}
