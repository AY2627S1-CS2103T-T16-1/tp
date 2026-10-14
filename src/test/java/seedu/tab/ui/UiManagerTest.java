package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.Window;
import seedu.tab.commons.core.GuiSettings;
import seedu.tab.logic.Logic;
import seedu.tab.logic.commands.CommandResult;
import seedu.tab.model.student.Student;

public class UiManagerTest {

    private static final Path DATA_FILE_PATH = Paths.get("data", "tab.json");

    @BeforeAll
    public static void startToolkit() throws Exception {
        FxToolkit.start();
    }

    @Test
    public void start_dataLoadingError_showsErrorInsteadOfStartingMainWindow() {
        UiManagerStub uiManager = new UiManagerStub("Student record 2 has an invalid Phone field.");

        uiManager.start(null);

        assertTrue(uiManager.wasDataLoadingErrorShown);
        assertEquals("Student record 2 has an invalid Phone field.", uiManager.shownError);
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
            uiManager.showDataLoadingErrorDialogAndWait("Invalid data");
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
    public void createDataLoadingErrorAlert_usesTabThemeAndActionableContent() throws Exception {
        String error = "Student record 4 (\"Yun\"): Phone \"09\" is not valid: "
                + "a phone number needs at least 3 digits";
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
        assertEquals(error, details.errorText);
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

    private static class AutoClosingUiManager extends UiManager {
        private Alert alert;

        AutoClosingUiManager() {
            super(null, DATA_FILE_PATH, Optional.of("Invalid data"));
        }

        @Override
        Alert createDataLoadingErrorAlert(String error) {
            alert = super.createDataLoadingErrorAlert(error);
            Platform.runLater(alert::close);
            return alert;
        }
    }

    private static class UiManagerStub extends UiManager {
        private boolean wasDataLoadingErrorShown;
        private String shownError;

        UiManagerStub(String error) {
            super(null, DATA_FILE_PATH, Optional.of(error));
        }

        @Override
        void showDataLoadingErrorDialogAndWait(String error) {
            wasDataLoadingErrorShown = true;
            shownError = error;
        }
    }
}
