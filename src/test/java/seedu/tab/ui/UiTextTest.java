package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class UiTextTest {

    private static final int LIMIT = 18;

    /** Returns {@code value} cut from the middle to {@code LIMIT}. */
    private static String middleShortened(String value) {
        return UiText.middleShortened(value, LIMIT);
    }

    @Test
    public void middleShortened_aValueOverTheCount_keepsBothEnds() {
        assertEquals("attendanc…icipation", middleShortened("attendance-and-participation"));
    }

    @Test
    public void middleShortened_valuesSharingTheirOpening_readDifferently() {
        assertEquals("Mohamed A…in Ismail", middleShortened("Mohamed Abdul Rahman bin Ismail"));
        assertEquals("Mohamed A…bin Ahmad", middleShortened("Mohamed Abdul Rahman bin Ahmad"));
    }

    @Test
    public void middleShortened_aValueAtOrOneOverTheCount_isShownWhole() {
        assertEquals("Siti Nur-Aisyah Bt", middleShortened("Siti Nur-Aisyah Bt"));
        // one over, where the ellipsis would take back the one place the cut saves
        assertEquals("Siti Nur-Aisyah Bte", middleShortened("Siti Nur-Aisyah Bte"));
    }

    @Test
    public void middleShortened_aValueTwoOverTheCount_isCut() {
        assertEquals("Siti Nur-…syah Bint", middleShortened("Siti Nur-Aisyah Bint"));
    }

    @Test
    public void middleShortened_aCountTooSmallToHoldBothEnds_keepsTheOpeningLonger() {
        // the opening is what a reader scans by, so the close never takes more than half
        assertEquals("Moh…il", UiText.middleShortened("Mohamed Abdul Rahman bin Ismail", 5));
    }

    @Test
    public void middleShortened_anEmojiAtEitherCut_keepsItWhole() {
        String graduate = "🎓";

        // an emoji is one character to a reader and two to Java, and half of one draws as a box
        assertEquals("A".repeat(8) + graduate + "…" + "C".repeat(9),
                middleShortened("A".repeat(8) + graduate + "B".repeat(20) + "C".repeat(9)));
        assertEquals("A".repeat(9) + "…" + graduate + "C".repeat(8),
                middleShortened("A".repeat(9) + "B".repeat(20) + graduate + "C".repeat(8)));
    }

    @Test
    public void middleShortened_aCombiningAccentAtEitherCut_keepsItOnItsLetter() {
        String accented = "é"; // "é", with the accent held as a separate code point

        assertEquals("A".repeat(8) + accented + "…" + "C".repeat(9),
                middleShortened("A".repeat(8) + accented + "B".repeat(20) + "C".repeat(9)));
        assertEquals("A".repeat(9) + "…" + accented + "C".repeat(8),
                middleShortened("A".repeat(9) + "B".repeat(20) + accented + "C".repeat(8)));
    }

    @Test
    public void middleShortened_aSpaceAtEitherCut_leavesNoGapBesideTheEllipsis() {
        assertEquals("Mohammed…in Ismail", middleShortened("Mohammed Abdul Rahman bin Ismail"));
        assertEquals("Mohamed A…Abdullah", middleShortened("Mohamed Abdul Rahman Abdullah"));
    }

    @Test
    public void middleShortened_aValueInAnotherScript_keepsBothEnds() {
        assertEquals("陈伟明李小龙王大锤…"
                        + "赵四九孙悟空诸葛亮",
                middleShortened("陈伟明李小龙王大锤"
                        + "张三丰赵四九孙悟空诸葛亮"));
        // Arabic reads right to left, which is the drawing's business rather than the cut's
        assertEquals("محمد عبد…"
                        + "ن إسماعيل",
                middleShortened("محمد عبد "
                        + "الرحمن بن "
                        + "إسماعيل"));
    }
}
