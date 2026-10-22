package seedu.tab.model.util;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import seedu.tab.model.ReadOnlyStudentBook;
import seedu.tab.model.StudentBook;
import seedu.tab.model.student.Email;
import seedu.tab.model.student.Name;
import seedu.tab.model.student.NusStudentId;
import seedu.tab.model.student.Phone;
import seedu.tab.model.student.Student;
import seedu.tab.model.tag.Tag;

/**
 * Contains utility methods for populating {@code StudentBook} with sample data.
 */
public class SampleDataUtil {
    private SampleDataUtil() {} // this class only holds helpers

    public static Student[] getSampleStudents() {
        return new Student[] {
            new Student(new Name("Alex Yeoh"), new Phone("87438807"), Optional.of(new Email("alexyeoh@example.com")),
                Optional.of(new NusStudentId("A01234567X")), getTagSet("friends")),
            new Student(new Name("Bernice Yu"), new Phone("99272758"), Optional.of(new Email("berniceyu@example.com")),
                Optional.empty(), getTagSet("colleagues", "friends")),
            new Student(new Name("Charlotte Oliveiro"), new Phone("93210283"),
                Optional.of(new Email("charlotte@example.com")), Optional.of(new NusStudentId("A02345678G")),
                getTagSet("neighbours")),
            new Student(new Name("David Li"), new Phone("91031282"), Optional.of(new Email("lidavid@example.com")),
                Optional.of(new NusStudentId("A03456789H")), getTagSet("family")),
            new Student(new Name("Irfan Ibrahim"), new Phone("92492021"), Optional.of(new Email("irfan@example.com")),
                Optional.empty(), getTagSet("classmates")),
            new Student(new Name("Roy Balakrishnan"), new Phone("92624417"), Optional.of(new Email("royb@example.com")),
                Optional.of(new NusStudentId("A04567890J")), getTagSet("colleagues"))
        };
    }

    public static ReadOnlyStudentBook getSampleStudentBook() {
        StudentBook sampleAb = new StudentBook();
        for (Student sampleStudent : getSampleStudents()) {
            sampleAb.addStudent(sampleStudent);
        }
        return sampleAb;
    }

    /**
     * Returns a tag set containing the list of strings given.
     */
    public static Set<Tag> getTagSet(String... strings) {
        return Arrays.stream(strings)
                .map(Tag::new)
                .collect(Collectors.toSet());
    }

}
