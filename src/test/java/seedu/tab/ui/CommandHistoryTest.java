package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class CommandHistoryTest {

    @Test
    public void navigate_emptyHistory_keepsTheText() {
        CommandHistory history = new CommandHistory();
        assertEquals("draft", history.previous("draft"));
        assertEquals("draft", history.next("draft"));
    }

    @Test
    public void navigate_submissionsAndDraft_restoresDraftAtTheEnd() {
        CommandHistory history = new CommandHistory();
        history.add("list");
        history.add("find Tan");
        assertEquals("find Tan", history.previous("add 陈"));
        assertEquals("list", history.previous("find Tan"));
        assertEquals("edited oldest", history.previous("edited oldest"));
        assertEquals("find Tan", history.next("edited oldest"));
        assertEquals("add 陈", history.next("find Tan"));
        assertEquals("new draft", history.next("new draft"));
    }

    @Test
    public void add_consecutiveDuplicatesAndNewSubmissions_resetsNavigation() {
        CommandHistory history = new CommandHistory();
        history.add("list");
        history.add("list");
        history.add("LIST");
        history.add("list");
        assertEquals("list", history.previous(""));
        assertEquals("LIST", history.previous("list"));
        assertEquals("list", history.previous("LIST"));
        assertEquals("oldest", history.previous("oldest"));
        history.add("  ");
        assertEquals("  ", history.previous("new draft"));
        assertEquals("new draft", history.next("  "));
    }
}
