package seedu.tab.logic;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javafx.collections.ObservableList;
import seedu.tab.commons.core.GuiSettings;
import seedu.tab.commons.core.LogsCenter;
import seedu.tab.logic.commands.ClearCommand;
import seedu.tab.logic.commands.Command;
import seedu.tab.logic.commands.CommandResult;
import seedu.tab.logic.commands.exceptions.CommandException;
import seedu.tab.logic.parser.StudentBookParser;
import seedu.tab.logic.parser.exceptions.ParseException;
import seedu.tab.model.Model;
import seedu.tab.model.student.Student;
import seedu.tab.storage.Storage;

/**
 * The main LogicManager of the app.
 */
public class LogicManager implements Logic {
    public static final String FILE_OPS_ERROR_FORMAT = "Could not save data due to the following error: %s";

    public static final String FILE_OPS_PERMISSION_ERROR_FORMAT =
            "Could not save data to file %s due to insufficient permissions to write to the file or the folder.";

    private static final Pattern SURROUNDING_WHITESPACE = Pattern.compile("(?U)^\\s+|\\s+$");

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
        CommandResult commandResult = command.execute(model);
        saveStudentBook();
        return commandResult;
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
        String reply = SURROUNDING_WHITESPACE.matcher(commandText).replaceAll("");
        if (reply.isEmpty() || reply.equalsIgnoreCase("n") || reply.equalsIgnoreCase("no")) {
            pendingClearCommand = Optional.empty();
            return new CommandResult(ClearCommand.MESSAGE_CANCELLED);
        }
        ClearCommand clearCommand = pendingClearCommand.orElseThrow();
        if (!reply.equalsIgnoreCase("y") && !reply.equalsIgnoreCase("yes")) {
            throw new ParseException(ClearCommand.MESSAGE_INVALID_REPLY + "\n"
                    + clearCommand.execute(model).getFeedbackToUser());
        }
        pendingClearCommand = Optional.empty();
        CommandResult result = clearCommand.executeConfirmed(model);
        saveStudentBook();
        return result;
    }

    /**
     * Saves the student book and translates storage errors into command feedback.
     */
    private void saveStudentBook() throws CommandException {
        try {
            storage.saveStudentBook(model.getStudentBook());
        } catch (AccessDeniedException e) {
            throw new CommandException(String.format(FILE_OPS_PERMISSION_ERROR_FORMAT, e.getMessage()), e);
        } catch (IOException ioe) {
            throw new CommandException(String.format(FILE_OPS_ERROR_FORMAT, ioe.getMessage()), ioe);
        }
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
