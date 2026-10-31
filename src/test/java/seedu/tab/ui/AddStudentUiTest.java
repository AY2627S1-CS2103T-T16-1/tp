package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import seedu.tab.commons.exceptions.DataLoadingException;
import seedu.tab.logic.LogicManager;
import seedu.tab.logic.commands.AddCommand;
import seedu.tab.model.Model;
import seedu.tab.model.ModelManager;
import seedu.tab.model.ReadOnlyStudentBook;
import seedu.tab.model.StudentBook;
import seedu.tab.model.student.Student;
import seedu.tab.storage.JsonStudentBookStorage;
import seedu.tab.storage.JsonUserPrefsStorage;
import seedu.tab.storage.StorageManager;
import seedu.tab.testutil.StudentBuilder;
import seedu.tab.testutil.StudentUtil;

public class AddStudentUiTest {
    private static final int STUDENTS = 40;
    private static final double WINDOW_HEIGHT = 600;

    // the shortest the window goes, as MainWindow.fxml sets it
    private static final double MINIMUM_WINDOW_HEIGHT = 400;

    // the pulses a card has to spend in view before its geometry is taken as settled
    private static final int PULSES_IN_VIEW = 16;

    @TempDir
    public Path testFolder;

    private final Model model = new ModelManager();
    private int pulsesInView;
    private boolean isSaveFailing;
    private JsonStudentBookStorage storage;
    private MainWindow window;
    private TextField field;
    private ListView<?> list;

    @BeforeAll
    public static void startToolkit() throws Exception {
        FxToolkit.start();
    }

    @Test
    public void add_overflowingLists_revealsSavedCardAndPreservesFocusAndSearchRules() throws Exception {
        for (String search : List.of("list", "find -t T1", "find -t T2", "find -t Missing", "find -t Empty")) {
            FxToolkit.onFxThread(() -> {
                createWindow();
                enter(search);
                list.scrollTo(0);
                return null;
            });
            try {
                FxToolkit.awaitAssertion(() -> assertVisible(model.getFilteredStudentList().isEmpty()
                        ? null : model.getFilteredStudentList().getFirst()));
                Predicate<? super Student> predicate = model.getFilteredStudentPredicate();
                FxToolkit.onFxThread(() -> {
                    enter("add Newly Added Student -p 91234567 -t "
                            + (search.equals("find -t Missing") ? "Missing" : "T1"));
                    return null;
                });
                Student added = model.getStudentBook().getStudentList().getLast();
                FxToolkit.awaitAssertion(() -> {
                    assertVisible(added);
                    assertSame(field, window.getPrimaryStage().getScene().getFocusOwner());
                });
                FxToolkit.onFxThread(() -> {
                    assertSavedStudent(added, search, predicate);
                    return null;
                });
            } finally {
                closeWindow();
            }
        }
    }

    @Test
    public void add_cardWhoseDetailsWrap_keepsTheWholeCardInView() throws Exception {
        assertWrappingCardStaysInView(STUDENTS, WINDOW_HEIGHT);
    }

    @Test
    public void add_cardWhoseDetailsWrapInAShortWindow_keepsTheWholeCardInView() throws Exception {
        assertWrappingCardStaysInView(10, MINIMUM_WINDOW_HEIGHT);
    }

    /**
     * Adds a student whose contact details wrap and requires the whole card to settle in view.
     *
     * <p>A card grows after the scroll that revealed it, once its contact row has a width to
     * wrap against. A reveal that stopped before that growth left the bottom of the card under
     * the edge of the list, where nothing scrolled to it again. In a window of the minimum
     * height the card was wholly in view for one pulse before it grew, so the arrival in view
     * is not enough to ask for; it has to stay.
     */
    private void assertWrappingCardStaysInView(int students, double height) throws Exception {
        Student added = FxToolkit.onFxThread(() -> {
            createWindow(students, height);
            enter("add Zed Probe -p 91234567 -t T1"
                    + " -e muhammad.haziq.bin.abdul.rahman.shah.the.second@alumni.example.edu.sg");
            return model.getStudentBook().getStudentList().getLast();
        });
        try {
            pulsesInView = 0;
            FxToolkit.awaitAssertion(() -> assertStaysInView(added));
        } finally {
            closeWindow();
        }
    }

    @Test
    public void add_equalRecord_revealsNewCardRatherThanEarlierEqualCard() throws Exception {
        Student added = FxToolkit.onFxThread(() -> {
            createWindow();
            Student existing = model.getFilteredStudentList().get(1);
            enter(StudentUtil.getAddCommand(existing));
            Student saved = model.getStudentBook().getStudentList().getLast();
            assertEquals(existing, saved);
            assertNotSame(existing, saved);
            return saved;
        });
        try {
            FxToolkit.awaitAssertion(() -> {
                assertVisible(added);
                assertSame(field, window.getPrimaryStage().getScene().getFocusOwner());
            });
        } finally {
            closeWindow();
        }
    }

    @Test
    public void add_targetFilteredBeforeLayout_revealsNextSavedStudent() throws Exception {
        Student added = FxToolkit.onFxThread(() -> {
            createWindow();
            enter("add First Addition -p 91234567 -t T1");
            enter("find -t T2");
            var predicate = model.getFilteredStudentPredicate();
            enter("add Second Addition -p 91234568 -t T2");
            assertSame(predicate, model.getFilteredStudentPredicate());
            assertEquals(42, model.getStudentBook().getStudentList().size());
            assertEquals(21, list.getItems().size());
            return model.getStudentBook().getStudentList().getLast();
        });
        try {
            FxToolkit.awaitAssertion(() -> {
                assertVisible(added);
                assertSame(field, window.getPrimaryStage().getScene().getFocusOwner());
            });
        } finally {
            closeWindow();
        }
    }

    private void assertSavedStudent(Student added, String search, Predicate<? super Student> predicate)
            throws IOException, DataLoadingException {
        assertEquals(added, storage.readStudentBook().orElseThrow().getStudentList().getLast());
        if (search.equals("find -t T1") || search.equals("list")
                || search.equals("find -t Missing")) {
            assertSame(predicate, model.getFilteredStudentPredicate());
            assertFalse(feedback().contains(AddCommand.MESSAGE_SEARCH_CLEARED));
        } else {
            assertEquals(41, list.getItems().size());
            assertTrue(feedback().contains(AddCommand.MESSAGE_SEARCH_CLEARED));
        }
    }

    @Test
    public void add_rejectedOrSaveFailed_keepsTopCardAndRestoresDataAndSearch() throws Exception {
        FxToolkit.onFxThread(() -> {
            createWindow();
            enter("find -t T1");
            list.scrollTo(0);
            return null;
        });
        try {
            Student first = model.getFilteredStudentList().getFirst();
            FxToolkit.awaitAssertion(() -> assertVisible(first));
            var predicate = model.getFilteredStudentPredicate();
            var savedStudents = List.copyOf(model.getStudentBook().getStudentList());
            for (String command : List.of("add -p 91234567",
                    "add Duplicate -p 91234567 --id A01234567X", "add Unsaved -p 91234567 -t T2")) {
                FxToolkit.onFxThread(() -> {
                    assertRejectedAddition(command, savedStudents, predicate);
                    return null;
                });
                FxToolkit.awaitAssertion(() -> assertVisible(first));
            }
        } finally {
            closeWindow();
        }
    }

    private void assertRejectedAddition(String command, List<Student> savedStudents,
            Predicate<? super Student> predicate) throws IOException, DataLoadingException {
        isSaveFailing = command.contains("Unsaved");
        enter(command);
        assertFalse(feedback().startsWith("New student added:"));
        if (isSaveFailing) {
            assertEquals(String.format(LogicManager.FILE_OPS_ERROR_FORMAT,
                    "Simulated save failure"), feedback());
        }
        assertEquals(savedStudents, model.getStudentBook().getStudentList());
        assertSame(predicate, model.getFilteredStudentPredicate());
        assertEquals(savedStudents, storage.readStudentBook().orElseThrow().getStudentList());
        assertSame(field, window.getPrimaryStage().getScene().getFocusOwner());
    }

    private void createWindow() throws IOException {
        createWindow(STUDENTS, WINDOW_HEIGHT);
    }

    private void createWindow(int students, double height) throws IOException {
        model.setStudentBook(new StudentBook());
        model.updateFilteredStudentList(Model.PREDICATE_SHOW_ALL_STUDENTS);
        for (int i = 0; i < students; i++) {
            model.addStudent(new StudentBuilder().withName("Student " + i).withPhone("900000" + i)
                    .withTags(i % 2 == 0 ? "T1" : "T2")
                    .withoutNusStudentId().build());
        }
        model.setStudent(model.getFilteredStudentList().getFirst(), new StudentBuilder(
                model.getFilteredStudentList().getFirst()).withNusStudentId("A01234567X").build());
        Path dataFile = testFolder.resolve("tab.json");
        storage = new JsonStudentBookStorage(dataFile) {
            @Override
            public void saveStudentBook(ReadOnlyStudentBook book) throws IOException {
                if (isSaveFailing) {
                    throw new IOException("Simulated save failure");
                }
                super.saveStudentBook(book);
            }
        };
        storage.saveStudentBook(model.getStudentBook());
        LogicManager logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(testFolder.resolve("preferences.json"))));
        window = new MainWindow(new Stage(), logic, dataFile);
        window.fillInnerParts();
        window.getPrimaryStage().setWidth(740);
        window.getPrimaryStage().setHeight(height);
        window.show();
        field = (TextField) window.getRoot().getScene().lookup("#commandTextField");
        list = (ListView<?>) window.getStudentListPanel().getRoot().lookup("#studentListView");
        field.requestFocus();
    }

    private void enter(String command) {
        field.setText(command);
        field.fireEvent(new ActionEvent());
    }

    private String feedback() {
        return ((TextArea) window.getRoot().getScene().lookup("#resultDisplay")).getText();
    }

    private void assertVisible(Student student) {
        if (student == null) {
            assertTrue(list.getItems().isEmpty());
            return;
        }
        assertTrue(isInView(student), "the expected student's card must be visible inside the list: "
                + describeList(student));
    }

    /**
     * Requires the card to have been in view for {@code PULSES_IN_VIEW} pulses in a row, so that
     * a card which is in view only until it grows does not pass for one that is revealed.
     */
    private void assertStaysInView(Student student) {
        pulsesInView = isInView(student) ? pulsesInView + 1 : 0;
        assertTrue(pulsesInView >= PULSES_IN_VIEW, "the expected student's card must stay inside the"
                + " list once revealed, rather than the " + pulsesInView + " pulses it managed: "
                + describeList(student));
    }

    private boolean isInView(Student student) {
        Bounds viewport = list.localToScene(list.getLayoutBounds());
        return list.lookupAll(".list-cell").stream()
                .filter(node -> node instanceof ListCell<?> cell && student == cell.getItem())
                .anyMatch(node -> {
                    Bounds card = node.localToScene(node.getLayoutBounds());
                    return card.getMinY() >= viewport.getMinY() && card.getMaxY() <= viewport.getMaxY();
                });
    }

    private String describeList(Student student) {
        return student.getName() + "; viewport: " + list.localToScene(list.getLayoutBounds())
                + "; cells: " + list.lookupAll(".list-cell").stream()
                        .map(node -> ((ListCell<?>) node).getIndex() + ": "
                                + node.localToScene(node.getLayoutBounds()))
                        .toList();
    }

    private void closeWindow() throws Exception {
        FxToolkit.onFxThread(() -> {
            window.getPrimaryStage().close();
            return null;
        });
    }
}
