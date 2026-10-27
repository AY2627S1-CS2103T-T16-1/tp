package seedu.tab.ui;

import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import seedu.tab.commons.core.LogsCenter;
import seedu.tab.commons.util.AppUtil;
import seedu.tab.commons.util.StringUtil;
import seedu.tab.logic.Logic;

/**
 * The manager of the UI component.
 */
public class UiManager implements Ui {

    public static final String ALERT_DIALOG_PANE_FIELD_ID = "alertDialogPane";
    static final String DATA_LOADING_ERROR_TITLE = "TAB — Unable to load saved data";
    static final String DATA_LOADING_ERROR_HEADER = "TAB could not read your saved data";
    static final String DATA_LOADING_ERROR_FILE_LABEL = "DATA FILE";
    static final String DATA_LOADING_ERROR_RECOVERY =
            "Repair or replace it, then restart TAB.";
    static final String DATA_LOADING_ERROR_CLOSE = "Close TAB";
    static final String FATAL_ERROR_TITLE = "Fatal error during initializing";

    private static final String STYLESHEET_RESOURCE = "/view/DarkTheme.css";
    private static final String DATA_LOADING_ERROR_DIALOG_STYLE = "data-loading-error-dialog";
    private static final String DATA_LOADING_ERROR_ICON_STYLE = "data-loading-error-icon";
    private static final String ERROR_ACCESSIBLE_TEXT = "Error";
    private static final String DATA_LOADING_FILE_CAPTION_STYLE = "data-loading-error-file-caption";
    private static final String DATA_LOADING_FILE_STYLE = "data-loading-error-file";
    private static final String DATA_LOADING_DETAIL_STYLE = "data-loading-error-detail";
    private static final String DATA_LOADING_RECOVERY_STYLE = "data-loading-error-recovery";
    private static final String DATA_LOADING_CONTENT_STYLE = "data-loading-error-content";
    private static final String DATA_LOADING_FILE_ID = "dataLoadingErrorFile";
    private static final String DATA_LOADING_DETAIL_ID = "dataLoadingErrorDetail";
    private static final String DATA_LOADING_RECOVERY_ID = "dataLoadingErrorRecovery";


    private static final Logger logger = LogsCenter.getLogger(UiManager.class);
    private static final String ICON_APPLICATION = "/images/address_book_32.png";

    private Logic logic;
    private Path dataFilePath;
    private final Optional<String> studentBookLoadingError;
    private MainWindow mainWindow;

    /**
     * Creates a {@code UiManager} with the given {@code Logic} and the data file path
     * to show in the status bar.
     */
    public UiManager(Logic logic, Path dataFilePath) {
        this(logic, dataFilePath, Optional.empty());
    }

    /**
     * Creates a {@code UiManager} that reports {@code studentBookLoadingError} instead of opening
     * the main window when the saved student book could not be read.
     */
    public UiManager(Logic logic, Path dataFilePath, Optional<String> studentBookLoadingError) {
        this.logic = logic;
        this.dataFilePath = dataFilePath;
        this.studentBookLoadingError = studentBookLoadingError;
    }

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting UI...");

        if (studentBookLoadingError.isPresent()) {
            showDataLoadingErrorDialogAndWait(studentBookLoadingError.get());
            return;
        }

        //Set the application icon.
        primaryStage.getIcons().add(AppUtil.getImage(ICON_APPLICATION));

        try {
            mainWindow = new MainWindow(primaryStage, logic, dataFilePath);
            mainWindow.show(); //This should be called before creating other UI parts
            mainWindow.fillInnerParts();

        } catch (Throwable e) {
            showFatalErrorDialogAndShutdown(primaryStage, FATAL_ERROR_TITLE, e);
        }
    }

    /**
     * Shows a modal alert owned by the main window.
     */
    void showAlertDialogAndWait(Alert.AlertType type, String title, String headerText, String contentText) {
        showAlertDialogAndWait(mainWindow.getPrimaryStage(), type, title, headerText, contentText);
    }

    /**
     * Shows an alert dialog on {@code owner} with the given parameters.
     * This method only returns after the user has closed the alert dialog.
     */
    private static void showAlertDialogAndWait(Stage owner, AlertType type, String title, String headerText,
                                               String contentText) {
        Alert alert = createAlert(owner, type, title, headerText);
        alert.setContentText(contentText);
        alert.showAndWait();
    }

    /**
     * Creates a styled alert, optionally owned by the given stage.
     */
    static Alert createAlert(Stage owner, AlertType type, String title, String headerText) {
        Alert alert = new Alert(type);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(UiManager.class.getResource(STYLESHEET_RESOURCE).toExternalForm());
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        dialogPane.setId(ALERT_DIALOG_PANE_FIELD_ID);
        alert.setOnShown(event -> UiFonts.applyTo(dialogPane.getScene()));
        return alert;
    }

    /**
     * Shows the startup data error before the main window can expose incomplete records.
     */
    void showDataLoadingErrorDialogAndWait(String error) {
        createDataLoadingErrorAlert(error).showAndWait();
    }

    /**
     * Creates a data-loading alert with the file location and recovery instructions.
     */
    Alert createDataLoadingErrorAlert(String error) {
        Alert alert = createAlert(null, Alert.AlertType.ERROR,
                DATA_LOADING_ERROR_TITLE, DATA_LOADING_ERROR_HEADER);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStyleClass().add(DATA_LOADING_ERROR_DIALOG_STYLE);
        dialogPane.setMinHeight(Region.USE_PREF_SIZE);
        dialogPane.setContent(createDataLoadingErrorContent(error));

        Label icon = new Label("!");
        icon.getStyleClass().add(DATA_LOADING_ERROR_ICON_STYLE);
        icon.setAccessibleText(ERROR_ACCESSIBLE_TEXT);
        alert.setGraphic(icon);

        Button closeButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        closeButton.setText(DATA_LOADING_ERROR_CLOSE);
        ButtonBar.setButtonUniformSize(closeButton, false);
        closeButton.setMinWidth(Region.USE_PREF_SIZE);
        return alert;
    }

    /**
     * Builds the file, error and recovery sections of the startup alert.
     */
    private VBox createDataLoadingErrorContent(String error) {
        Label fileCaption = createWrappingLabel(DATA_LOADING_ERROR_FILE_LABEL,
                DATA_LOADING_FILE_CAPTION_STYLE);
        Label filePath = createWrappingLabel(dataFilePath.toString(), DATA_LOADING_FILE_STYLE);
        filePath.setId(DATA_LOADING_FILE_ID);

        Label errorDetail = createWrappingLabel(error, DATA_LOADING_DETAIL_STYLE);
        errorDetail.setId(DATA_LOADING_DETAIL_ID);

        Label recovery = createWrappingLabel(DATA_LOADING_ERROR_RECOVERY,
                DATA_LOADING_RECOVERY_STYLE);
        recovery.setId(DATA_LOADING_RECOVERY_ID);

        VBox content = new VBox(fileCaption, filePath, errorDetail, recovery);
        content.getStyleClass().add(DATA_LOADING_CONTENT_STYLE);
        content.setMinHeight(Region.USE_PREF_SIZE);
        return content;
    }

    /**
     * Creates a styled label that wraps long file paths and error descriptions.
     */
    private static Label createWrappingLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setMinWidth(0);
        label.setMinHeight(Region.USE_PREF_SIZE);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setWrapText(true);
        return label;
    }

    /**
     * Shows an error alert dialog on {@code owner} with {@code title} and error message,
     * {@code e}, and exits the application after the user has closed the alert dialog.
     */
    private void showFatalErrorDialogAndShutdown(Stage owner, String title, Throwable e) {
        logger.severe(title + ": " + StringUtil.getDetails(e));
        createFatalErrorAlert(owner, title, e).showAndWait();
        shutdown();
    }

    /**
     * Returns the dialog that reports {@code e} as having stopped TAB from starting.
     *
     * <p>Its owner is the stage TAB was started with rather than the main window, because the
     * main window failing to be built is one of the failures that leads here, and a dialog owned
     * by a window that was never built cannot be shown.
     */
    Alert createFatalErrorAlert(Stage owner, String title, Throwable e) {
        Alert alert = createAlert(owner, AlertType.ERROR, title, e.getMessage());
        alert.setContentText(e.toString());
        return alert;
    }

    /**
     * Stops TAB, reporting to whatever started it that the run failed.
     */
    void shutdown() {
        Platform.exit();
        System.exit(1);
    }

}
