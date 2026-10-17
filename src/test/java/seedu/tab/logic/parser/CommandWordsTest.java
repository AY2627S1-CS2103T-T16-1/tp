package seedu.tab.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.testutil.Assert.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

public class CommandWordsTest {

    private static final Path PARSER_SOURCE = Paths.get("src", "main", "java", "seedu", "tab",
            "logic", "parser", "StudentBookParser.java");

    @Test
    public void commandWords_areInAlphabeticalOrderWithoutRepeats() {
        List<String> sorted = new ArrayList<>(CommandWords.COMMAND_WORDS);
        sorted.sort(String::compareTo);
        assertEquals(sorted, CommandWords.COMMAND_WORDS);
        assertEquals(CommandWords.COMMAND_WORDS.size(), CommandWords.COMMAND_WORDS.stream().distinct().count());
    }

    @Test
    public void commandWords_coverEveryWordTheParserDispatchesOn() throws IOException {
        // a command word left out of the list would work when typed while being suggested to
        // nobody, which no test of either class on its own would notice
        String source = Files.readString(PARSER_SOURCE, StandardCharsets.UTF_8);
        long dispatchedWords = Pattern.compile("case \\w+Command\\.COMMAND_WORD").matcher(source).results().count();
        assertEquals(dispatchedWords, CommandWords.COMMAND_WORDS.size());
    }

    @Test
    public void findClosestTo_commandWordItself_returnsThatWord() {
        for (String commandWord : CommandWords.COMMAND_WORDS) {
            assertEquals(Optional.of(commandWord), CommandWords.findClosestTo(commandWord));
        }
    }

    @Test
    public void findClosestTo_missingLetter_returnsTheIntendedWord() {
        assertEquals(Optional.of("delete"), CommandWords.findClosestTo("delte"));
        assertEquals(Optional.of("find"), CommandWords.findClosestTo("fnd"));
        assertEquals(Optional.of("clear"), CommandWords.findClosestTo("clar"));
    }

    @Test
    public void findClosestTo_extraLetter_returnsTheIntendedWord() {
        assertEquals(Optional.of("list"), CommandWords.findClosestTo("liist"));
        assertEquals(Optional.of("add"), CommandWords.findClosestTo("addd"));
        assertEquals(Optional.of("delete"), CommandWords.findClosestTo("deleete"));
    }

    @Test
    public void findClosestTo_swappedLetters_returnsTheIntendedWord() {
        assertEquals(Optional.of("list"), CommandWords.findClosestTo("lsit"));
        assertEquals(Optional.of("help"), CommandWords.findClosestTo("hepl"));
        assertEquals(Optional.of("delete"), CommandWords.findClosestTo("deelte"));
    }

    @Test
    public void findClosestTo_wrongLetter_returnsTheIntendedWord() {
        assertEquals(Optional.of("find"), CommandWords.findClosestTo("fine"));
        assertEquals(Optional.of("edit"), CommandWords.findClosestTo("edut"));
        assertEquals(Optional.of("clear"), CommandWords.findClosestTo("clexr"));
    }

    @Test
    public void findClosestTo_differentCase_returnsTheLowercaseWord() {
        assertEquals(Optional.of("list"), CommandWords.findClosestTo("LIST"));
        assertEquals(Optional.of("delete"), CommandWords.findClosestTo("Delte"));
        assertEquals(Optional.of("add"), CommandWords.findClosestTo("Add"));
    }

    @Test
    public void findClosestTo_twoEditsFromALongWord_returnsThatWord() {
        assertEquals(Optional.of("delete"), CommandWords.findClosestTo("dlte"));
        assertEquals(Optional.of("clear"), CommandWords.findClosestTo("clr"));
    }

    @Test
    public void findClosestTo_twoEditsFromAShortWord_returnsNothing() {
        // half of a four-letter word is too little to go on: "fit" is as much an attempt at
        // "find" as at "list", and "ls" at "list" as at nothing at all
        assertEquals(Optional.empty(), CommandWords.findClosestTo("fit"));
        assertEquals(Optional.empty(), CommandWords.findClosestTo("ls"));
        assertEquals(Optional.empty(), CommandWords.findClosestTo("hp"));
    }

    @Test
    public void findClosestTo_equallyNearWords_returnsTheAlphabeticallyFirst() {
        // dropping the second letter of either leaves a word one edit from both
        assertEquals(Optional.of("edit"), CommandWords.findClosestTo("eit"));
        assertEquals(Optional.of("edit"), CommandWords.findClosestTo("elit"));
    }

    @Test
    public void findClosestTo_wordFarFromEveryCommand_returnsNothing() {
        assertEquals(Optional.empty(), CommandWords.findClosestTo("qwerty"));
        assertEquals(Optional.empty(), CommandWords.findClosestTo("uicfhmowqewca"));
        assertEquals(Optional.empty(), CommandWords.findClosestTo("remove"));
    }

    @Test
    public void findClosestTo_oneCharacter_returnsNothing() {
        assertEquals(Optional.empty(), CommandWords.findClosestTo("a"));
        assertEquals(Optional.empty(), CommandWords.findClosestTo("l"));
        assertEquals(Optional.empty(), CommandWords.findClosestTo("?"));
    }

    @Test
    public void findClosestTo_nonAsciiWord_returnsNothing() {
        assertEquals(Optional.empty(), CommandWords.findClosestTo("日本語"));
        assertEquals(Optional.empty(), CommandWords.findClosestTo("удалить"));
        assertEquals(Optional.empty(), CommandWords.findClosestTo("🙂🙂"));
    }

    @Test
    public void findClosestTo_pastedParagraph_returnsNothingPromptly() {
        String pasted = "lorem ipsum ".repeat(100_000);
        long startTime = System.nanoTime();
        assertEquals(Optional.empty(), CommandWords.findClosestTo(pasted));
        long elapsedMillis = (System.nanoTime() - startTime) / 1_000_000;
        assertTrue(elapsedMillis < 1000, "measuring a pasted paragraph took " + elapsedMillis + "ms");
    }

    @Test
    public void findClosestTo_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> CommandWords.findClosestTo(null));
    }

}
