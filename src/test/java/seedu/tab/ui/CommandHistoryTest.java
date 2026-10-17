package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

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
        history.add("find 陈");
        assertEquals("find 陈", history.previous("new draft"));
        assertEquals("new draft", history.next("find 陈"));
    }

    @Test
    public void previous_textMatchesNewest_skipsItAndRestoresDraft() {
        CommandHistory history = new CommandHistory();
        history.add("list");
        history.add("addd");
        assertEquals("list", history.previous("addd"));
        assertEquals("list", history.previous("list"));
        assertEquals("addd", history.next("list"));
        assertEquals("addd", history.next("addd"));
    }

    @Test
    public void previous_onlyEntryMatchesText_keepsItAvailable() {
        CommandHistory history = new CommandHistory();
        history.add("addd");
        assertEquals("addd", history.previous("addd"));
        assertEquals("corrected", history.previous("corrected"));
        assertEquals("addd", history.next("corrected"));
    }

    @Test
    public void add_blankInput_skipsEntryAndResetsNavigation() {
        for (String blank : List.of("", "  ", "\t", "\u00a0", "\u2003")) {
            CommandHistory history = new CommandHistory();
            history.add(blank);
            assertEquals("draft", history.previous("draft"));
            history.add("list");
            history.add("clear");
            assertEquals("clear", history.previous(""));
            history.add(blank);
            assertEquals("clear", history.previous("new draft"));
            assertEquals("list", history.previous("clear"));
            assertEquals("clear", history.next("list"));
            assertEquals("new draft", history.next("clear"));
        }
    }
}
