package seedu.tab.logic.parser;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import seedu.tab.commons.core.index.Index;
import seedu.tab.commons.util.StringUtil;
import seedu.tab.logic.Messages;
import seedu.tab.logic.parser.exceptions.ParseException;
import seedu.tab.model.student.Email;
import seedu.tab.model.student.Name;
import seedu.tab.model.student.NusStudentId;
import seedu.tab.model.student.Phone;
import seedu.tab.model.tag.Tag;

/**
 * Contains utility methods used for parsing strings in the various *Parser classes.
 */
public class ParserUtil {
    public static final String MESSAGE_INVALID_INDEX = "Index must be a positive integer.";
    public static final String MESSAGE_VALUE_AFTER_INDEX =
            "Unexpected value after %1$s: \"%2$s\". Put field values after an option.";
    public static final String MESSAGE_VALUE_AFTER_INDEX_ONLY =
            "Unexpected value after %1$s: \"%2$s\". %3$s accepts one index and nothing else.";

    /**
     * Parses {@code oneBasedIndex} into an {@code Index} and returns it. Leading and trailing whitespaces will be
     * trimmed.
     * @throws ParseException if the specified index is invalid (not a non-zero unsigned integer).
     */
    public static Index parseIndex(String oneBasedIndex) throws ParseException {
        String trimmedIndex = oneBasedIndex.trim();
        if (!StringUtil.isNonZeroUnsignedInteger(trimmedIndex)) {
            throw new ParseException(MESSAGE_INVALID_INDEX);
        }
        return Index.fromOneBased(Integer.parseInt(trimmedIndex));
    }

    /**
     * Parses a required index, reporting an empty preamble as the missing {@code fieldName}.
     *
     * @throws ParseException if the index is missing or invalid.
     */
    public static Index parseRequiredIndex(String oneBasedIndex, String fieldName) throws ParseException {
        if (oneBasedIndex.isEmpty()) {
            throw new ParseException(Messages.getErrorMessageForMissingFields(fieldName));
        }
        String[] values = oneBasedIndex.trim().split("(?U)\\s+", 2);
        Index index = parseIndex(values[0]);
        if (values.length > 1) {
            throw new ParseException(String.format(MESSAGE_VALUE_AFTER_INDEX, fieldName, Messages.shorten(values[1])));
        }
        return index;
    }

    /**
     * Parses a {@code String name} into a {@code Name}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code name} is invalid.
     */
    public static Name parseName(String name) throws ParseException {
        requireNonNull(name);
        String trimmedName = name.trim();
        if (!Name.isValidName(trimmedName)) {
            throw new ParseException(Messages.getErrorMessageForInvalidValue(
                    Name.FIELD_NAME, trimmedName, Name.getFailureReason(trimmedName)));
        }
        return new Name(trimmedName);
    }

    /**
     * Parses a {@code String phone} into a {@code Phone}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code phone} is invalid.
     */
    public static Phone parsePhone(String phone) throws ParseException {
        requireNonNull(phone);
        String trimmedPhone = phone.trim();
        if (!Phone.isValidPhone(trimmedPhone)) {
            throw new ParseException(Messages.getErrorMessageForInvalidValue(
                    Phone.FIELD_NAME, trimmedPhone, Phone.getFailureReason(trimmedPhone)));
        }
        return new Phone(trimmedPhone);
    }

    /**
     * Parses a {@code String email} into an {@code Email}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code email} is invalid.
     */
    public static Email parseEmail(String email) throws ParseException {
        requireNonNull(email);
        String trimmedEmail = email.trim();
        // the length is examined first, because Email.isValidEmail exhausts the stack on a long
        // enough address: an address the User typed has to clear the limit before being matched
        if (!Email.isWithinLengthLimit(trimmedEmail) || !Email.isValidEmail(trimmedEmail)) {
            throw new ParseException(Messages.getErrorMessageForInvalidValue(
                    Email.FIELD_NAME, trimmedEmail, Email.getFailureReason(trimmedEmail)));
        }
        return new Email(trimmedEmail);
    }

    /**
     * Parses a {@code String nusStudentId} into a {@code NusStudentId}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code nusStudentId} is invalid.
     */
    public static NusStudentId parseNusStudentId(String nusStudentId) throws ParseException {
        requireNonNull(nusStudentId);
        String trimmedNusStudentId = nusStudentId.trim();
        if (!NusStudentId.isValidNusStudentId(trimmedNusStudentId)) {
            throw new ParseException(Messages.getErrorMessageForInvalidValue(
                    NusStudentId.FIELD_NAME, trimmedNusStudentId, NusStudentId.getFailureReason(trimmedNusStudentId)));
        }
        return new NusStudentId(trimmedNusStudentId);
    }

    /**
     * Parses a {@code String tag} into a {@code Tag}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code tag} is invalid.
     */
    public static Tag parseTag(String tag) throws ParseException {
        requireNonNull(tag);
        String trimmedTag = tag.trim();
        if (!Tag.isValidTagName(trimmedTag)) {
            throw new ParseException(Messages.getErrorMessageForInvalidValue(
                    Tag.FIELD_NAME, trimmedTag, Tag.getFailureReason(trimmedTag)));
        }
        return new Tag(trimmedTag);
    }

    /**
     * Parses {@code Collection<String> tags} into a {@code Set<Tag>}.
     */
    public static Set<Tag> parseTags(Collection<String> tags) throws ParseException {
        requireNonNull(tags);
        final Set<Tag> tagSet = new HashSet<>();
        for (String tagName : tags) {
            tagSet.add(parseTag(tagName));
        }
        return tagSet;
    }
}
