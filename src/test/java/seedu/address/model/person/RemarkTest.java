package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static seedu.address.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class RemarkTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Remark(null));
    }

    @Test
    public void equals() {
        Remark remark = new Remark("Likes to swim.");

        assertEquals(remark, new Remark("Likes to swim."));
        assertNotEquals(remark, new Remark("Likes to ski."));
        assertNotEquals(remark, "Likes to swim.");
    }

    @Test
    public void emptyRemark_isAllowed() {
        assertEquals("", new Remark("").value);
    }
}
