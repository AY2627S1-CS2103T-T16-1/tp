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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import seedu.tab.logic.commands.Command;

public class CommandWordsTest {

    /** Relative because Gradle runs the tests from the project root. */
    private static final Path PARSER_SOURCE = Paths.get("src", "main", "java", "seedu", "tab",
            "logic", "parser", "StudentBookParser.java");

    /** A case of the parser's dispatch, however its arms are spaced or wrapped. */
    private static final Pattern DISPATCHED_COMMAND =
            Pattern.compile("case\\s+(\\w+Command)\\s*\\.\\s*COMMAND_WORD");

    @Test
    public void commandWords_areInAlphabeticalOrderWithoutRepeats() {
        List<String> sorted = new ArrayList<>(CommandWords.COMMAND_WORDS);
        sorted.sort(String::compareTo);
        assertEquals(sorted, CommandWords.COMMAND_WORDS);
        assertEquals(CommandWords.COMMAND_WORDS.size(), CommandWords.COMMAND_WORDS.stream().distinct().count());
    }

    @Test
    public void commandWords_coverEveryWordTheParserDispatchesOn() throws ReflectiveOperationException,
            IOException {
        // a command word left out of the list would work when typed while being suggested to
        // nobody, which no test of either class on its own would notice
        assertEquals(Set.copyOf(CommandWords.COMMAND_WORDS), wordsTheParserDispatchesOn());
    }

    /**
     * Returns the command words the parser's own cases name, read out of its source and
     * resolved to the values the command classes declare.
     */
    private static Set<String> wordsTheParserDispatchesOn() throws ReflectiveOperationException, IOException {
        String source = Files.readString(PARSER_SOURCE, StandardCharsets.UTF_8);
        Matcher matcher = DISPATCHED_COMMAND.matcher(source);
        Set<String> dispatchedWords = new HashSet<>();
        while (matcher.find()) {
            dispatchedWords.add(commandWordDeclaredBy(matcher.group(1)));
        }
        return dispatchedWords;
    }

    /** Returns the COMMAND_WORD that the command class named {@code className} declares. */
    private static String commandWordDeclaredBy(String className) throws ReflectiveOperationException {
        Class<?> commandClass = Class.forName(Command.class.getPackageName() + "." + className);
        return (String) commandClass.getField("COMMAND_WORD").get(null);
    }

    @Test
    public void suggestFor_commandWordItself_returnsThatWord() {
        for (String commandWord : CommandWords.COMMAND_WORDS) {
            assertEquals(List.of(commandWord), CommandWords.suggestFor(commandWord));
        }
    }

    @Test
    public void suggestFor_openingOfOneCommandWord_returnsThatWord() {
        assertEquals(List.of("delete"), CommandWords.suggestFor("del"));
        assertEquals(List.of("delete"), CommandWords.suggestFor("dele"));
        assertEquals(List.of("clear"), CommandWords.suggestFor("cle"));
        assertEquals(List.of("list"), CommandWords.suggestFor("li"));
    }

    @Test
    public void suggestFor_openingOfSeveralCommandWords_returnsEveryOneOfThem() {
        assertEquals(List.of("edit", "exit"), CommandWords.suggestFor("e"));
        assertEquals(List.of("edit", "exit"), CommandWords.suggestFor("E"));
    }

    @Test
    public void suggestFor_openingWithTheWrongLettersForIt_isMeasuredByEdits() {
        // "ls" opens no command word, so it has to reach "list" by edits
        assertEquals(List.of("list"), CommandWords.suggestFor("ls"));
        assertEquals(List.of("delete"), CommandWords.suggestFor("dlte"));
    }

    @Test
    public void suggestFor_everyCommandWordAtOnce_returnsNothing() {
        // the empty word opens all of them, which names nothing in particular; the parser
        // never hands one over, but nothing here may depend on that
        assertEquals(List.of(), CommandWords.suggestFor(""));
    }

    @Test
    public void suggestFor_missingLetter_returnsTheIntendedWord() {
        assertEquals(List.of("delete"), CommandWords.suggestFor("delte"));
        assertEquals(List.of("find"), CommandWords.suggestFor("fnd"));
        assertEquals(List.of("clear"), CommandWords.suggestFor("clar"));
    }

    @Test
    public void suggestFor_extraLetter_returnsTheIntendedWord() {
        assertEquals(List.of("list"), CommandWords.suggestFor("liist"));
        assertEquals(List.of("add"), CommandWords.suggestFor("addd"));
        assertEquals(List.of("delete"), CommandWords.suggestFor("deleete"));
    }

    @Test
    public void suggestFor_swappedLetters_returnsTheIntendedWord() {
        assertEquals(List.of("list"), CommandWords.suggestFor("lsit"));
        assertEquals(List.of("help"), CommandWords.suggestFor("hepl"));
        assertEquals(List.of("delete"), CommandWords.suggestFor("deelte"));
    }

    @Test
    public void suggestFor_wrongLetter_returnsTheIntendedWord() {
        assertEquals(List.of("find"), CommandWords.suggestFor("fine"));
        assertEquals(List.of("edit"), CommandWords.suggestFor("edut"));
        assertEquals(List.of("clear"), CommandWords.suggestFor("clexr"));
    }

    @Test
    public void suggestFor_differentCase_returnsTheLowercaseWord() {
        assertEquals(List.of("list"), CommandWords.suggestFor("LIST"));
        assertEquals(List.of("delete"), CommandWords.suggestFor("Delte"));
        assertEquals(List.of("add"), CommandWords.suggestFor("Add"));
    }

    @Test
    public void suggestFor_twoEditsAway_returnsTheNearestWord() {
        assertEquals(List.of("list"), CommandWords.suggestFor("ls"));
        assertEquals(List.of("delete"), CommandWords.suggestFor("dlte"));
        assertEquals(List.of("clear"), CommandWords.suggestFor("clr"));
    }

    @Test
    public void suggestFor_oneEditAwayFromOneWordAndTwoFromAnother_returnsTheNearerOne() {
        // "exit" is two edits from "edut" and "edit" one, so the nearer word is named alone
        assertEquals(List.of("edit"), CommandWords.suggestFor("edut"));
        assertEquals(List.of("list"), CommandWords.suggestFor("lsit"));
    }

    @Test
    public void suggestFor_equallyNearWords_namesEveryOneOfThem() {
        // dropping the second letter of either leaves a word one edit from both, and which of
        // the two was meant is the User's to say rather than ours to pick
        assertEquals(List.of("edit", "exit"), CommandWords.suggestFor("eit"));
        assertEquals(List.of("edit", "exit"), CommandWords.suggestFor("ewit"));
    }

    @Test
    public void suggestFor_asManyWordsAsAreWorthNaming_namesThemAll() {
        assertEquals(List.of("edit", "exit", "help"), CommandWords.suggestFor("eli"));
    }

    @Test
    public void suggestFor_moreWordsThanAreWorthNaming_returnsNothing() {
        // "fit" is two edits from edit, exit, find and list alike, and a list of four says
        // only that TAB has no idea which was meant
        assertEquals(List.of(), CommandWords.suggestFor("fit"));
    }

    @Test
    public void suggestFor_wordFarFromEveryCommand_returnsNothing() {
        assertEquals(List.of(), CommandWords.suggestFor("qwerty"));
        assertEquals(List.of(), CommandWords.suggestFor("uicfhmowqewca"));
        assertEquals(List.of(), CommandWords.suggestFor("remove"));
    }

    @Test
    public void suggestFor_oneCharacter_returnsTheWordsItOpens() {
        assertEquals(List.of("add"), CommandWords.suggestFor("a"));
        assertEquals(List.of("list"), CommandWords.suggestFor("l"));
        assertEquals(List.of(), CommandWords.suggestFor("?"));
    }

    @Test
    public void suggestFor_nonAsciiWord_returnsNothing() {
        assertEquals(List.of(), CommandWords.suggestFor("日本語"));
        assertEquals(List.of(), CommandWords.suggestFor("удалить"));
        assertEquals(List.of(), CommandWords.suggestFor("🙂🙂"));
    }

    @Test
    public void suggestFor_pastedParagraph_returnsNothingPromptly() {
        String pasted = "lorem ipsum ".repeat(100_000);
        long startTime = System.nanoTime();
        assertEquals(List.of(), CommandWords.suggestFor(pasted));
        long elapsedMillis = (System.nanoTime() - startTime) / 1_000_000;
        assertTrue(elapsedMillis < 1000, "measuring a pasted paragraph took " + elapsedMillis + "ms");
    }

    @Test
    public void suggestFor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> CommandWords.suggestFor(null));
    }

}
