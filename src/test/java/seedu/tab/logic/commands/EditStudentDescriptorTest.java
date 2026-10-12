package seedu.tab.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.logic.commands.CommandTestUtil.DESC_AMY;
import static seedu.tab.logic.commands.CommandTestUtil.DESC_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_EMAIL_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_NAME_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_PHONE_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
import static seedu.tab.testutil.Assert.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.tab.logic.commands.EditCommand.EditStudentDescriptor;
import seedu.tab.model.student.Email;
import seedu.tab.testutil.EditStudentDescriptorBuilder;

public class EditStudentDescriptorTest {

    @Test
    public void equals() {
        // same values -> returns true
        EditStudentDescriptor descriptorWithSameValues = new EditStudentDescriptor(DESC_AMY);
        assertTrue(DESC_AMY.equals(descriptorWithSameValues));

        // same object -> returns true
        assertTrue(DESC_AMY.equals(DESC_AMY));

        // null -> returns false
        assertFalse(DESC_AMY.equals(null));

        // different types -> returns false
        assertFalse(DESC_AMY.equals(5));

        // different values -> returns false
        assertFalse(DESC_AMY.equals(DESC_BOB));

        // different name -> returns false
        EditStudentDescriptor editedAmy = new EditStudentDescriptorBuilder(DESC_AMY).withName(VALID_NAME_BOB).build();
        assertFalse(DESC_AMY.equals(editedAmy));

        // different phone -> returns false
        editedAmy = new EditStudentDescriptorBuilder(DESC_AMY).withPhone(VALID_PHONE_BOB).build();
        assertFalse(DESC_AMY.equals(editedAmy));

        // different email -> returns false
        editedAmy = new EditStudentDescriptorBuilder(DESC_AMY).withEmail(VALID_EMAIL_BOB).build();
        assertFalse(DESC_AMY.equals(editedAmy));


        // different tags -> returns false
        editedAmy = new EditStudentDescriptorBuilder(DESC_AMY).withTags(VALID_TAG_HUSBAND).build();
        assertFalse(DESC_AMY.equals(editedAmy));

        // different toggle intent -> returns false
        editedAmy = new EditStudentDescriptorBuilder(DESC_AMY).withFlagToggled(true).build();
        assertFalse(DESC_AMY.equals(editedAmy));
    }

    @Test
    public void copyConstructor_toggleIntent_copied() {
        EditStudentDescriptor source = new EditStudentDescriptorBuilder()
                .withFlagToggled(true).build();
        assertEquals(source, new EditStudentDescriptor(source));
    }

    @Test
    public void isAnyFieldEdited_toggleIntentOnly_returnsTrue() {
        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder()
                .withFlagToggled(true).build();
        assertTrue(descriptor.isAnyFieldEdited());
    }

    @Test
    public void isAnyFieldEdited_nonEmailFields_returnsTrue() {
        assertTrue(new EditStudentDescriptorBuilder().withName(VALID_NAME_BOB).build().isAnyFieldEdited());
        assertTrue(new EditStudentDescriptorBuilder().withPhone(VALID_PHONE_BOB).build().isAnyFieldEdited());
        assertTrue(new EditStudentDescriptorBuilder().withTags(VALID_TAG_HUSBAND).build().isAnyFieldEdited());
    }

    @Test
    public void copyConstructor_emailEditIntent_preserved() {
        EditStudentDescriptor untouched = new EditStudentDescriptor();
        EditStudentDescriptor cleared = new EditStudentDescriptorBuilder().withoutEmail().build();
        assertFalse(untouched.isAnyFieldEdited());
        assertFalse(new EditStudentDescriptor(untouched).isEmailEdited());
        assertTrue(cleared.isAnyFieldEdited());
        assertTrue(new EditStudentDescriptor(cleared).isEmailEdited());
        assertEquals(cleared, new EditStudentDescriptor(cleared));
        assertFalse(cleared.equals(untouched));
    }

    @Test
    public void setEmail_null_rejectsAndPreservesExistingEdit() {
        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder().withEmail(VALID_EMAIL_BOB).build();
        EditStudentDescriptor expected = new EditStudentDescriptor(descriptor);
        assertThrows(NullPointerException.class, () -> descriptor.setEmail(null));
        assertEquals(expected, descriptor);
    }

    @Test
    public void clearEmail_thenSetEmail_preservesIntentThroughCopies() {
        EditStudentDescriptor descriptor = new EditStudentDescriptor();
        Email replacement = new Email(VALID_EMAIL_BOB);
        descriptor.setEmail(replacement);
        assertEquals(Optional.of(replacement), descriptor.getEmail());
        assertEquals(descriptor, new EditStudentDescriptor(descriptor));
        descriptor.clearEmail();
        assertTrue(descriptor.isEmailEdited());
        assertEquals(Optional.empty(), descriptor.getEmail());
        assertEquals(descriptor, new EditStudentDescriptor(descriptor));
        descriptor.setEmail(replacement);
        assertTrue(descriptor.isEmailEdited());
        assertEquals(Optional.of(replacement), descriptor.getEmail());
        assertEquals(descriptor, new EditStudentDescriptor(descriptor));
    }

    @Test
    public void toStringMethod() {
        EditStudentDescriptor editStudentDescriptor = new EditStudentDescriptor();
        String expected = EditStudentDescriptor.class.getCanonicalName() + "{name="
                + editStudentDescriptor.getName().orElse(null) + ", phone="
                + editStudentDescriptor.getPhone().orElse(null) + ", email="
                + editStudentDescriptor.getEmail() + ", isEmailEdited="
                + editStudentDescriptor.isEmailEdited() + ", tags="
                + editStudentDescriptor.getTags().orElse(null) + ", shouldToggleFlag="
                + editStudentDescriptor.shouldToggleFlag() + "}";
        assertEquals(expected, editStudentDescriptor.toString());
    }
}
