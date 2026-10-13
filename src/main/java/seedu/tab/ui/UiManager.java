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
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import seedu.tab.MainApp;
import seedu.tab.commons.core.LogsCenter;
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
        primaryStage.getIcons().add(getImage(ICON_APPLICATION));

        try {
            mainWindow = new MainWindow(primaryStage, logic, dataFilePath);
            mainWindow.show(); //This should be called before creating other UI parts
            mainWindow.fillInnerParts();

        } catch (Throwable e) {
            logger.severe(StringUtil.getDetails(e));
            showFatalErrorDialogAndShutdown("Fatal error during initializing", e);
        }
    }

    private Image getImage(String imagePath) {
        return new Image(MainApp.class.getResourceAsStream(imagePath));
    }

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

    private static Alert createAlert(Stage owner, AlertType type, String title, String headerText) {
        Alert alert = new Alert(type);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(UiManager.class.getResource("/view/DarkTheme.css").toExternalForm());
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        dialogPane.setId(ALERT_DIALOG_PANE_FIELD_ID);
        alert.setOnShown(event -> UiFonts.applyTo(dialogPane.getScene()));
        return alert;
    }

    void showDataLoadingErrorDialogAndWait(String error) {
        createDataLoadingErrorAlert(error).showAndWait();
    }

    Alert createDataLoadingErrorAlert(String error) {
        Alert alert = createAlert(null, Alert.AlertType.ERROR,
                DATA_LOADING_ERROR_TITLE, DATA_LOADING_ERROR_HEADER);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStyleClass().add("data-loading-error-dialog");
        dialogPane.setMinHeight(Region.USE_PREF_SIZE);
        dialogPane.setContent(createDataLoadingErrorContent(error));

        Label icon = new Label("!");
        icon.getStyleClass().add("data-loading-error-icon");
        icon.setAccessibleText("Error");
        alert.setGraphic(icon);

        Button closeButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        closeButton.setText(DATA_LOADING_ERROR_CLOSE);
        ButtonBar.setButtonUniformSize(closeButton, false);
        closeButton.setMinWidth(Region.USE_PREF_SIZE);
        return alert;
    }

    private VBox createDataLoadingErrorContent(String error) {
        Label fileCaption = createWrappingLabel(DATA_LOADING_ERROR_FILE_LABEL,
                "data-loading-error-file-caption");
        Label filePath = createWrappingLabel(dataFilePath.toString(), "data-loading-error-file");
        filePath.setId("dataLoadingErrorFile");

        Label errorDetail = createWrappingLabel(error, "data-loading-error-detail");
        errorDetail.setId("dataLoadingErrorDetail");

        Label recovery = createWrappingLabel(DATA_LOADING_ERROR_RECOVERY,
                "data-loading-error-recovery");
        recovery.setId("dataLoadingErrorRecovery");

        VBox content = new VBox(fileCaption, filePath, errorDetail, recovery);
        content.getStyleClass().add("data-loading-error-content");
        content.setMinHeight(Region.USE_PREF_SIZE);
        return content;
    }

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
     * Shows an error alert dialog with {@code title} and error message, {@code e},
     * and exits the application after the user has closed the alert dialog.
     */
    private void showFatalErrorDialogAndShutdown(String title, Throwable e) {
        logger.severe(title + " " + e.getMessage() + StringUtil.getDetails(e));
        showAlertDialogAndWait(Alert.AlertType.ERROR, title, e.getMessage(), e.toString());
        Platform.exit();
        System.exit(1);
    }

}
