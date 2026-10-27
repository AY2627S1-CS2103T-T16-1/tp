package seedu.tab.model.student;

import static seedu.tab.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import seedu.tab.commons.util.ToStringBuilder;
import seedu.tab.model.tag.FollowUpFlag;
import seedu.tab.model.tag.Tag;

/**
 * Represents a Student in the student book.
 * Guarantees: field values are validated and immutable. Only the name is always present; a student may be
 * held before any of the optional details is known.
 */
public class Student {
    // Required field
    private final Name name;

    private final Optional<Phone> phone;
    private final Optional<Email> email;
    private final Optional<NusStudentId> nusStudentId;

    // Data fields
    private final Set<Tag> tags = new HashSet<>();
    private final Optional<FollowUpFlag> flag;

    /**
     * Constructs an unflagged student. Every argument must be non-null;
     * unknown optional details are represented by {@link Optional#empty()}.
     */
    public Student(Name name, Optional<Phone> phone, Optional<Email> email, Optional<NusStudentId> nusStudentId,
            Set<Tag> tags) {
        this(name, phone, email, nusStudentId, tags, false);
    }

    /**
     * Constructs a student with the given follow-up state. Every argument must be non-null;
     * unknown optional details are represented by {@link Optional#empty()}.
     */
    public Student(Name name, Optional<Phone> phone, Optional<Email> email, Optional<NusStudentId> nusStudentId,
            Set<Tag> tags, boolean isFlagged) {
        requireAllNonNull(name, phone, email, nusStudentId, tags);
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.nusStudentId = nusStudentId;
        this.tags.addAll(tags);
        this.flag = isFlagged ? Optional.of(new FollowUpFlag()) : Optional.empty();
    }

    public Name getName() {
        return name;
    }

    public Optional<Phone> getPhone() {
        return phone;
    }

    public Optional<Email> getEmail() {
        return email;
    }

    public Optional<NusStudentId> getNusStudentId() {
        return nusStudentId;
    }

    /**
     * Returns an immutable tag set, which throws {@code UnsupportedOperationException}
     * if modification is attempted.
     */
    public Set<Tag> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public boolean isFlagged() {
        return flag.isPresent();
    }

    /** Returns the follow-up flag, when this student needs follow-up. */
    public Optional<FollowUpFlag> getFlag() {
        return flag;
    }

    /**
     * Returns true if both students have the same NUS Student ID.
     * Students without an NUS Student ID do not have enough information to be
     * considered the same student. This rule is used to detect duplicate records,
     * while {@link #equals(Object)} compares every field.
     */
    public boolean isSameStudent(Student otherStudent) {
        return otherStudent != null
                && nusStudentId.isPresent()
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
                && email.equals(otherStudent.email)
                && nusStudentId.equals(otherStudent.nusStudentId)
                && tags.equals(otherStudent.tags)
                && flag.equals(otherStudent.flag);
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
                .add("phone", phone.map(Phone::toString).orElse("null"))
                .add("email", email.map(Email::toString).orElse("null"))
                .add("nusStudentId", nusStudentId.map(NusStudentId::toString).orElse("null"))
                .add("tags", tags)
                .add("isFlagged", isFlagged())
                .toString();
    }
}
