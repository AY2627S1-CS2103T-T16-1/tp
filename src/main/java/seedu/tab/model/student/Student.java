package seedu.tab.model.student;

import static seedu.tab.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import seedu.tab.commons.util.ToStringBuilder;
import seedu.tab.model.tag.Flag;
import seedu.tab.model.tag.Tag;

/**
 * Represents a Student in the student book.
 * Guarantees: field values are validated and immutable. Every detail but the email and NUS Student ID is
 * present; a student may be held before either optional detail is known.
 */
public class Student {
    // Required fields
    private final Name name;
    private final Phone phone;

    /** Null when the teaching assistant has not recorded one. */
    private final Email email;
    /** Null when the teaching assistant has not recorded one. */
    private final NusStudentId nusStudentId;

    // Data fields
    private final Set<Tag> tags = new HashSet<>();
    private final Flag flag;

    /**
     * Every field but the email and NUS Student ID must be present and not null.
     */
    public Student(Name name, Phone phone, Email email, Set<Tag> tags) {
        this(name, phone, email, null, tags, false);
    }

    /**
     * Every field but the email and NUS Student ID must be present and not null.
     */
    public Student(Name name, Phone phone, Email email, Set<Tag> tags, boolean isFlagged) {
        this(name, phone, email, null, tags, isFlagged);
    }

    /**
     * Every field but the email and NUS Student ID must be present and not null.
     */
    public Student(Name name, Phone phone, Email email, NusStudentId nusStudentId, Set<Tag> tags) {
        this(name, phone, email, nusStudentId, tags, false);
    }

    /**
     * Every field but the email and NUS Student ID must be present and not null.
     */
    public Student(Name name, Phone phone, Email email, NusStudentId nusStudentId, Set<Tag> tags, boolean isFlagged) {
        requireAllNonNull(name, phone, tags);
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.nusStudentId = nusStudentId;
        this.tags.addAll(tags);
        this.flag = isFlagged ? new Flag() : null;
    }

    public Name getName() {
        return name;
    }

    public Phone getPhone() {
        return phone;
    }

    public Optional<Email> getEmail() {
        return Optional.ofNullable(email);
    }

    public Optional<NusStudentId> getNusStudentId() {
        return Optional.ofNullable(nusStudentId);
    }

    /**
     * Returns an immutable tag set, which throws {@code UnsupportedOperationException}
     * if modification is attempted.
     */
    public Set<Tag> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public boolean isFlagged() {
        return flag != null;
    }

    /** Returns the follow-up flag, when this student needs follow-up. */
    public Optional<Flag> getFlag() {
        return Optional.ofNullable(flag);
    }

    /**
     * Returns true if both students have the same NUS Student ID.
     * Students without an NUS Student ID do not have enough information to be
     * considered the same student. This rule is used to detect duplicate records,
     * while {@link #equals(Object)} compares every field.
     */
    public boolean isSameStudent(Student otherStudent) {
        return otherStudent != null
                && nusStudentId != null
                && nusStudentId.equals(otherStudent.nusStudentId);
    }

    /**
     * Returns true if both students have the same identity and data fields.
     * This defines a stronger notion of equality between two students.
     */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Student otherStudent)) {
            return false;
        }

        return name.equals(otherStudent.name)
                && phone.equals(otherStudent.phone)
                && Objects.equals(email, otherStudent.email)
                && Objects.equals(nusStudentId, otherStudent.nusStudentId)
                && tags.equals(otherStudent.tags)
                && Objects.equals(flag, otherStudent.flag);
    }

    @Override
    public int hashCode() {
        // use this method for custom fields hashing instead of implementing your own
        return Objects.hash(name, phone, email, nusStudentId, tags, flag);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("name", name)
                .add("phone", phone)
                .add("email", email)
                .add("nusStudentId", nusStudentId)
                .add("tags", tags)
                .add("isFlagged", isFlagged())
                .toString();
    }
}
