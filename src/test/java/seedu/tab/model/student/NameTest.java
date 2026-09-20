package seedu.tab.model.student;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class NameTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Name(null));
    }

    @Test
    public void constructor_invalidName_throwsIllegalArgumentException() {
        String invalidName = "";
        assertThrows(IllegalArgumentException.class, () -> new Name(invalidName));
    }

    @Test
    public void isValidName_carriesNoIdentity_returnsFalse() {
        assertThrows(NullPointerException.class, () -> Name.isValidName(null));

        assertFalse(Name.isValidName("")); // empty string
        assertFalse(Name.isValidName(" ")); // spaces only
        assertFalse(Name.isValidName("---")); // punctuation only
        assertFalse(Name.isValidName("^")); // a single symbol
        assertFalse(Name.isValidName("😀")); // emoji only
        assertFalse(Name.isValidName("​")); // zero-width space only
        assertFalse(Name.isValidName("َُِ")); // Arabic harakat with no base letters
    }

    @Test
    public void isValidName_surroundingWhitespace_returnsFalse() {
        assertFalse(Name.isValidName(" Peter")); // leading space
        assertFalse(Name.isValidName("Peter ")); // trailing space
        assertFalse(Name.isValidName(" Peter")); // leading non-breaking space
        assertFalse(Name.isValidName("Peter ")); // trailing non-breaking space
    }

    @Test
    public void isValidName_ordinaryNames_returnsTrue() {
        assertTrue(Name.isValidName("peter jack")); // letters only
        // Numerals only. Courts have held that a pure number is not a legal name: North Dakota
        // (Petition of Dengler, 1976) and Minnesota (Application of Dengler, 1979) both refused
        // Michael Dengler's petition to become "1069". TAB accepts it regardless, because the
        // name is stored rather than parsed, and the value a TA types may be a roster identifier
        // rather than a legal name. Blocking it would buy nothing and would risk blocking the
        // names below, which legitimately carry numerals.
        assertTrue(Name.isValidName("12345"));
        assertTrue(Name.isValidName("peter the 2nd")); // letters and numbers
        assertTrue(Name.isValidName("Capital Tan")); // capital letters
        assertTrue(Name.isValidName("David Roger Jackson Ray Jr 2nd")); // long name
        assertTrue(Name.isValidName("A")); // single character
    }

    @Test
    public void isValidName_punctuationRealNamesUse_returnsTrue() {
        assertTrue(Name.isValidName("Ravi s/o Kumaran")); // s/o, which the parser once ate
        assertTrue(Name.isValidName("Anita d/o Rajan")); // d/o
        assertTrue(Name.isValidName("Siti Nur-Aisyah")); // hyphen
        assertTrue(Name.isValidName("Ma Ying-jeou")); // hyphen inside a given name
        assertTrue(Name.isValidName("Sean O'Brien")); // apostrophe
        assertTrue(Name.isValidName("Nurul Izzah bte Hassan")); // bte
        assertTrue(Name.isValidName("J. R. R. Tolkien")); // full stops
        assertTrue(Name.isValidName("Abd al-Rahman")); // al-
        assertTrue(Name.isValidName("-Ahmad")); // leading hyphen
        assertTrue(Name.isValidName("James&")); // ampersand
        assertTrue(Name.isValidName("X Æ A-Xii")); // ligature, hyphen and single letters together
        assertTrue(Name.isValidName("X Æ A-Ⅻ")); // Roman numeral twelve, category Nl
        assertTrue(Name.isValidName("Æ")); // a ligature on its own
    }

    @Test
    public void isValidName_nonLatinScripts_returnsTrue() {
        assertTrue(Name.isValidName("陈伟明")); // Chinese, simplified
        assertTrue(Name.isValidName("陳水扉")); // Chinese, traditional
        assertTrue(Name.isValidName("李")); // a single-character Chinese surname
        assertTrue(Name.isValidName("김민준")); // Korean
        assertTrue(Name.isValidName("田中さくら")); // Japanese, kanji and kana
        assertTrue(Name.isValidName("ピーター・パーカー")); // katakana, with a middle dot
        assertTrue(Name.isValidName("Nguyễn Văn An")); // Vietnamese, stacked diacritics
        assertTrue(Name.isValidName("முது")); // Tamil
        assertTrue(Name.isValidName("मोहन")); // Devanagari
        assertTrue(Name.isValidName("Иван")); // Cyrillic
        assertTrue(Name.isValidName("Δημήτρης")); // Greek
        assertTrue(Name.isValidName("François")); // Latin with accents
        assertTrue(Name.isValidName("Ægir Þrósson")); // Icelandic
        assertTrue(Name.isValidName("𠮷田")); // outside the basic multilingual plane
    }

    @Test
    public void isValidName_unusualScripts_returnsTrue() {
        // Each of these stresses a different Unicode property rather than simply
        // being another alphabet, so a future tightening of the rule trips on them.

        // written vertically, and separated by punctuation rather than spaces
        assertTrue(Name.isValidName("ᠮᠣᠩᠭᠣᠯ")); // Mongolian
        assertTrue(Name.isValidName("བསོད་ནམ")); // Tibetan, tsheg

        // written without spaces between words, with vowels above and below
        assertTrue(Name.isValidName("สมชาย")); // Thai
        assertTrue(Name.isValidName("សុភា")); // Khmer

        // right to left, some with combining marks
        assertTrue(Name.isValidName("מֹשֶׁה")); // Hebrew, niqqud
        assertTrue(Name.isValidName("ދިވެ")); // Thaana
        assertTrue(Name.isValidName("ߒߞߏ")); // N'Ko
        assertTrue(Name.isValidName("ܝܠܘ")); // Syriac

        // a zero-width joiner inside the name, which is a format character
        assertTrue(Name.isValidName("ශ්‍රී")); // Sinhala

        // syllabaries rather than alphabets
        assertTrue(Name.isValidName("ኃይለ")); // Ethiopic
        assertTrue(Name.isValidName("ᏣᎳᎩ")); // Cherokee
        assertTrue(Name.isValidName("ᐃᓄᒃᑎ")); // Inuktitut
        assertTrue(Name.isValidName("ꔀꔊ")); // Vai

        // outside the basic multilingual plane, so two chars per code point
        assertTrue(Name.isValidName("𞤀𞤣")); // Adlam
        assertTrue(Name.isValidName("𒀣𒀀")); // Cuneiform
        assertTrue(Name.isValidName("𓀀𓀁")); // Egyptian hieroglyphs
        assertTrue(Name.isValidName("𐐀𐐨")); // Deseret, which has case

        // alphabets that are simply less common
        assertTrue(Name.isValidName("გიორგი")); // Georgian
        assertTrue(Name.isValidName("Սարգիս")); // Armenian
        assertTrue(Name.isValidName("ⵜⵉⴼⵉⵏ")); // Tifinagh
    }

    @Test
    public void isValidName_arabicScriptDetails_returnsTrue() {
        assertTrue(Name.isValidName("محمد")); // plain
        assertTrue(Name.isValidName("عبد الرحمن")); // two words
        assertTrue(Name.isValidName("مُحَمَّد")); // with harakat
        assertTrue(Name.isValidName("محـمد")); // with tatweel
        assertTrue(Name.isValidName("١٢٣")); // Arabic-Indic digits
        assertTrue(Name.isValidName("محمد Ali")); // right-to-left beside left-to-right
    }

    @Test
    public void isValidName_composedAndDecomposedFormsBothAccepted() {
        // The same Vietnamese name in the two Unicode normalisation forms. They render
        // identically but are different strings, so duplicate detection cannot rely on
        // string equality alone.
        String composed = "Nguyễn Văn An";
        String decomposed = "Nguyễn Văn An";
        assertTrue(Name.isValidName(composed));
        assertTrue(Name.isValidName(decomposed));
        assertNotEquals(composed, decomposed);
    }

    @Test
    public void isValidName_innerWhitespaceIsKept() {
        // Inner runs of whitespace are accepted and stored verbatim. Two students whose names
        // differ only by an extra inner space are therefore distinct today, which duplicate
        // detection will have to account for.
        String doubled = "X  Æ   A-Xii";
        assertTrue(Name.isValidName(doubled));
        assertEquals(doubled, new Name(doubled).fullName);
    }

    @Test
    public void equals() {
        Name name = new Name("Valid Name");

        // same values -> returns true
        assertTrue(name.equals(new Name("Valid Name")));

        // same object -> returns true
        assertTrue(name.equals(name));

        // null -> returns false
        assertFalse(name.equals(null));

        // different types -> returns false
        assertFalse(name.equals(5.0f));

        // different values -> returns false
        assertFalse(name.equals(new Name("Other Valid Name")));
    }
}
