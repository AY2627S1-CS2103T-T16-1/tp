package seedu.tab.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javafx.scene.text.Text;
import seedu.tab.logic.parser.CommandWords;

/**
 * Colours the command reference the help pager shows.
 *
 * <p>The reference is one block of text holding section headings, command names, syntax lines
 * and prose. Drawn in a single fill it reads as a wall, and the same syntax the command box
 * colours as the User types it goes flat the moment they look it up. The classes handed out
 * here are the command box's own, so a marker is the same colour in both places.
 */
final class HelpText {

    /**
     * A value the User fills in, such as NAME or NUS_STUDENT_ID. Three characters at the least,
     * so that "an ID instead of an index" reads as the sentence it is rather than as a field.
     */
    private static final Pattern PLACEHOLDER = Pattern.compile("[A-Z][A-Z0-9_]{2,}");

    private static final String OPENERS = "[(";
    private static final String CLOSERS = ")]...,|";

    private HelpText() {} // this class only builds the spans

    /**
     * Returns {@code reference} as coloured spans, one run of text per span, with the line
     * breaks kept so that the pager still measures the block by its lines.
     */
    static List<Text> colour(String reference) {
        List<Text> spans = new ArrayList<>();
        String[] lines = reference.split("\n", -1);
        for (int index = 0; index < lines.length; index++) {
            addLine(spans, lines[index]);
            if (index < lines.length - 1) {
                spans.add(span("\n", "help-plain"));
            }
        }
        return spans;
    }

    private static void addLine(List<Text> spans, String line) {
        if (line.isBlank()) {
            spans.add(span(line, "help-plain"));
            return;
        }
        if (!Character.isWhitespace(line.charAt(0))) {
            // a line against the margin is a heading; everything the reference says sits under one
            spans.add(span(line, "help-heading"));
            return;
        }
        addWords(spans, line);
    }

    private static void addWords(List<Text> spans, String line) {
        int index = 0;
        boolean isFirstWord = true;
        while (index < line.length()) {
            int wordStart = index;
            while (index < line.length() && !Character.isWhitespace(line.charAt(index))) {
                index++;
            }
            if (index > wordStart) {
                addWord(spans, line.substring(wordStart, index), isFirstWord);
                isFirstWord = false;
            }
            int spaceStart = index;
            while (index < line.length() && Character.isWhitespace(line.charAt(index))) {
                index++;
            }
            if (index > spaceStart) {
                spans.add(span(line.substring(spaceStart, index), "help-plain"));
            }
        }
    }

    /**
     * Adds {@code word} under the class its core asks for, leaving the brackets and
     * punctuation around it plain so that an optional field reads as one the User may leave out
     * rather than as part of the marker.
     */
    private static void addWord(List<Text> spans, String word, boolean isFirstWord) {
        int start = 0;
        while (start < word.length() && OPENERS.indexOf(word.charAt(start)) >= 0) {
            start++;
        }
        int end = word.length();
        while (end > start && CLOSERS.indexOf(word.charAt(end - 1)) >= 0) {
            end--;
        }
        if (start > 0) {
            spans.add(span(word.substring(0, start), "help-plain"));
        }
        String core = word.substring(start, end);
        if (!core.isEmpty()) {
            // a command word opens a line of syntax; one carrying punctuation is being talked
            // about in a sentence, as in "help, list, clear and exit take no arguments", where
            // colouring the first of the four and none of the rest would read as an accident
            boolean opensSyntax = isFirstWord && start == 0 && end == word.length();
            spans.add(span(core, styleOf(core, opensSyntax)));
        }
        if (end < word.length()) {
            spans.add(span(word.substring(end), "help-plain"));
        }
    }

    private static String styleOf(String core, boolean isFirstWord) {
        if (isFirstWord && CommandWords.COMMAND_WORDS.contains(core)) {
            return "help-command-word";
        }
        if (core.startsWith("-")) {
            return "help-option";
        }
        if (PLACEHOLDER.matcher(core).matches()) {
            return "help-placeholder";
        }
        return "help-plain";
    }

    private static Text span(String text, String styleClass) {
        Text span = new Text(text);
        span.getStyleClass().addAll("help-token", styleClass);
        return span;
    }
}
