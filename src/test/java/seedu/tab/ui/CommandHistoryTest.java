package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
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
        // At the oldest-entry boundary: another Previous preserves the edited text.
        assertEquals("edited oldest", history.recallPrevious("edited oldest"));
        assertEquals("find Tan", history.recallNext("edited oldest"));
        assertEquals("add 陈", history.recallNext("find Tan"));
        // Past the newest-entry boundary: Next preserves the current draft.
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
    public void add_entriesUpToCapacity_keepsEveryCommand() {
        assertEquals(oldestToNewest(1, 99), recallAll(historyOf(99)));
        assertEquals(oldestToNewest(1, 100), recallAll(historyOf(100)));
    }

    @Test
    public void add_pastCapacity_dropsOneOldestCommandPerRecording() {
        assertEquals(oldestToNewest(2, 101), recallAll(historyOf(101)));
        assertEquals(oldestToNewest(51, 150), recallAll(historyOf(150)));
    }

    @Test
    public void add_duplicateAtCapacity_dropsNothing() {
        CommandHistory history = historyOf(100);
        history.add("c100");
        history.add("   ");
        assertEquals(oldestToNewest(1, 100), recallAll(history));
    }

    @Test
    public void navigate_afterEviction_stopsAtTheOldestKeptCommandAndRestoresTheDraft() {
        CommandHistory history = historyOf(101);
        String text = "draft";
        for (int number = 101; number >= 2; number--) {
            text = history.recallPrevious(text);
            assertEquals("c" + number, text);
        }
        // The dropped c1 is not reachable, so the oldest kept command is the boundary.
        assertEquals("edited oldest", history.recallPrevious("edited oldest"));
        assertEquals("c3", history.recallNext("edited oldest"));
        for (int number = 4; number <= 101; number++) {
            assertEquals("c" + number, history.recallNext("c" + (number - 1)));
        }
        assertEquals("draft", history.recallNext("c101"));
    }

    /** Returns a history given {@code count} commands, numbered {@code c1} upwards in order. */
    private static CommandHistory historyOf(int count) {
        CommandHistory history = new CommandHistory();
        for (int number = 1; number <= count; number++) {
            history.add("c" + number);
        }
        return history;
    }

    /** Returns the commands numbered {@code first} to {@code last}, oldest first. */
    private static List<String> oldestToNewest(int first, int last) {
        List<String> commands = new ArrayList<>();
        for (int number = first; number <= last; number++) {
            commands.add("c" + number);
        }
        return commands;
    }

    /**
     * Returns every command Up reaches, oldest first, stopping where Up stops changing the text.
     *
     * <p>That boundary is the oldest kept command only because no two neighboring entries are
     * equal, which {@link CommandHistory#add} ensures by skipping a command repeated straight
     * after itself. A history filled another way would stop the walk early instead.
     */
    private static List<String> recallAll(CommandHistory history) {
        List<String> recalled = new ArrayList<>();
        String text = "draft";
        for (String previous = history.recallPrevious(text); !previous.equals(text);
                previous = history.recallPrevious(text)) {
            recalled.add(previous);
            text = previous;
        }
        return recalled.reversed();
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
