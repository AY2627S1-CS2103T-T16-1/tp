package seedu.tab.ui;

import java.util.ArrayList;
import java.util.List;

import seedu.tab.commons.util.StringUtil;

/** Keeps submitted commands and an unsent draft for one command-box session. */
final class CommandHistory {

    private final List<String> commands = new ArrayList<>();
    private int position;
    private String draft = "";

    /** Records a submission, skipping blank input and consecutive identical commands. */
    void add(String command) {
        boolean isBlank = command.chars().allMatch(character -> StringUtil.isWhitespace((char) character));
        if (!isBlank && (commands.isEmpty() || !commands.getLast().equals(command))) {
            commands.add(command);
        }
        resetNavigation();
    }

    /** Starts navigation at the newest command without recording a confirmation reply. */
    void resetNavigation() {
        position = commands.size();
        draft = "";
    }

    /** Returns the previous command, saving the draft when navigation begins. */
    String previous(String text) {
        if (position == 0) {
            return text;
        }
        if (position == commands.size()) {
            draft = text;
            if (position > 1 && text.equals(commands.getLast())) {
                position--;
            }
        }
        return commands.get(--position);
    }

    /** Returns the next command, or restores the draft after the newest command. */
    String next(String text) {
        if (position == commands.size()) {
            return text;
        }
        position++;
        return position == commands.size() ? draft : commands.get(position);
    }
}
