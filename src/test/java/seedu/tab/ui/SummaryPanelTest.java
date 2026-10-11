package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import seedu.tab.model.student.Student;
import seedu.tab.testutil.StudentBuilder;

/**
 * Tests the counting the summary panel shows. The arithmetic is kept apart from the nodes, so
 * it can be checked without a toolkit.
 */
public class SummaryPanelTest {

    private static Student tagged(String name, boolean isFlagged, String... tags) {
        return new StudentBuilder().withName(name).withTags(tags).withFlag(isFlagged).build();
    }

    @Test
    public void countByTag_mixedBook_countsEachTag() {
        List<Student> students = List.of(
                tagged("Alice", false, "CS2103T", "T1"),
                tagged("Bob", true, "CS2103T"),
                tagged("Cara", false, "CS2103T", "T1", "Lab 3"));

        assertEquals(Map.of("CS2103T", 3, "T1", 2, "Lab 3", 1), SummaryPanel.countByTag(students));
    }

    @Test
    public void countByTag_ties_areOrderedByCountThenAlphabetically() {
        List<Student> students = List.of(
                tagged("Alice", false, "T3", "T1", "CS2103T"),
                tagged("Bob", false, "T3", "T1", "CS2103T"),
                tagged("Cara", false, "CS2103T"));

        // a set would let equally used tags swap places between runs
        assertEquals(List.of("CS2103T", "T1", "T3"),
                List.copyOf(SummaryPanel.countByTag(students).keySet()));
    }

    @Test
    public void countByTag_noStudents_isEmpty() {
        assertTrue(SummaryPanel.countByTag(List.of()).isEmpty());
    }

    @Test
    public void countByTag_studentWithoutTags_contributesNothing() {
        List<Student> students = List.of(tagged("Alice", false), tagged("Bob", false, "T1"));

        assertEquals(Map.of("T1", 1), SummaryPanel.countByTag(students));
    }

    @Test
    public void countFlagged_countsOnlyTheFlagged() {
        List<Student> students = List.of(
                tagged("Alice", true), tagged("Bob", false), tagged("Cara", true));

        assertEquals(2, SummaryPanel.countFlagged(students));
    }

    @Test
    public void countFlagged_noStudents_isZero() {
        assertEquals(0, SummaryPanel.countFlagged(List.of()));
    }

    @Test
    public void describeTotal_oneStudent_usesTheSingular() {
        assertEquals("1 student", SummaryPanel.describeTotal(1));
    }

    @Test
    public void describeTotal_anyOtherCount_usesThePlural() {
        assertEquals("0 students", SummaryPanel.describeTotal(0));
        assertEquals("48 students", SummaryPanel.describeTotal(48));
    }
}
