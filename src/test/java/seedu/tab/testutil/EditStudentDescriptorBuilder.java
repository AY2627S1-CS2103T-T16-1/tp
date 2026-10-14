package seedu.tab.testutil;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import seedu.tab.logic.commands.EditCommand.EditStudentDescriptor;
import seedu.tab.model.student.Email;
import seedu.tab.model.student.Name;
import seedu.tab.model.student.NusStudentId;
import seedu.tab.model.student.Phone;
import seedu.tab.model.student.Student;
import seedu.tab.model.tag.Tag;

/**
 * A utility class to help with building EditStudentDescriptor objects.
 */
public class EditStudentDescriptorBuilder {

    private EditStudentDescriptor descriptor;

    public EditStudentDescriptorBuilder() {
        descriptor = new EditStudentDescriptor();
    }

    public EditStudentDescriptorBuilder(EditStudentDescriptor descriptor) {
        this.descriptor = new EditStudentDescriptor(descriptor);
    }

    /**
     * Returns an {@code EditStudentDescriptor} with fields containing {@code student}'s details
     */
    public EditStudentDescriptorBuilder(Student student) {
        descriptor = new EditStudentDescriptor();
        descriptor.setName(student.getName());
        descriptor.setPhone(student.getPhone());
        copyOptionalField(student.getEmail(), descriptor::setEmail, descriptor::clearEmail);
        copyOptionalField(student.getNusStudentId(), descriptor::setNusStudentId, descriptor::clearNusStudentId);
        descriptor.setTags(student.getTags());
    }

    /**
     * Sets the {@code Name} of the {@code EditStudentDescriptor} that we are building.
     */
    public EditStudentDescriptorBuilder withName(String name) {
        descriptor.setName(new Name(name));
        return this;
    }

    /**
     * Sets the {@code Phone} of the {@code EditStudentDescriptor} that we are building.
     */
    public EditStudentDescriptorBuilder withPhone(String phone) {
        descriptor.setPhone(new Phone(phone));
        return this;
    }

    /**
     * Sets the {@code Email} of the {@code EditStudentDescriptor} that we are building.
     */
    public EditStudentDescriptorBuilder withEmail(String email) {
        descriptor.setEmail(new Email(email));
        return this;
    }

    /**
     * Sets the email edit to clear the student's email.
     */
    public EditStudentDescriptorBuilder withoutEmail() {
        descriptor.clearEmail();
        return this;
    }

    /**
     * Sets the {@code NusStudentId} of the {@code EditStudentDescriptor} that we are building.
     */
    public EditStudentDescriptorBuilder withNusStudentId(String nusStudentId) {
        descriptor.setNusStudentId(new NusStudentId(nusStudentId));
        return this;
    }

    /**
     * Sets the NUS Student ID edit to clear the student's NUS Student ID.
     */
    public EditStudentDescriptorBuilder withoutNusStudentId() {
        descriptor.clearNusStudentId();
        return this;
    }

    /**
     * Parses the {@code tags} into a {@code Set<Tag>} and sets it to the {@code EditStudentDescriptor}
     * that we are building.
     */
    public EditStudentDescriptorBuilder withTags(String... tags) {
        Set<Tag> tagSet = Stream.of(tags).map(Tag::new).collect(Collectors.toSet());
        descriptor.setTags(tagSet);
        return this;
    }

    /**
     * Sets whether the {@code EditStudentDescriptor} should toggle the follow-up flag.
     */
    public EditStudentDescriptorBuilder withFlagToggled(boolean shouldToggleFlag) {
        descriptor.setFlagToggled(shouldToggleFlag);
        return this;
    }

    public EditStudentDescriptor build() {
        return descriptor;
    }

    /**
     * Copies an optional field as either a replacement or an explicit clear operation.
     */
    private static <T> void copyOptionalField(Optional<T> value, Consumer<T> setter, Runnable clearer) {
        value.ifPresentOrElse(setter, clearer);
    }
}
