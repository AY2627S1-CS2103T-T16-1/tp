package seedu.tab.model.student;

import static seedu.tab.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import seedu.tab.commons.util.ToStringBuilder;
import seedu.tab.model.tag.Tag;

/**
 * Represents a Student in the student book.
 * Guarantees: field values are validated and immutable. Every detail but the email is
 * present; a student may be held before an email address is known.
 */
public class Student {
    // Identity fields
    private final Name name;
    private final Phone phone;

    /** Null when the teaching assistant has not recorded one. */
    private final Email email;

    // Data fields
    private final Set<Tag> tags = new HashSet<>();
    private final boolean isFlagged;

    /**
     * Every field but the email must be present and not null. A student may be recorded before
     * an email address is known, rather than the address being invented to satisfy the command.
     */
    public Student(Name name, Phone phone, Email email, Set<Tag> tags) {
        this(name, phone, email, tags, false);
    }

    /**
     * Every field but the email must be present and not null. A student may be recorded before
     * an email address is known, rather than the address being invented to satisfy the command.
     */
    public Student(Name name, Phone phone, Email email, Set<Tag> tags, boolean isFlagged) {
        requireAllNonNull(name, phone, tags);
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.tags.addAll(tags);
        this.isFlagged = isFlagged;
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

    /**
     * Returns an immutable tag set, which throws {@code UnsupportedOperationException}
     * if modification is attempted.
     */
    public Set<Tag> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public boolean isFlagged() {
        return isFlagged;
    }

    /**
     * Returns true if both students have the same name.
     * This defines a weaker notion of equality between two students.
     */
    public boolean isSameStudent(Student otherStudent) {
        if (otherStudent == this) {
            return true;
        }

        return otherStudent != null
                && otherStudent.getName().equals(getName());
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
                && tags.equals(otherStudent.tags)
                && isFlagged == otherStudent.isFlagged;
    }

    @Override
    public int hashCode() {
        // use this method for custom fields hashing instead of implementing your own
        return Objects.hash(name, phone, email, tags, isFlagged);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("name", name)
                .add("phone", phone)
                .add("email", email)
                .add("tags", tags)
                .add("isFlagged", isFlagged)
                .toString();
    }
}
