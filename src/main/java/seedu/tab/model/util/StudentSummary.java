package seedu.tab.model.util;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BinaryOperator;

import seedu.tab.model.student.Student;

/**
 * Counts student statuses and tag groups independently of their presentation.
 */
public final class StudentSummary {
    private StudentSummary() {}

    /**
     * Returns how many students each tag is on, most-used first and alphabetically among ties,
     * so that the order does not shift between runs for tags used equally often.
     *
     * <p>Case variants of one tag are counted together, because one tag is what {@code tag}
     * makes of them: it refuses to add a tag a student already carries in another case. A row
     * therefore gives the size of the group a reader is looking for rather than the size of one
     * spelling of it.
     *
     * <p>Two spellings are of one tag when {@link String#CASE_INSENSITIVE_ORDER} calls them
     * equal, which is exactly when {@code String#equalsIgnoreCase} does, and so exactly when
     * {@code tag} refuses the second of them. Folding case by lowercasing would answer a
     * different question and split a group {@code tag} has already joined: a Greek capital
     * sigma lowercases to {@code \u03C3}, while the final sigma {@code \u03C2} a student may
     * carry the same tag under lowercases to itself.
     */
    public static Map<String, Integer> countByTag(List<Student> students) {
        Map<String, Integer> byVariant = new TreeMap<>();
        Map<String, Integer> counts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Student student : students) {
            student.getTags().forEach(tag -> byVariant.merge(tag.getTagName(), 1, Integer::sum));
            // a student carrying two spellings of one tag is still one student in its group
            getTags(student).forEach(tag -> counts.merge(tag, 1, Integer::sum));
        }
        return orderByUse(counts, nameEachTag(byVariant));
    }

    /**
     * Returns the tags {@code student} carries, with two spellings of one tag coming back once.
     */
    private static Set<String> getTags(Student student) {
        Set<String> tags = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        student.getTags().forEach(tag -> tags.add(tag.getTagName()));
        return tags;
    }

    /**
     * Returns the name to list each tag under, which any of its spellings looks up.
     *
     * <p>A group is named with the spelling most of its students carry, so that the panel reads
     * as the cohort was typed. Spellings carried equally often are settled by order, because a
     * name that moved with the list would read as a second tag appearing.
     */
    private static Map<String, String> nameEachTag(Map<String, Integer> byVariant) {
        BinaryOperator<String> moreUsed = (named, other) ->
                byVariant.get(other) > byVariant.get(named) ? other : named;
        Map<String, String> names = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        byVariant.keySet().forEach(variant -> names.merge(variant, variant, moreUsed));
        return names;
    }

    /**
     * Returns the tags in {@code counts} most-used first, each under the name {@code names}
     * gives it.
     *
     * <p>The keys of {@code counts} hold one spelling per tag, and no two of them are case
     * variants of each other, so ordering them without regard to case orders the rows
     * alphabetically and leaves no tie for the spelling a key happens to hold to settle.
     */
    private static Map<String, Integer> orderByUse(Map<String, Integer> counts, Map<String, String> names) {
        Map<String, Integer> ordered = new LinkedHashMap<>();
        counts.entrySet().stream()
                // two tags counted the same read as a list in alphabetical order, and a
                // reader does not read "Zoo" as coming before "apple"
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER))
                .forEach(entry -> ordered.put(names.get(entry.getKey()), entry.getValue()));
        return ordered;
    }

    /**
     * Returns how many of {@code students} need follow-up.
     */
    public static long countFlagged(List<Student> students) {
        return students.stream().filter(Student::isFlagged).count();
    }

}
