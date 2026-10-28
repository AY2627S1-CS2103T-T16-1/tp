package seedu.tab.logic;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import seedu.tab.commons.core.GuiSettings;
import seedu.tab.commons.core.LogsCenter;
import seedu.tab.commons.util.StringUtil;
import seedu.tab.logic.commands.ClearCommand;
import seedu.tab.logic.commands.Command;
import seedu.tab.logic.commands.CommandResult;
import seedu.tab.logic.commands.exceptions.CommandException;
import seedu.tab.logic.parser.StudentBookParser;
import seedu.tab.logic.parser.exceptions.ParseException;
import seedu.tab.model.Model;
import seedu.tab.model.StudentBook;
import seedu.tab.model.student.Student;
import seedu.tab.storage.Storage;

/**
 * The main LogicManager of the app.
 */
public class LogicManager implements Logic {
    public static final String FILE_OPS_ERROR_FORMAT =
            "Could not save data due to the following error: %s. No changes were made.";

    public static final String FILE_OPS_MISSING_FILE_ERROR_FORMAT =
            "Could not save data to file %s because %s. No changes were made.";

    public static final String FILE_OPS_PERMISSION_ERROR_FORMAT =
            "Could not save data to file %s due to insufficient permissions to write to the file or the folder. "
                    + "No changes were made.";

    private final Logger logger = LogsCenter.getLogger(LogicManager.class);

    private final Model model;
    private final Storage storage;
    private final StudentBookParser studentBookParser;
    private Optional<ClearCommand> pendingClearCommand = Optional.empty();

    /**
     * Constructs a {@code LogicManager} with the given {@code Model} and {@code Storage}.
     */
    public LogicManager(Model model, Storage storage) {
        this.model = model;
        this.storage = storage;
        studentBookParser = new StudentBookParser();
    }

    @Override
    public CommandResult execute(String commandText) throws CommandException, ParseException {
        logger.info("----------------[USER COMMAND][" + commandText + "]");

        if (isAwaitingConfirmation()) {
            return respondToClearConfirmation(commandText);
        }
        Command command = studentBookParser.parseCommand(commandText);
        if (command instanceof ClearCommand clearCommand) {
            return requestClearConfirmation(clearCommand);
        }
        return executeAndSaveChanges(command);
    }

    /**
     * Prompts without saving and keeps a request only when there are students to clear.
     */
    private CommandResult requestClearConfirmation(ClearCommand clearCommand) {
        CommandResult result = clearCommand.execute(model);
        if (!model.getStudentBook().getStudentList().isEmpty()) {
            pendingClearCommand = Optional.of(clearCommand);
        }
        return result;
    }

    /**
     * Handles a reply without parsing it as a command or saving unconfirmed data.
     */
    private CommandResult respondToClearConfirmation(String commandText) throws CommandException, ParseException {
        ClearCommand clearCommand = pendingClearCommand.orElseThrow(() ->
                new CommandException(ClearCommand.MESSAGE_NO_PENDING_CONFIRMATION));
        String reply = stripSurroundingWhitespace(StringUtil.stripFormatCharacters(commandText));
        if (reply.isEmpty() || reply.equalsIgnoreCase("n") || reply.equalsIgnoreCase("no")) {
            pendingClearCommand = Optional.empty();
            return new CommandResult(ClearCommand.MESSAGE_CANCELLED);
        }
        if (!reply.equalsIgnoreCase("y") && !reply.equalsIgnoreCase("yes")) {
            throw new ParseException(ClearCommand.MESSAGE_INVALID_REPLY + "\n"
                    + clearCommand.execute(model).getFeedbackToUser());
        }
        pendingClearCommand = Optional.empty();
        StudentBook studentBookBeforeCommand = new StudentBook(model.getStudentBook());
        Predicate<? super Student> filterBeforeCommand = model.getFilteredStudentPredicate();
        CommandResult result = clearCommand.executeConfirmed(model);
        saveStudentBookIfChanged(studentBookBeforeCommand, filterBeforeCommand);
        return result;
    }

    /**
     * Executes a command and saves only when it changes student records.
     */
    private CommandResult executeAndSaveChanges(Command command) throws CommandException {
        StudentBook studentBookBeforeCommand = new StudentBook(model.getStudentBook());
        Predicate<? super Student> filterBeforeCommand = model.getFilteredStudentPredicate();
        CommandResult commandResult = command.execute(model);
        saveStudentBookIfChanged(studentBookBeforeCommand, filterBeforeCommand);
        return commandResult;
    }

    /**
     * Trims the same whitespace as command parsing without rescanning internal whitespace runs.
     */
    private static String stripSurroundingWhitespace(String reply) {
        int start = 0;
        int end = reply.length();
        while (start < end && StringUtil.isWhitespace(reply.charAt(start))) {
            start++;
        }
        while (end > start && StringUtil.isWhitespace(reply.charAt(end - 1))) {
            end--;
        }
        return reply.substring(start, end);
    }

    /**
     * Saves changed student data, restoring the previous data and filter if the save fails.
     */
    private void saveStudentBookIfChanged(StudentBook studentBookBeforeCommand,
            Predicate<? super Student> filterBeforeCommand) throws CommandException {
        if (studentBookBeforeCommand.equals(model.getStudentBook())) {
            return;
        }
        try {
            storage.saveStudentBook(model.getStudentBook());
        } catch (IOException e) {
            model.setStudentBook(studentBookBeforeCommand);
            model.updateFilteredStudentList(filterBeforeCommand);
            logger.warning("Restored the student book and filter after save failed: " + e);
            if (e instanceof AccessDeniedException) {
                throw new CommandException(String.format(FILE_OPS_PERMISSION_ERROR_FORMAT, e.getMessage()), e);
            }
            if (e instanceof NoSuchFileException missingFile) {
                throw new CommandException(String.format(FILE_OPS_MISSING_FILE_ERROR_FORMAT,
                        missingFile.getFile(), describeMissingFile(missingFile)), e);
            }
            throw new CommandException(String.format(FILE_OPS_ERROR_FORMAT, e.getMessage()), e);
        }
    }

    /**
     * Returns why a save found no file to write, which the exception carries as its reason when
     * one was given and otherwise only as the missing path.
     */
    private static String describeMissingFile(NoSuchFileException exception) {
        String reason = exception.getReason();
        return reason == null ? "it does not exist" : reason;
    }

    @Override
    public boolean isAwaitingConfirmation() {
        return pendingClearCommand.isPresent();
    }

    @Override
    public ObservableList<Student> getFilteredStudentList() {
        return model.getFilteredStudentList();
    }

    @Override
    public GuiSettings getGuiSettings() {
        return model.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        model.setGuiSettings(guiSettings);
    }
}
