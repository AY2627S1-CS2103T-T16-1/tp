package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class RemarkTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Remark(null));
    }

    @Test
    public void constructor_unconstrainedValues_preservesValue() {
        assertEquals("", new Remark("").value);
        assertEquals(" ", new Remark(" ").value);
        String remark = "Likes baseball, swimming & café visits!\nCall after 6pm.";
        assertEquals(remark, new Remark(remark).value);
    }

    @Test
    public void equals() {
        Remark remark = new Remark("Likes baseball");

        assertTrue(remark.equals(remark));
        assertTrue(remark.equals(new Remark("Likes baseball")));
        assertFalse(remark.equals(null));
        assertFalse(remark.equals("Likes baseball"));
        assertFalse(remark.equals(new Remark("Likes swimming")));
        assertFalse(remark.equals(new Remark("")));
    }

    @Test
    public void hashCode_sameValue_sameHashCode() {
        assertEquals(new Remark("Likes baseball").hashCode(), new Remark("Likes baseball").hashCode());
    }

    @Test
    public void toString_returnsValue() {
        assertEquals("Likes baseball", new Remark("Likes baseball").toString());
    }
}
