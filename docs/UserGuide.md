---
  layout: default.md
  title: "User Guide"
  pageNav: 3
---

# TAB User Guide

TAB is a **desktop application for teaching assistants to manage student records, optimized for use through a Command Line Interface (CLI)** while retaining the benefits of a Graphical User Interface (GUI). If you type quickly, TAB can help you manage students faster than traditional GUI applications.

<!-- * Table of Contents -->
<page-nav-print />

--------------------------------------------------------------------------------------------------------------------

## Quick start

1. Ensure that Java `25` or later is installed on your computer.<br>
   TAB includes JavaFX `17.0.7`, used with an x64 JDK without bundled JavaFX
   (for Zulu, choose the `jdk` package).<br>
   **Mac users:** You can instead use Zulu JDK+FX `25.0.3`, installed by the
   [course's Mac setup guide](https://se-education.org/guides/tutorials/javaInstallationMac.html).
   This setup uses the JDK's JavaFX `25` and runs natively on Apple Silicon.<br>
   **Apple Silicon users choosing plain JDK + JavaFX 17:** Use an x64 JDK under
   Rosetta, as TAB's bundled JavaFX libraries target Intel Macs. This requirement
   does not apply to the course's native JDK+FX setup.

1. Download the latest `.jar` file from [here](https://github.com/AY2627S1-CS2103T-T16-1/tp/releases).

1. Copy the file to a folder you can write to, which will be the _home folder_ for TAB.
   If the folder is read-only, TAB can open but cannot save changes to student
   records or window preferences there. `list`, `find`, `help`, and `exit`
   still work. It reports a warning in the terminal
   if it cannot create its log file. Move TAB and any existing data to a
   writable folder before making changes you want to keep.

1. Open a terminal, `cd` to the folder containing the JAR file, and run `java -jar tab.jar`.<br>
   A GUI similar to the one below should appear in a few seconds. Note how the app contains some sample data.
   The picture was taken just after adding one student, so a fresh launch shows the six sample students, no
   reply above the list, and nobody marked as needing a follow-up.<br>
   ![Ui](images/Ui.png)

1. Type a command in the command box and press Enter to execute it. For example, type **`help`** and press Enter to show the command summary in the main window.<br>
   Some example commands you can try:

   * `list` : Lists all students.

   * `add John Doe -p 98765432 -e johnd@example.com --id A05678901K` : Adds a student named `John Doe` with an NUS Student ID.

   * `delete 3` : Deletes the 3rd student shown in the current list. `delete A01234567X` deletes by NUS Student ID instead.

   * `edit 1 -p 98765432` : Edits the 1st student's phone number. `edit A01234567X -p 98765432` edits by NUS Student ID instead.

   * `clear` : Asks before deleting all students. Type `y` and press Enter
     to confirm, or press Enter without a reply to cancel.

   * `exit` : Exits the app.

1. Refer to the [Features](#features) section below for details of each command.

--------------------------------------------------------------------------------------------------------------------

Text pasted into the command box keeps its word boundaries: every separator
TAB splits a command on becomes a space, from a tab to a line break to the
soft line break a slide carries. This lets you paste names or values from
spreadsheet cells, slides and documents without joining their words. Pasted
text remains one command; inspect it before pressing Enter.

## Features

<box type="info" seamless>

**Notes about the command format:**<br>

* Words in `UPPER_CASE` are the parameters to be supplied by the user.<br>
  For example, in `add NAME`, replace `NAME` with a value such as `John Doe`.

* A command word may be typed in any case, so `list`, `List` and `LIST` all
  list your students. Only the first word is read this way: the rest of the
  command keeps the capitals you typed, so `Find -n Alex` still looks for `Alex`.
  Option spellings are case-sensitive, so `-P` is not the `-p` option.

* Items in square brackets are optional.<br>
  For example, `NAME [-t TAG]` can be used as `John Doe -t friend` or as `John Doe`.

* A vertical bar `|` separates alternatives; choose at most one and do not type the bar.<br>
  For example, `[--follow-up | --no-follow-up]` allows either option or neither.

* Items followed by `...` can appear zero or more times.<br>
  For example, `[-t TAG]...` may be omitted, or written as `-t friend` or as
  `-t friend -t family`.

* Parameters can be in any order.<br>
  For example, if the command specifies `-p PHONE -e EMAIL`,
  `-e EMAIL -p PHONE` is also acceptable. A parameter given before any
  option, such as the name in `add`, keeps its place at the front.

* Commands use options such as `-p` for fields, except positional parameters
  such as the leading name in `add` or the target in `edit`. Value options also
  have descriptive long aliases such as `--phone`; the command-specific
  sections list them. Separate a value from its option with a space, as in
  `--phone 91234567`, or join it with an equals sign, as in
  `--phone=91234567`; short options accept both forms too. Only the first
  equals sign is a separator, so the rest belong to the value. A value holding
  spaces goes in double quotes after an option or after its equals sign, as in
  `--tag="Lab 3"`. Quote any value opening with a hyphen when using a space;
  after an equals sign it is already unambiguous. An equals sign still needs a
  value after it. Where `edit` allows an empty value, write the deliberate
  empty value as `OPTION=""`, for example `--email=""`.

* Straight (`"`), curly (`“` and `”`), and full-width (`＂`) double quotes
  all group values and may be mixed. For example, `tag 2 -a “组 1”` and
  `tag 1 -a ＂Lab 3＂` work like straight-quoted values. To keep any of these
  quote marks as part of a value, put a backslash before it, as in `\“`.
  Single quotation marks and apostrophes remain literal characters.

* If a non-repeatable option is given twice, TAB names the repeated option
  and asks you to give it only once. For example, repeating `-f` reports
  `Options given more than once: -f. Give each listed option only once.`
  This also covers mixed short and long spellings, such as `-f` and `--follow-up`.

* An `INDEX` refers to a positive position in the **displayed** student list.
  If it exceeds that list, TAB quotes the index and reports the number of students
  currently shown; `list` restores all students before you choose an index again.
  Indices up to 2147483647 can be read; a larger positive integer gets a specific
  upper-bound message instead of being described as non-positive.

* Commands that take no parameters (`help`, `list`, `exit`, and `clear`)
  reject unexpected input and report it without executing the command.<br>
  For example, `help 123` reports `help takes no arguments. Unexpected input:
  "123".` Whitespace after the command is accepted, but quoted values,
  including `""`, are still arguments.

* Invisible format characters and non-whitespace control characters are ignored
  in commands and stored field values, including names, tags, phone numbers,
  emails and NUS Student IDs. A pasted hidden control cannot make an
  identical-looking name or tag differ in search or
  duplicate warnings. Tabs and line breaks still separate values or become spaces
  inside a field. The joiners some scripts need for shaping are kept.

* If you are using a PDF version of this document, be careful when copying and pasting commands that span multiple lines as space characters surrounding line-breaks may be omitted when copied over to the application.
</box>

### Typing a command

TAB uses an English interface. Counts, displayed indices and data-file line
numbers use digits 0–9 regardless of your computer’s regional settings.

The command line marks the parts of a command as you type, so a mistake is
visible before you press Enter:

* the command word is in the line's accent color and bold, and ends at the
  first space, so a quote typed against it is part of the word
* an option such as `-p` is in mauve, as is the `--tag=` of an option written
  with its value attached
* a value in double quotes is in green, including one attached to an option
* anything else is in ordinary text

If a command is refused, the whole line turns red until you start correcting
it. A command longer than the line scrolls with the caret, so you can always
see where you are typing, and selecting part of it (for example with
`Ctrl`+`A`) draws a band behind the selection. Arabic, Hebrew and mixed-script
commands also stay on this single line; the caret and selection follow the
text’s displayed order.

<box type="tip" seamless>

**Tip:** The colors break a command where the parser breaks it, so if a value
you meant to quote is not colored as a quoted value, the parser will not read
it as one either.
</box>

### Moving around with the keyboard

From the command box, press `Tab` to focus the student list. An accent outline
shows when the list has focus. Use `Up`/`Down`
to move between students, `Page Up`/`Page Down` to move a page at a time, and
`Home`/`End` to reach the first/last student. The list scrolls to keep the
selected student visible. Press `Shift`+`Tab` to return to the command box.

Press `Tab` again from the student list to focus the summary's tag list.
An accent outline shows when the tag list has focus. Use the same keys to
scroll it: `Up`/`Down`, `Page Up`/`Page Down`, and `Home`/`End`.
From the tag list, press `Shift`+`Tab` twice to return to the command box.

`Tab` cycles through the command box, student list, tag list, and command
reply (when a reply is showing), then back to the command box. `Shift`+`Tab`
cycles in reverse. The reply remains selectable for copying; arrow and page
keys let you read a reply that is too long to fit. Empty student and tag lists
remain in the cycle, so returning to command entry works the same way.
Navigation does not change student records or run commands.

<box type="tip" seamless>

**Tip:** `Up`/`Down` recall recent commands when the command box has focus.
Press `Tab` first when you want to move through students instead.
</box>

### Reusing recent commands

In the command box, press `Up` to recall the previous submitted command and
`Down` to move forward. Moving past the newest command restores the draft you
were typing before pressing `Up`. The caret moves to the end of recalled text,
and its colors follow the command shown. Press Enter to submit it again.

Rejected commands are included so you can correct a typo. Consecutive identical
submissions appear once; blank input and all replies to a pending `clear`
confirmation are omitted, including invalid replies. The `clear` command itself
remains available for recall. If the
box already shows the newest submission (for example after a rejection), the
first `Up` skips to the preceding entry when there is one. `Down` still returns
the rejected text. At the oldest entry, `Up` keeps the text unchanged;
with no history, both keys keep the text unchanged. Editing a recalled entry
does not change the saved command, and navigating away discards that edit.
History lasts only until TAB closes and is not saved in the data file.

### Mistyping a command word

If the first word of a command is not one TAB knows, the reply names the word
you typed, and names the commands it was likely meant to be:

* `del 3` answers `Unknown command "del". Did you mean delete?`
* `delte 3` answers `Unknown command "delte". Did you mean delete?`
* `eit` answers `Unknown command "eit". Did you mean edit or exit?`
* `qwerty` answers `Unknown command "qwerty". Enter help to see every command.`

A word that opens a command is read as that command being typed: `del` and
`dele` reach `delete`, `cle` reaches `clear`, and `e` reaches both `edit` and
`exit`. Every command the word opens is named, because TAB cannot tell which
of them you meant.

Any other word is matched letter by letter, and reaches a command when at most
two letters have to be added, removed, swapped or replaced to turn it into
that command, so `ls` reaches `list` and `dlte` reaches `delete`. A word that
is two letters out has to start with the same letter as the command, since the
first letter is the one you are least likely to get wrong. A word that is one
letter out may differ in it, so `felete` still reaches `delete`.

The comparison ignores case, so `Delte 3` is answered in the same way. A word
that reaches nothing, such as `dlt`, is answered with the pointer to `help`.
A word spelled correctly is no mistyping whatever case it is in: `List` and
`LIST` run `list` rather than being suggested back to you.

<box type="warning" seamless>

**Caution:** TAB never runs the command it suggests. Nothing is added, changed
or deleted by a mistyped command word, and what you typed stays in the command
box, so you can correct the word rather than type the command again.
</box>

### Reading the summary beside the list

The panel on the left of the window summarizes the students the list is
showing, so you can see the shape of your cohort without running a command.

* **Flagged** counts the students marked as needing a follow-up.
* **Tags** lists each tag in use with the number of students carrying it,
  most used first. Case variants of one tag share a row, as they do for `tag`,
  so `T1` and `t1` count as one tag; the row is named with the spelling most
  of those students carry. Tags used by the same number of
  students are listed alphabetically, ignoring case. Long tag names wrap onto
  multiple lines so you can read their endings beside the count. From the
  command box, press `Tab` twice to focus the tag list, then use `Up`/`Down`,
  `Page Up`/`Page Down`, or `Home`/`End` to scroll to rows outside the visible
  area. Press `Shift`+`Tab` twice to return to the command box.
* The foot of the panel gives the total.

![Summary counts beside student cards with a shortened name, hidden tag count and follow-up marker](images/cardSummary.png)

In this example, three students are listed and one needs follow-up. `Lab3` and
`T1` each belong to two students. The first card keeps both ends of its long
name, shows one tag chip and counts the other two as `2 more…`.

<box type="tip" seamless>

**Tip:** The summary describes the students currently listed, not the whole
student book. Run `find -n Alex` and it counts only the students that match;
run `list` to see the whole cohort again.
</box>

### Viewing help: `help`

Opens a help page inside TAB, replacing the student list and command box until
you leave help. It shows every supported command, its purpose and concise syntax,
option aliases, quoting rules and a shortcut to copy the User Guide address.

The reference is coloured the way the command box colours what you type, so a
command word, an option such as `-p` and a value you fill in such as `NAME` keep
the same colour in both places. Section headings stand out from the body, and the
brackets marking an optional field stay in the body colour.

Use these pager keys immediately, without clicking or pressing Enter after a letter:

| Key | Action |
| --- | --- |
| Space, `f`, PgDn | Next screenful |
| `b`, PgUp | Previous screenful |
| Enter, `j`, Down | Next line |
| `k`, Up | Previous line |
| `d` / `u` | Forward / backward half a screenful |
| `g` / `G`, Home / End | Beginning / end |
| `q`, Esc | Return to commands |
| `c` | Copy the User Guide URL |

Press **c** to copy the complete User Guide address to your clipboard. The footer
confirms `User Guide URL copied.`; paste it into a browser to read the full guide.
Opening help shows `Help mode: press q or Esc to return to commands.`.
Navigation keys, including Enter, leave this notice unchanged.
Typing an unused key shows `Press q or Esc to return to the command box.` so you
can leave help before typing a command.

The footer keeps the navigation keys visible and shows your progress, or `(END)`
at the last screenful. Help has no scrollbars. Text wraps to the window width;
you can resize the window while reading. Paging keys stay at the beginning or
end when there is no more text in that direction. Press `q` or Esc to restore
the student list, previous result and any partially typed command. The command
is unselected with the caret at the end, ready for you to continue typing.
**F1** and **Help → Help** open the same page at the beginning
without submitting a partially typed command. Pager keys apply only while help is open.

![Current help page showing tag addition and removal, ID targets, and all-term search](images/helpMessage.png)

This view shows tag removal, ID targets and field-based search. Press Space to
continue through the remaining commands and syntax notes; the footer shows
your progress.

Format: `help`


### Adding a student: `add`

Adds a student to the student book. After a successful save, TAB scrolls the new
student's card into view and keeps focus in the command box.

If the new student matches the active search, the search stays active and the
student appears at the end of its results. Otherwise, TAB shows all students
and replies `Search cleared to show the new student.` Re-read the displayed
indexes before your next command. A rejected addition leaves the search unchanged.

Format: `add NAME [-p PHONE] [-e EMAIL] [--id NUS_STUDENT_ID] [-t TAG]... [--follow-up]`

The value options accept these equivalent spellings: `-p` and `--phone`,
`-e` and `--email`, `--id` and `-i`, and `-t` and `--tag`. The spellings may
be mixed in one command, but giving a single-valued field (phone, email or NUS
Student ID) twice is an error even when the two spellings differ. Tags are
repeatable: use `-t` or `--tag` once for each tag, mixing the spellings if you wish.
Tags differing only in case or normalized whitespace become one tag; the first
spelling in the command is kept. For example, `-t Lab -t lab -t LAB` keeps `Lab`.

<box type="tip" seamless>

**Tip:** The email is optional. Record a student you only have a phone number
for, rather than inventing an address to satisfy the command. To remove an
email entered by mistake, use `edit INDEX -e ""`.

An address may run to 254 characters. A longer value is refused and TAB
tells you how long it was — useful when a copy-and-paste has picked up more
than you meant.
</box>

The email's part before `@` accepts ASCII letters, digits and the symbols
<code>!#$%&amp;'*+/=?^_&#96;{|}~-</code>, including repeated symbols and symbols
at either end. Dots may separate non-empty groups; leading, trailing and
consecutive dots are refused. For example, `mei__tan@hotmail.com` and
`sean.o'brien@example.com` are accepted and stored as entered. Addresses that
require a quoted local part (such as one containing spaces) are unsupported.
The domain uses letters and digits with internal hyphens, separated by dots;
its final label must have at least two characters. If an address starts with
`-`, wrap the whole address in double quotes so it is read as a value.

<box type="tip" seamless>

**Tip:** The NUS Student ID is optional. Give it with `--id`, or
use the shorter `-i` alias. It must contain between 1 and 32 ASCII letters and
digits, such as `A01234567X` or `E0123456`. Lowercase letters are accepted and
stored as uppercase. To remove an NUS Student ID entered by mistake, use
`edit INDEX --id ""`; a value containing only spaces clears it as well.

TAB uses the NUS Student ID to detect duplicate student records. Two students
may share a name, and students without an NUS Student ID are not treated as
duplicates. An `add` or `edit` is refused only when another student already
has the same NUS Student ID, ignoring letter case; the error names that ID and
the student who already holds it.
</box>

<box type="tip" seamless>

**Tip:** After a successful `add` or `edit`, TAB warns you when another student
has the same name, phone number, or email address. The record is still saved.
One warning line is shown for each matching field. It names up to two other
students, then counts any further matches. For example:

```
WARNING: Possible duplicate student: Alex Yeoh has the same email (alex@example.com).
Review the records and delete duplicates only if they are the same person.
```

Names and emails are compared without regard to letter case. Names also ignore
Latin accents and straight/curly apostrophe differences: `José` matches `Jose`,
and `O’Brien` matches `O'Brien`. Repeated, leading, and trailing whitespace in
names is insignificant. Phone numbers match when their complete digit sequences
match, ignoring spaces and other non-digit characters; equivalent Unicode
decimal digits also match. For example, `9123-4567`, `91234567`, and
`９１２３４５６７` prompt the same phone warning. Entered phone formatting is
preserved. Phone warnings omit the number because the matching records may
format it differently. The review advice appears once after all matching fields.

The warning is only a hint. It does not prove that two records describe the
same person, and it does not find misspelled names, email aliases, or phone
numbers with different digit sequences, such as a local number and the same
number with an added country code or extension. A student who has no phone
number or no email is never reported as sharing that detail with anyone. Review the records before
deleting either one. A matching NUS Student ID is handled separately and
still refuses the command, as described above.
</box>

<box type="tip" seamless>

**Tip:** A student can have any number of tags, including zero. A tag may hold
spaces, hyphens and any script, so `Lab 3`, `needs-followup` and
`AY2627 Sem 1 CS2103T T16` are all valid. The one name a tag cannot take is
`Needs follow-up`, which labels the follow-up marker; use `--follow-up` for
that. Names built around it, such as `Needs follow-up soon`, are ordinary tags.
</box>

<box type="tip" seamless>

**Tip:** Add `--follow-up` when the student already needs a reply or another
action from you. This option takes no value, and the shorter `-f` alias is also
accepted. Without either option, the student starts unflagged.
</box>

<box type="tip" seamless>

**Tip:** A phone number may be written the way you would write it down:
`+65 9123 4567`, `6516-2727 ext 21`, or even
`1234 5678 (HP) 1111-3333 (Office)`. It only has to hold at least 3 digits.

The phone number is optional too. Record a student from a class roster that
gives only a name, an NUS Student ID and a university email, rather than
inventing a number to satisfy the command: `add Email Only
--email=e1234567@u.nus.edu --id=E1234567`. Supply the number later with
`edit INDEX -p PHONE`, and remove one entered by mistake with
`edit INDEX -p ""`. A number you do give is still checked for its 3 digits.
</box>

<box type="tip" seamless>

**Tip:** A name may hold anything you would write on a roster: slashes,
hyphens, apostrophes, full stops, and any script. It only has to contain at
least one letter or number, in any writing system.
</box>

<box type="tip" seamless>

**Tip:** The name comes first, without `-n` or `--name`, and may hold spaces as it is, so
`add Ravi s/o Kumaran -p 91234567` needs no quotes at all. Slashes, hyphens and
apostrophes are ordinary characters here.

If you use a name option from `edit`, TAB explains where to put the name:

```text
add --name "Betsy Crowe" --phone 1234567
```

```text
There is no --name option.
In the add command, the name comes first, without an option: add NAME -p PHONE ...
```

The same placement hint appears for `-n` and when the name is missing.

Use double quotes for an option value holding spaces, and for any value opening
with a hyphen, so that TAB does not read it as an option:
`add Siti Nur-Aisyah -p 84420917 -t "Lab 3"` and `add "-Ahmad" -p 84001122`.

To put a double quote or a backslash inside a value, write `\"` or `\\`:
`add Dwayne \"The Rock\" Johnson -p 91234567`.
</box>

Examples:
* `add John Doe -p 98765432 -e johnd@example.com`
* `add Mei Tan -p 91234567 -e mei__tan@hotmail.com`
* `add Sean O'Brien -p 82345678 -e sean.o'brien@example.com`
* `add Alice Tan --phone=91234567 --email=alice@example.com --tag="Lab 3"`
*  `add "Betsy Crowe" --tag T1 --email betsycrowe@example.com --phone 1234567 --id A06789012L -t Lab3`
* `add "Ravi s/o Kumaran" -p 91234567 -e e0923841@u.nus.edu`
* `add Siti Nur-Aisyah -p 84420917 -e e1147203@u.nus.edu -i a11472030k -t T1 -t "Lab 3"`
* `add Priya Nair -p 91234567 -e priya@example.com --follow-up`
* `add 陈伟明 -p 98123344 -e e1077310@u.nus.edu`
* `add Email Only --email=e1234567@u.nus.edu --id=E1234567` Records a roster entry whose phone number is unknown.

A student's NUS Student ID appears among their card's details and in successful
command feedback. A student without one has no placeholder or blank gap there.
The same holds for a student without a phone number: the remaining details
close up rather than leaving an empty line where the number would have been.
A flagged student's card carries a `Needs follow-up` marker at its right-hand
edge, on the same line as the name, and the command result says so too.

A name too long for a card is shortened with an ellipsis in its middle, which
keeps both the start and the end of the name to help distinguish similar names.
Hover over any name the card cannot show in full,
whether it was shortened this way or left narrow by a crowded row, to see the
whole of it. NUS Student ID, phone and email share a line when they fit. Details
use the full width below the name, including beneath the follow-up marker.
Cards grow as details wrap onto another line, so a long phone cannot hide
the email or overlap the name.
A value wider than a whole line ends with an ellipsis; hover over it to see
its full value. Widening the window shows more text and brings details back
onto one line without changing the stored record.
Names and tag labels appear in full whenever there is room, and the card shows
as many complete tag chips as fit. At narrow widths, the name keeps both ends
while tags give way to a count of those hidden. When no chip fits, the count reads `N tags`; otherwise
it reads `N more…`. Unused count space returns to the name. A tag chip too
wide for its allocation is shortened; hover over it to read the full tag.
With several tags, if there is too little room for a complete short tag or at
least four characters of a longer tag, the row shows `N tags` instead.
Hover over the count to read the hidden tag names. The
[summary illustration](#reading-the-summary-beside-the-list) shows this
shortened name, hidden tag count and follow-up marker together.

When a student is added, the command result displays the new record
(`New student added: NAME; Phone: PHONE...`) so you can confirm the details
immediately. Consistent with the student card, optional fields appear in the
result only when present: an unflagged student or a student without a phone
number, an email or tags leaves no empty placeholder labels in the message,
and tags appear in alphabetical order (ignoring case).

Leading and trailing spaces are removed, repeated spaces inside a name are
collapsed to one, and invisible characters are discarded, so that a name is
always stored the way it looks.

If a field is rejected, TAB quotes the value it could not accept and says what
is wrong with it, rather than restating the whole rule:

```
> add John Doe -p 98765432 -e e1234567
Email "e1234567" is not valid: an email needs an @ between the local part and the domain
```

Once TAB can tell which value belongs to which field, every field is checked
before the command is refused, so a command with more than one mistake tells
you about all of them at once:

```
> add -e e1234567
Missing required field(s): NAME
In the add command, the name comes first, without an option: add NAME -p PHONE ...
Email "e1234567" is not valid: an email needs an @ between the local part and the domain
```

For example, an ID containing punctuation is rejected specifically:

```
> add John Doe -p 98765432 --id A0123-456X
NUS Student ID "A0123-456X" is not valid: a NUS Student ID can only contain ASCII letters and digits
```

### Listing all students: `list`

Shows a list of all students in the student book.

Format: `list`

TAB reports the number of displayed students: `Listed 0 students.` when the list is empty,
`Listed 1 student.` for a single student, and `Listed 24 students.` for several.

### Editing a student: `edit`

Edits an existing student in the student book.

Format: `edit TARGET [-n NAME] [-p PHONE] [-e EMAIL] [--id NUS_STUDENT_ID] [-t TAG]... [--follow-up | --no-follow-up]`

`TARGET` is a displayed index, a letter-leading NUS Student ID, or `--target-id NUS_STUDENT_ID`.
For example, `edit A01234567X -p 91234567`
edits the student holding that ID, found among all students even while a search is hiding
them, and matching ignores case. `--id` still sets or clears the student's NUS Student ID,
so `edit A01234567X --id A05678901K` changes the ID of the student found by the old one.
Use `edit --target-id 1234 --no-follow-up` to clear follow-up for the student whose ID is
`1234`, even when a search hides them. `--target-id` also accepts `--target-id=VALUE`.

The value options accept these equivalent spellings: `-n` and `--name`, `-p`
and `--phone`, `-e` and `--email`, `--id` and `-i`, and `-t` and `--tag`.
The spellings may be mixed in one command, but giving a single-valued field
(name, phone, email or NUS Student ID) twice is an error even when the two
spellings differ. Tags are repeatable: use `-t` or `--tag` once for each
replacement tag, mixing the spellings if you wish. Case and normalized whitespace
variants become one tag, retaining the first spelling supplied in the command.

<box type="tip" seamless>

**Tip:** `edit` reads field values the same way as `add`. A name holding slashes, hyphens or apostrophes needs nothing
done to it beyond the quotes any value with spaces takes:
`edit 1 -n "Ravi s/o Kumaran"`.
</box>

* Edits the student at the specified `INDEX`, or the student who holds the given
  `NUS_STUDENT_ID`. An index refers to the index number shown in the displayed student list and
  **must be a positive integer** 1, 2, 3, ... An NUS Student ID names its holder wherever they
  are in the book, even while a search hides them, and the match ignores case. A target
  starting with a digit reads as an index; use `--target-id NUS_STUDENT_ID` to select
  such an ID explicitly. Here `--id` always sets the student's ID rather than choosing who to edit.
  A zero or non-integer index reports `Index must be a positive integer.`
  without the full usage block; any invalid field values are reported alongside it.
  The current search stays active after editing, so the index keeps referring to
  the same student when you repeat a command, as long as that student still
  matches the search. An edit that removes a matching detail, such as renaming
  the only `john` in the list, takes that student off the displayed list and
  shifts the remaining indices up. Check the displayed list before repeating a
  command after such an edit. An ID target does not shift, so it stays reliable
  through edits that change the displayed list.
* A target without an option comes first. `--target-id` may appear among the other options.
  Give exactly one target: do not combine it with a preamble target or repeat `--target-id`.
  An unquoted negative target reports both that an index must be positive and
  that the negative number is not an option. Extra words after a valid target
  are reported as unexpected values. Put a name change after `-n` rather than
  immediately after the target.
* At least one of the optional fields must be provided.
* Existing values will be updated to the input values. If every requested value
  already matches the record, TAB replies `Student already has the requested details. No changes made.`
  The displayed list, search and saved file stay unchanged.
* `--follow-up` (alias `-f`) sets follow-up; `--no-follow-up` (alias `-nf`) clears it.
  Both options are also available with `find` to filter by follow-up status.
  New students are unflagged by default when added.
  Both options take no value, so forms such as `--follow-up=true` are refused.
  Repeating either request keeps the requested state and reports no changes
  unless another field changes.
  Leaving both out preserves the student's follow-up status.
* Choose at most one follow-up option. Combining `--follow-up` or `-f` with
  `--no-follow-up` or `-nf` is rejected without changing any student details.
* To remove a student's phone number, use `-p ""` or `--phone=""`. A quoted
  value containing only spaces also removes it. Leaving out `-p` keeps the
  number unchanged; a bare `-p` or `--phone=` is an error. Clearing an already
  absent phone number succeeds. A number that is supplied is still checked for
  its 3 digits.
* To remove a student's email, use `-e ""` or `--email=""`. A quoted value
  containing only spaces also removes it. Leaving out `-e` keeps the email
  unchanged; a bare `-e` or `--email=` is an error. Clearing an already absent
  email succeeds.
* Email syntax follows the rules in [Adding a student](#adding-a-student-add),
  including repeated underscores and apostrophes.
* A new email may run to 254 characters, the same limit `add` applies. A longer
  value is refused, TAB tells you how long it was, and the student's details
  stay as they were.
* To remove a student's NUS Student ID, use `--id ""`, `--id=""`, or the
  corresponding `-i` form. Leaving the option out keeps the NUS Student ID
  unchanged; a quoted value containing only spaces removes it, while a bare
  option or an unquoted empty value such as `--id=` is an error. Clearing an
  already absent NUS Student ID succeeds. The name is the one detail that is
  required and cannot be cleared.
* An edit may give two students the same name, but cannot give a student an NUS
  Student ID already held by another record. NUS Student ID matching ignores case.
  A saved edit produces the same likely-duplicate notes described under `add`
  only when a name, phone number, or email changed by that command matches
  another student. Unchanged fields are not reported again, and the edited
  student is not compared with its own earlier record.
* `Needs follow-up` names the follow-up marker rather than a tag, so `-t` refuses
  it whatever its case or spacing and points you to `--follow-up`.
* When editing tags, all of the student's existing tags are removed; adding tags is not cumulative.
  To add one tag while keeping existing tags, use [the `tag` command](#adding-one-tag-tag).
* To remove all of a student's tags, give `-t` an empty value: `-t ""` or
  `--tag=""`. A quoted whitespace-only value, such as `-t "   "`, also clears
  all tags, including when the spaces were copied as Unicode spaces. Supply
  the blank tag value on its own; combining it with another tag is refused with
  `A blank tag clears all tags only when it is given on its own.`
  A bare `-t` or unquoted `--tag=` is refused, so that a mistyped
  option is reported rather than read as a request to clear.
* An NUS Student ID target that no student holds changes nothing and shows
  `No student has NUS Student ID A0123456X.` A letter-first target that is not
  a valid ID, such as `A0123-456`, is refused with the field-level reason.

Examples:

*  `edit --target-id 1234 --id A0123456X` Changes the ID of the student holding `1234` to `A0123456X`.
*  `edit 1 --phone 91234567 --email johndoe@example.com` Edits the phone number and email address of the 1st student to be `91234567` and `johndoe@example.com` respectively.
*  `edit 1 -e mei__tan@hotmail.com` Records the address with both underscores.
*  `edit 1 --email ""` Removes the 1st student's email while keeping all other details.
*  `edit 1 -p ""` Removes the 1st student's phone number while keeping all other details.
*  `edit 2 --name "Betsy Crower" --tag ""` Edits the name of the 2nd student to be `Betsy Crower` and clears all existing tags.
*  `edit 1 --id A08901234N` Records or replaces the 1st student's NUS Student ID.
*  `edit 1 -i ""` Removes the 1st student's NUS Student ID while keeping all other details.
*  `edit 1 -n "Ravi s/o Kumaran"` Edits the name of the 1st student to be `Ravi s/o Kumaran`.
*  `edit 3 -p "+65 9123 4567" -t T1 -t "Lab 3"` Replaces the phone number and both tags of the 3rd student.
*  `edit 1 --follow-up` Flags the 1st student for follow-up. Repeating it keeps the flag set.
*  `edit 1 --no-follow-up` Clears the flag. Repeating it keeps the flag cleared.
*  `edit 2 -p 91234567 --follow-up` Updates the phone number and sets follow-up in one command.
*  `edit A02345678G --phone 91234567` Edits the phone number of the student holding the NUS
   Student ID `A02345678G`, even while a search hides that student.
*  `edit a02345678g -n "Betsy Crower"` Edits the same student, because matching an ID ignores case.
*  `edit A02345678G --id A7654321Y` Changes that student's NUS Student ID from `A02345678G` to
   `A7654321Y`, keeping all other details.

A cleared phone number, email or NUS Student ID, a cleared tag set, and an explicitly set or cleared follow-up status are
saved at once, and return as they were when you next launch TAB. The command result displays the updated
record (`Edited student: NAME; Phone: PHONE...`) with fields formatted consistently
with the student card, omitting cleared or absent optional fields and sorting tags
alphabetically (ignoring case).

### Adding one tag: `tag`

Adds one tag to an existing student, keeping every existing tag and all other
details, including follow-up status.

Format: `tag TARGET -a TAG`

* `TARGET` is a positive index in the displayed list, a NUS Student ID starting
  with a letter, or `--id NUS_STUDENT_ID` (alias `-i`). An ID is matched
  case-insensitively across the whole student book, even when the current
  search hides that student. IDs starting with a digit need `--id` or `-i`;
  a bare number always means a displayed index. The current search stays active.
* Give exactly one target. Mixing an index or plain ID with `--id`, or
  repeating the ID option, is refused without changing or saving any record.
* `-a` selects addition; `--add` is its long alias. Supply exactly one tag per command; repeat the
  command to add another. For replacing the full tag set, use `edit INDEX -t TAG`.
  To remove one tag, use [`tag TARGET -r TAG`](#removing-one-tag-tag).
* A tag must contain at least one letter or number in any writing system.
  Symbols are allowed alongside them. Put a tag containing spaces or starting
  with a hyphen in double quotes. Escape a literal quote as `\"` and a
  backslash as `\\` inside a quoted value.
* `Needs follow-up` is the label of the follow-up marker rather than a tag, so
  it is refused whatever its case or spacing, with a reply pointing to
  `--follow-up`. A card would otherwise show the same label twice, once as a
  tag and once as the marker.
* Leading and trailing whitespace is removed and runs of whitespace become
  one space. Tags retain their letter case. When checking whether the student
  already has the tag, case and insignificant whitespace are ignored, so
  `Lab 3` and `lab  3` count as the same tag for this command.
* An existing tag reports `This student already has tag [TAG]. No changes made.`
  The reply uses the stored spelling: if the tag is `Lab 3`, entering `lab  3`
  reports `[Lab 3]`. Case variants are merged when tags are entered or loaded
  from a data file, keeping the first normalized spelling.
* An invalid target or tag, a missing value, or a repeated add option is refused
  without changing any student details or writing the data file. An empty
  quoted tag is invalid; it does not clear tags.
* If you omit an action option, the reply explains how to add or remove a tag. A tag entered
  directly after a valid index is reported as an unexpected value, and `-t`
  is reported as an unknown option. Use `tag 1 -a lab4` to add `lab4`.
* An unquoted negative index reports both that the index must be positive
  and that the negative number is not an option, followed by the action
  guidance.
* A successful addition is saved at once and remains after restarting TAB.
  The feedback starts with `Added tag [TAG]:` and shows the updated record.

Examples:

* `tag E7654321 -a "Lab 3"` adds a tag by ID, even if the student is hidden.
* `tag --id 1234 --add T2` targets the student whose ID is `1234`.
* `tag 1 -a T2` adds `T2` to the 1st displayed student while keeping existing tags.
* `tag 1 --add "Lab 3"` adds a tag containing a space.
* `tag 2 -a "Project A/B"` adds a tag containing a slash.
* `tag 2 -a "组 1"` adds a tag in another writing system.
* `tag 1 -a "-Lab"` adds a tag starting with a hyphen.

Examples of refused commands and their replies:

```text
tag 1 -t T2
There is no -t option.
To add a tag, use -a or --add, e.g. tag 1 -a "Lab 3". To remove a tag, use -r or --remove, e.g. tag 1 -r "Lab 3".

tag 1 lab4
Unexpected value after INDEX: "lab4". Put field values after an option.
Missing required field(s): -a TAG or -r TAG
To add a tag, use -a or --add, e.g. tag 1 -a "Lab 3". To remove a tag, use -r or --remove, e.g. tag 1 -r "Lab 3".

tag -1 -a T2
Index must be a positive integer.
There is no -1 option.
To add a tag, use -a or --add, e.g. tag 1 -a "Lab 3". To remove a tag, use -r or --remove, e.g. tag 1 -r "Lab 3".
```

### Removing one tag: `tag`

Removes one tag from an existing student, keeping unrelated tags and all other
details, including follow-up status.

Format: `tag TARGET -r TAG`

* `-r` selects removal; `--remove` is its long alias. Use exactly one action
  and one tag per command. Combining addition and removal, or repeating the
  removal option (including mixed aliases), is refused without saving changes.
* Student targets, quoting, valid tag characters and whitespace follow the
  [same rules as adding one tag](#adding-one-tag-tag). Option names are
  case-sensitive; tag matching ignores case and insignificant whitespace.
* If the tag is present, feedback starts with `Removed tag [TAG]:` using its
  stored spelling, followed by the updated student details. Case variants from a hand-edited file merge when loaded, so removing a tag
  removes that group. Unrelated tags keep their spelling.
* If the tag is absent, TAB reports
  `This student does not have tag [TAG]. No changes made.` using your
  normalized input. The record, displayed list and saved file stay unchanged.
  Asking a student flagged for follow-up to have `Needs follow-up` removed
  adds `This student is flagged for follow-up, which is not a tag. To clear
  the flag, use edit INDEX --no-follow-up.`, because the label beside the
  name is that flag rather than a tag. If the student is hidden by the current
  search, the hint gives `edit --target-id NUS_STUDENT_ID --no-follow-up` with their ID.
* An invalid target or tag, an empty quoted tag, or a missing value is refused
  with a specific error and leaves records and the saved file unchanged.
* Removing the last tag leaves a student with no general tags. A general tag
  named `Needs follow-up` can no longer be added, and a data file holding one
  no longer loads: TAB names the record at startup and asks you to take the
  entry out of `tags` and set `"flag": true` instead. Correct the file while
  TAB is closed, as described in [Editing the data file](#editing-the-data-file).
  The flag itself is cleared with `edit INDEX --no-follow-up`.
* The current search stays active. If it matched only the removed tag, the
  student may disappear from the displayed list; use `list` to see them again.
* A successful removal is saved at once and remains after restarting TAB.

Examples:

* `tag E7654321 -r "Lab 3"` removes a tag by ID.
* `tag -i 1234 --remove T2` removes `T2` from the student whose ID is `1234`.
* `tag 1 -r T2` removes `T2` from the 1st displayed student.
* `tag 1 --remove "  lab   3  "` removes `Lab 3`, ignoring case and extra spaces.
* `tag 2 -r "Project A/B"` removes a tag containing a slash.
* `tag 2 -r "组 1"` removes a tag in another writing system.
* `tag 1 -r "-Lab"` removes a tag starting with a hyphen.

### Locating students by detail: `find`

Finds students matching every supplied search term within its selected field.

Format: `find [FIELD_OPTION TERM]... [-f | -nf]`

Provide at least one field term or one follow-up status option. Every search term needs a field option, which takes one word or one double-quoted phrase.

| Field | Options | Example |
| --- | --- | --- |
| Name | `-n`, `--name` | `find -n Jason` |
| NUS Student ID | `--id`, `-i` | `find --id A0123` |
| Phone | `-p`, `--phone` | `find -p 9123` |
| Email | `-e`, `--email` | `find -e nus.edu` |
| Tag | `-t`, `--tag` | `find -t friend` |
| Needs follow-up | `-f`, `--follow-up` (no value) | `find -f` |
| Does not need follow-up | `-nf`, `--no-follow-up` (no value) | `find -nf` |

* Matching is case-insensitive and allows partial values: `find -n jas` matches `Jason`.
* All terms use **AND** matching, within and between fields. There is no OR search mode.
  For example, `find -n Jason -t Lab -n Tan` requires both name terms and the tag term.
  The order of the options does not matter.
* Field options restrict each term to that field. `find -t friend` excludes students
  who have `friend` only in their name or email. Absent optional fields do not match.
* Repeat an option to require more terms. `find -n Jason -t lab -t tutorial` matches
  Jason Lim with tag `lab tutorial` and Jason Zhou with tags `lab 5` and `tutorial 3`,
  but excludes Jason Tan with only tag `lab`. Terms may match the same tag or different tags.
* Enclose a multi-word term in double quotes: `find -n "Jason Tan"` requires that whole
  phrase in the name. `find -t "lab tutorial"` requires the phrase within one tag;
  separate tags are never joined. Repeated spaces inside a phrase are collapsed to one space.
* Name searches ignore Latin accents and straight/curly apostrophe differences:
  `find -n nguyen` matches `Nguyễn Văn An`, `find -n jose` matches `José Rivera`,
  and `find -n O'Brien` matches `Sean O’Brien`. The stored spelling stays unchanged.
* Unicode case variants match in names and tags: `find -n σ` matches `ΝΙΚΟΣ`,
  and `find -t οσ` matches tag `ΟΣ`, consistent with adding/removing that tag.
  Accents and apostrophes remain significant in tags and other fields.
* Single quotation marks do not group words; use double quotes for phrases.
* Bare terms are refused, including a name given before any option. Use a field option
  for every term. An extra word after an option is also refused; repeat the option or quote a phrase.
* Status options take no value. `find -f` lists students needing follow-up, and `find -nf`
  lists students not needing it. Combine a status option with field terms using AND:
  `find -f -n Jason -t Lab` requires the status, name fragment and tag fragment.
  Omitting both status options searches students of either status.
* Use at most one status option. Conflicting options (`-f` with `-nf`) or a repeated
  status option, including mixed aliases, are refused without changing the results.
* To include a literal double quote or backslash inside a term, escape it with a backslash:
  `\"` or `\\`. An unclosed quote or an empty or whitespace-only term is refused
  without changing the current student list.
* Quote a term starting with a hyphen, or attach it with `=`:
  `find -t "-Lab"` and `find --tag=-Lab` search for the literal tag text `-Lab`.
* Phone terms containing only digits, grouping spaces and phone punctuation (`+`, `-`, `(`, `)`, `.`)
  also search the phone with all non-digit characters removed, provided the term contains a digit.
  A student who holds no phone number matches no phone term at all.
  `find -p 91234567`, `find -p 91234`, `find -p 9123-4567` and `find -p "9123 4567"`
  all match `+65 9123 4567` or `91234567`. `find -p +6591234567` also matches the number
  stored with the country code. Quote terms containing spaces;
  copied Unicode spaces work too. Equivalent Unicode decimal digits match across scripts:
  `find -p 9123` matches `９１２３４５６７`, and `find -p "９１２３ ４５６７"` matches `91234567`.
  Displayed and saved numbers keep their formatting. Terms containing other characters, such as `"ext 21"`,
  and punctuation-only terms match the stored text.
* TAB reports the number of matches: `Found 0 students.`, `Found 1 student.` or `Found 3 students.`.

Examples:
* `find -n john` returns students whose names contain `john`, including `John Doe`.
* `find -t "Lab 3"` returns students with a tag containing the phrase `Lab 3`.
* `find -n Alice -t "Lab 3"` requires both the name fragment and the tag phrase.
* `find -p 91234 -t friend` requires both the phone fragment and the tag fragment.
* `find --id a0123 -p 9123 -e nus.edu` requires all three field conditions.

### Deleting a student: `delete`

Deletes the specified student from the student book.

Format: `delete INDEX` or `delete NUS_STUDENT_ID` or `delete --id NUS_STUDENT_ID`

* An `INDEX` deletes the student at that position in the displayed student list.
* An `INDEX` **must be a positive integer** 1, 2, 3, ...
* An `NUS_STUDENT_ID` deletes the student who holds that ID, wherever they are in
  the book — even while a search is hiding them. The match ignores case.
* `--id NUS_STUDENT_ID` names the value as an ID, which an ID starting with a digit
  needs — the plain form would read it as an index. `-i` works in place of `--id`.
* A target that is not a positive integer, such as `0`, `-1` or `1.5`, reports
  `Index must be a positive integer.` without the full usage block. A refused
  target that reads as an NUS Student ID, such as `1ABC`, also names the
  `delete --id 1ABC` command that addresses it.
* If an out-of-range numeric target is also a stored NUS Student ID, TAB suggests
  the exact `delete --id VALUE` command. Leading zeroes in an ID are preserved.
  A bare number that is a valid displayed position still deletes by that position.
* The command result identifies the removed record (`Deleted student: NAME; Phone: PHONE...`)
  so you can verify exactly which student was deleted from the book.
* `delete` takes no field values, so a value found after the target is refused with
  `Unexpected value after INDEX: "..."`, naming the part to remove, as in
  `Unexpected value after INDEX: "2". delete accepts one index and nothing else.`

Examples:
* `list` followed by `delete 2` deletes the 2nd student in the student book.
* `find -n Betsy` followed by `delete 1` deletes the 1st student in the results of the `find` command.
* `delete A01234567X` deletes the student holding the NUS Student ID `A01234567X`.
* `delete a01234567x` deletes the same student, because matching an ID ignores case.
* `delete --id 1A2B3C` deletes the student whose NUS Student ID is `1A2B3C`, which the plain
  form would otherwise read as an index.
* `find -n Alex` followed by `delete A02345678G` deletes Charlotte Oliveiro, whose NUS Student
  ID is `A02345678G` in the sample data, even while the search hides her.

### Clearing all entries: `clear`

Asks for confirmation before clearing all entries from the student book.

Format: `clear`

TAB shows a prompt such as `Clear all 6 students? This cannot be undone. [y/N]`.
The count includes students hidden by a search. The records and saved file
stay unchanged until you reply in the command line and press Enter:

* `y` or `yes` deletes all students and saves the empty book. This cannot be undone.
* `n`, `no`, or Enter without a reply cancels without changing any records or the saved file.

Replies ignore case and surrounding whitespace: `Y`, `yEs`, and `yeS` also
confirm; `N` and `nO` also cancel. Invisible format marks copied with a reply
are ignored, just as in commands; the shaping joiners needed by some scripts
are preserved. A reply containing only ignored marks and whitespace cancels.
Cancellation is the `[y/N]` default.
Only the exact replies above are accepted: words such as
`yellow` and `nope` are invalid. Any other reply, including another command,
leaves the data unchanged and answers `Invalid reply. Enter y/yes or n/no
(Enter cancels).` The request to clear stays pending until you confirm or
cancel, so cancel first if you want to run a different command. Closing TAB
while the prompt is pending also leaves the student data unchanged. If the
book is already empty, TAB reports that there is nothing to clear and does
not ask for confirmation.

### Exiting the program: `exit`

Exits the program.

Format: `exit`

### Saving the data

The status bar shows the absolute path to your student data file. If the path
does not fit, it is shortened in the middle; hover over it to read the full path.

TAB automatically saves changes to student records. You do not need to save manually.
If TAB cannot save a change, it reports the error and leaves the student records as
they were before the command. Commands that do not change student records, including
`list`, `find`, `help`, and `exit`, do not write to the data file. Clear prompts,
cancellation, and invalid replies also leave the saved file unchanged.
A failed first save leaves no data file behind. After freeing disk space or
restoring write access, retry the command, or restart with the sample students.

If another TAB window has saved different student records since this window
loaded them, TAB refuses this window's change and preserves both the saved file
and this window's previous records. Close the stale window and restart TAB to
load the saved records, then retry the change. If another window is saving at
that moment, wait and retry; restart if TAB then reports changed records.
Use one window per data file for editing. TAB keeps a small `.tab.json.lock`
file beside `tab.json` to coordinate saves; leave it in place, even after exit.
It contains no student records, and its presence alone does not block saving.

Window size and position are saved separately in `preferences.json` in TAB's
home folder. On launch, TAB keeps the saved size and position when they fit
an available screen. If a screen was disconnected, TAB moves the window onto
an available screen; oversized windows shrink to its usable area. This also
applies after a resolution or display-scaling change. You can also resize the
window to fit a 1280×720 display at 150% scaling; the student list and help text
scroll while the summary total, help footer and status bar remain visible.
If the file contains only `null`, `guiSettings` is `null`, or the
file cannot be parsed, TAB opens with default window settings and rewrites the
preferences file when it can. This recovery does not change saved student records in `data/tab.json`.
If preferences cannot be saved, TAB prints a concise warning naming the path
and reason. It uses the loaded or default settings for that session. A warning
on exit means the window settings could not be saved; TAB still closes normally.

If saving reports that the required data folder is occupied by a file, close TAB
and move or rename that unrelated file before restarting. TAB preserves it.
A folder occupying the save-lock path (normally `data/.tab.json.lock`) also blocks
saving. Close every TAB window before moving or renaming that folder. Keep any
contents you need; TAB does not remove it automatically. The lock path must be a
writable file, and both the data file and its folder need write permission.
A failed save restores the previous student records and active search.

### Editing the data file

TAB data is saved automatically as a JSON file `[JAR file location]/data/tab.json`.
Advanced users can edit this file directly:

1. Close every TAB window using this file, then make a backup of `data/tab.json`.
1. Edit the file, preserve its JSON structure, and follow the encoding guidance
   below when saving.
1. Restart TAB to load the edited records and check the changes.

TAB reads student data only at startup. Edits made while TAB is open do not
appear in that session. TAB refuses a later save if the student records in the
file differ from the records that window loaded. Text editors do not participate
in TAB's save locking, so always close TAB before editing the file.

If TAB cannot open `data/tab.json` or the folder holding it, it reports that
at the next run instead of opening the main window. It never treats a file it
cannot read as a first launch, so your records are not replaced by sample
students. Restore your permission to the file and the folder, then restart TAB.

Save the file as **UTF-8**, with or without a leading byte order mark (BOM).
TAB accepts both forms and preserves accented letters and other Unicode text.
Files saved in another encoding, such as Windows-1252 (ANSI), may contain
invalid UTF-8. TAB rejects those files without changing their bytes and asks
you to reopen them with their original encoding and save them as UTF-8.

Field names are case-sensitive. Each student record accepts `name`, `phone`,
`email`, `nusStudentId`, `tags` and `flag`. The removed `address` field is still
accepted and ignored for compatibility with older files. Other keys, such as
`nusStudentID`, `Email` or `tag`, stop loading with an error naming the unknown
key, student record number (counting from 1), and name when present, rather
than silently losing that value on the next save.
Within a student's `tags` array, case and whitespace variants merge on loading,
keeping the first normalized spelling (for example, `[" t1 ", "T1"]` becomes
one `t1` tag). The next successful save writes the merged tags.
The top-level object accepts `students` and an optional `_comment` description.
Correct the spelling or remove the unsupported field, then restart TAB; the
rejected file stays unchanged.

Keep one student-book object in the file, with each key appearing only once
within its object. TAB rejects extra records or JSON values after the closing
brace and repeated keys such as two `phone` entries in one student or two
top-level `students` lists. The error identifies the location and, for a
repeated key, its name. Move extra records into the `students` array or keep
only the intended value for the repeated key, then restart TAB. The rejected
file stays unchanged.

The `tags` field holds a JSON array of strings, such as `"tags": ["Lab 3", "组 1"]`.
An absent key, `null`, or `[]` means no tags. A single string without array
brackets, or an array entry that is a number, Boolean, object, array or `null`,
is rejected with an error naming `tags` and the student record. For an invalid
entry, the error also gives its position, counting from 1. Correct the value
and restart; TAB leaves the rejected file unchanged.

A student who needs follow-up has `"flag": true` in their JSON record. An
unflagged student normally has no `flag` key; absence, `false`, and `null` all
mean that no follow-up is needed. Data files created before this field existed
continue to load, with every record that lacks the key treated as unflagged.
Use actual JSON Boolean values, without quotation marks. Numbers such as
`0`, `1` or `2`, strings such as `"true"`, and other value types are rejected
with an error naming the student record and `flag`. TAB leaves the file
unchanged so that you can correct the value and restart.

`Needs follow-up` is the label TAB shows on a flagged student's card, so it
names no tag. A record whose `tags` list holds it, in any letter case or
surrounding spacing, stops loading with an error naming the student record and
the field to correct. Remove the entry from `tags` and set `"flag": true`
instead. A file written before this rule needs that one edit before it loads. A
name built around the label, such as `Needs follow-up soon`, stays an ordinary
tag.

A student with an NUS Student ID has a `"nusStudentId"` string such as
`"A01234567X"` in their JSON record. The key is optional: a missing key or
`null` means that no NUS Student ID has been recorded. A non-null value must
contain between 1 and 32 ASCII letters and digits. TAB accepts lowercase letters
and saves valid IDs in uppercase.

TAB identifies duplicate records by NUS Student ID, ignoring letter case. Two
records with the same name load when their NUS Student IDs differ or either ID
is absent. Two records with the same NUS Student ID make the file unreadable;
the startup error names the repeated ID and both record locations so that you
can correct it. Phone numbers and emails are searchable details rather than
identity keys.

A phone key is optional in the same way: a missing key, `null`, an empty
string, or a string containing only spaces means that no phone number has been
recorded, and TAB leaves the key out when it saves such a record. A non-null
value must still hold at least 3 digits, so a malformed number stops the file
loading rather than being dropped from the record. Files written before the
phone became optional continue to load unchanged.

An email key is optional: a missing key, `null`, an empty string, or a string
containing only spaces means that no email has been recorded. TAB removes
leading and trailing spaces from a non-empty saved email before validating it.

<box type="warning" seamless>

**Caution:**
If your changes make the data file unreadable, TAB shows an error at the next
run instead of opening the main window. The error identifies `data/tab.json`
and, when possible, the affected student record or field. For a JSON syntax
error with a usable location, it gives the line and column and shows a short,
line-numbered excerpt around the mistake. Long lines are shortened around the
error instead of being shown in full. Without a usable location, it gives a
concise explanation without showing the file contents. TAB leaves the file
unchanged so that you can repair or replace it, then restart TAB.<br>
Saved email addresses longer than 254 characters are rejected before validation
because their shape can otherwise exhaust Java's stack. This is the same limit
used by commands. We still recommend backing up the file before editing it.
</box>

--------------------------------------------------------------------------------------------------------------------

## FAQ

**Q**: How do I transfer my data to another computer?<br>
**A**: Install TAB on the other computer, then copy `data/tab.json` from your
previous TAB home folder to the same place beside the new JAR, creating the
`data` folder if it is not there. A new install has no data file to overwrite:
TAB writes one only when a command changes a record, so a fresh TAB that has
only been looked at leaves the folder without it. Copy the file while TAB is
closed, since TAB rewrites it from its own copy at the next change.

--------------------------------------------------------------------------------------------------------------------

## Known issues

The linked reports track these limitations and their fixes:

* **Read-only save lock:** A read-only `data/.tab.json.lock` can block saving
  while the error names `data/tab.json` instead. Close all TAB windows and
  restore write access to the lock file before restarting. See
  [#438](https://github.com/AY2627S1-CS2103T-T16-1/tp/issues/438).

--------------------------------------------------------------------------------------------------------------------

## Command summary

Action     | Format, Examples
-----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------
**Add**    | Keeps the search if the new student matches; otherwise shows all students with a notice. <br> `add NAME [-p PHONE] [-e EMAIL] [--id NUS_STUDENT_ID] [-t TAG]... [--follow-up]` <br> Alternate spellings: `-p`/`--phone`, `-e`/`--email`, `--id`/`-i`, `-t`/`--tag`; `OPTION=VALUE` is also accepted <br> e.g., `add James Ho --phone=22224444 -i=A07890123M --tag=T1 -t Lab3 --follow-up`
**Clear**  | `clear`, then `y`/`yes` to confirm; `n`/`no` or an empty reply to cancel (replies ignore case)
**Delete** | `delete INDEX` or `delete NUS_STUDENT_ID` or `delete --id NUS_STUDENT_ID`<br> The ID option, spelled `-i`/`--id` or `--id=VALUE`, is what an ID starting with a digit needs<br> e.g., `delete 3`, `delete A01234567X`, `delete --id 1A2B3C`
**Edit**   | `edit TARGET [-n NAME] [-p PHONE] [-e EMAIL] [--id NUS_STUDENT_ID] [-t TAG]... [--follow-up \| --no-follow-up]`<br> `TARGET`: displayed index, letter-leading NUS Student ID, or `--target-id NUS_STUDENT_ID`; `--id` sets the ID field <br> Alternate value spellings: `-n`/`--name`, `-p`/`--phone`, `-e`/`--email`, `--id`/`-i`, `-t`/`--tag`; `OPTION=VALUE` is also accepted <br> Follow-up aliases: `-f` sets; `-nf` clears.<br> e.g., `edit 2 --name="James Lee" --id=A08901234N`, `edit A01234567X -p 91234567`
**Tag**    | `tag TARGET -a TAG` or `tag TARGET -r TAG` <br> `TARGET`: displayed index, letter-leading NUS Student ID, or `--id NUS_STUDENT_ID` (alias `-i`) <br> Alternate spellings: `--add`, `--remove`; `OPTION=VALUE` is also accepted <br> e.g., `tag 1 --add="Lab 3"`, `tag 1 --remove="Lab 3"`, `tag --id A02345678G --add T2`
**Find**   | `find [FIELD_OPTION TERM]... [-f \| -nf]`<br> e.g., `find -n Jason -t lab -t tutorial` (all terms required)
**List**   | `list`
**Exit**   | `exit`
**Help**   | `help` (also `F1` or Help menu); Space/`b` to page, `c` to copy the guide URL, `q`/Esc to return
**Recall** | `Up` / `Down` in the command box; session only
