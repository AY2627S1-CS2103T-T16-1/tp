package seedu.tab.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.testutil.TypicalStudents.getTypicalStudentBook;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.tab.commons.core.LogsCenter;
import seedu.tab.commons.core.index.Index;
import seedu.tab.logic.commands.exceptions.CommandException;
import seedu.tab.model.Model;
import seedu.tab.model.ModelManager;
import seedu.tab.model.UserPrefs;
import seedu.tab.model.student.DetailsContainsKeywordsPredicate;
import seedu.tab.model.student.DetailsContainsKeywordsPredicate.Field;
import seedu.tab.model.student.NusStudentId;
import seedu.tab.model.student.Student;
import seedu.tab.testutil.EditStudentDescriptorBuilder;
import seedu.tab.testutil.StudentBuilder;

public class CommandLoggingTest {

    private static final Index FIRST = Index.fromOneBased(1);

    private final List<LogRecord> written = new ArrayList<>();
    private Model model;
    private Logger allLoggers;
    private Level wasAt;
    private Handler listener;

    @BeforeEach
    public void listenToTheLog() {
        model = new ModelManager(getTypicalStudentBook(), new UserPrefs());
        // every logger LogsCenter hands out is a child of one parent and takes its level,
        // so listening there hears all of them, and FINE is needed to hear find at all
        allLoggers = LogsCenter.getLogger(CommandLoggingTest.class).getParent();
        wasAt = allLoggers.getLevel();
        allLoggers.setLevel(Level.FINE);
        listener = new Handler() {
            @Override
            public void publish(LogRecord record) {
                written.add(record);
            }

            @Override
            public void flush() {
                // nothing is buffered
            }

            @Override
            public void close() {
                // nothing is held open
            }
        };
        listener.setLevel(Level.ALL);
        allLoggers.addHandler(listener);
    }

    @AfterEach
    public void leaveTheLogAsItWas() {
        allLoggers.removeHandler(listener);
        allLoggers.setLevel(wasAt);
    }

    private List<String> messagesAt(Level level) {
        return written.stream()
                .filter(record -> record.getLevel().equals(level))
                .map(LogRecord::getMessage)
                .toList();
    }

    @Test
    public void execute_add_recordsTheBookSizeAtInfo() throws Exception {
        new AddCommand(new StudentBuilder().withName("Zoe Quartermain").build()).execute(model);

        assertTrue(messagesAt(Level.INFO).contains("Added a student; the book now holds 8"),
                "logged: " + messagesAt(Level.INFO));
    }

    @Test
    public void execute_delete_recordsTheIndexAndBookSizeAtInfo() throws Exception {
        new DeleteCommand(FIRST).execute(model);

        assertTrue(messagesAt(Level.INFO)
                        .contains("Deleted the student shown at 1; the book now holds 6"),
                "logged: " + messagesAt(Level.INFO));
    }

    @Test
    public void execute_deleteByNusStudentId_recordsTheReferralAndBookSizeAtInfo() throws Exception {
        new DeleteCommand(StudentTarget.byNusStudentId(new NusStudentId("A12345678C"))).execute(model);

        // the log names how the student was addressed without repeating their NUS Student ID
        assertTrue(messagesAt(Level.INFO)
                        .contains("Deleted the student identified by NUS Student ID; the book now holds 6"),
                "logged: " + messagesAt(Level.INFO));
    }

    @Test
    public void execute_edit_recordsTheIndexAndBookSizeAtInfo() throws Exception {
        new EditCommand(FIRST, new EditStudentDescriptorBuilder().withPhone("91234567").build())
                .execute(model);

        assertTrue(messagesAt(Level.INFO)
                        .contains("Edited the student shown at 1; the book now holds 7"),
                "logged: " + messagesAt(Level.INFO));
    }

    @Test
    public void executeConfirmed_clear_recordsHowManyWentAtInfo() {
        new ClearCommand().executeConfirmed(model);

        assertTrue(messagesAt(Level.INFO).contains("Cleared 7 students"),
                "logged: " + messagesAt(Level.INFO));
    }

    @Test
    public void execute_find_recordsTheMatchesAtFine() {
        new FindCommand(new DetailsContainsKeywordsPredicate(Map.of(Field.NAME, List.of("Alice")))).execute(model);

        // find is the one entry below the level the product runs at, so nothing but a
        // listener turned down to FINE says whether it is written at all
        assertTrue(messagesAt(Level.FINE).contains("Find matched 1 of 7"),
                "logged at FINE: " + messagesAt(Level.FINE));
    }

    @Test
    public void execute_clearAwaitingConfirmation_recordsNothingAboutClearing() {
        new ClearCommand().execute(model);

        assertFalse(writtenAnywhere().stream().anyMatch(message -> message.contains("Cleared")),
                "a clear nobody confirmed was logged as done: " + writtenAnywhere());
        assertEquals(7, model.getStudentBook().getStudentList().size());
    }

    @Test
    public void execute_deleteRefused_recordsNothingAboutDeleting() {
        Index pastTheEnd = Index.fromOneBased(model.getFilteredStudentList().size() + 1);

        assertThrows(CommandException.class, () -> new DeleteCommand(pastTheEnd).execute(model));

        assertFalse(writtenAnywhere().stream().anyMatch(message -> message.contains("Deleted")),
                "a delete that was refused was logged as done: " + writtenAnywhere());
    }

    @Test
    public void execute_everyCommand_writesNoFieldOfAStudent() throws Exception {
        Student newcomer = new StudentBuilder().withName("Zoe Quartermain").withPhone("87654321")
                .withEmail("zoe@example.com").withNusStudentId("A09876543Z").build();
        String replacementPhone = "91234567";
        Set<String> details = new HashSet<>();
        model.getStudentBook().getStudentList().forEach(student -> details.addAll(fieldsOf(student)));
        details.addAll(fieldsOf(newcomer));
        details.add(replacementPhone);

        new AddCommand(newcomer).execute(model);
        new FindCommand(new DetailsContainsKeywordsPredicate(Map.of(Field.NAME, List.of("Alice")))).execute(model);
        new EditCommand(FIRST, new EditStudentDescriptorBuilder().withPhone(replacementPhone).build())
                .execute(model);
        new DeleteCommand(FIRST).execute(model);
        // addressing a student by their NUS Student ID must not put the ID in the log either
        new DeleteCommand(StudentTarget.byNusStudentId(new NusStudentId("A09876543Z"))).execute(model);
        new ClearCommand().executeConfirmed(model);

        // the command text LogicManager writes already holds whatever was typed; these
        // entries are not the place to put a second copy of someone's details
        List<String> leaked = writtenAnywhere().stream()
                .filter(message -> details.stream().anyMatch(message::contains))
                .toList();
        assertTrue(leaked.isEmpty(), "a student's own details reached the log: " + leaked);
    }

    /** Everything about {@code student} that belongs to the student rather than to the book. */
    private static Set<String> fieldsOf(Student student) {
        Set<String> fields = new HashSet<>();
        fields.add(student.getName().fullName);
        fields.add(student.getPhone().value);
        student.getEmail().ifPresent(email -> fields.add(email.value));
        student.getNusStudentId().ifPresent(id -> fields.add(id.value));
        student.getTags().forEach(tag -> fields.add(tag.tagName));
        return fields;
    }

    private List<String> writtenAnywhere() {
        return written.stream().map(LogRecord::getMessage).toList();
    }
}
