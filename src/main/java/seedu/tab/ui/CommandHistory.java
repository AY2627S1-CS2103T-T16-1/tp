package seedu.tab.ui;

import java.util.ArrayList;
import java.util.List;

/** Keeps submitted commands and an unsent draft for one command-box session. */
final class CommandHistory {

    private final List<String> commands = new ArrayList<>();
    private int position;
    private String draft = "";

    /** Records a submission, skipping only a consecutive identical command. */
    void add(String command) {
        if (commands.isEmpty() || !commands.getLast().equals(command)) {
            commands.add(command);
        }
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
