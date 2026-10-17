package seedu.tab.ui;

import javafx.scene.control.TextField;

/**
 * A single-line command field that preserves word boundaries in pasted text.
 */
public class CommandTextField extends TextField {

    /**
     * Replaces text, converting pasted cell and line separators to spaces before JavaFX
     * removes control characters. A Windows line ending represents one separator.
     */
    @Override
    public void replaceText(int start, int end, String text) {
        String singleLine = text.replace("\r\n", " ").replace('\r', ' ')
                .replace('\n', ' ').replace('\t', ' ');
        super.replaceText(start, end, singleLine);
    }
}
