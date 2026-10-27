package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class CommandHistoryTest {

    @Test
    public void navigate_emptyHistory_keepsTheText() {
        CommandHistory history = new CommandHistory();
        assertEquals("draft", history.recallPrevious("draft"));
        assertEquals("draft", history.recallNext("draft"));
    }

    @Test
    public void navigate_submissionsAndDraft_restoresDraftAtTheEnd() {
        CommandHistory history = new CommandHistory();
        history.add("list");
        history.add("find Tan");
        assertEquals("find Tan", history.recallPrevious("add 陈"));
        assertEquals("list", history.recallPrevious("find Tan"));
        assertEquals("edited oldest", history.recallPrevious("edited oldest"));
        assertEquals("find Tan", history.recallNext("edited oldest"));
        assertEquals("add 陈", history.recallNext("find Tan"));
        assertEquals("new draft", history.recallNext("new draft"));
    }

    @Test
    public void add_consecutiveDuplicatesAndNewSubmissions_resetsNavigation() {
        CommandHistory history = new CommandHistory();
        history.add("list");
        history.add("list");
        history.add("LIST");
        history.add("list");
        assertEquals("list", history.recallPrevious(""));
        assertEquals("LIST", history.recallPrevious("list"));
        assertEquals("list", history.recallPrevious("LIST"));
        assertEquals("oldest", history.recallPrevious("oldest"));
        history.add("find 陈");
        assertEquals("find 陈", history.recallPrevious("new draft"));
        assertEquals("new draft", history.recallNext("find 陈"));
    }

    @Test
    public void recallPrevious_textMatchesNewest_skipsItAndRestoresDraft() {
        CommandHistory history = new CommandHistory();
        history.add("list");
        history.add("addd");
        assertEquals("list", history.recallPrevious("addd"));
        assertEquals("list", history.recallPrevious("list"));
        assertEquals("addd", history.recallNext("list"));
        assertEquals("addd", history.recallNext("addd"));
    }

    @Test
    public void recallPrevious_onlyEntryMatchesText_keepsItAvailable() {
        CommandHistory history = new CommandHistory();
        history.add("addd");
        assertEquals("addd", history.recallPrevious("addd"));
        assertEquals("corrected", history.recallPrevious("corrected"));
        assertEquals("addd", history.recallNext("corrected"));
    }

    @Test
    public void resetNavigation_browsingCommands_startsFromNewestWithNewDraft() {
        CommandHistory history = new CommandHistory();
        history.add("list");
        history.add("clear");
        assertEquals("clear", history.recallPrevious("old draft"));
        assertEquals("list", history.recallPrevious("clear"));
        history.resetNavigation();
        assertEquals("clear", history.recallPrevious("new draft"));
        assertEquals("new draft", history.recallNext("clear"));
    }

    @Test
    public void add_blankInput_skipsEntryAndResetsNavigation() {
        for (String blank : List.of("", "  ", "\t", "\u00a0", "\u2003")) {
            CommandHistory history = new CommandHistory();
            history.add(blank);
            assertEquals("draft", history.recallPrevious("draft"));
            history.add("list");
            history.add("clear");
            assertEquals("clear", history.recallPrevious(""));
            history.add(blank);
            assertEquals("clear", history.recallPrevious("new draft"));
            assertEquals("list", history.recallPrevious("clear"));
            assertEquals("clear", history.recallNext("list"));
            assertEquals("new draft", history.recallNext("clear"));
        }
    }
}
