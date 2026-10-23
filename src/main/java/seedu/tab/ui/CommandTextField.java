package seedu.tab.ui;

import javafx.scene.control.TextField;
import seedu.tab.commons.util.StringUtil;

/**
 * A single-line command field that preserves word boundaries in pasted text.
 */
public class CommandTextField extends TextField {

    /**
     * Replaces text, converting pasted cell and line separators to spaces before JavaFX
     * removes control characters. A Windows line ending represents one separator.
     *
     * <p>Every character the parser reads as a separator is converted, so that the box shows
     * the words the parser will see. Leaving one alone shows them joined instead: JavaFX
     * deletes a vertical tab or a form feed along with the other control characters, and
     * draws a line, paragraph or next-line separator with no width at all.
     */
    @Override
    public void replaceText(int start, int end, String text) {
        StringBuilder singleLine = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (!StringUtil.isWhitespace(character)) {
                singleLine.append(character);
            } else if (!isSecondHalfOfWindowsLineEnding(text, i)) {
                singleLine.append(' ');
            }
        }
        super.replaceText(start, end, singleLine.toString());
    }

    /**
     * Returns true if the character of {@code text} at {@code index} closes a Windows line
     * ending, which stands for one separator rather than two.
     */
    private static boolean isSecondHalfOfWindowsLineEnding(String text, int index) {
        return text.charAt(index) == '\n' && index > 0 && text.charAt(index - 1) == '\r';
    }
}
