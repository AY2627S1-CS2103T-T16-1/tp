package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class UiTextTest {

    private static final int LIMIT = 18;

    @BeforeAll
    public static void startToolkit() throws Exception {
        FxToolkit.start();
    }

    private static <T> T onFxThread(Callable<T> work) throws Exception {
        return FxToolkit.onFxThread(work);
    }

    /** Returns a label holding {@code value} cut from the middle to {@code LIMIT}. */
    private static Label middleShortened(String value) throws Exception {
        return onFxThread(() -> {
            Label label = new Label();
            UiText.showMiddleShortened(label, value, LIMIT);
            return label;
        });
    }

    /** Returns a label holding {@code value}, laid out at {@code width}. */
    private static Label labelAt(String value, double width) {
        Label label = new Label();
        UiText.showShortenedToFit(label, value, LIMIT);
        StackPane holder = new StackPane(label);
        new Scene(holder, width, 40);
        holder.applyCss();
        holder.layout();
        label.resize(width, label.prefHeight(-1));
        return label;
    }

    @Test
    public void showShortenedToFit_aValueTheWidthCuts_namesItOnHover() throws Exception {
        // under the count of characters it would be cut to, but too wide for the label
        String whole = "CS2103T-TUTORIAL";
        Label label = onFxThread(() -> labelAt(whole, 40));

        assertEquals(whole, label.getText(), "the text is under the count it is cut to");
        assertNotNull(label.getTooltip(), "a value the width cut should still be readable");
        assertEquals(whole, label.getTooltip().getText());
    }

    @Test
    public void showShortenedToFit_aValueThatFits_needsNoTooltip() throws Exception {
        Label label = onFxThread(() -> labelAt("T1", 200));

        assertEquals("T1", label.getText());
        assertNull(label.getTooltip(), "a value shown whole needs no tooltip");
    }

    @Test
    public void showShortenedToFit_aValueLongerThanTheCount_namesItOnHover() throws Exception {
        String whole = "attendance-and-participation";
        Label label = onFxThread(() -> labelAt(whole, 400));

        assertTrue(label.getText().length() < whole.length(), "the value should be cut");
        assertEquals(whole, label.getTooltip().getText());
    }

    @Test
    public void showShortenedToFit_aLabelGivenMoreRoom_takesTheTooltipBackOff() throws Exception {
        String whole = "CS2103T-TUTORIAL";
        Label label = onFxThread(() -> {
            Label narrow = labelAt(whole, 40);
            assertNotNull(narrow.getTooltip(), "a value the width cut should name itself");
            narrow.resize(400, narrow.prefHeight(-1));
            return narrow;
        });

        // a tooltip repeating what is already on the label is noise, so it goes when the
        // label has room for the whole value again
        assertNull(label.getTooltip(), "the tooltip should go once the value fits");
    }

    @Test
    public void showMiddleShortened_aValueOverTheCount_keepsBothEnds() throws Exception {
        String whole = "attendance-and-participation";
        Label label = middleShortened(whole);

        assertEquals("attendanc\u2026icipation", label.getText());
        assertEquals(whole, label.getTooltip().getText(), "the whole should still be reachable");
    }

    @Test
    public void showMiddleShortened_valuesSharingTheirOpening_readDifferently() throws Exception {
        Label ismail = middleShortened("Mohamed Abdul Rahman bin Ismail");
        Label ahmad = middleShortened("Mohamed Abdul Rahman bin Ahmad");

        assertEquals("Mohamed A\u2026in Ismail", ismail.getText());
        assertEquals("Mohamed A\u2026bin Ahmad", ahmad.getText());
    }

    @Test
    public void showMiddleShortened_aValueAtOrOneOverTheCount_isShownWhole() throws Exception {
        Label atTheCount = middleShortened("Siti Nur-Aisyah Bt");
        // one over, where the ellipsis would take back the one place the cut saves
        Label oneOver = middleShortened("Siti Nur-Aisyah Bte");

        assertEquals("Siti Nur-Aisyah Bt", atTheCount.getText());
        assertNull(atTheCount.getTooltip(), "a value shown whole needs no tooltip");
        assertEquals("Siti Nur-Aisyah Bte", oneOver.getText());
        assertNull(oneOver.getTooltip(), "a value shown whole needs no tooltip");
    }

    @Test
    public void showMiddleShortened_aValueTwoOverTheCount_isCut() throws Exception {
        Label label = middleShortened("Siti Nur-Aisyah Bint");

        assertEquals("Siti Nur-\u2026syah Bint", label.getText());
    }

    @Test
    public void showMiddleShortened_aCountTooSmallToHoldBothEnds_keepsTheOpeningLonger() throws Exception {
        Label label = onFxThread(() -> {
            Label narrow = new Label();
            UiText.showMiddleShortened(narrow, "Mohamed Abdul Rahman bin Ismail", 5);
            return narrow;
        });

        // the opening is what a reader scans by, so the close never takes more than half
        assertEquals("Moh\u2026il", label.getText());
    }

    @Test
    public void showMiddleShortened_anEmojiAtEitherCut_keepsItWhole() throws Exception {
        String graduate = "\uD83C\uDF93";
        Label atTheOpening = middleShortened("A".repeat(8) + graduate + "B".repeat(20) + "C".repeat(9));
        Label atTheClose = middleShortened("A".repeat(9) + "B".repeat(20) + graduate + "C".repeat(8));

        // an emoji is one character to a reader and two to Java, and half of one draws as a box
        assertEquals("A".repeat(8) + graduate + "\u2026" + "C".repeat(9), atTheOpening.getText());
        assertEquals("A".repeat(9) + "\u2026" + graduate + "C".repeat(8), atTheClose.getText());
    }

    @Test
    public void showMiddleShortened_aCombiningAccentAtEitherCut_keepsItOnItsLetter() throws Exception {
        String accented = "e\u0301"; // "\u00E9", with the accent held as a separate code point
        Label atTheOpening = middleShortened("A".repeat(8) + accented + "B".repeat(20) + "C".repeat(9));
        Label atTheClose = middleShortened("A".repeat(9) + "B".repeat(20) + accented + "C".repeat(8));

        assertEquals("A".repeat(8) + accented + "\u2026" + "C".repeat(9), atTheOpening.getText());
        assertEquals("A".repeat(9) + "\u2026" + accented + "C".repeat(8), atTheClose.getText());
    }

    @Test
    public void showMiddleShortened_aSpaceAtEitherCut_leavesNoGapBesideTheEllipsis() throws Exception {
        Label atTheOpening = middleShortened("Mohammed Abdul Rahman bin Ismail");
        Label atTheClose = middleShortened("Mohamed Abdul Rahman Abdullah");

        assertEquals("Mohammed\u2026in Ismail", atTheOpening.getText());
        assertEquals("Mohamed A\u2026Abdullah", atTheClose.getText());
    }

    @Test
    public void showMiddleShortened_aValueInAnotherScript_keepsBothEnds() throws Exception {
        Label han = middleShortened("\u9648\u4F1F\u660E\u674E\u5C0F\u9F99\u738B\u5927\u9524"
                + "\u5F20\u4E09\u4E30\u8D75\u56DB\u4E5D\u5B59\u609F\u7A7A\u8BF8\u845B\u4EAE");
        // Arabic reads right to left, which is the drawing's business rather than the cut's
        Label arabic = middleShortened("\u0645\u062D\u0645\u062F \u0639\u0628\u062F "
                + "\u0627\u0644\u0631\u062D\u0645\u0646 \u0628\u0646 "
                + "\u0625\u0633\u0645\u0627\u0639\u064A\u0644");

        assertEquals("\u9648\u4F1F\u660E\u674E\u5C0F\u9F99\u738B\u5927\u9524\u2026"
                + "\u8D75\u56DB\u4E5D\u5B59\u609F\u7A7A\u8BF8\u845B\u4EAE", han.getText());
        assertEquals("\u0645\u062D\u0645\u062F \u0639\u0628\u062F\u2026"
                + "\u0646 \u0625\u0633\u0645\u0627\u0639\u064A\u0644", arabic.getText());
    }
}
