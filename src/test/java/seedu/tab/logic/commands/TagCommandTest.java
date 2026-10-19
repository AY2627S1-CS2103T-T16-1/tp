package seedu.tab.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.testutil.Assert.assertThrows;
import static seedu.tab.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;
import static seedu.tab.testutil.TypicalIndexes.INDEX_SECOND_STUDENT;

import org.junit.jupiter.api.Test;

import seedu.tab.logic.commands.TagCommand.Action;
import seedu.tab.model.tag.Tag;

public class TagCommandTest {

    @Test
    public void constructor_nullArguments_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new TagCommand(null, new Tag("T1")));
        assertThrows(NullPointerException.class, () -> new TagCommand(INDEX_FIRST_STUDENT, null));
        assertThrows(NullPointerException.class, () -> new TagCommand(INDEX_FIRST_STUDENT, new Tag("T1"), null));
    }

    @Test
    public void execute_nullModel_throwsNullPointerException() {
        TagCommand command = new TagCommand(INDEX_FIRST_STUDENT, new Tag("T1"));
        assertThrows(NullPointerException.class, () -> command.execute(null));
    }

    @Test
    public void equals_sameObject_returnsTrue() {
        TagCommand command = new TagCommand(INDEX_FIRST_STUDENT, new Tag("T1"));
        assertTrue(command.equals(command));
    }

    @Test
    public void equals_sameIndexAndTag_returnsTrue() {
        TagCommand command = new TagCommand(INDEX_FIRST_STUDENT, new Tag("T1"));
        assertTrue(command.equals(new TagCommand(INDEX_FIRST_STUDENT, new Tag("T1"))));
    }

    @Test
    public void hashCode_equalCommands_returnsSameHashCode() {
        TagCommand command = new TagCommand(INDEX_FIRST_STUDENT, new Tag("T1"), Action.REMOVE);
        TagCommand equalCommand = new TagCommand(INDEX_FIRST_STUDENT, new Tag("T1"), Action.REMOVE);
        assertEquals(command.hashCode(), equalCommand.hashCode());
    }

    @Test
    public void equals_nullOrDifferentType_returnsFalse() {
        TagCommand command = new TagCommand(INDEX_FIRST_STUDENT, new Tag("T1"));
        assertFalse(command.equals(null));
        assertFalse(command.equals(new DeleteCommand(INDEX_FIRST_STUDENT)));
    }

    @Test
    public void equals_differentIndex_returnsFalse() {
        TagCommand command = new TagCommand(INDEX_FIRST_STUDENT, new Tag("T1"));
        assertFalse(command.equals(new TagCommand(INDEX_SECOND_STUDENT, new Tag("T1"))));
    }

    @Test
    public void equals_differentTag_returnsFalse() {
        TagCommand command = new TagCommand(INDEX_FIRST_STUDENT, new Tag("T1"));
        assertFalse(command.equals(new TagCommand(INDEX_FIRST_STUDENT, new Tag("T2"))));
    }

    @Test
    public void equals_sameIndexAndTagButDifferentAction_returnsFalse() {
        TagCommand removal = new TagCommand(INDEX_FIRST_STUDENT, new Tag("T1"), Action.REMOVE);
        assertFalse(removal.equals(new TagCommand(INDEX_FIRST_STUDENT, new Tag("T1"))));
        assertTrue(removal.equals(new TagCommand(INDEX_FIRST_STUDENT, new Tag("T1"), Action.REMOVE)));
    }

    @Test
    public void toString_containsTargetIndexAndTag() {
        Tag tag = new Tag("Lab 3");
        TagCommand command = new TagCommand(INDEX_FIRST_STUDENT, tag);
        String expected = TagCommand.class.getCanonicalName()
                + "{targetIndex=" + INDEX_FIRST_STUDENT + ", tag=" + tag + ", action=ADD}";
        assertEquals(expected, command.toString());
    }
}
