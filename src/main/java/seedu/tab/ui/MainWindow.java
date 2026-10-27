package seedu.tab.ui;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import seedu.tab.commons.core.GuiSettings;
import seedu.tab.commons.core.LogsCenter;
import seedu.tab.logic.Logic;
import seedu.tab.logic.commands.CommandResult;
import seedu.tab.logic.commands.exceptions.CommandException;
import seedu.tab.logic.parser.exceptions.ParseException;

/**
 * The Main Window. Provides the basic application layout containing
 * a menu bar and space where other JavaFX elements can be placed.
 */
public class MainWindow extends UiPart<Stage> {

    private static final String FXML = "MainWindow.fxml";

    private final Logger logger = LogsCenter.getLogger(getClass());

    private Stage primaryStage;
    private Logic logic;
    private Path dataFilePath;

    // Independent Ui parts residing in this Ui container
    private StudentListPanel studentListPanel;
    private ResultDisplay resultDisplay;
    private CommandBox commandBox;
    private HelpPage helpPage;

    @FXML
    private VBox studentPage;

    @FXML
    private StackPane helpPagePlaceholder;

    @FXML
    private StackPane commandBoxPlaceholder;

    @FXML
    private MenuItem helpMenuItem;

    @FXML
    private StackPane studentListPanelPlaceholder;

    @FXML
    private StackPane summaryPanelPlaceholder;

    @FXML
    private StackPane resultDisplayPlaceholder;

    @FXML
    private StackPane statusbarPlaceholder;

    /**
     * Creates a {@code MainWindow} with the given {@code Stage}, {@code Logic},
     * and the data file path to show in the status bar.
     */
    public MainWindow(Stage primaryStage, Logic logic, Path dataFilePath) {
        super(FXML, primaryStage);

        // Set dependencies
        this.primaryStage = primaryStage;
        this.logic = logic;
        this.dataFilePath = dataFilePath;

        // Configure the UI
        UiFonts.applyTo(primaryStage.getScene());
        restoreWindowBounds(logic.getGuiSettings());

        setAccelerators();
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    private void setAccelerators() {
        setAccelerator(helpMenuItem, KeyCombination.valueOf("F1"));
    }

    /**
     * Sets the accelerator of a MenuItem.
     *
     * @param menuItem The menu item receiving the shortcut.
     * @param keyCombination the KeyCombination value of the accelerator.
     */
    private void setAccelerator(MenuItem menuItem, KeyCombination keyCombination) {
        menuItem.setAccelerator(keyCombination);

        /*
         * TODO: the code below can be removed once the bug reported here
         * https://bugs.openjdk.java.net/browse/JDK-8131666
         * is fixed in a later version of the SDK.
         *
         * According to the bug report, TextInputControl (TextField, TextArea) will
         * consume function-key events. Because CommandBox contains a TextField and
         * ResultDisplay contains a TextArea, some accelerators (e.g., F1) will
         * not work when the focus is in them because the key event is consumed by
         * the TextInputControl(s).
         *
         * For now, we add the following event filter to capture such key events and open
         * the in-window help summary so as to support accelerators even when focus is
         * in CommandBox or ResultDisplay.
         */
        getRoot().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getTarget() instanceof TextInputControl && keyCombination.match(event)) {
                menuItem.getOnAction().handle(new ActionEvent());
                event.consume();
            }
        });
    }

    /**
     * Fills up all the placeholders of this window.
     */
    void fillInnerParts() {
        studentListPanel = new StudentListPanel(logic.getFilteredStudentList());
        studentListPanelPlaceholder.getChildren().add(studentListPanel.getRoot());

        SummaryPanel summaryPanel = new SummaryPanel(logic.getFilteredStudentList());
        summaryPanelPlaceholder.getChildren().add(summaryPanel.getRoot());

        resultDisplay = new ResultDisplay();
        resultDisplayPlaceholder.getChildren().add(resultDisplay.getRoot());
        // the pane carries padding of its own, so it has to collapse with what it holds or the
        // gap stays behind after the result area has gone
        resultDisplayPlaceholder.managedProperty().bind(resultDisplay.getRoot().managedProperty());
        resultDisplayPlaceholder.visibleProperty().bind(resultDisplay.getRoot().visibleProperty());

        StatusBarFooter statusBarFooter = new StatusBarFooter(dataFilePath);
        statusbarPlaceholder.getChildren().add(statusBarFooter.getRoot());

        commandBox = new CommandBox(this::executeCommand, logic::isAwaitingConfirmation);
        commandBoxPlaceholder.getChildren().add(commandBox.getRoot());
        fillHelpPage();
        setKeyboardNavigation(summaryPanel);
    }

    /**
     * Keeps the student list one Tab from commands, regardless of whether a reply is showing.
     */
    private void setKeyboardNavigation(SummaryPanel summaryPanel) {
        Node commandField = commandBox.getRoot().lookup("#commandTextField");
        Node students = studentListPanel.getRoot().lookup("#studentListView");
        Node tags = summaryPanel.getRoot().lookup("#tagScroll");
        Node reply = resultDisplay.getRoot().lookup("#resultDisplay");
        List<Node> readingPanes = List.of(commandField, students, tags);
        List<Node> panesWithReply = List.of(commandField, students, tags, reply);
        studentPage.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() != KeyCode.TAB || event.isControlDown() || event.isAltDown() || event.isMetaDown()) {
                return;
            }
            List<Node> panes = resultDisplay.getRoot().isVisible() ? panesWithReply : readingPanes;
            int current = panes.indexOf(primaryStage.getScene().getFocusOwner());
            if (current < 0) {
                return;
            }
            int direction = event.isShiftDown() ? -1 : 1;
            panes.get(Math.floorMod(current + direction, panes.size())).requestFocus();
            event.consume();
        });
    }

    private void fillHelpPage() {
        helpPage = new HelpPage(this::closeHelp);
        helpPagePlaceholder.getChildren().add(helpPage.getRoot());
    }

    /**
     * Restores the student view, its previous result, and any unfinished command.
     */
    private void closeHelp() {
        helpPagePlaceholder.setVisible(false);
        helpPagePlaceholder.setManaged(false);
        studentPage.setManaged(true);
        studentPage.setVisible(true);
        commandBox.focusDraftAtEnd();
    }

    /**
     * Restores the saved size and position onto an available screen, lowering minimums if needed.
     */
    private void restoreWindowBounds(GuiSettings guiSettings) {
        List<Rectangle2D> displays = WindowBounds.prioritizePrimary(Screen.getPrimary().getVisualBounds(),
                Screen.getScreens().stream().map(Screen::getVisualBounds).toList());
        Rectangle2D bounds = WindowBounds.restore(guiSettings, displays,
                primaryStage.getMinWidth(), primaryStage.getMinHeight());
        // A saved minimum must not force the window beyond a smaller or scaled display.
        primaryStage.setMinWidth(Math.min(primaryStage.getMinWidth(), bounds.getWidth()));
        primaryStage.setMinHeight(Math.min(primaryStage.getMinHeight(), bounds.getHeight()));
        primaryStage.setWidth(bounds.getWidth());
        primaryStage.setHeight(bounds.getHeight());
        primaryStage.setX(bounds.getMinX());
        primaryStage.setY(bounds.getMinY());
    }

    /**
     * Shows the full-page command summary with keyboard paging.
     */
    @FXML
    public void handleHelp() {
        studentPage.setVisible(false);
        studentPage.setManaged(false);
        helpPagePlaceholder.setManaged(true);
        helpPagePlaceholder.setVisible(true);
        helpPage.show();
    }

    void show() {
        primaryStage.show();
    }

    /**
     * Closes the application.
     */
    @FXML
    private void handleExit() {
        GuiSettings guiSettings = new GuiSettings(primaryStage.getWidth(), primaryStage.getHeight(),
                (int) primaryStage.getX(), (int) primaryStage.getY());
        logic.setGuiSettings(guiSettings);
        primaryStage.hide();
    }

    public StudentListPanel getStudentListPanel() {
        return studentListPanel;
    }

    /**
     * Executes the command and returns the result.
     *
     * @see seedu.tab.logic.Logic#execute(String)
     */
    private CommandResult executeCommand(String commandText) throws CommandException, ParseException {
        try {
            CommandResult commandResult = logic.execute(commandText);
            logger.info("Result: " + commandResult.getFeedbackToUser());
            if (commandResult.isHelpRequested()) {
                handleHelp();
            } else {
                resultDisplay.setFeedbackToUser(commandResult.getFeedbackToUser());
            }

            commandResult.getStudentToReveal().ifPresent(studentListPanel::scrollTo);

            if (commandResult.isExitRequested()) {
                handleExit();
            }

            return commandResult;
        } catch (CommandException | ParseException e) {
            logger.info("An error occurred while executing command: " + commandText);
            resultDisplay.setFeedbackToUser(e.getMessage());
            throw e;
        }
    }
}
