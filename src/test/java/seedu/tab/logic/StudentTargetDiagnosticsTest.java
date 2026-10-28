package seedu.tab.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static seedu.tab.testutil.Assert.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.tab.logic.commands.exceptions.CommandException;
import seedu.tab.logic.parser.ParserUtil;
import seedu.tab.logic.parser.exceptions.ParseException;
import seedu.tab.model.ModelManager;
import seedu.tab.model.StudentBook;
import seedu.tab.model.student.Student;
import seedu.tab.storage.JsonStudentBookStorage;
import seedu.tab.storage.JsonUserPrefsStorage;
import seedu.tab.storage.StorageManager;
import seedu.tab.testutil.StudentBuilder;

public class StudentTargetDiagnosticsTest {
    @TempDir
    public Path testFolder;

    private ModelManager model;
    private Logic logic;
    private JsonStudentBookStorage storage;
    private Student first;
    private Student second;

    @BeforeEach
    public void setUp() throws Exception {
        first = new StudentBuilder().withNusStudentId("001234").build();
        second = new StudentBuilder().withName("Other Student").withPhone("82223333").build();
        model = new ModelManager();
        model.addStudent(first);
        model.addStudent(second);
        storage = new JsonStudentBookStorage(testFolder.resolve("students.json"));
        storage.saveStudentBook(model.getStudentBook());
        logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json"))));
    }

    @Test
    public void execute_outOfRangeIndex_explainsFilteredAndEmptyListsWithoutMutation() throws Exception {
        for (int count : List.of(2, 1, 0)) {
            model.updateFilteredStudentList(student -> count == 2 || count == 1 && student.equals(first));
            // no student holds "3" as an ID, so the reply offers no ID form to recover with
            String expected = "There is no student at index \"3\" in the displayed list (" + count
                    + (count == 1 ? " student). " : " students). ") + "Use list to show all students.";
            for (String command : List.of("delete 3", "edit 3 -p 81112222", "tag 3 -a T1")) {
                assertRejectedWithoutMutation(command, CommandException.class, expected);
            }
        }
    }

    @Test
    public void execute_oversizedIndex_explainsBoundWithoutMutation() throws Exception {
        String expected = "Index \"6591234567\" exceeds the largest supported index (2147483647). "
                + "Use an index from the displayed list.";
        assertRejectedWithoutMutation("edit 6591234567 -p 81112222", ParseException.class, expected
                + " If \"6591234567\" is an NUS Student ID, use edit --target-id 6591234567.");
        assertRejectedWithoutMutation("tag 6591234567 -a T1", ParseException.class, expected
                + " If \"6591234567\" is an NUS Student ID, use tag --id 6591234567.");
        assertRejectedWithoutMutation("delete 6591234567", ParseException.class, expected
                + " If \"6591234567\" is an NUS Student ID, use delete --id 6591234567.");
    }

    @Test
    public void execute_unusableIndex_explainsReasonWithoutUsageOrMutation() throws Exception {
        for (String command : List.of("edit 1.5 -p 81112222", "tag 1.5 -a T1", "delete 1.5")) {
            assertRejectedWithoutMutation(command, ParseException.class, ParserUtil.MESSAGE_INVALID_INDEX);
        }
        assertRejectedWithoutMutation("edit 0 -p 81112222", ParseException.class,
                ParserUtil.MESSAGE_INVALID_INDEX + " If \"0\" is an NUS Student ID, use edit --target-id 0.");
        assertRejectedWithoutMutation("tag 0 -a T1", ParseException.class,
                ParserUtil.MESSAGE_INVALID_INDEX + " If \"0\" is an NUS Student ID, use tag --id 0.");
        assertRejectedWithoutMutation("delete 0", ParseException.class, ParserUtil.MESSAGE_INVALID_INDEX
                + " If \"0\" is an NUS Student ID, use delete --id 0.");
    }

    @Test
    public void execute_digitLeadingIdTarget_namesTheOptionOfEachCommand() throws Exception {
        assertRejectedWithoutMutation("edit 1ABC -p 81112222", ParseException.class,
                ParserUtil.MESSAGE_INVALID_INDEX + " If \"1ABC\" is an NUS Student ID, use edit --target-id 1ABC.");
        assertRejectedWithoutMutation("tag 1ABC -a T1", ParseException.class,
                ParserUtil.MESSAGE_INVALID_INDEX + " If \"1ABC\" is an NUS Student ID, use tag --id 1ABC.");
        assertRejectedWithoutMutation("delete 1ABC", ParseException.class,
                ParserUtil.MESSAGE_INVALID_INDEX + " If \"1ABC\" is an NUS Student ID, use delete --id 1ABC.");
    }

    @Test
    public void execute_numericIdHiddenByFilter_suggestsExplicitIdWithoutLosingLeadingZeros() throws Exception {
        model.updateFilteredStudentList(student -> false);
        String outOfRange = "There is no student at index \"001234\" in the displayed list (0 students). "
                + "Use list to show all students. ";
        assertRejectedWithoutMutation("edit 001234 -p 81112222", CommandException.class,
                outOfRange + "If \"001234\" is an NUS Student ID, use edit --target-id 001234.");
        assertRejectedWithoutMutation("tag 001234 -a T1", CommandException.class,
                outOfRange + "If \"001234\" is an NUS Student ID, use tag --id 001234.");
        assertRejectedWithoutMutation("delete 001234", CommandException.class,
                outOfRange + "If \"001234\" is an NUS Student ID, use delete --id 001234.");
        logic.execute("edit --target-id 001234 -p 81112222");
        logic.execute("tag --id 001234 -a T1");
        Student reached = new StudentBuilder(first).withPhone("81112222").withTags("T1").build();
        assertEquals(List.of(reached, second), storage.readStudentBook().orElseThrow().getStudentList());
        logic.execute("delete --id 001234");
        assertEquals(List.of(second), storage.readStudentBook().orElseThrow().getStudentList());
        assertEquals(List.of(), model.getFilteredStudentList());
    }

    @Test
    public void execute_numericTargetWithinDisplayedList_keepsIndexMeaning() throws Exception {
        Student firstWithNumericId = new StudentBuilder(first).withNusStudentId("2").build();
        model.setStudent(first, firstWithNumericId);
        storage.saveStudentBook(model.getStudentBook());
        logic.execute("delete 2");
        assertEquals(List.of(firstWithNumericId), storage.readStudentBook().orElseThrow().getStudentList());
    }

    private void assertRejectedWithoutMutation(String command, Class<? extends Throwable> exception,
            String expectedMessage) throws Exception {
        StudentBook before = new StudentBook(model.getStudentBook());
        List<Student> displayed = List.copyOf(model.getFilteredStudentList());
        Predicate<? super Student> filter = model.getFilteredStudentPredicate();
        String savedJson = Files.readString(storage.getStudentBookFilePath());
        assertThrows(exception, expectedMessage, () -> logic.execute(command));
        assertEquals(before, model.getStudentBook());
        assertEquals(displayed, model.getFilteredStudentList());
        assertSame(filter, model.getFilteredStudentPredicate());
        assertEquals(savedJson, Files.readString(storage.getStudentBookFilePath()));
    }
}
