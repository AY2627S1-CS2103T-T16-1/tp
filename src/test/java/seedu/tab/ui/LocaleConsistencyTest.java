package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import seedu.tab.commons.exceptions.DataLoadingException;
import seedu.tab.logic.parser.StudentBookParser;
import seedu.tab.model.ModelManager;
import seedu.tab.storage.JsonStudentBookStorage;
import seedu.tab.testutil.StudentBuilder;

/** Tests English feedback and option styles under regional defaults. */
public class LocaleConsistencyTest {
    private static final List<String> LOCALES = List.of("en-SG", "tr-TR", "az-AZ", "fa-IR", "ar-EG", "bn-BD");

    @TempDir
    public Path temporaryFolder;

    @BeforeAll
    public static void startToolkit() throws Exception {
        FxToolkit.start();
    }

    @Test
    public void regionalDefaults_countsAndDiagnostics_keepAsciiDigits() throws Exception {
        withRegionalDefaults(() -> {
            for (int count : new int[] {0, 1, 24}) {
                ModelManager model = new ModelManager();
                for (int index = 0; index < count; index++) {
                    model.addStudent(new StudentBuilder().withName("Priya " + index).withoutNusStudentId().build());
                }
                StudentBookParser parser = new StudentBookParser();
                String total = count + (count == 1 ? " student" : " students");
                assertEquals("Listed " + total + ".", parser.parseCommand("list").execute(model).getFeedbackToUser());
                assertEquals("Found " + total + ".",
                        parser.parseCommand("find -n Priya").execute(model).getFeedbackToUser());
                assertEquals(count == 0 ? "The student book is already empty. Nothing to clear."
                        : "Clear all " + total + "? This cannot be undone. [y/N]",
                        parser.parseCommand("clear").execute(model).getFeedbackToUser());
                assertEquals(count, model.getStudentBook().getStudentList().size());
                assertEquals(total, SummaryPanel.describeTotal(count));
            }
            assertMalformedRecords();
        });
    }

    private void assertMalformedRecords() throws Exception {
        assertRejectedSource("{\"students\":[null]}", "Student record 1 is missing.");
        assertRejectedSource("{\"students\":[{}]}", "Student record 1: Name field is missing.");
        String student = "{\"name\":\"Priya\",\"phone\":\"91234567\",\"nusStudentId\":\"A1234\"}";
        assertRejectedSource("{\"students\":[" + student + "," + student + "]}",
                "Student record 2 (\"Priya\"): NUS Student ID A1234 is already used by "
                        + "student record 1 (\"Priya\").");
        String longEmail = "x".repeat(255);
        assertRejectedSource("{\"students\":[{\"name\":\"Priya\",\"phone\":\"91234567\",\"email\":\""
                + longEmail + "\"}]}", "Student record 1 (\"Priya\"): Email field is too long to read safely: "
                + "it holds 255 characters, but saved emails may hold at most 254");
    }

    private void assertRejectedSource(String source, String expectedMessage) throws Exception {
        Path path = temporaryFolder.resolve("invalid.json");
        Files.writeString(path, source);
        DataLoadingException exception = assertThrows(
                DataLoadingException.class, () -> new JsonStudentBookStorage(path).readStudentBook());
        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(source, Files.readString(path));
    }

    @Test
    public void regionalDefaults_optionStyles_matchTheTheme() throws Exception {
        withRegionalDefaults(() -> {
            FxToolkit.onFxThread(() -> {
                CommandBox box = new CommandBox(unused -> null);
                new Scene(box.getRoot(), 900, 60).getStylesheets().add(
                        UiPart.class.getResource("/view/DarkTheme.css").toExternalForm());
                String command = "add Priya -p 91234567 --follow-up";
                TextField field = (TextField) box.getRoot().lookup("#commandTextField");
                field.setText(command);
                box.getRoot().applyCss();
                TextFlow flow = (TextFlow) box.getRoot().lookup("#highlighted");
                List<Text> options = flow.getChildren().stream().map(Text.class::cast)
                        .filter(text -> text.getText().startsWith("-")).toList();
                assertEquals(List.of("-p", "--follow-up"), options.stream().map(Text::getText).toList());
                for (Text option : options) {
                    assertTrue(option.getStyleClass().contains("command-option"), option.getStyleClass().toString());
                    assertEquals(Color.web("#cab4c7"), option.getFill());
                }
                assertEquals(command, flow.getChildren().stream().map(Text.class::cast)
                        .map(Text::getText).reduce("", String::concat));
                return null;
            });
        });
    }

    private static void withRegionalDefaults(RegionalAssertion assertion) throws Exception {
        Locale general = Locale.getDefault();
        Locale display = Locale.getDefault(Locale.Category.DISPLAY);
        Locale format = Locale.getDefault(Locale.Category.FORMAT);
        try {
            for (String tag : LOCALES) {
                Locale.setDefault(Locale.forLanguageTag(tag));
                assertion.run();
            }
        } finally {
            Locale.setDefault(general);
            Locale.setDefault(Locale.Category.DISPLAY, display);
            Locale.setDefault(Locale.Category.FORMAT, format);
        }
    }

    @FunctionalInterface
    private interface RegionalAssertion {
        void run() throws Exception;
    }
}
