package seedu.tab.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.tab.logic.parser.CliFlags.FLAG_EMAIL;
import static seedu.tab.logic.parser.CliFlags.FLAG_FOLLOW_UP;
import static seedu.tab.logic.parser.CliFlags.FLAG_NAME;
import static seedu.tab.logic.parser.CliFlags.FLAG_NO_FOLLOW_UP;
import static seedu.tab.logic.parser.CliFlags.FLAG_NUS_STUDENT_ID;
import static seedu.tab.logic.parser.CliFlags.FLAG_PHONE;
import static seedu.tab.logic.parser.CliFlags.FLAG_TAG;
import static seedu.tab.logic.parser.CliFlags.FLAG_TARGET_ID;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import seedu.tab.commons.core.LogsCenter;
import seedu.tab.commons.core.index.Index;
import seedu.tab.commons.util.ToStringBuilder;
import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.exceptions.CommandException;
import seedu.tab.model.Model;
import seedu.tab.model.student.Email;
import seedu.tab.model.student.Name;
import seedu.tab.model.student.NusStudentId;
import seedu.tab.model.student.Phone;
import seedu.tab.model.student.Student;
import seedu.tab.model.tag.Tag;

/**
 * Edits the details of an existing student in the student book, identified by their
 * displayed index or their NUS Student ID.
 */
public class EditCommand extends Command {
    public static final String COMMAND_WORD = "edit";

    /** The target is given before any option rather than marked by one. */
    public static final String FIELD_INDEX = "INDEX";

    /** How the missing target is named, so a user who typed only an option is shown both forms. */
    public static final String FIELD_TARGET = FIELD_INDEX + " or NUS_STUDENT_ID";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Edits the details of the student identified "
            + "by the index number used in the displayed student list, or by the NUS Student ID "
            + "they hold anywhere in the book. Use " + FLAG_TARGET_ID.getLabel()
            + " for an ID starting with a digit; " + FLAG_NUS_STUDENT_ID + " sets the ID field. "
            + "Existing values will be overwritten by the input values, while "
            + FLAG_FOLLOW_UP.getMarkerSummary() + " sets follow-up and "
            + FLAG_NO_FOLLOW_UP.getMarkerSummary() + " clears it.\n"
            + "Parameters: " + FIELD_INDEX + " (must be a positive integer), NUS_STUDENT_ID, or "
            + FLAG_TARGET_ID.getLabel() + " "
            + "[" + FLAG_NAME.getLabel() + "] "
            + "[" + FLAG_PHONE.getLabel() + "] "
            + "[" + FLAG_EMAIL.getLabel() + "] "
            + "[" + FLAG_NUS_STUDENT_ID.getLabel() + "] "
            + "[" + FLAG_TAG.getLabel() + "]... "
            + "[" + FLAG_FOLLOW_UP.getLabel() + " | " + FLAG_NO_FOLLOW_UP.getLabel() + "]\n"
            + "A target without an option comes first. Any option value holding spaces, and any value opening with "
            + "a hyphen, goes in double quotes. The value options accept either spelling: "
            + FLAG_NAME.getMarkerSummary() + ", " + FLAG_PHONE.getMarkerSummary() + ", "
            + FLAG_EMAIL.getMarkerSummary() + ", " + FLAG_NUS_STUDENT_ID.getMarkerSummary() + " and "
            + FLAG_TAG.getMarkerSummary() + ". "
            + FLAG_TAG + " \"\" clears every tag; "
            + FLAG_PHONE + " \"\" clears the phone number; "
            + FLAG_EMAIL + " \"\" clears the email; "
            + FLAG_NUS_STUDENT_ID + " \"\" clears it.\n"
            + "Example: " + COMMAND_WORD + " 1 "
            + FLAG_PHONE + " 91234567 "
            + FLAG_EMAIL + " johndoe@example.com "
            + FLAG_NUS_STUDENT_ID + " A01234567X\n"
            + "Example: " + COMMAND_WORD + " A0123456X "
            + FLAG_PHONE + " 91234567\n"
            + "Example: " + COMMAND_WORD + " " + FLAG_TARGET_ID + " 1234 " + FLAG_NO_FOLLOW_UP;

    public static final String MESSAGE_EDIT_STUDENT_SUCCESS = "Edited student: %1$s";
    public static final String MESSAGE_UNCHANGED = "Student already has the requested details. No changes made.";
    public static final String MESSAGE_MULTIPLE_TARGETS =
            "Give one student target: an index, a letter-leading NUS Student ID, or --target-id NUS_STUDENT_ID.";
    public static final String MESSAGE_NOT_EDITED = "At least one field to edit must be provided.";

    private static final Logger logger = LogsCenter.getLogger(EditCommand.class);

    private final StudentTarget target;
    private final EditStudentDescriptor editStudentDescriptor;

    /**
     * Creates an edit command for a displayed student index.
     *
     * @param targetIndex of the student in the filtered student list to edit.
     * @param editStudentDescriptor details to edit the student with.
     */
    public EditCommand(Index targetIndex, EditStudentDescriptor editStudentDescriptor) {
        this(StudentTarget.byIndex(targetIndex), editStudentDescriptor);
    }

    /**
     * Creates an edit command for the given student target.
     *
     * @param target names the student to edit, by displayed index or by NUS Student ID.
     * @param editStudentDescriptor details to edit the student with.
     */
    public EditCommand(StudentTarget target, EditStudentDescriptor editStudentDescriptor) {
        requireNonNull(target);
        requireNonNull(editStudentDescriptor);

        this.target = target;
        this.editStudentDescriptor = new EditStudentDescriptor(editStudentDescriptor);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Student studentToEdit = target.resolve(model);
        Student editedStudent = createEditedStudent(studentToEdit, editStudentDescriptor);
        if (studentToEdit.equals(editedStudent)) {
            return new CommandResult(MESSAGE_UNCHANGED);
        }

        StudentIdValidation.requireNoDuplicate(model, editedStudent, studentToEdit);

        String feedback = LikelyDuplicateWarning.appendChangedFieldsTo(
                String.format(MESSAGE_EDIT_STUDENT_SUCCESS, Messages.format(editedStudent)),
                studentToEdit, editedStudent, model.getStudentBook().getStudentList());
        model.setStudent(studentToEdit, editedStudent);
        logger.info("Edited the student " + target.describe()
                + "; the book now holds " + model.getStudentBook().getStudentList().size());
        return new CommandResult(feedback);
    }

    /**
     * Creates and returns a {@code Student} with the details of {@code studentToEdit}
     * edited with {@code editStudentDescriptor}.
     */
    private static Student createEditedStudent(Student studentToEdit, EditStudentDescriptor editStudentDescriptor) {
        assert studentToEdit != null;

        Name updatedName = editStudentDescriptor.getName().orElse(studentToEdit.getName());
        Optional<Phone> updatedPhone = editStudentDescriptor.isPhoneEdited()
                ? editStudentDescriptor.getPhone()
                : studentToEdit.getPhone();
        Optional<Email> updatedEmail = editStudentDescriptor.isEmailEdited()
                ? editStudentDescriptor.getEmail()
                : studentToEdit.getEmail();
        Optional<NusStudentId> updatedNusStudentId = editStudentDescriptor.isNusStudentIdEdited()
                ? editStudentDescriptor.getNusStudentId()
                : studentToEdit.getNusStudentId();
        Set<Tag> updatedTags = editStudentDescriptor.getTags().orElse(studentToEdit.getTags());
        boolean isFlaggedAfterEdit = editStudentDescriptor.getFlagged().orElse(studentToEdit.isFlagged());

        return new Student(updatedName, updatedPhone, updatedEmail, updatedNusStudentId,
                updatedTags, isFlaggedAfterEdit);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof EditCommand otherEditCommand)) {
            return false;
        }

        return target.equals(otherEditCommand.target)
                && editStudentDescriptor.equals(otherEditCommand.editStudentDescriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(target, editStudentDescriptor);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("target", target)
                .add("editStudentDescriptor", editStudentDescriptor)
                .toString();
    }

    /**
     * Stores the details to edit the student with. Each non-empty field value will replace the
     * corresponding field value of the student.
     */
    public static class EditStudentDescriptor {
        private Name name;
        private final OptionalFieldEdit<Phone> phoneEdit;
        private final OptionalFieldEdit<Email> emailEdit;
        private final OptionalFieldEdit<NusStudentId> nusStudentIdEdit;
        private Set<Tag> tags;
        private Boolean isFlagged;

        /**
         * Constructs an empty descriptor whose optional fields have not been edited.
         */
        public EditStudentDescriptor() {
            phoneEdit = new OptionalFieldEdit<>();
            emailEdit = new OptionalFieldEdit<>();
            nusStudentIdEdit = new OptionalFieldEdit<>();
        }

        /**
         * Copies the changes recorded in the descriptor.
         * A defensive copy of {@code tags} is used internally.
         */
        public EditStudentDescriptor(EditStudentDescriptor toCopy) {
            requireNonNull(toCopy);
            phoneEdit = new OptionalFieldEdit<>(toCopy.phoneEdit);
            emailEdit = new OptionalFieldEdit<>(toCopy.emailEdit);
            nusStudentIdEdit = new OptionalFieldEdit<>(toCopy.nusStudentIdEdit);
            setName(toCopy.name);
            setTags(toCopy.tags);
            isFlagged = toCopy.isFlagged;
        }

        public void setName(Name name) {
            this.name = name;
        }

        public Optional<Name> getName() {
            return Optional.ofNullable(name);
        }

        /**
         * Sets a replacement phone number and marks the phone as edited.
         *
         * @throws NullPointerException if {@code phone} is null.
         */
        public void setPhone(Phone phone) {
            phoneEdit.set(requireNonNull(phone));
        }

        /**
         * Marks the phone number for removal without changing any other field.
         */
        public void clearPhone() {
            phoneEdit.clear();
        }

        public boolean isPhoneEdited() {
            return phoneEdit.isEdited();
        }

        public Optional<Phone> getPhone() {
            return phoneEdit.getValue();
        }

        /**
         * Sets a replacement email and marks the email as edited.
         *
         * @throws NullPointerException if {@code email} is null.
         */
        public void setEmail(Email email) {
            emailEdit.set(requireNonNull(email));
        }

        /**
         * Marks the email for removal without changing any other field.
         */
        public void clearEmail() {
            emailEdit.clear();
        }

        public boolean isEmailEdited() {
            return emailEdit.isEdited();
        }

        public Optional<Email> getEmail() {
            return emailEdit.getValue();
        }

        /**
         * Sets a replacement NUS Student ID and marks the NUS Student ID as edited.
         *
         * @throws NullPointerException if {@code nusStudentId} is null.
         */
        public void setNusStudentId(NusStudentId nusStudentId) {
            nusStudentIdEdit.set(requireNonNull(nusStudentId));
        }

        /**
         * Marks the NUS Student ID for removal without changing any other field.
         */
        public void clearNusStudentId() {
            nusStudentIdEdit.clear();
        }

        public boolean isNusStudentIdEdited() {
            return nusStudentIdEdit.isEdited();
        }

        public Optional<NusStudentId> getNusStudentId() {
            return nusStudentIdEdit.getValue();
        }

        /**
         * Sets {@code tags} to this object's {@code tags}.
         * A defensive copy of {@code tags} is used internally.
         */
        public void setTags(Set<Tag> tags) {
            this.tags = (tags != null) ? new HashSet<>(tags) : null;
        }

        /**
         * Returns an unmodifiable tag set, which throws {@code UnsupportedOperationException}
         * if modification is attempted.
         * Returns {@code Optional#empty()} if {@code tags} is null.
         */
        public Optional<Set<Tag>> getTags() {
            return (tags != null) ? Optional.of(Collections.unmodifiableSet(tags)) : Optional.empty();
        }

        public void setFlagged(boolean isFlagged) {
            this.isFlagged = isFlagged;
        }

        /**
         * Returns the requested follow-up state, or empty if it should remain unchanged.
         */
        public Optional<Boolean> getFlagged() {
            return Optional.ofNullable(isFlagged);
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }

            // instanceof handles nulls
            if (!(other instanceof EditStudentDescriptor otherEditStudentDescriptor)) {
                return false;
            }

            return Objects.equals(name, otherEditStudentDescriptor.name)
                    && phoneEdit.hasSameStateAs(otherEditStudentDescriptor.phoneEdit)
                    && emailEdit.hasSameStateAs(otherEditStudentDescriptor.emailEdit)
                    && nusStudentIdEdit.hasSameStateAs(otherEditStudentDescriptor.nusStudentIdEdit)
                    && Objects.equals(tags, otherEditStudentDescriptor.tags)
                    && Objects.equals(isFlagged, otherEditStudentDescriptor.isFlagged);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, phoneEdit.isEdited(), phoneEdit.getValue(),
                    emailEdit.isEdited(), emailEdit.getValue(),
                    nusStudentIdEdit.isEdited(), nusStudentIdEdit.getValue(), tags, isFlagged);
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .add("name", name)
                    .add("phone", phoneEdit.getValue())
                    .add("isPhoneEdited", phoneEdit.isEdited())
                    .add("email", emailEdit.getValue())
                    .add("isEmailEdited", emailEdit.isEdited())
                    .add("nusStudentId", nusStudentIdEdit.getValue())
                    .add("isNusStudentIdEdited", nusStudentIdEdit.isEdited())
                    .add("tags", tags)
                    .add("isFlagged", isFlagged)
                    .toString();
        }

        /**
         * Encapsulates the three states of an optional field in an edit: unchanged, cleared,
         * or replaced with a value.
         */
        private static final class OptionalFieldEdit<T> {
            private Optional<T> value;
            private boolean isEdited;

            private OptionalFieldEdit() {
                value = Optional.empty();
            }

            private OptionalFieldEdit(OptionalFieldEdit<T> toCopy) {
                value = toCopy.getValue();
                isEdited = toCopy.isEdited;
            }

            private void set(T value) {
                this.value = Optional.of(value);
                isEdited = true;
            }

            private void clear() {
                value = Optional.empty();
                isEdited = true;
            }

            private boolean isEdited() {
                return isEdited;
            }

            private Optional<T> getValue() {
                return value;
            }

            private boolean hasSameStateAs(OptionalFieldEdit<?> other) {
                return isEdited == other.isEdited && value.equals(other.getValue());
            }
        }
    }
}
