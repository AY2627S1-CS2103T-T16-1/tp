package seedu.tab.commons.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class FileUtilTest {

    @TempDir
    public Path testFolder;

    @Test
    public void readFromFile_validUtf8_preservesTextAndRemovesOnlyLeadingBom() throws Exception {
        Path file = testFolder.resolve("utf8.txt");
        for (String text : new String[] {"", " ", "José 王小明 😀", "a\uFEFFb"}) {
            for (String prefix : new String[] {"", "\uFEFF"}) {
                Files.writeString(file, prefix + text);
                assertEquals(text, FileUtil.readFromFile(file));
            }
        }
    }

    @Test
    public void readFromFile_malformedUtf8_rejectsWithoutReplacingBytes() throws Exception {
        Path file = testFolder.resolve("malformed.txt");
        byte[][] malformedInputs = {
            {(byte) 0xE9}, // Windows-1252 e-acute
            {(byte) 0xC3}, // truncated multibyte character
            {(byte) 0xC3, 0x28}, // invalid continuation byte
            {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, (byte) 0x80}, // BOM followed by invalid UTF-8
        };
        for (byte[] bytes : malformedInputs) {
            Files.write(file, bytes);
            assertThrows(MalformedInputException.class, () -> FileUtil.readFromFile(file));
            assertArrayEquals(bytes, Files.readAllBytes(file));
        }
    }
}
