package seedu.tab.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

public class CliFlagTest {

    @Test
    public void getLabel_joinsTheMarkerAndTheFieldName() {
        assertEquals("-e EMAIL", new CliFlag("-e", "EMAIL").getLabel());
    }

    @Test
    public void markerOnlyFlag_labelIsJustTheMarkerAndTakesNoValue() {
        CliFlag markerOnly = new CliFlag("-f");
        assertEquals("-f", markerOnly.getLabel());
        assertFalse(markerOnly.takesValue());
    }

    @Test
    public void markerOnlyFlagWithAlias_returnsEveryMarker() {
        CliFlag markerOnly = new CliFlag("--follow-up", List.of("-f"));

        assertEquals(List.of("--follow-up", "-f"), markerOnly.getMarkers());
        assertEquals("--follow-up/-f", markerOnly.getMarkerSummary());
        assertEquals("--follow-up", markerOnly.getLabel());
    }

    @Test
    public void getMarkerWithAliases_flagWithoutAliases_returnsJustThePrimaryMarker() {
        assertEquals("-e", new CliFlag("-e", "EMAIL").getMarkerWithAliases());
        assertEquals("-f", new CliFlag("-f").getMarkerWithAliases());
    }

    @Test
    public void getMarkerWithAliases_flagWithOneAlias_namesItAfterOr() {
        assertEquals("--follow-up (or -f)", new CliFlag("--follow-up", List.of("-f")).getMarkerWithAliases());
    }

    @Test
    public void getMarkerWithAliases_flagWithSeveralAliases_namesThemAllAfterOr() {
        CliFlag valueTaking = new CliFlag("-a", List.of("--add", "--append"), "TAG");

        assertEquals("-a (or --add, --append)", valueTaking.getMarkerWithAliases());
    }

    @Test
    public void valueTakingFlag_takesValue() {
        assertTrue(new CliFlag("-e", "EMAIL").takesValue());
    }

    @Test
    public void valueTakingFlagWithAlias_returnsEveryMarkerAndLabelsEitherOne() {
        CliFlag valueTaking = new CliFlag("-e", List.of("--email"), "EMAIL");

        assertEquals(List.of("-e", "--email"), valueTaking.getMarkers());
        assertEquals("-e/--email", valueTaking.getMarkerSummary());
        assertEquals("-e EMAIL", valueTaking.getLabel());
        assertEquals("--email EMAIL", valueTaking.getLabel("--email"));
        assertTrue(valueTaking.takesValue());
    }

    @Test
    public void valueTakingFlagWithShortAlias_returnsEveryMarkerAndLabelsEitherOne() {
        CliFlag valueTaking = new CliFlag("--id", List.of("-i"), "NUS_STUDENT_ID");

        assertEquals(List.of("--id", "-i"), valueTaking.getMarkers());
        assertEquals("--id/-i", valueTaking.getMarkerSummary());
        assertEquals("--id NUS_STUDENT_ID", valueTaking.getLabel());
        assertEquals("-i NUS_STUDENT_ID", valueTaking.getLabel("-i"));
        assertTrue(valueTaking.takesValue());
    }

    @Test
    public void getLabel_unknownMarker_throwsAssertionError() {
        CliFlag valueTaking = new CliFlag("-e", List.of("--email"), "EMAIL");

        assertThrows(AssertionError.class, () -> valueTaking.getLabel("--phone"));
    }

    @Test
    public void toString_isJustTheMarker() {
        // the marker alone is what a user typed, so it is what a message quotes back
        assertEquals("-e", new CliFlag("-e", "EMAIL").toString());
    }

    @Test
    public void equals_sameMarkerDifferentFieldName_returnsTrue() {
        // a flag is identified by the characters that mark it, not by what it is called,
        // so naming the fields cannot change how the tokenizer matches them
        assertEquals(new CliFlag("-e", "MAIL"), new CliFlag("-e", "EMAIL"));
    }

    @Test
    public void equals_sameObject_returnsTrue() {
        CliFlag flag = new CliFlag("-e", "EMAIL");
        assertTrue(flag.equals(flag));
    }

    @Test
    public void equals_null_returnsFalse() {
        assertFalse(new CliFlag("-e", "EMAIL").equals(null));
    }

    @Test
    public void equals_differentType_returnsFalse() {
        assertFalse(new CliFlag("-e", "EMAIL").equals("-e"));
    }

    @Test
    public void equals_differentMarker_returnsFalse() {
        assertNotEquals(new CliFlag("-e", "EMAIL"), new CliFlag("-p", "EMAIL"));
    }

    @Test
    public void hashCode_sameMarkerDifferentFieldName_returnsSameHashCode() {
        assertEquals(new CliFlag("-e", "EMAIL").hashCode(), new CliFlag("-e", "MAIL").hashCode());
    }
}
