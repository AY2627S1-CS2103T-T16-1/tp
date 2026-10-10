package seedu.tab.testutil;

import static seedu.tab.logic.parser.CliFlags.FLAG_EMAIL;
import static seedu.tab.logic.parser.CliFlags.FLAG_FOLLOW_UP;
import static seedu.tab.logic.parser.CliFlags.FLAG_NAME;
import static seedu.tab.logic.parser.CliFlags.FLAG_PHONE;
import static seedu.tab.logic.parser.CliFlags.FLAG_TAG;

import java.util.Set;

import seedu.tab.logic.commands.AddCommand;
import seedu.tab.logic.commands.EditCommand.EditStudentDescriptor;
import seedu.tab.model.student.Student;
import seedu.tab.model.tag.Tag;

/**
 * A utility class for Student.
 */
public class StudentUtil {

    /**
     * Returns an add command string for adding the {@code student}.
     */
    public static String getAddCommand(Student student) {
        return AddCommand.COMMAND_WORD + " " + getStudentDetails(student);
    }

    /**
     * Returns the part of command string for the given {@code student}'s details. Every value
     * is quoted, since a name, a tag and even a phone number may hold spaces.
     */
    public static String getStudentDetails(Student student) {
        StringBuilder sb = new StringBuilder();
        sb.append(quoted(student.getName().fullName)).append(" ");
        sb.append(FLAG_PHONE).append(" ").append(quoted(student.getPhone().value)).append(" ");
        student.getEmail().ifPresent(email ->
                sb.append(FLAG_EMAIL).append(" ").append(quoted(email.value)).append(" "));
        student.getTags().forEach(tag ->
                sb.append(FLAG_TAG).append(" ").append(quoted(tag.tagName)).append(" "));
        if (student.isFlagged()) {
            sb.append(FLAG_FOLLOW_UP).append(" ");
        }
        return sb.toString();
    }

    private static String quoted(String value) {
        String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }

    /**
     * Returns the part of command string for the given {@code EditStudentDescriptor}'s details.
     * Every value is quoted, since a name, a tag and even a phone number may hold spaces.
     */
    public static String getEditStudentDescriptorDetails(EditStudentDescriptor descriptor) {
        StringBuilder sb = new StringBuilder();
        descriptor.getName().ifPresent(name ->
                sb.append(FLAG_NAME).append(" ").append(quoted(name.fullName)).append(" "));
        descriptor.getPhone().ifPresent(phone ->
                sb.append(FLAG_PHONE).append(" ").append(quoted(phone.value)).append(" "));
        descriptor.getEmail().ifPresent(email ->
                sb.append(FLAG_EMAIL).append(" ").append(quoted(email.value)).append(" "));
        if (descriptor.getTags().isPresent()) {
            Set<Tag> tags = descriptor.getTags().get();
            if (tags.isEmpty()) {
                // an explicitly empty value, which is how every tag is removed
                sb.append(FLAG_TAG).append(" \"\" ");
            } else {
                tags.forEach(tag ->
                        sb.append(FLAG_TAG).append(" ").append(quoted(tag.tagName)).append(" "));
            }
        }
        if (descriptor.shouldToggleFlag()) {
            sb.append(FLAG_FOLLOW_UP);
        }
        return sb.toString();
    }
}
