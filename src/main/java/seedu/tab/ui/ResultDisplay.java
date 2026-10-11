package seedu.tab.ui;

import static java.util.Objects.requireNonNull;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;

/**
 * A UI component that displays the result of a command execution.
 */
public class ResultDisplay extends UiPart<Region> {

    private static final String FXML = "ResultDisplay.fxml";

    /**
     * The tallest the area grows before it starts scrolling, so that a long reply cannot push
     * the student list off the window.
     */
    private static final double MAX_HEIGHT = 120;

    /** What the text sits in, above and below, inside the area. */
    private static final double VERTICAL_PADDING = 10;

    @FXML
    private TextArea resultDisplay;

    /**
     * Creates a {@code ResultDisplay}, which stays out of the window until it has something to
     * report.
     */
    public ResultDisplay() {
        super(FXML);
        fitToText("");
    }

    public void setFeedbackToUser(String feedbackToUser) {
        requireNonNull(feedbackToUser);
        resultDisplay.setText(feedbackToUser);
        fitToText(feedbackToUser);
    }

    /**
     * Sizes the area to what it has to say, and takes it out of the window when that is
     * nothing.
     *
     * <p>A text area reserves room for a whole number of rows whatever it holds, which left a
     * band of empty space under the command line. The height is taken from the text once it
     * has been laid out instead, so the area is as tall as its content wrapped at the width it
     * actually has. Past {@link #MAX_HEIGHT} it scrolls rather than growing, and it stays a
     * text area rather than becoming a label so that a reply too long to show at once can
     * still be scrolled to and selected.
     */
    private void fitToText(String text) {
        boolean hasSomethingToSay = !text.isBlank();
        getRoot().setManaged(hasSomethingToSay);
        getRoot().setVisible(hasSomethingToSay);
        if (!hasSomethingToSay) {
            return;
        }
        // the wrapped height is only known once the skin has laid the text out
        Platform.runLater(this::matchHeightToText);
    }

    /**
     * Sets the area's height to the height of the text now laid out in it.
     */
    private void matchHeightToText() {
        Node text = resultDisplay.lookup(".text");
        if (text == null) {
            return;
        }
        double wanted = text.getBoundsInLocal().getHeight() + VERTICAL_PADDING;
        resultDisplay.setPrefHeight(Math.min(wanted, MAX_HEIGHT));
    }

}
