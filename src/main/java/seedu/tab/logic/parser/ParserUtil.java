package seedu.tab.logic.parser;

import static java.util.Objects.requireNonNull;
import static seedu.tab.logic.parser.CliFlags.FLAG_FOLLOW_UP;
import static seedu.tab.logic.parser.CliFlags.FLAG_NO_FOLLOW_UP;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import seedu.tab.commons.core.index.Index;
import seedu.tab.commons.util.StringUtil;
import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.StudentTarget;
import seedu.tab.logic.parser.exceptions.ParseException;
import seedu.tab.model.student.Email;
import seedu.tab.model.student.Name;
import seedu.tab.model.student.NusStudentId;
import seedu.tab.model.student.Phone;
import seedu.tab.model.tag.FollowUpFlag;
import seedu.tab.model.tag.Tag;

/**
 * Contains utility methods used for parsing strings in the various *Parser classes.
 */
public class ParserUtil {
    public static final String MESSAGE_INVALID_INDEX = "Index must be a positive integer.";
    public static final String MESSAGE_INDEX_TOO_LARGE = "Index \"%s\" exceeds the largest supported index ("
            + Integer.MAX_VALUE + "). Use an index from the displayed list.";
    public static final String MESSAGE_VALUE_AFTER_TARGET =
            "Unexpected value after %1$s: \"%2$s\". Put field values after an option.";
    public static final String MESSAGE_VALUE_AFTER_TARGET_ONLY =
            "Unexpected value after %1$s: \"%2$s\". %3$s accepts one index or NUS Student ID "
                    + "and nothing else.";
    public static final String MESSAGE_FOLLOW_UP_IS_NOT_A_TAG =
            getFollowUpIsNotATagMessage(FLAG_FOLLOW_UP.getLabel());

    /** How a target read as an NUS Student ID is named, as the usage messages name it. */
    public static final String FIELD_NUS_STUDENT_ID = "NUS_STUDENT_ID";

    private static final Pattern TARGET_SEPARATOR_PATTERN = Pattern.compile("(?U)\\s+");

    private ParserUtil() {} // this class only holds helpers

    /**
     * Returns the refusal of the follow-up label as a tag, pointing at {@code flagCommand} as the
     * way to mark the student instead.
     *
     * @param flagCommand What the user types to flag the student. It varies with the command being
     *     refused, because only some commands take the follow-up option themselves.
     */
    public static String getFollowUpIsNotATagMessage(String flagCommand) {
        requireNonNull(flagCommand);
        return FollowUpFlag.MESSAGE_RESERVED + " Use " + flagCommand + " to mark the student.";
    }

    /**
     * Returns the optional follow-up selector, rejecting repeated or conflicting options.
     *
     * @throws ParseException if a selector is repeated or both states are requested.
     */
    public static Optional<Boolean> parseFollowUpSelector(FlagArgumentMap arguments) throws ParseException {
        arguments.verifyNoDuplicateFlagsFor(FLAG_FOLLOW_UP, FLAG_NO_FOLLOW_UP);
        boolean hasFollowUp = arguments.getValue(FLAG_FOLLOW_UP).isPresent();
        boolean hasNoFollowUp = arguments.getValue(FLAG_NO_FOLLOW_UP).isPresent();
        if (hasFollowUp && hasNoFollowUp) {
            throw new ParseException(Messages.MESSAGE_CONFLICTING_FOLLOW_UP);
        }
        if (hasFollowUp) {
            return Optional.of(true);
        }
        return hasNoFollowUp ? Optional.of(false) : Optional.empty();
    }

    /**
     * Parses {@code oneBasedIndex} into an {@code Index} and returns it. Leading and trailing whitespaces will be
     * trimmed.
     *
     * @throws ParseException if the specified index is invalid (not a non-zero unsigned integer).
     */
    public static Index parseIndex(String oneBasedIndex) throws ParseException {
        String trimmedIndex = oneBasedIndex.trim();
        if (!StringUtil.isNonZeroUnsignedInteger(trimmedIndex)) {
            boolean hasOnlyDigits = trimmedIndex.chars().allMatch(Character::isDigit);
            boolean hasNonZeroDigit = trimmedIndex.chars().anyMatch(digit -> Character.digit(digit, 10) > 0);
            if (hasOnlyDigits && hasNonZeroDigit) {
                throw new ParseException(String.format(MESSAGE_INDEX_TOO_LARGE, Messages.shorten(trimmedIndex)));
            }
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
        String[] values = TARGET_SEPARATOR_PATTERN.split(oneBasedIndex.trim(), 2);
        Index index = parseIndex(values[0]);
        if (values.length > 1) {
            throw new ParseException(String.format(MESSAGE_VALUE_AFTER_TARGET, fieldName, Messages.shorten(values[1])));
        }
        return index;
    }

    /**
     * Parses a required student target, reporting an empty preamble as the missing
     * {@code missingField}. The first word is read the way {@link #parseStudentTarget} reads
     * it, as a displayed index or an NUS Student ID, and any word after it is refused,
     * because a target is one word before the command's options. Invalid indices keep
     * their concise validation reason rather than the full command usage.
     *
     * @param preamble The text before the first option.
     * @param fieldName The field named in diagnostics about a target read as an index.
     * @param missingField How a missing target is named, so the caller can show every target form.
     * @throws ParseException if the target is missing, holds extra words, or has invalid syntax.
     */
    public static StudentTarget parseRequiredStudentTarget(String preamble, String missingField, String fieldName)
            throws ParseException {
        requireNonNull(missingField);
        requireNonNull(fieldName);
        String trimmedPreamble = preamble.trim();
        if (trimmedPreamble.isEmpty()) {
            throw new ParseException(Messages.getErrorMessageForMissingFields(missingField));
        }
        String[] values = TARGET_SEPARATOR_PATTERN.split(trimmedPreamble, 2);
        StudentTarget target = parseNonBlankStudentTarget(values[0]);
        if (values.length > 1) {
            throw new ParseException(String.format(MESSAGE_VALUE_AFTER_TARGET,
                    getTargetFieldName(values[0], fieldName), Messages.shorten(values[1])));
        }
        return target;
    }

    /**
     * Returns how the target in {@code target} is named, so that a diagnostic about the
     * words after it describes the form the target was read as. A target {@link #isIdTarget
     * read as an NUS Student ID} is named as one, and every other target is named by
     * {@code indexFieldName}.
     *
     * @param target The one token the user gave as the target, which may be empty.
     * @param indexFieldName How the calling command names a target read as an index.
     */
    public static String getTargetFieldName(String target, String indexFieldName) {
        requireNonNull(target);
        requireNonNull(indexFieldName);
        return isIdTarget(target.trim()) ? FIELD_NUS_STUDENT_ID : indexFieldName;
    }

    /**
     * Returns true if {@code target} addresses a student by their NUS Student ID rather than by
     * the position the student holds in the displayed list. An index is all digits and a natural
     * NUS Student ID opens with a letter, so the leading character tells the two apart; an
     * all-digit ID is the exception, and needs an explicit ID option to be selected as an ID.
     *
     * @param target The one token the user gave as the target, which may be empty.
     */
    static boolean isIdTarget(String target) {
        return !target.isEmpty() && Character.isLetter(target.charAt(0));
    }

    /**
     * Parses {@code target} as the address of one student: a target opening with a letter is
     * an NUS Student ID, and any other target is the index shown in the displayed student
     * list, as {@link #isIdTarget} tells the two shapes apart.
     *
     * <p>An NUS Student ID that is not a valid one keeps the reason it was refused, so the
     * user is told which value was rejected and why. Oversized positive indices retain their
     * bounded-index diagnostic, and an index that is not a positive integer keeps the concise
     * positive-integer reason rather than the full command usage. A target given as no value
     * at all names no field to explain, so that one is reported through {@code usage}.
     *
     * @param target The one token the user gave as the target.
     * @param usage What the calling command shows when no target is given.
     * @throws ParseException if {@code target} is blank, opens with a letter without holding
     *     a valid NUS Student ID, or is otherwise not a positive integer.
     */
    public static StudentTarget parseStudentTarget(String target, String usage) throws ParseException {
        requireNonNull(target);
        requireNonNull(usage);
        String trimmedTarget = target.trim();
        if (trimmedTarget.isEmpty()) {
            throw new ParseException(String.format(Messages.MESSAGE_INVALID_COMMAND_FORMAT, usage));
        }
        return parseNonBlankStudentTarget(trimmedTarget);
    }

    /** Returns a non-blank target while retaining the field-specific validation reason. */
    private static StudentTarget parseNonBlankStudentTarget(String target) throws ParseException {
        if (isIdTarget(target)) {
            return StudentTarget.byNusStudentId(parseNusStudentId(target));
        }
        return StudentTarget.byIndex(parseIndex(target), target);
    }

    /**
     * Returns the target {@code preamble} names, as {@code readTarget} reads it, offering the ID
     * form of {@code idTargetCommand} when a value refused as an index holds an NUS Student ID: a
     * target opening with a digit reads as an index, so the user who meant such an ID has no
     * other way to reach their student.
     *
     * @param preamble The text the user gave before the command's first option.
     * @param idTargetCommand The command form that addresses a student by their NUS Student ID.
     * @param readTarget How the calling command reads its own preamble as a target.
     * @throws ParseException if {@code preamble} does not name a student.
     */
    public static StudentTarget parsePreambleTarget(String preamble, String idTargetCommand,
            ParseProblems.ValueParser<StudentTarget> readTarget) throws ParseException {
        try {
            return readTarget.parse(preamble);
        } catch (ParseException e) {
            throw withIdTargetHint(e, preamble, idTargetCommand);
        }
    }

    /**
     * Returns {@code failure} with the ID form of {@code idTargetCommand} after it when
     * {@code target} holds a valid NUS Student ID, because a digit-leading ID is refused as an
     * index and plausibly was meant as an ID. Any other refusal is returned as it is, so the
     * recovery is offered only where it leads to the student.
     *
     * @param failure Why the target was refused.
     * @param target The one token the user gave as the target.
     * @param idTargetCommand The command form that addresses a student by their NUS Student ID.
     */
    private static ParseException withIdTargetHint(ParseException failure, String target,
            String idTargetCommand) {
        requireNonNull(failure);
        requireNonNull(target);
        requireNonNull(idTargetCommand);
        if (!NusStudentId.isValidNusStudentId(target)) {
            return failure;
        }
        return new ParseException(failure.getMessage() + " "
                + Messages.getIdTargetHint(idTargetCommand, target), failure);
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
     * Parses a {@code String email} into an {@code Email}, after normalizing it as described by
     * {@link StringUtil#normalizeFieldValue(String)}.
     *
     * @throws ParseException if the given {@code email} is invalid.
     */
    public static Email parseEmail(String email) throws ParseException {
        requireNonNull(email);
        String normalizedEmail = StringUtil.normalizeFieldValue(email);
        // Email.isValidEmail judges the shape alone, so an address the user typed has to clear
        // the entry limit as well before it is accepted
        if (!Email.isWithinLengthLimit(normalizedEmail) || !Email.isValidEmail(normalizedEmail)) {
            throw new ParseException(Messages.getErrorMessageForInvalidValue(
                    Email.FIELD_NAME, normalizedEmail, Email.getFailureReason(normalizedEmail)));
        }
        return new Email(normalizedEmail);
    }

    /**
     * Parses a {@code String nusStudentId} into a {@code NusStudentId}, after normalizing it as
     * described by {@link StringUtil#normalizeFieldValue(String)}.
     *
     * @throws ParseException if the given {@code nusStudentId} is invalid.
     */
    public static NusStudentId parseNusStudentId(String nusStudentId) throws ParseException {
        requireNonNull(nusStudentId);
        String normalizedNusStudentId = StringUtil.normalizeFieldValue(nusStudentId);
        if (!NusStudentId.isValidNusStudentId(normalizedNusStudentId)) {
            throw new ParseException(Messages.getErrorMessageForInvalidValue(
                    NusStudentId.FIELD_NAME, normalizedNusStudentId,
                    NusStudentId.getFailureReason(normalizedNusStudentId)));
        }
        return new NusStudentId(normalizedNusStudentId);
    }

    /**
     * Parses tags while recording every invalid value, keeping the first spelling of
     * case-insensitive duplicates consistently with the tag command and summary.
     */
    public static Set<Tag> parseTags(Collection<String> values, ParseProblems problems) {
        Set<Tag> tags = new TreeSet<>(Comparator.comparing(tag -> tag.getTagName(), String.CASE_INSENSITIVE_ORDER));
        tags.addAll(problems.collectEach(values, ParserUtil::parseTagToAdd));
        return new HashSet<>(tags);
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
     * Parses a {@code String tag} into a {@code Tag} a student can be given.
     *
     * <p>The follow-up label names no tag, so a command that would put it on a student is
     * refused: a flagged student would otherwise carry it as a tag beside the marker that
     * already says the same thing, and the summary would count it as a tag of its own.
     * Taking it off is still read as a tag, so that {@code tag -r} can explain that the
     * marker is what the student carries.
     *
     * @throws ParseException if the given {@code tag} is invalid or names the follow-up flag.
     */
    public static Tag parseTagToAdd(String tag) throws ParseException {
        return parseTagToAdd(tag, FLAG_FOLLOW_UP.getLabel());
    }

    /**
     * Parses a {@code String tag} into a {@code Tag} a student can be given, as
     * {@link #parseTagToAdd(String)} does, naming {@code flagCommand} as the way to mark the
     * student when the label is the reserved one. A command that does not take the follow-up
     * option itself names the command that does, so that the remedy offered can be typed as it
     * stands.
     *
     * @throws ParseException if the given {@code tag} is invalid or names the follow-up flag.
     */
    public static Tag parseTagToAdd(String tag, String flagCommand) throws ParseException {
        Tag parsed = parseTag(tag);
        if (FollowUpFlag.isFollowUpName(parsed.getTagName())) {
            throw new ParseException(getFollowUpIsNotATagMessage(flagCommand));
        }
        return parsed;
    }
}
