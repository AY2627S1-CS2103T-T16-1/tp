package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import seedu.tab.commons.core.DataLoadingErrorMessage;
import seedu.tab.commons.core.GuiSettings;
import seedu.tab.commons.core.LogsCenter;
import seedu.tab.logic.Logic;
import seedu.tab.logic.commands.CommandResult;
import seedu.tab.model.student.Student;

public class UiManagerTest {

    private static final Path DATA_FILE_PATH = Paths.get("data", "tab.json");
    private static final String BROKEN_WINDOW_MESSAGE = "the window settings could not be read";
    // layout arithmetic lands a fraction of a pixel out, which no reader of the dialog can see
    private static final double MEASUREMENT_TOLERANCE = 0.5;
    // no line of the dialog's text is drawn in as few pixels as this, so one line per ten pixels
    // of screen height is more text than any screen the test may run on can show at once
    private static final int MIN_TEXT_LINE_HEIGHT = 10;
    private static final DataLoadingErrorMessage PARSE_ERROR = new DataLoadingErrorMessage(
            "The data is not in the expected JSON format at line 4, column 6: Unexpected character",
            """
              3 |     "name": "Yun"
            > 4 |     "phone": "09"
              5 |   },""");

    @BeforeAll
    public static void startToolkit() throws Exception {
        FxToolkit.start();
    }

    @Test
    public void start_dataLoadingError_showsErrorInsteadOfStartingMainWindow() throws Exception {
        DataLoadingErrorMessage error =
                DataLoadingErrorMessage.of("Student record 2 has an invalid Phone field.");

        StartOutcome outcome = FxToolkit.onFxThread(() -> {
            Stage stage = new Stage();
            UiManagerStub uiManager = new UiManagerStub(error);
            try {
                uiManager.start(stage);
                // the main window takes the stage as its own root, so a stage that has been
                // given neither a scene nor the application icon never reached a main window
                return new StartOutcome(uiManager.wasDataLoadingErrorShown, uiManager.shownError,
                        stage.getScene() != null, !stage.getIcons().isEmpty(), isAnyWindowShowing(stage));
            } finally {
                stage.hide();
            }
        });

        assertTrue(outcome.wasDataLoadingErrorShown);
        assertEquals(error, outcome.shownError);
        assertFalse(outcome.wasMainWindowBuilt, "the main window should not have been built");
        assertFalse(outcome.wasIconSet, "start-up should have stopped before the main window was prepared");
        assertFalse(outcome.isAnyWindowShowing, "no window of TAB's should have become visible");
    }

    /** Returns whether {@code stage}, or any stage owned by it, is showing. */
    private static boolean isAnyWindowShowing(Stage stage) {
        return Window.getWindows().stream()
                .anyMatch(window -> window == stage
                        || (window instanceof Stage owned && owned.getOwner() == stage));
    }

    @Test
    public void start_withoutDataLoadingError_opensMainWindow() throws Exception {
        boolean wasShown = FxToolkit.onFxThread(() -> {
            Stage stage = new Stage();
            UiManager uiManager = new UiManager(new LogicStub(), DATA_FILE_PATH);
            try {
                uiManager.start(stage);
                return stage.isShowing();
            } finally {
                stage.hide();
            }
        });

        assertTrue(wasShown);
    }

    @Test
    public void createAlert_withOwner_assignsOwner() throws Exception {
        boolean hasOwner = FxToolkit.onFxThread(() -> {
            Stage owner = new Stage();
            owner.setScene(new Scene(new StackPane()));
            Alert alert = UiManager.createAlert(owner, Alert.AlertType.INFORMATION,
                    "Title", "Header");
            return alert.getOwner() == owner;
        });

        assertTrue(hasOwner);
    }

    @Test
    public void showDataLoadingErrorDialogAndWait_dialogDismissed_returns() throws Exception {
        boolean isOwnAlertClosed = FxToolkit.onFxThread(() -> {
            AutoClosingUiManager uiManager = new AutoClosingUiManager();
            uiManager.showDataLoadingErrorDialogAndWait(DataLoadingErrorMessage.of("Invalid data"));
            return !uiManager.alert.isShowing();
        });

        assertTrue(isOwnAlertClosed);
    }

    @Test
    public void showAlertDialogAndWait_dialogDismissed_returnsToMainWindow() throws Exception {
        boolean isMainWindowStillShowing = FxToolkit.onFxThread(() -> {
            Stage stage = new Stage();
            UiManager uiManager = new UiManager(new LogicStub(), DATA_FILE_PATH);
            try {
                uiManager.start(stage);
                Platform.runLater(() -> List.copyOf(Window.getWindows()).stream()
                        .filter(window -> window instanceof Stage ownedStage && ownedStage.getOwner() == stage)
                        .forEach(Window::hide));

                uiManager.showAlertDialogAndWait(Alert.AlertType.INFORMATION,
                        "Title", "Header", "Content");
                return stage.isShowing();
            } finally {
                stage.hide();
            }
        });

        assertTrue(isMainWindowStillShowing);
    }

    @Test
    public void start_mainWindowFailsToBuild_showsFatalErrorOwnedByPrimaryStageAndShutsDown()
            throws Exception {
        FatalErrorDetails details = FxToolkit.onFxThread(() -> {
            Stage stage = new Stage();
            FatalErrorUiManager uiManager = new FatalErrorUiManager();
            try {
                uiManager.start(stage);
                return new FatalErrorDetails(uiManager.alert != null, uiManager.owner == stage,
                        uiManager.alert != null && uiManager.alert.getOwner() == stage,
                        uiManager.alert != null && !uiManager.alert.isShowing(),
                        uiManager.wasShutDown, uiManager.headerText);
            } finally {
                stage.hide();
            }
        });

        assertTrue(details.wasAlertCreated, "a dialog should report a main window that cannot be built");
        assertTrue(details.isOwnedByPrimaryStage, "the dialog should be owned by the stage TAB was given");
        assertTrue(details.isAlertOwnerPrimaryStage, "the dialog should take that stage as its owner");
        assertTrue(details.wasAlertClosed, "the dialog should have been shown and waited on");
        assertTrue(details.wasShutDown, "TAB should stop once the user closes the dialog");
        assertEquals(BROKEN_WINDOW_MESSAGE, details.headerText);
    }

    @Test
    public void start_mainWindowFailsToBuild_logsTheFailureOnce() throws Exception {
        List<LogRecord> severe = new ArrayList<>();
        // every logger LogsCenter hands out is a child of one parent and takes its handlers,
        // so listening there hears the whole of start-up rather than one class of it
        Logger allLoggers = LogsCenter.getLogger(UiManagerTest.class).getParent();
        Handler listener = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel().intValue() >= Level.SEVERE.intValue()) {
                    severe.add(record);
                }
            }

            @Override
            public void flush() {
                // nothing is buffered
            }

            @Override
            public void close() {
                // nothing is held open
            }
        };
        allLoggers.addHandler(listener);

        try {
            FxToolkit.onFxThread(() -> {
                Stage stage = new Stage();
                try {
                    new FatalErrorUiManager().start(stage);
                    return null;
                } finally {
                    stage.hide();
                }
            });
        } finally {
            allLoggers.removeHandler(listener);
        }

        assertEquals(1, severe.size(), "the failure should be reported to the log once");
        assertTrue(severe.get(0).getMessage().contains(UiManager.FATAL_ERROR_TITLE));
        assertTrue(severe.get(0).getMessage().contains(BROKEN_WINDOW_MESSAGE));
    }

    @Test
    public void createDataLoadingErrorAlert_boxedContent_startsItsTextAtOneOffset() throws Exception {
        DataLoadingErrorMessage error = DataLoadingErrorMessage.of(
                "TAB does not have permission to read this file or the folder holding it.");
        UiManager uiManager = new UiManager(null, DATA_FILE_PATH, Optional.of(error));

        double[] textStarts = FxToolkit.onFxThread(() -> {
            Alert alert = uiManager.createDataLoadingErrorAlert(error);
            DialogPane dialogPane = alert.getDialogPane();
            try {
                alert.show();
                dialogPane.applyCss();
                dialogPane.layout();
                return new double[] {
                    renderedTextStart(dialogPane, "#dataLoadingErrorFile"),
                    renderedTextStart(dialogPane, "#dataLoadingErrorDetail"),
                };
            } finally {
                alert.close();
            }
        });

        // measured where the glyphs land, not where the padding says they should: the path box
        // also draws a one-pixel border, which padding alone does not account for
        assertEquals(textStarts[0], textStarts[1], 0.5,
                "both boxes should start their text at the same offset");
    }

    @Test
    public void createDataLoadingErrorAlert_sourceExcerpt_isListedInItsOwnMonospacedBox() throws Exception {
        UiManager uiManager = new UiManager(null, DATA_FILE_PATH, Optional.of(PARSE_ERROR));

        ExcerptDetails details = FxToolkit.onFxThread(() -> {
            Alert alert = uiManager.createDataLoadingErrorAlert(PARSE_ERROR);
            DialogPane dialogPane = alert.getDialogPane();
            try {
                alert.show();
                dialogPane.applyCss();
                dialogPane.layout();
                Label detail = (Label) dialogPane.getContent().lookup("#dataLoadingErrorDetail");
                Label excerpt = (Label) dialogPane.getContent().lookup("#dataLoadingErrorExcerpt");
                Label file = (Label) dialogPane.getContent().lookup("#dataLoadingErrorFile");
                return new ExcerptDetails(detail.getText(), excerpt.getText(),
                        excerpt.getStyleClass().contains("data-loading-error-excerpt"),
                        excerpt.getFont().getFamily(), file.getFont().getFamily(),
                        textWidth("iiii", excerpt.getFont()), textWidth("WWWW", excerpt.getFont()));
            } finally {
                alert.close();
            }
        });

        assertEquals(PARSE_ERROR.description(), details.detailText,
                "the explanation should be shown without the listing");
        assertEquals(PARSE_ERROR.sourceExcerpt(), details.excerptText);
        assertTrue(details.hasExcerptStyle);
        assertEquals(details.fileFontFamily, details.excerptFontFamily,
                "the listing should be written in the font the data file path is written in");
        assertEquals(details.narrowGlyphsWidth, details.wideGlyphsWidth, 0.5,
                "every character of the listing should be the same width, or its columns cannot line up");
    }

    /** Returns the width {@code text} is drawn in at {@code font}. */
    private static double textWidth(String text, Font font) {
        Text measured = new Text(text);
        measured.setFont(font);
        return measured.getLayoutBounds().getWidth();
    }

    /** Returns the scene x at which the control's drawn text begins. */
    private static double renderedTextStart(DialogPane dialogPane, String id) {
        Node control = dialogPane.lookup(id);
        Node text = control.lookup(".text");
        return text.localToScene(text.getBoundsInLocal()).getMinX();
    }

    @Test
    public void createDataLoadingErrorAlert_usesTabThemeAndActionableContent() throws Exception {
        DataLoadingErrorMessage error = PARSE_ERROR;
        UiManager uiManager = new UiManager(null, DATA_FILE_PATH, Optional.of(error));

        AlertDetails details = FxToolkit.onFxThread(() -> {
            Alert alert = uiManager.createDataLoadingErrorAlert(error);
            try {
                alert.show();
                DialogPane dialogPane = alert.getDialogPane();
                dialogPane.applyCss();
                dialogPane.layout();
                Label file = (Label) dialogPane.getContent().lookup("#dataLoadingErrorFile");
                Label errorDetail = (Label) dialogPane.getContent().lookup("#dataLoadingErrorDetail");
                Label recovery = (Label) dialogPane.getContent().lookup("#dataLoadingErrorRecovery");
                Button closeButton = (Button) dialogPane.lookupButton(ButtonType.OK);
                Text renderedCloseButtonText = (Text) closeButton.lookup(".text");
                return new AlertDetails(alert.getTitle(), alert.getHeaderText(),
                        dialogPane.getStyleClass().contains("data-loading-error-dialog"),
                        file.getText(), errorDetail.getText(), errorDetail.isWrapText(),
                        errorDetail.getMinHeight(), errorDetail.getHeight(),
                        errorDetail.prefHeight(errorDetail.getWidth()), recovery.getText(),
                        dialogPane.getContent().lookupAll(".data-loading-error-unchanged").isEmpty(),
                        closeButton.getText(), renderedCloseButtonText.getText(),
                        ButtonBar.isButtonUniformSize(closeButton), closeButton.getWidth(), closeButton.prefWidth(-1),
                        alert.getGraphic() != null);
            } finally {
                alert.close();
            }
        });

        assertEquals(UiManager.DATA_LOADING_ERROR_TITLE, details.title);
        assertEquals(UiManager.DATA_LOADING_ERROR_HEADER, details.header);
        assertTrue(details.hasTabStyle);
        assertEquals(DATA_FILE_PATH.toString(), details.fileText);
        assertEquals(error.description(), details.errorText);
        assertTrue(details.isErrorWrapped);
        assertEquals(Region.USE_PREF_SIZE, details.errorMinHeight);
        assertTrue(details.errorHeight >= details.errorPreferredHeight - 0.5,
                "the error message should be tall enough to show every wrapped line");
        assertEquals(UiManager.DATA_LOADING_ERROR_RECOVERY, details.recoveryText);
        assertTrue(details.isUnchangedMessageAbsent);
        assertEquals(UiManager.DATA_LOADING_ERROR_CLOSE, details.closeButtonText);
        assertEquals(UiManager.DATA_LOADING_ERROR_CLOSE, details.renderedCloseButtonText);
        assertFalse(details.isCloseButtonUniform);
        assertTrue(details.closeButtonWidth >= details.closeButtonPreferredWidth - 0.5,
                "the close button should be wide enough to show its whole label");
        assertTrue(details.hasGraphic);
    }

    @Test
    public void createDataLoadingErrorAlert_messageTallerThanScreen_fitsScreenAndScrolls() throws Exception {
        ScrollDetails details = FxToolkit.onFxThread(() -> {
            DataLoadingErrorMessage error = overflowingError();
            return measureScrolling(new UiManager(null, DATA_FILE_PATH, Optional.of(error)), error);
        });

        // the dialog cannot be made shorter than the parts it draws around its message, its
        // border, and the least room a message is ever given, however short the screen is
        double irreducibleHeight = details.dialogHeight - details.viewportHeight
                + UiManager.MIN_MESSAGE_HEIGHT + details.dialogBorderHeight;
        assertTrue(details.dialogHeight
                        <= Math.max(details.screenCapHeight, irreducibleHeight) + MEASUREMENT_TOLERANCE,
                "the dialog should be no taller than the share of the screen it may fill, unless it"
                        + " cannot be made that short; " + details);
        assertTrue(details.dialogHeight <= Math.max(details.screenHeight, irreducibleHeight),
                "the dialog should fit the usable screen unless it cannot be made that short; "
                        + details);
        assertTrue(details.hasScrollBar, "the diagnostic should be given a bar to scroll it by; " + details);
        assertTrue(details.isFitToWidth,
                "the diagnostic should wrap to the dialog rather than scroll sideways; " + details);
        assertTrue(details.contentHeight > details.viewportHeight,
                "this message should be too long for the capped dialog, or the test proves nothing; "
                        + details);
        assertTrue(details.lastLineBottom <= details.viewportHeight + MEASUREMENT_TOLERANCE,
                "scrolling to the end should bring the last line of the diagnostic into view; " + details);
    }

    @Test
    public void createDataLoadingErrorAlert_shortMessage_fitsWithoutScrolling() throws Exception {
        UiManager uiManager = new UiManager(null, DATA_FILE_PATH, Optional.of(PARSE_ERROR));

        ScrollDetails details = FxToolkit.onFxThread(() -> measureScrolling(uiManager, PARSE_ERROR));

        // the dialog's own parts take what the message does not, so this is the height the whole
        // dialog would want; a screen that cannot give it that has nothing to say about a message
        // being shown whole, since every message is scrolled on such a screen
        double wantedHeight = details.dialogHeight - details.viewportHeight + details.contentPreferredHeight;
        assumeTrue(wantedHeight <= details.screenCapHeight,
                "the screen is too short to show even a short message whole; " + details);
        assertFalse(details.hasScrollBar,
                "a message this short should be read without a scroll bar; " + details);
        assertTrue(details.viewportHeight >= details.contentHeight - MEASUREMENT_TOLERANCE,
                "a message this short should be shown whole; " + details);
        assertEquals(details.contentPreferredHeight, details.contentHeight, MEASUREMENT_TOLERANCE,
                "a message this short should be given the room it asks for, and no more; " + details);
    }

    /**
     * Returns a message whose explanation is longer than the screen is tall, on whatever screen
     * the test runs, so that the dialog cannot show all of it at once.
     */
    private static DataLoadingErrorMessage overflowingError() {
        double screenHeight = Screen.getPrimary().getVisualBounds().getHeight();
        int lines = (int) Math.ceil(screenHeight / MIN_TEXT_LINE_HEIGHT);
        return DataLoadingErrorMessage.of(IntStream.rangeClosed(1, lines)
                .mapToObj(line -> "The data is not in the expected JSON format at line " + line + ".")
                .collect(Collectors.joining("\n")));
    }

    /** Shows {@code error}, scrolls its dialog to the end and reports what was measured there. */
    private static ScrollDetails measureScrolling(UiManager uiManager, DataLoadingErrorMessage error) {
        Alert alert = uiManager.createDataLoadingErrorAlert(error);
        DialogPane dialogPane = alert.getDialogPane();
        try {
            alert.show();
            dialogPane.applyCss();
            dialogPane.layout();
            ScrollPane scroller = (ScrollPane) dialogPane.getContent();
            scroller.setVvalue(scroller.getVmax());
            dialogPane.layout();
            Region content = (Region) scroller.getContent();
            Node lastLine = content.lookup("#dataLoadingErrorRecovery");
            double lastLineBottom = scroller.sceneToLocal(lastLine.localToScene(lastLine.getBoundsInLocal()))
                    .getMaxY();
            double borderHeight = dialogPane.getInsets().getTop() + dialogPane.getInsets().getBottom();
            return new ScrollDetails(dialogPane.getHeight(), borderHeight,
                    Screen.getPrimary().getVisualBounds().getHeight(), UiManager.maxDialogHeight(),
                    scroller.getViewportBounds().getHeight(), content.getHeight(),
                    content.prefHeight(content.getWidth()), lastLineBottom, scroller.isFitToWidth(),
                    isVerticalScrollBarShown(scroller));
        } finally {
            alert.close();
        }
    }

    /** Returns whether {@code scroller} has a vertical scroll bar on display. */
    private static boolean isVerticalScrollBarShown(ScrollPane scroller) {
        return scroller.lookupAll(".scroll-bar").stream()
                .anyMatch(node -> node instanceof ScrollBar bar
                        && bar.getOrientation() == Orientation.VERTICAL && bar.isVisible());
    }

    private record ScrollDetails(double dialogHeight, double dialogBorderHeight, double screenHeight,
                                 double screenCapHeight, double viewportHeight, double contentHeight,
                                 double contentPreferredHeight, double lastLineBottom,
                                 boolean isFitToWidth, boolean hasScrollBar) {}

    private record StartOutcome(boolean wasDataLoadingErrorShown, DataLoadingErrorMessage shownError,
                                boolean wasMainWindowBuilt, boolean wasIconSet,
                                boolean isAnyWindowShowing) {}

    private record ExcerptDetails(String detailText, String excerptText, boolean hasExcerptStyle,
                                  String excerptFontFamily, String fileFontFamily,
                                  double narrowGlyphsWidth, double wideGlyphsWidth) {}

    private record FatalErrorDetails(boolean wasAlertCreated, boolean isOwnedByPrimaryStage,
                                     boolean isAlertOwnerPrimaryStage, boolean wasAlertClosed,
                                     boolean wasShutDown, String headerText) {}

    private record AlertDetails(String title, String header, boolean hasTabStyle,
                                String fileText, String errorText, boolean isErrorWrapped,
                                double errorMinHeight, double errorHeight,
                                double errorPreferredHeight, String recoveryText,
                                boolean isUnchangedMessageAbsent, String closeButtonText,
                                String renderedCloseButtonText, boolean isCloseButtonUniform, double closeButtonWidth,
                                double closeButtonPreferredWidth, boolean hasGraphic) {}

    private static class LogicStub implements Logic {
        private final ObservableList<Student> students = FXCollections.observableArrayList();

        @Override
        public CommandResult execute(String commandText) {
            return new CommandResult(commandText);
        }

        @Override
        public boolean isAwaitingConfirmation() {
            return false;
        }

        @Override
        public ObservableList<Student> getFilteredStudentList() {
            return students;
        }

        @Override
        public GuiSettings getGuiSettings() {
            return new GuiSettings();
        }

        @Override
        public void setGuiSettings(GuiSettings guiSettings) {
            // This UI test never changes the window settings.
        }
    }

    /**
     * A {@code Logic} whose window settings cannot be read, which is what makes the
     * {@code MainWindow} constructor throw.
     */
    private static class FailingLogicStub extends LogicStub {
        @Override
        public GuiSettings getGuiSettings() {
            throw new IllegalStateException(BROKEN_WINDOW_MESSAGE);
        }
    }

    /**
     * A {@code UiManager} that closes its fatal-error dialog for the test and records the
     * shutdown instead of ending the test run with it.
     */
    private static class FatalErrorUiManager extends UiManager {
        private Alert alert;
        private Stage owner;
        private String headerText;
        private boolean wasShutDown;

        FatalErrorUiManager() {
            super(new FailingLogicStub(), DATA_FILE_PATH);
        }

        @Override
        Alert createFatalErrorAlert(Stage owner, String title, Throwable e) {
            this.owner = owner;
            alert = super.createFatalErrorAlert(owner, title, e);
            headerText = alert.getHeaderText();
            Platform.runLater(alert::close);
            return alert;
        }

        @Override
        void shutdown() {
            wasShutDown = true;
        }
    }

    private static class AutoClosingUiManager extends UiManager {
        private Alert alert;

        AutoClosingUiManager() {
            super(null, DATA_FILE_PATH, Optional.of(DataLoadingErrorMessage.of("Invalid data")));
        }

        @Override
        Alert createDataLoadingErrorAlert(DataLoadingErrorMessage error) {
            alert = super.createDataLoadingErrorAlert(error);
            Platform.runLater(alert::close);
            return alert;
        }
    }

    /**
     * A {@code UiManager} that records the data loading error it reports instead of putting a
     * dialog on screen. Its {@code Logic} is one a main window could be built from, so that a
     * main window the test does not expect would be built rather than fail on the way.
     */
    private static class UiManagerStub extends UiManager {
        private boolean wasDataLoadingErrorShown;
        private DataLoadingErrorMessage shownError;

        UiManagerStub(DataLoadingErrorMessage error) {
            super(new LogicStub(), DATA_FILE_PATH, Optional.of(error));
        }

        @Override
        void showDataLoadingErrorDialogAndWait(DataLoadingErrorMessage error) {
            wasDataLoadingErrorShown = true;
            shownError = error;
        }
    }
}
