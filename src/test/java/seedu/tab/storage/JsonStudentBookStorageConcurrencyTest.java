package seedu.tab.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.testutil.Assert.assertThrows;
import static seedu.tab.testutil.TypicalStudents.ALICE;
import static seedu.tab.testutil.TypicalStudents.HOON;
import static seedu.tab.testutil.TypicalStudents.IDA;
import static seedu.tab.testutil.TypicalStudents.getTypicalStudentBook;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.tab.logic.LogicManager;
import seedu.tab.logic.commands.exceptions.CommandException;
import seedu.tab.model.Model;
import seedu.tab.model.ModelManager;
import seedu.tab.model.ReadOnlyStudentBook;
import seedu.tab.model.StudentBook;
import seedu.tab.model.UserPrefs;

public class JsonStudentBookStorageConcurrencyTest {
    @TempDir
    public Path testFolder;

    @Test
    public void saveStudentBook_twoWindowsWithoutData_refusesStaleSave() throws Exception {
        assertStaleSaveRefused(false);
    }

    @Test
    public void saveStudentBook_twoWindowsWithData_refusesStaleSave() throws Exception {
        assertStaleSaveRefused(true);
    }

    private void assertStaleSaveRefused(boolean hasSavedBook) throws Exception {
        Path path = testFolder.resolve("tab.json");
        JsonStudentBookStorage first = new JsonStudentBookStorage(path);
        if (hasSavedBook) {
            first.saveStudentBook(getTypicalStudentBook());
        }
        StudentBook firstBook = new StudentBook(first.readStudentBook().orElseGet(() -> getTypicalStudentBook()));
        JsonStudentBookStorage second = new JsonStudentBookStorage(path);
        Model secondModel = new ModelManager(second.readStudentBook().orElseGet(() -> getTypicalStudentBook()),
                new UserPrefs());
        secondModel.updateFilteredStudentList(ALICE::equals);
        LogicManager secondLogic = createLogic(secondModel, second);
        firstBook.addStudent(HOON);
        first.saveStudentBook(firstBook);
        assertRejectedSavePreservesState(secondLogic, secondModel, path);
        JsonStudentBookStorage restarted = new JsonStudentBookStorage(path);
        StudentBook recovered = new StudentBook(restarted.readStudentBook().orElseThrow());
        assertEquals(firstBook, recovered);
        recovered.addStudent(IDA);
        restarted.saveStudentBook(recovered);
        assertEquals(recovered, new JsonStudentBookStorage(path).readStudentBook().orElseThrow());
    }

    private void assertRejectedSavePreservesState(LogicManager secondLogic, Model secondModel, Path path)
            throws Exception {
        var filter = secondModel.getFilteredStudentPredicate();
        StudentBook before = new StudentBook(secondModel.getStudentBook());
        byte[] savedBytes = Files.readAllBytes(path);

        String message = "Could not save data due to the following error: Student data in " + path
                + " has changed since this window loaded it. Close this window and restart TAB to load the saved "
                + "records before retrying. No changes were made.";
        for (int attempt = 0; attempt < 2; attempt++) {
            assertThrows(CommandException.class, message, () -> secondLogic.execute("delete 1"));
            assertEquals(before, secondModel.getStudentBook());
            assertEquals(filter, secondModel.getFilteredStudentPredicate());
            assertEquals(List.of(ALICE), secondModel.getFilteredStudentList());
            assertArrayEquals(savedBytes, Files.readAllBytes(path));
        }
        secondLogic.execute("list");
        assertArrayEquals(savedBytes, Files.readAllBytes(path));
    }

    @Test
    public void saveStudentBook_singleWindowMultipleSaves_updatesBaselineWithoutAliasing() throws Exception {
        Path path = testFolder.resolve("tab.json");
        JsonStudentBookStorage storage = new JsonStudentBookStorage(path);
        storage.saveStudentBook(getTypicalStudentBook());
        StudentBook loaded = (StudentBook) storage.readStudentBook().orElseThrow();
        loaded.addStudent(HOON);
        storage.saveStudentBook(loaded);
        loaded.addStudent(IDA);
        storage.saveStudentBook(loaded);
        assertEquals(loaded, new JsonStudentBookStorage(path).readStudentBook().orElseThrow());
    }

    @Test
    public void saveStudentBook_deletedOrMalformedFile_refusesWithoutReplacingFile() throws Exception {
        Path path = testFolder.resolve("tab.json");
        JsonStudentBookStorage storage = new JsonStudentBookStorage(path);
        storage.saveStudentBook(getTypicalStudentBook());
        ReadOnlyStudentBook loaded = storage.readStudentBook().orElseThrow();
        Files.delete(path);
        assertThrows(IOException.class, () -> storage.saveStudentBook(loaded));
        assertFalse(Files.exists(path));
        Files.writeString(path, "not JSON");
        String message = "Cannot verify saved student data in " + path
                + ". Close this window, check the file and restart TAB before retrying";
        assertThrows(IOException.class, message, () -> storage.saveStudentBook(loaded));
        assertEquals("not JSON", Files.readString(path));
    }

    @Test
    public void saveStudentBook_symbolicLink_usesDestinationLockAndPreservesLink() throws Exception {
        Path target = testFolder.resolve("real.json");
        Path link = testFolder.resolve("tab.json");
        JsonStudentBookStorage storage = new JsonStudentBookStorage(target);
        storage.saveStudentBook(getTypicalStudentBook());
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException e) {
            Assumptions.assumeTrue(false, "Symbolic links unavailable: " + e.getMessage());
        }
        JsonStudentBookStorage linkedStorage = new JsonStudentBookStorage(link);
        StudentBook book = new StudentBook(linkedStorage.readStudentBook().orElseThrow());
        book.addStudent(HOON);
        byte[] savedBytes = Files.readAllBytes(target);
        try (FileChannel channel = FileChannel.open(testFolder.resolve(".real.json.lock"), StandardOpenOption.WRITE);
                FileLock lock = channel.lock()) {
            assertTrue(lock.isValid());
            assertThrows(IOException.class, () -> linkedStorage.saveStudentBook(book));
            assertArrayEquals(savedBytes, Files.readAllBytes(target));
        }
        linkedStorage.saveStudentBook(book);
        assertTrue(Files.isSymbolicLink(link));
        assertEquals(book, new JsonStudentBookStorage(target).readStudentBook().orElseThrow());
    }

    @Test
    public void execute_lockHeldInSameJvm_restoresRecordsAndFilter() throws Exception {
        Path path = testFolder.resolve("tab.json");
        JsonStudentBookStorage storage = new JsonStudentBookStorage(path);
        storage.saveStudentBook(getTypicalStudentBook());
        Model model = new ModelManager(storage.readStudentBook().orElseThrow(), new UserPrefs());
        model.updateFilteredStudentList(ALICE::equals);
        var filter = model.getFilteredStudentPredicate();
        StudentBook before = new StudentBook(model.getStudentBook());
        byte[] savedBytes = Files.readAllBytes(path);
        LogicManager logic = createLogic(model, storage);
        try (FileChannel channel = FileChannel.open(testFolder.resolve(".tab.json.lock"), StandardOpenOption.WRITE);
                FileLock lock = channel.lock()) {
            assertTrue(lock.isValid());
            String message = "Could not save data due to the following error: Another TAB window is saving "
                    + path + ". Wait for it to finish and retry; if the data changed, restart this window first. "
                    + "No changes were made.";
            assertThrows(CommandException.class, message, () -> logic.execute("delete 1"));
            assertEquals(before, model.getStudentBook());
            assertEquals(filter, model.getFilteredStudentPredicate());
            assertEquals(List.of(ALICE), model.getFilteredStudentList());
            assertArrayEquals(savedBytes, Files.readAllBytes(path));
        }
        logic.execute("delete 1");
        assertEquals(model.getStudentBook(), new JsonStudentBookStorage(path).readStudentBook().orElseThrow());
    }

    @Test
    public void saveStudentBook_lockHeldByAnotherProcess_refusesAndCanRetryAfterRelease() throws Exception {
        Path path = testFolder.resolve("tab.json");
        JsonStudentBookStorage storage = new JsonStudentBookStorage(path);
        storage.saveStudentBook(getTypicalStudentBook());
        StudentBook book = new StudentBook(storage.readStudentBook().orElseThrow());
        book.addStudent(HOON);
        byte[] savedBytes = Files.readAllBytes(path);
        String classes = Path.of(LockHolder.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                .toString();
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", classes, LockHolder.class.getName(), testFolder.resolve(".tab.json.lock").toString())
                .redirectError(ProcessBuilder.Redirect.INHERIT).start();
        try {
            assertEquals("LOCKED", process.inputReader().readLine());
            String message = "Another TAB window is saving " + path
                    + ". Wait for it to finish and retry; if the data changed, restart this window first";
            assertThrows(IOException.class, message, () -> storage.saveStudentBook(book));
            assertArrayEquals(savedBytes, Files.readAllBytes(path));
        } finally {
            process.getOutputStream().close();
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        }
        storage.saveStudentBook(book);
        assertEquals(book, new JsonStudentBookStorage(path).readStudentBook().orElseThrow());
        assertTrue(Files.exists(testFolder.resolve(".tab.json.lock")));
    }

    private LogicManager createLogic(Model model, JsonStudentBookStorage storage) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(testFolder.resolve("preferences.json"))));
    }

    /**
     * Holds an operating-system lock in a separate JVM until its input closes.
     */
    public static class LockHolder {
        public static void main(String[] args) throws Exception {
            try (FileChannel channel = FileChannel.open(Path.of(args[0]), StandardOpenOption.WRITE);
                    FileLock lock = channel.lock()) {
                if (lock.isValid()) {
                    System.out.println("LOCKED");
                    System.out.flush();
                    System.in.read();
                }
            }
        }
    }
}
