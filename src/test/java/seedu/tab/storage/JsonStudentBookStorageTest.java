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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.tab.commons.exceptions.DataLoadingException;
import seedu.tab.model.ReadOnlyStudentBook;
import seedu.tab.model.StudentBook;
import seedu.tab.model.student.Email;
import seedu.tab.model.student.NusStudentId;
import seedu.tab.model.student.Phone;
import seedu.tab.model.student.Student;
import seedu.tab.testutil.StudentBuilder;

public class JsonStudentBookStorageTest {
    private static final Path TEST_DATA_FOLDER = Paths.get("src", "test", "data", "JsonStudentBookStorageTest");

    @TempDir
    public Path testFolder;

    @Test
    public void saveStudentBook_firstSaveFails_leavesNoFileAndAllowsRestart() throws Exception {
        Path path = testFolder.resolve("data/tab.json");
        StudentBook original = getTypicalStudentBook();
        JsonStudentBookStorage failingStorage = new JsonStudentBookStorage(path) {
            @Override
            void writeStudentBook(ReadOnlyStudentBook book, Path destination) throws IOException {
                throw new IOException("No space left on device");
            }
        };

        assertThrows(IOException.class, "No space left on device", () -> failingStorage.saveStudentBook(original));
        assertFalse(Files.exists(path));
        JsonStudentBookStorage restartedStorage = new JsonStudentBookStorage(path);
        assertEquals(Optional.empty(), restartedStorage.readStudentBook());
        restartedStorage.saveStudentBook(original);
        assertEquals(original, new JsonStudentBookStorage(path).readStudentBook().orElseThrow());
    }

    @Test
    public void readStudentBook_trailingContent_reportsLocationAndPreservesBytes() throws Exception {
        for (String suffix : List.of("{}", "[]", "null", "true", "42", "\"extra\"", ", {}", "extra")) {
            String json = "{\"students\":[]}\n" + suffix + "\n";
            Path path = testFolder.resolve("trailing.json");
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            Files.write(path, bytes);

            DataLoadingException exception = org.junit.jupiter.api.Assertions.assertThrows(
                    DataLoadingException.class, () -> new JsonStudentBookStorage(path).readStudentBook());

            assertTrue(exception.getMessage().contains("at line 2, column"), exception.getMessage());
            assertTrue(exception.getMessage().contains("Affected source:"));
            assertArrayEquals(bytes, Files.readAllBytes(path));
        }
    }

    @Test
    public void readStudentBook_duplicateKeys_reportsKeyAndPreservesBytes() throws Exception {
        for (String json : List.of("{\"students\":[],\"students\":[]}",
                "{\"students\":[{\"name\":\"Alex\",\"phone\":\"91112222\",\"phone\":\"87438807\"}]}")) {
            Path path = testFolder.resolve("duplicate.json");
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            Files.write(path, bytes);

            DataLoadingException exception = org.junit.jupiter.api.Assertions.assertThrows(
                    DataLoadingException.class, () -> new JsonStudentBookStorage(path).readStudentBook());

            String key = json.contains("phone") ? "phone" : "students";
            assertTrue(exception.getMessage().contains("Duplicate field '" + key + "'"), exception.getMessage());
            assertTrue(exception.getMessage().contains("at line 1"), exception.getMessage());
            assertArrayEquals(bytes, Files.readAllBytes(path));
        }
    }

    @Test
    public void readStudentBook_multilineDuplicateKey_reportsOffendingLineAndPreservesBytes() throws Exception {
        String json = """
                {
                  "students": [
                    {"name":"Before","phone":"81111111"},
                    {
                      "name": "Alex",
                      "phone": "91112222",
                      "tags": [
                        "Lab 3"
                      ],
                      "phone": "87438807"
                    },
                    {"name":"After","phone":"83333333"}
                  ]
                }
                """;
        Path path = testFolder.resolve("multiline-duplicate.json");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Files.write(path, bytes);

        DataLoadingException exception = org.junit.jupiter.api.Assertions.assertThrows(
                DataLoadingException.class, () -> new JsonStudentBookStorage(path).readStudentBook());

        assertTrue(exception.getMessage().contains("at line 10, column"), exception.getMessage());
        assertTrue(exception.getMessage().contains("Duplicate field 'phone'"));
        assertTrue(exception.getMessage().contains("> 10 |       \"phone\": \"87438807\""));
        assertFalse(exception.getMessage().contains("After"));
        assertArrayEquals(bytes, Files.readAllBytes(path));
    }

    @Test
    public void readStudentBook_handEditedWhitespaceAndRepeatedKeysAcrossStudents_loads() throws Exception {
        Path path = testFolder.resolve("hand-edited.json");
        String json = """
                  {"students":[{"name":"Alex","phone":"91112222"},
                {"phone":"87438807","name":"Bernice"}]} \t\r

                """;
        Files.writeString(path, json);

        ReadOnlyStudentBook loaded = new JsonStudentBookStorage(path).readStudentBook().orElseThrow();

        assertEquals(List.of(new StudentBuilder().withName("Alex").withPhone("91112222").withoutEmail().build(),
                new StudentBuilder().withName("Bernice").withPhone("87438807").withoutEmail().build()),
                loaded.getStudentList());
        assertEquals(json, Files.readString(path));
    }

    @Test
    public void readAndSaveStudentBook_hiddenControls_normalizesNamesAndTagsWithoutRewritingOnRead() throws Exception {
        Path path = testFolder.resolve("control-characters.json");
        String json = "{\"students\":[{\"name\":\"Am\\u0001y\",\"phone\":\"91110000\","
                + "\"tags\":[\"La\\u007fb\",\"组\\u009f1\"]}]}";
        Files.writeString(path, json);
        JsonStudentBookStorage storage = new JsonStudentBookStorage(path);
        ReadOnlyStudentBook loaded = storage.readStudentBook().orElseThrow();
        Student expected = new StudentBuilder().withName("Amy").withPhone("91110000").withoutEmail()
                .withTags("Lab", "组1").build();

        assertEquals(List.of(expected), loaded.getStudentList());
        assertEquals(json, Files.readString(path));
        storage.saveStudentBook(loaded);
        assertEquals(loaded, new JsonStudentBookStorage(path).readStudentBook().orElseThrow());
        assertFalse(Files.readString(path).contains("\\u0001"));
        assertFalse(Files.readString(path).contains("\\u007f"));
        assertFalse(Files.readString(path).contains("\\u009f"));
    }

    @Test
    public void readStudentBook_tagVariants_keepsFirstNormalizedSpellingAcrossSaveAndReload() throws Exception {
        Path path = testFolder.resolve("tag-variants.json");
        String json = """
                {"students":[{"name":"Casey Tan","phone":"91110000",
                "tags":[" t1 ","T1","Lab  3","lab 3","组 1","组\u00a01","Café","Café","İ","i"]}]}
                """;
        Files.writeString(path, json);
        JsonStudentBookStorage storage = new JsonStudentBookStorage(path);
        ReadOnlyStudentBook loaded = storage.readStudentBook().orElseThrow();
        Student expected = new StudentBuilder().withName("Casey Tan").withPhone("91110000").withoutEmail()
                .withTags("t1", "Lab 3", "组 1", "Café", "İ").build();

        assertEquals(List.of(expected), loaded.getStudentList());
        assertEquals(json, Files.readString(path));
        storage.saveStudentBook(loaded);
        assertEquals(loaded, new JsonStudentBookStorage(path).readStudentBook().orElseThrow());
        assertFalse(Files.readString(path).contains("\"T1\""));
        assertFalse(Files.readString(path).contains("\"lab 3\""));
    }

    @Test
    public void readStudentBook_nonBooleanFlag_reportsRecordAndPreservesBytes() throws Exception {
        for (String value : List.of("0", "1", "2", "-1", "1.5", "1e3", "999999999999999999999999",
                "\"true\"", "\"false\"", "\"\"", "[]", "{}")) {
            String json = "{\"students\":[{\"name\":\"Before\",\"phone\":\"81111111\"},"
                    + "{\"name\":\"Alice Tan\",\"phone\":\"91234567\",\"flag\":" + value + "}]}";
            Path path = testFolder.resolve("invalid-flag.json");
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            Files.write(path, bytes);

            DataLoadingException exception = org.junit.jupiter.api.Assertions.assertThrows(
                    DataLoadingException.class, () -> new JsonStudentBookStorage(path).readStudentBook());

            assertTrue(exception.getMessage().startsWith("Student record 2: "), exception.getMessage());
            String shownValue = value.equals("[]") ? "[" : value.equals("{}") ? "{" : value;
            assertTrue(exception.getMessage().contains("Invalid flag value " + shownValue
                    + ". Use JSON true, false or null for the follow-up flag."), exception.getMessage());
            assertArrayEquals(bytes, Files.readAllBytes(path));
        }
    }

    @Test
    public void readStudentBook_nonArrayTags_reportsFieldAndPreservesBytes() throws Exception {
        for (String value : List.of("\"friends\"", "5", "true", "false", "{}")) {
            String shownValue = value.equals("{}") ? "an object" : value;
            assertInvalidTagsPreservesFile(value, "Invalid tags value: " + shownValue
                    + ". Use a JSON array of tag strings, such as [\"Lab 3\"].");
        }
    }

    @Test
    public void readStudentBook_nonStringTag_reportsEntryAndPreservesBytes() throws Exception {
        for (String value : List.of("5", "1.5", "true", "false", "null", "{}", "[]")) {
            String shownValue = value.equals("{}") ? "an object" : value.equals("[]") ? "an array" : value;
            assertInvalidTagsPreservesFile("[\"Lab 3\"," + value + "]", "Invalid tags entry 2 value: " + shownValue
                    + ". Each tag must be a JSON string, such as \"Lab 3\".");
        }
    }

    private void assertInvalidTagsPreservesFile(String tags, String expectedReason) throws Exception {
        Path path = testFolder.resolve("invalid-tags.json");
        String json = "{\"students\":[{\"name\":\"Before\",\"phone\":\"81111111\"},"
                + "{\"name\":\"Alice Tan\",\"phone\":\"91234567\",\"tags\":" + tags + "}]}";
        Files.writeString(path, json);
        DataLoadingException exception = org.junit.jupiter.api.Assertions.assertThrows(
                DataLoadingException.class, () -> new JsonStudentBookStorage(path).readStudentBook());

        assertTrue(exception.getMessage().startsWith("Student record 2: "), exception.getMessage());
        assertTrue(exception.getMessage().contains(expectedReason), exception.getMessage());
        assertTrue(exception.getMessage().contains("at line 1, column"), exception.getMessage());
        assertFalse(exception.getMessage().contains("java.util"));
        assertFalse(exception.getMessage().contains("seedu.tab"));
        assertEquals(json, Files.readString(path));
    }

    @Test
    public void readAndSaveStudentBook_validTagRepresentations_preservesTags() throws Exception {
        for (String field : List.of("", ",\"tags\":null", ",\"tags\":[]", ",\"tags\":[\"Lab 3\",\"组 1\"]")) {
            Path path = testFolder.resolve("valid-tags.json");
            String json = "{\"students\":[{\"name\":\"Alice Tan\",\"phone\":\"91234567\"" + field + "}]}";
            Files.writeString(path, json);
            JsonStudentBookStorage storage = new JsonStudentBookStorage(path);
            ReadOnlyStudentBook loaded = storage.readStudentBook().orElseThrow();
            StudentBuilder expected = new StudentBuilder().withName("Alice Tan").withPhone("91234567")
                    .withoutEmail();
            if (field.contains("Lab 3")) {
                expected.withTags("Lab 3", "组 1");
            }
            assertEquals(List.of(expected.build()), loaded.getStudentList());
            assertEquals(json, Files.readString(path));
            storage.saveStudentBook(loaded);
            assertEquals(loaded, storage.readStudentBook().orElseThrow());
        }
    }

    @Test
    public void readAndSaveStudentBook_documentedFlagValues_preservesStatus() throws Exception {
        for (String field : List.of("", ",\"flag\":null", ",\"flag\":false", ",\"flag\":true")) {
            Path path = testFolder.resolve("valid-flag.json");
            String json = "{\"students\":[{\"name\":\"Alice Tan\",\"phone\":\"91234567\"" + field + "}]}";
            Files.writeString(path, json);
            JsonStudentBookStorage storage = new JsonStudentBookStorage(path);

            ReadOnlyStudentBook loaded = storage.readStudentBook().orElseThrow();

            assertEquals(field.endsWith("true"), loaded.getStudentList().getFirst().isFlagged());
            assertEquals(json, Files.readString(path));
            storage.saveStudentBook(loaded);
            assertEquals(loaded, storage.readStudentBook().orElseThrow());
        }
    }

    @Test
    public void readAndSaveStudentBook_utf8WithOrWithoutBom_preservesUnicodeRecords() throws Exception {
        StudentBook expected = new StudentBook();
        expected.addStudent(new StudentBuilder().withName("José Tan").build());
        expected.addStudent(new StudentBuilder().withName("王小明").build());
        Path path = testFolder.resolve("unicode.json");
        JsonStudentBookStorage storage = new JsonStudentBookStorage(path);
        storage.saveStudentBook(expected);
        String json = Files.readString(path);
        for (String prefix : new String[] {"", "\uFEFF"}) {
            Files.writeString(path, prefix + json, StandardCharsets.UTF_8);
            ReadOnlyStudentBook loaded = storage.readStudentBook().orElseThrow();
            assertEquals(expected, loaded);
            assertEquals(prefix + json, Files.readString(path), "loading should not rewrite the file");
            storage.saveStudentBook(loaded);
            assertEquals(expected, storage.readStudentBook().orElseThrow());
        }
    }

    @Test
    public void readStudentBook_unopenableFolder_reportsTheFailureRatherThanAFirstRun() throws Exception {
        // a first run shows sample students and saves them over the file. Doing that for a
        // folder TAB merely cannot open would replace records it was never able to read
        Path directory = Files.createDirectory(testFolder.resolve("data"));
        Path path = directory.resolve("tab.json");
        JsonStudentBookStorage storage = new JsonStudentBookStorage(path);
        storage.saveStudentBook(getTypicalStudentBook());
        byte[] saved = Files.readAllBytes(path);
        Assumptions.assumeTrue(Files.getFileStore(directory).supportsFileAttributeView("posix"),
                "POSIX permissions are unavailable.");
        Assumptions.assumeFalse("root".equals(System.getProperty("user.name")),
                "root is allowed past file permissions.");
        Set<PosixFilePermission> originalPermissions = Files.getPosixFilePermissions(directory);
        Files.setPosixFilePermissions(directory, Set.of());

        try {
            Assumptions.assumeFalse(Files.isReadable(path), "The test account can still read the file.");
            DataLoadingException exception = org.junit.jupiter.api.Assertions.assertThrows(
                    DataLoadingException.class, storage::readStudentBook);

            assertEquals("TAB does not have permission to read this file or the folder holding it.",
                    exception.getMessage());
        } finally {
            Files.setPosixFilePermissions(directory, originalPermissions);
        }
        assertArrayEquals(saved, Files.readAllBytes(path));
    }

    @Test
    public void readStudentBook_windows1252_reportsEncodingAndPreservesBytes() throws Exception {
        Path path = testFolder.resolve("ansi.json");
        byte[] bytes = "{\"students\":[{\"name\":\"José Tan\",\"phone\":\"91234567\"}]}"
                .getBytes(Charset.forName("windows-1252"));
        Files.write(path, bytes);
        DataLoadingException exception = org.junit.jupiter.api.Assertions.assertThrows(
                DataLoadingException.class, () -> new JsonStudentBookStorage(path).readStudentBook());
        assertEquals("The file is not valid UTF-8. Reopen it with its original encoding and save it as UTF-8,"
                + " then restart TAB.", exception.getMessage());
        assertArrayEquals(bytes, Files.readAllBytes(path));
    }

    @Test
    public void readStudentBook_unknownStudentKeys_reportsRecordAndPreservesFile() throws Exception {
        // the key is rejected by its name, before its value is looked at, so the two
        // dimensions are walked separately rather than multiplied: every unknown key against
        // one value, then every value type against one key
        for (String field : List.of("nusStudentID", "Email", "tag", "Flag", "notes", "Address")) {
            assertUnknownFieldIsReported(field, "\"A0999999Z\"");
        }
        for (String value : List.of("null", "{}", "[]")) {
            assertUnknownFieldIsReported("notes", value);
        }
    }

    private void assertUnknownFieldIsReported(String field, String value) throws Exception {
        // Put the unknown field before the name to cover property-order independence.
        String json = "{\"students\":[{\"name\":\"Alex\",\"phone\":\"87438807\"},"
                + "{\"" + field + "\":" + value + ",\"name\":\"Bernice Yu\",\"phone\":\"99272758\"}]}";
        Path path = testFolder.resolve("unknown.json");
        Files.writeString(path, json);
        DataLoadingException exception = org.junit.jupiter.api.Assertions.assertThrows(
                DataLoadingException.class, () -> new JsonStudentBookStorage(path).readStudentBook());
        assertTrue(exception.getMessage().startsWith("Student record 2: "), exception.getMessage());
        assertTrue(exception.getMessage().contains("Unknown field \"" + field + "\" in student \"Bernice Yu\""),
                exception.getMessage());
        assertTrue(exception.getMessage().contains("Expected: name, phone, email, nusStudentId, tags, flag."));
        assertEquals(json, Files.readString(path));
    }

    @Test
    public void readStudentBook_unknownKeyWithoutName_reportsUnnamedRecordAndPreservesFile() throws Exception {
        Path path = testFolder.resolve("unnamed-student.json");
        for (String nameField : List.of("", "\"name\":null,")) {
            String json = "{\"students\":[{" + nameField
                    + "\"phone\":\"99272758\",\"Email\":\"bernice@example.com\"}]}";
            Files.writeString(path, json);

            DataLoadingException exception = org.junit.jupiter.api.Assertions.assertThrows(
                    DataLoadingException.class, () -> new JsonStudentBookStorage(path).readStudentBook());

            assertEquals("Student record 1: The data is not in the expected JSON format: "
                    + "Unknown field \"Email\" in student record."
                    + " Check its spelling. Expected: name, phone, email, nusStudentId, tags, flag."
                    + " The legacy address field is also accepted and ignored.", exception.getMessage());
            assertEquals(json, Files.readString(path));
        }
    }

    @Test
    public void readStudentBook_unknownKeyInSecondRecord_reportsIndexAndPreservesFile() throws Exception {
        Path path = testFolder.resolve("ambiguous-student.json");
        for (String nameField : List.of("\"name\":\"Alex\",", "", "\"name\":null,")) {
            String json = "{\"students\":[{\"name\":\"Alex\",\"phone\":\"87438807\"},{"
                    + nameField + "\"phone\":\"99272758\",\"emial\":\"a@b.c\"}]}";
            Files.writeString(path, json);

            DataLoadingException exception = org.junit.jupiter.api.Assertions.assertThrows(
                    DataLoadingException.class, () -> new JsonStudentBookStorage(path).readStudentBook());

            String record = nameField.contains("Alex") ? "student \"Alex\"" : "student record";
            assertEquals("Student record 2: The data is not in the expected JSON format: Unknown field \"emial\" in "
                    + record + ". Check its spelling. Expected: name, phone, email, nusStudentId, tags, flag."
                    + " The legacy address field is also accepted and ignored.", exception.getMessage());
            assertEquals(json, Files.readString(path));
        }
    }

    @Test
    public void readStudentBook_unknownRootKey_reportsKeyAndPreservesFile() throws Exception {
        Path path = testFolder.resolve("unknown-root.json");
        String json = "{\"Students\":[],\"students\":[]}";
        Files.writeString(path, json);
        DataLoadingException exception = org.junit.jupiter.api.Assertions.assertThrows(
                DataLoadingException.class, () -> new JsonStudentBookStorage(path).readStudentBook());
        assertTrue(exception.getMessage().contains("Unknown field \"Students\" in the student book."));
        assertTrue(exception.getMessage().contains(
                "Expected top-level fields: students, _comment (optional description)."));
        assertEquals(json, Files.readString(path));
    }

    @Test
    public void readAndSaveStudentBook_legacyAddressAndOptionalFields_preservesSupportedDetails() throws Exception {
        Path path = testFolder.resolve("legacy.json");
        Files.writeString(path, """
                {"_comment":"A hand-edited file","students":[{"name":"Bernice Yu","phone":"99272758",
                "address":{"old":"value"},
                "email":"bernice@example.com","nusStudentId":"a0999999z","tags":["T2"],"flag":true}]}
                """);
        JsonStudentBookStorage storage = new JsonStudentBookStorage(path);
        ReadOnlyStudentBook loaded = storage.readStudentBook().orElseThrow();
        Student expected = new StudentBuilder().withName("Bernice Yu").withPhone("99272758")
                .withEmail("bernice@example.com").withNusStudentId("A0999999Z").withTags("T2")
                .withFlag(true).build();
        assertEquals(List.of(expected), loaded.getStudentList());
        storage.saveStudentBook(loaded);
        assertFalse(Files.readString(path).contains("address"));
        assertEquals(loaded, storage.readStudentBook().orElseThrow());
    }

    @Test
    public void readStudentBook_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> readStudentBook(null));
    }

    private java.util.Optional<ReadOnlyStudentBook> readStudentBook(String filePath) throws Exception {
        return new JsonStudentBookStorage(Paths.get(filePath)).readStudentBook(addToTestDataPathIfNotNull(filePath));
    }

    private Path addToTestDataPathIfNotNull(String prefsFileInTestDataFolder) {
        return prefsFileInTestDataFolder != null
                ? TEST_DATA_FOLDER.resolve(prefsFileInTestDataFolder)
                : null;
    }

    @Test
    public void read_missingFile_emptyResult() throws Exception {
        assertFalse(readStudentBook("NonExistentFile.json").isPresent());
    }

    @Test
    public void read_notJsonFormat_exceptionThrown() {
        DataLoadingException exception = org.junit.jupiter.api.Assertions.assertThrows(
                DataLoadingException.class, () -> readStudentBook("notJsonFormatStudentBook.json"));

        assertTrue(exception.getMessage().contains("The data is not in the expected JSON format"));
        assertTrue(exception.getMessage().contains("line 1, column 4"));
    }

    @Test
    public void readStudentBook_nullDocument_throwsDataLoadingException() {
        assertThrows(DataLoadingException.class, "The JSON document must not be null.", () ->
                readStudentBook("nullStudentBook.json"));
    }

    @Test
    public void readStudentBook_syntaxError_reportsAffectedLinesAndPreservesFile() throws Exception {
        Path filePath = testFolder.resolve("SyntaxError.json");
        String originalContents = """
                {
                  "students": [ {
                    "name": "Before",
                    "phone": "81111111"
                  }, {
                    "name": "Alice"
                    "phone": "82222222"
                  }, {
                    "name": "After",
                    "phone": "83333333"
                  } ]
                }
                """;
        Files.writeString(filePath, originalContents);
        JsonStudentBookStorage storage = new JsonStudentBookStorage(filePath);

        DataLoadingException exception = org.junit.jupiter.api.Assertions.assertThrows(
                DataLoadingException.class, storage::readStudentBook);

        assertTrue(exception.getMessage().contains("at line 7, column 6"));
        assertTrue(exception.getMessage().contains("  6 |     \"name\": \"Alice\""));
        assertTrue(exception.getMessage().contains("> 7 |     \"phone\": \"82222222\""));
        assertFalse(exception.getMessage().contains("Before"));
        assertFalse(exception.getMessage().contains("After"));
        assertEquals(originalContents, Files.readString(filePath));
    }

    @Test
    public void readStudentBook_invalidStudentStudentBook_throwDataLoadingException() {
        String expectedMessage = "Student record 1 (\"---\"): Name \"---\" is not valid: "
                + "it holds no letter or number";
        assertThrows(DataLoadingException.class, expectedMessage, () ->
                readStudentBook("invalidStudentStudentBook.json"));
    }

    @Test
    public void readStudentBook_invalidAndValidStudentStudentBook_throwDataLoadingException() {
        String expectedMessage = "Student record 2 (\"Student With Invalid Phone Field\"): "
                + "Phone \"12\" is not valid: a phone number needs at least 3 digits";
        assertThrows(DataLoadingException.class, expectedMessage, () ->
                readStudentBook("invalidAndValidStudentStudentBook.json"));
    }

    @Test
    public void readStudentBook_missingStudentList_reportsErrorAndPreservesFile() throws Exception {
        Path filePath = testFolder.resolve("MissingStudentList.json");
        String originalContents = "{}";
        Files.writeString(filePath, originalContents);
        JsonStudentBookStorage storage = new JsonStudentBookStorage(filePath);

        DataLoadingException exception = org.junit.jupiter.api.Assertions.assertThrows(
                DataLoadingException.class, storage::readStudentBook);

        assertEquals(JsonSerializableStudentBook.MESSAGE_MISSING_STUDENT_LIST, exception.getMessage());
        assertEquals(originalContents, Files.readString(filePath));
    }

    @Test
    public void readStudentBook_extremelyLongEmail_reportsRecordAndPreservesFile() throws Exception {
        Path filePath = testFolder.resolve("LongEmail.json");
        String longEmail = "a.".repeat(300) + "a@example.com";
        String originalContents = String.format("""
                {
                  "students": [ {
                    "name": "Ravi",
                    "phone": "91234567",
                    "email": "%s"
                  } ]
                }
                """, longEmail);
        Files.writeString(filePath, originalContents);
        JsonStudentBookStorage storage = new JsonStudentBookStorage(filePath);

        DataLoadingException exception = org.junit.jupiter.api.Assertions.assertThrows(
                DataLoadingException.class, storage::readStudentBook);

        assertTrue(exception.getMessage().contains("Student record 1 (\"Ravi\"): Email field"));
        assertTrue(exception.getMessage().contains(String.valueOf(longEmail.length())));
        assertEquals(originalContents, Files.readString(filePath));
    }

    @Test
    public void readStudentBook_invalidNusStudentIdStudentBook_throwDataLoadingException() {
        String expectedMessage = "Student record 1 (\"Student With Invalid NUS Student ID\"): "
                + NusStudentId.MESSAGE_CONSTRAINTS;

        assertThrows(DataLoadingException.class, expectedMessage, () ->
                readStudentBook("invalidNusStudentIdStudentBook.json"));
    }

    @Test
    public void readStudentBook_reservedFollowUpTag_reportsTheFieldToEdit() {
        String expectedMessage = "Student record 1 (\"Flagged One\"): " + JsonAdaptedTag.MESSAGE_RESERVED_TAG;

        assertThrows(DataLoadingException.class, expectedMessage, () ->
                readStudentBook("reservedTagStudentBook.json"));
    }

    @Test
    public void readStudentBook_reservedFollowUpTagInAnotherCase_isRefusedToo() {
        // the rule folds case and spacing, so a file cannot slip the label past by shouting it
        String expectedMessage = "Student record 1 (\"Flagged Two\"): " + JsonAdaptedTag.MESSAGE_RESERVED_TAG;

        assertThrows(DataLoadingException.class, expectedMessage, () ->
                readStudentBook("reservedTagCaseVariantStudentBook.json"));
    }

    @Test
    public void readStudentBook_duplicateNusStudentId_reportsNormalizedId() {
        String expectedMessage = "Student record 2 (\"Lee Wei Ming\"): NUS Student ID A01234567X "
                + "is already used by student record 1 (\"Tan Wei Ming\").";

        assertThrows(DataLoadingException.class, expectedMessage, () ->
                readStudentBook("duplicateNusStudentIdStudentBook.json"));
    }

    @Test
    public void readStudentBook_fileHoldingTheRemovedAddressField_loadsWithoutIt() throws Exception {
        // Every data file written before the address field was removed still holds an
        // "address" key. Those files have to keep loading, with the key ignored.
        ReadOnlyStudentBook loaded = readStudentBook("withAddressFieldStudentBook.json").get();

        Student expected = new StudentBuilder().withName("Alice Pauline").withPhone("94351253")
                .withEmail("alice@example.com").withTags("friends").build();

        assertEquals(1, loaded.getStudentList().size());
        assertEquals(expected, loaded.getStudentList().get(0));
    }

    @Test
    public void readStudentBook_handEditedNullEmail_loadsWithoutOne() throws Exception {
        // a file edited by hand may spell an absent email either way, and both have to load
        ReadOnlyStudentBook loaded = readStudentBook("nullEmailStudentBook.json").get();

        Student explicitNull = new StudentBuilder().withName("Alice Pauline").withPhone("94351253")
                .withTags("friends").withoutEmail().build();
        Student keyLeftOut = new StudentBuilder().withName("Benson Meier").withPhone("98765432")
                .withTags("owesMoney").withoutEmail().build();

        assertEquals(2, loaded.getStudentList().size());
        assertEquals(explicitNull, loaded.getStudentList().get(0));
        assertEquals(keyLeftOut, loaded.getStudentList().get(1));
    }

    @Test
    public void readStudentBook_handEditedNullPhone_loadsWithoutOne() throws Exception {
        // a file edited by hand may spell an absent phone either way, and both have to load
        // alongside a record that does hold one
        ReadOnlyStudentBook loaded = readStudentBook("nullPhoneStudentBook.json").get();

        Student explicitNull = new StudentBuilder().withName("Alice Pauline").withoutPhone()
                .withEmail("alice@example.com").withTags("friends").build();
        Student keyLeftOut = new StudentBuilder().withName("Benson Meier").withoutPhone()
                .withEmail("benson@example.com").withTags("owesMoney").build();
        Student keyPresent = new StudentBuilder().withName("Carl Kurz").withPhone("+65 9123 4567 (HP)")
                .withEmail("carl@example.com").withTags().build();

        assertEquals(3, loaded.getStudentList().size());
        assertEquals(explicitNull, loaded.getStudentList().get(0));
        assertEquals(keyLeftOut, loaded.getStudentList().get(1));
        assertEquals(keyPresent, loaded.getStudentList().get(2));
    }

    @Test
    public void readStudentBook_handEditedPhoneWhitespace_loadsWithoutOne() throws Exception {
        Path filePath = testFolder.resolve("PhoneWhitespace.json");
        Files.writeString(filePath, """
                {
                  "students": [ {
                    "name": "Alex Yeoh",
                    "phone": ""
                  }, {
                    "name": "Bernice Yu",
                    "phone": " 99272758 "
                  } ]
                }
                """);
        JsonStudentBookStorage storage = new JsonStudentBookStorage(filePath);

        ReadOnlyStudentBook loaded = storage.readStudentBook().get();

        assertEquals(Optional.empty(), loaded.getStudentList().get(0).getPhone());
        assertEquals(Optional.of(new Phone("99272758")), loaded.getStudentList().get(1).getPhone());
    }

    @Test
    public void readAndSaveStudentBook_studentWithoutPhone_roundTripsAndLeavesTheKeyOut() throws Exception {
        Path filePath = testFolder.resolve("NoPhone.json");
        Student student = new StudentBuilder().withName("Wong Mei Ling").withoutPhone()
                .withEmail("meiling@u.nus.edu").withTags("Lab 3").build();

        StudentBook original = new StudentBook();
        original.addStudent(student);

        JsonStudentBookStorage storage = new JsonStudentBookStorage(filePath);
        storage.saveStudentBook(original, filePath);

        // the file is meant to stay editable by hand, so an absent phone is an absent key
        assertFalse(Files.readString(filePath).contains("phone"));

        ReadOnlyStudentBook readBack = storage.readStudentBook(filePath).get();
        assertEquals(original, new StudentBook(readBack));
        assertEquals(Optional.empty(), readBack.getStudentList().get(0).getPhone());
    }

    @Test
    public void readStudentBook_handEditedEmailWhitespace_loadsAndPreservesAllStudents() throws Exception {
        Path filePath = testFolder.resolve("EmailWhitespace.json");
        Files.writeString(filePath, """
                {
                  "students": [ {
                    "name": "Alex Yeoh",
                    "phone": "87438807",
                    "email": ""
                  }, {
                    "name": "Bernice Yu",
                    "phone": "99272758",
                    "email": "berniceyu@example.com "
                  }, {
                    "name": "Charlotte Oliveiro",
                    "phone": "93210283"
                  } ]
                }
                """);
        JsonStudentBookStorage storage = new JsonStudentBookStorage(filePath);

        ReadOnlyStudentBook loaded = storage.readStudentBook().get();

        assertEquals(3, loaded.getStudentList().size());
        assertEquals(Optional.empty(), loaded.getStudentList().get(0).getEmail());
        assertEquals(Optional.of(new Email("berniceyu@example.com")), loaded.getStudentList().get(1).getEmail());
        assertEquals(Optional.empty(), loaded.getStudentList().get(2).getEmail());

        StudentBook updated = new StudentBook(loaded);
        updated.addStudent(new StudentBuilder().withName("David Li").withPhone("91031282").build());
        storage.saveStudentBook(updated);

        assertEquals(updated, new StudentBook(storage.readStudentBook().get()));
    }

    @Test
    public void readStudentBook_handEditedNullNusStudentId_loadsWithoutOne() throws Exception {
        ReadOnlyStudentBook loaded = readStudentBook("nullNusStudentIdStudentBook.json").get();

        Student explicitNull = new StudentBuilder().withName("Alice Pauline").withPhone("94351253")
                .withTags("friends").withoutEmail().withoutNusStudentId().build();
        Student keyLeftOut = new StudentBuilder().withName("Benson Meier").withPhone("98765432")
                .withTags("owesMoney").withoutEmail().withoutNusStudentId().build();

        assertEquals(2, loaded.getStudentList().size());
        assertEquals(explicitNull, loaded.getStudentList().get(0));
        assertEquals(keyLeftOut, loaded.getStudentList().get(1));
    }

    @Test
    public void readStudentBook_handEditedFlagValues_loadWithCompatibleDefaults() throws Exception {
        ReadOnlyStudentBook loaded = readStudentBook("followUpFlagStudentBook.json").get();

        assertEquals(4, loaded.getStudentList().size());
        assertTrue(loaded.getStudentList().get(0).isFlagged());
        assertFalse(loaded.getStudentList().get(1).isFlagged());
        assertFalse(loaded.getStudentList().get(2).isFlagged());
        assertFalse(loaded.getStudentList().get(3).isFlagged());
    }

    @Test
    public void readAndSaveStudentBook_allInOrder_success() throws Exception {
        Path filePath = testFolder.resolve("TempStudentBook.json");
        StudentBook original = getTypicalStudentBook();
        JsonStudentBookStorage jsonStudentBookStorage = new JsonStudentBookStorage(filePath);

        // Save in new file and read back
        jsonStudentBookStorage.saveStudentBook(original, filePath);
        ReadOnlyStudentBook readBack = jsonStudentBookStorage.readStudentBook(filePath).get();
        assertEquals(original, new StudentBook(readBack));

        // Modify data, overwrite existing file, and read back
        original.addStudent(HOON);
        original.removeStudent(ALICE);
        jsonStudentBookStorage.saveStudentBook(original, filePath);
        readBack = jsonStudentBookStorage.readStudentBook(filePath).get();
        assertEquals(original, new StudentBook(readBack));

        // Save and read without specifying file path
        original.addStudent(IDA);
        jsonStudentBookStorage.saveStudentBook(original); // file path not specified
        readBack = jsonStudentBookStorage.readStudentBook().get(); // file path not specified
        assertEquals(original, new StudentBook(readBack));
    }

    @Test
    public void readAndSaveStudentBook_longPhoneText_preservesCompleteValues() throws Exception {
        String digits = "1234567890".repeat(40);
        String annotated = "+65 9123 4567 (HP), 6516-2727 ext 21 (Office); " + "contact note ".repeat(40).strip();
        StudentBook original = new StudentBook();
        original.addStudent(new StudentBuilder().withName("Long Phone").withPhone(digits).build());
        original.addStudent(new StudentBuilder().withName("Annotated Phone").withPhone(annotated).build());
        Path filePath = testFolder.resolve("LongPhones.json");
        JsonStudentBookStorage storage = new JsonStudentBookStorage(filePath);

        storage.saveStudentBook(original);

        String saved = Files.readString(filePath);
        assertTrue(saved.contains(digits));
        assertTrue(saved.contains(annotated));
        ReadOnlyStudentBook loaded = new JsonStudentBookStorage(filePath).readStudentBook().orElseThrow();
        assertEquals(original, new StudentBook(loaded));
        assertEquals(digits, loaded.getStudentList().get(0).getPhone().orElseThrow().value);
        assertEquals(annotated, loaded.getStudentList().get(1).getPhone().orElseThrow().value);
    }

    @Test
    public void readAndSaveStudentBook_widenedPhoneAndTag_roundTrips() throws Exception {
        // A phone number with a country code and a tag with a space are newly accepted.
        // Saving is only half the claim; they have to come back unchanged.
        Path filePath = testFolder.resolve("WidenedValues.json");
        Student student = new StudentBuilder().withName("Ravi s/o Kumaran")
                .withPhone("+65 9123 4567").withEmail("e0923841@u.nus.edu")
                .withTags("Lab 3", "needs-followup").build();

        StudentBook original = new StudentBook();
        original.addStudent(student);

        JsonStudentBookStorage storage = new JsonStudentBookStorage(filePath);
        storage.saveStudentBook(original, filePath);
        ReadOnlyStudentBook readBack = storage.readStudentBook(filePath).get();

        assertEquals(original, new StudentBook(readBack));

        Student loaded = readBack.getStudentList().get(0);
        assertEquals("+65 9123 4567", loaded.getPhone().orElseThrow().value);
        assertEquals("Ravi s/o Kumaran", loaded.getName().fullName);
        assertEquals(student.getTags(), loaded.getTags());
    }

    @Test
    public void readAndSaveStudentBook_handEditedInvisibleMarks_loadsAndSavesBackWithoutThem() throws Exception {
        // A file edited by hand can hold a value with a format character pasted into it,
        // the state v1.3 left behind. Loading has to drop the mark, and every save after
        // that has to keep it out of the file.
        Path filePath = testFolder.resolve("InvisibleMarks.json");
        Files.writeString(filePath, """
                {
                  "students": [ {
                    "name": "\u200EJohn Doe",
                    "phone": "\u202A+65 9123 4567\u202C",
                    "tags": [ "\u200Ffriend" ]
                  } ]
                }
                """);
        JsonStudentBookStorage storage = new JsonStudentBookStorage(filePath);

        ReadOnlyStudentBook loaded = storage.readStudentBook().get();

        Student expected = new StudentBuilder().withName("John Doe").withPhone("+65 9123 4567")
                .withTags("friend").withoutEmail().withoutNusStudentId().build();
        assertEquals(1, loaded.getStudentList().size());
        assertEquals(expected, loaded.getStudentList().get(0));

        storage.saveStudentBook(new StudentBook(loaded));

        String saved = Files.readString(filePath);
        assertFalse(saved.contains("\u200E"));
        assertFalse(saved.contains("\u202A"));
        assertFalse(saved.contains("\u202C"));
        assertFalse(saved.contains("\u200F"));
        assertEquals(expected, storage.readStudentBook().get().getStudentList().get(0));
    }

    @Test
    public void readAndSaveStudentBook_studentWithoutEmail_roundTripsAndLeavesTheKeyOut() throws Exception {
        Path filePath = testFolder.resolve("NoEmail.json");
        Student student = new StudentBuilder().withName("Wong Mei Ling").withPhone("91234567")
                .withTags("Lab 3").withoutEmail().build();

        StudentBook original = new StudentBook();
        original.addStudent(student);

        JsonStudentBookStorage storage = new JsonStudentBookStorage(filePath);
        storage.saveStudentBook(original, filePath);

        // the file is meant to stay editable by hand, so an absent email is an absent key
        assertFalse(Files.readString(filePath).contains("email"));

        ReadOnlyStudentBook readBack = storage.readStudentBook(filePath).get();
        assertEquals(original, new StudentBook(readBack));
        assertEquals(Optional.empty(), readBack.getStudentList().get(0).getEmail());
    }

    @Test
    public void readAndSaveStudentBook_optionalNusStudentId_roundTripsAndLeavesAbsentKeyOut() throws Exception {
        Path filePath = testFolder.resolve("OptionalNusStudentId.json");
        Student withNusStudentId = new StudentBuilder().withName("Wong Mei Ling").withPhone("91234567")
                .withNusStudentId("A01234567X").build();
        Student withoutNusStudentId = new StudentBuilder().withName("Ravi Kumar").withPhone("91234568").build();
        StudentBook original = new StudentBook();
        original.addStudent(withNusStudentId);
        original.addStudent(withoutNusStudentId);

        JsonStudentBookStorage storage = new JsonStudentBookStorage(filePath);
        storage.saveStudentBook(original, filePath);

        String savedJson = Files.readString(filePath);
        assertTrue(savedJson.contains("\"nusStudentId\" : \"A01234567X\""));
        assertEquals(1, savedJson.split("\"nusStudentId\"", -1).length - 1);
        assertEquals(original, new StudentBook(storage.readStudentBook(filePath).get()));
    }

    @Test
    public void readAndSaveStudentBook_sameNameWithDifferentOrAbsentNusStudentIds_roundTrips() throws Exception {
        Path filePath = testFolder.resolve("StudentsWithSameName.json");
        Student first = new StudentBuilder().withName("Tan Wei Ming").withPhone("91234567")
                .withNusStudentId("A01234567X").build();
        Student second = new StudentBuilder(first).withPhone("92345678")
                .withNusStudentId("A76543210Y").build();
        Student withoutNusStudentId = new StudentBuilder(first).withPhone("93456789")
                .withoutNusStudentId().build();
        StudentBook original = new StudentBook();
        original.addStudent(first);
        original.addStudent(second);
        original.addStudent(withoutNusStudentId);
        JsonStudentBookStorage storage = new JsonStudentBookStorage(filePath);

        storage.saveStudentBook(original, filePath);
        ReadOnlyStudentBook readBack = storage.readStudentBook(filePath).get();

        assertEquals(original, new StudentBook(readBack));
        assertEquals(3, readBack.getStudentList().size());
    }

    @Test
    public void readAndSaveStudentBook_identicalStudentsWithoutNusStudentIds_preservesCountAndOrder()
            throws Exception {
        Path filePath = testFolder.resolve("IdenticalStudentsWithoutNusStudentIds.json");
        Student first = new StudentBuilder().withName("Tan Wei Ming").withPhone("91234567")
                .withoutNusStudentId().build();
        Student middle = new StudentBuilder().withName("Alice Tan").withPhone("92345678")
                .withoutNusStudentId().build();
        Student second = new StudentBuilder(first).build();
        StudentBook original = new StudentBook();
        original.addStudent(first);
        original.addStudent(middle);
        original.addStudent(second);
        JsonStudentBookStorage storage = new JsonStudentBookStorage(filePath);

        storage.saveStudentBook(original, filePath);
        ReadOnlyStudentBook readBack = storage.readStudentBook(filePath).orElseThrow();

        assertEquals(3, readBack.getStudentList().size());
        assertEquals(List.of(first, middle, second), readBack.getStudentList());
    }

    @Test
    public void readAndSaveStudentBook_followUpFlag_roundTripsAndOmitsFalse() throws Exception {
        Path filePath = testFolder.resolve("FollowUpFlag.json");
        Student flaggedStudent = new StudentBuilder().withName("Wong Mei Ling").withPhone("91234567")
                .withFlag(true).build();
        Student unflaggedStudent = new StudentBuilder().withName("Ravi Kumar").withPhone("91234568")
                .withFlag(false).build();
        StudentBook original = new StudentBook();
        original.addStudent(flaggedStudent);
        original.addStudent(unflaggedStudent);

        JsonStudentBookStorage storage = new JsonStudentBookStorage(filePath);
        storage.saveStudentBook(original, filePath);

        String savedJson = Files.readString(filePath);
        assertTrue(savedJson.contains("\"flag\" : true"));
        assertEquals(1, savedJson.split("\"flag\"", -1).length - 1);
        assertEquals(original, new StudentBook(storage.readStudentBook(filePath).get()));
    }

    @Test
    public void readAndSaveStudentBook_clearedOptionalDetails_omitsKeysAndPreservesOtherFields() throws Exception {
        Path filePath = testFolder.resolve("ClearedDetails.json");
        Student student = new StudentBuilder(ALICE).withFlag(true).build();
        StudentBook book = new StudentBook();
        book.addStudent(student);
        JsonStudentBookStorage storage = new JsonStudentBookStorage(filePath);
        storage.saveStudentBook(book);
        Student loaded = storage.readStudentBook().orElseThrow().getStudentList().get(0);
        assertEquals(student, loaded);

        Student cleared = new StudentBuilder(loaded).withoutEmail().withoutNusStudentId().build();
        book.setStudent(student, cleared);
        storage.saveStudentBook(book);

        String savedJson = Files.readString(filePath);
        assertFalse(savedJson.contains("\"email\""));
        assertFalse(savedJson.contains("\"nusStudentId\""));
        assertTrue(savedJson.contains("\"flag\" : true"));
        assertEquals(book, new StudentBook(storage.readStudentBook().orElseThrow()));
    }

    @Test
    public void saveStudentBook_nullStudentBook_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveStudentBook(null, "SomeFile.json"));
    }

    /**
     * Saves {@code studentBook} at the specified {@code filePath}.
     */
    private void saveStudentBook(ReadOnlyStudentBook studentBook, String filePath) {
        try {
            new JsonStudentBookStorage(Paths.get(filePath))
                    .saveStudentBook(studentBook, addToTestDataPathIfNotNull(filePath));
        } catch (IOException ioe) {
            throw new AssertionError("There should not be an error writing to the file.", ioe);
        }
    }

    @Test
    public void saveStudentBook_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveStudentBook(new StudentBook(), null));
    }
}
