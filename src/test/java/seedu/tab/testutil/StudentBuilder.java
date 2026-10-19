package seedu.tab.testutil;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import seedu.tab.model.student.Email;
import seedu.tab.model.student.Name;
import seedu.tab.model.student.NusStudentId;
import seedu.tab.model.student.Phone;
import seedu.tab.model.student.Student;
import seedu.tab.model.tag.Tag;
import seedu.tab.model.util.SampleDataUtil;

/**
 * A utility class to help with building Student objects.
 */
public class StudentBuilder {

    public static final String DEFAULT_NAME = "Amy Bee";
    public static final String DEFAULT_PHONE = "85355255";
    public static final String DEFAULT_EMAIL = "amy@gmail.com";

    private Name name;
    private Phone phone;
    private Optional<Email> email;
    private Optional<NusStudentId> nusStudentId;
    private Set<Tag> tags;
    private boolean isFlagged;

    /**
     * Creates a {@code StudentBuilder} with the default details.
     */
    public StudentBuilder() {
        name = new Name(DEFAULT_NAME);
        phone = new Phone(DEFAULT_PHONE);
        email = Optional.of(new Email(DEFAULT_EMAIL));
        nusStudentId = Optional.empty();
        tags = new HashSet<>();
        isFlagged = false;
    }

    /**
     * Initializes the StudentBuilder with the data of {@code studentToCopy}.
     */
    public StudentBuilder(Student studentToCopy) {
        name = studentToCopy.getName();
        phone = studentToCopy.getPhone();
        email = studentToCopy.getEmail();
        nusStudentId = studentToCopy.getNusStudentId();
        tags = new HashSet<>(studentToCopy.getTags());
        isFlagged = studentToCopy.isFlagged();
    }

    /**
     * Sets the {@code Name} of the {@code Student} that we are building.
     */
    public StudentBuilder withName(String name) {
        this.name = new Name(name);
        return this;
    }

    /**
     * Parses the {@code tags} into a {@code Set<Tag>} and sets it to the {@code Student} that we are building.
     */
    public StudentBuilder withTags(String ... tags) {
        this.tags = SampleDataUtil.getTagSet(tags);
        return this;
    }

    /**
     * Sets the {@code Phone} of the {@code Student} that we are building.
     */
    public StudentBuilder withPhone(String phone) {
        this.phone = new Phone(phone);
        return this;
    }

    /**
     * Sets the {@code Email} of the {@code Student} that we are building.
     */
    public StudentBuilder withEmail(String email) {
        this.email = Optional.of(new Email(email));
        return this;
    }

    /**
     * Leaves the {@code Student} that we are building without an email.
     */
    public StudentBuilder withoutEmail() {
        this.email = Optional.empty();
        return this;
    }

    /**
     * Sets the {@code NusStudentId} of the {@code Student} that we are building.
     */
    public StudentBuilder withNusStudentId(String nusStudentId) {
        this.nusStudentId = Optional.of(new NusStudentId(nusStudentId));
        return this;
    }

    /**
     * Leaves the {@code Student} that we are building without an NUS Student ID.
     */
    public StudentBuilder withoutNusStudentId() {
        this.nusStudentId = Optional.empty();
        return this;
    }

    /**
     * Sets whether the {@code Student} that we are building needs follow-up.
     */
    public StudentBuilder withFlag(boolean isFlagged) {
        this.isFlagged = isFlagged;
        return this;
    }

    public Student build() {
        return new Student(name, phone, email, nusStudentId, tags, isFlagged);
    }

}
