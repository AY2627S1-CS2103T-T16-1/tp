package seedu.tab.model;

import javafx.collections.ObservableList;
import seedu.tab.model.student.Student;

/**
 * Unmodifiable view of an student book
 */
public interface ReadOnlyStudentBook {

    /**
     * Returns an unmodifiable view of the students list.
     * This list will not contain any duplicate students.
     */
    ObservableList<Student> getStudentList();

}
