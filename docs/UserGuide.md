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

1. Copy the file to the folder you want to use as the _home folder_ for TAB.

1. Open a terminal, `cd` to the folder containing the JAR file, and run `java -jar tab.jar`.<br>
   A GUI similar to the one below should appear in a few seconds. Note how the app contains some sample data.
   The picture shows TAB in use, so a fresh launch has an empty command box and no reply above the list.<br>
   ![Ui](images/Ui.png)

1. Type a command in the command box and press Enter to execute it. For example, type **`help`** and press Enter to open the help window.<br>
   Some example commands you can try:

   * `list` : Lists all students.

   * `add John Doe -p 98765432 -e johnd@example.com --id A01234567X` : Adds a student named `John Doe` with an NUS Student ID.

   * `delete 3` : Deletes the 3rd student shown in the current list.

   * `clear` : Asks before deleting all students. Type `y` and press Enter
     to confirm, or press Enter without a reply to cancel.

   * `exit` : Exits the app.

1. Refer to the [Features](#features) section below for details of each command.

--------------------------------------------------------------------------------------------------------------------

Text pasted into the command box keeps its word boundaries: tabs and line
breaks become spaces. This lets you paste names or values from spreadsheet
cells without joining their words. Pasted text remains one command; inspect
it before pressing Enter.

## Features

<box type="info" seamless>

**Notes about the command format:**<br>

* Words in `UPPER_CASE` are the parameters to be supplied by the user.<br>
  For example, in `add NAME`, replace `NAME` with a value such as `John Doe`.

* Items in square brackets are optional.<br>
  For example, `NAME [-t TAG]` can be used as `John Doe -t friend` or as `John Doe`.

* A vertical bar `|` separates alternatives; choose at most one and do not type the bar.<br>
  For example, `[--follow-up | --no-follow-up]` allows either option or neither.

* Items followed by `...` can appear zero or more times.<br>
  For example, `[-t TAG]...` may be omitted, or written as `-t friend` or as
  `-t friend -t family`.

* Parameters can be in any order.<br>
  For example, if the command specifies `-p PHONE_NUMBER -e EMAIL`,
  `-e EMAIL -p PHONE_NUMBER` is also acceptable. A parameter given before any
  option, such as the name in `add`, keeps its place at the front.

* Every command marks its fields with options such as `-p`. Value options also
  have descriptive long aliases such as `--phone`; the command-specific
  sections list them. A value holding spaces goes in double quotes after an
  option, as does any value opening with a hyphen.

* Commands that take no parameters (`help`, `list`, `exit`, and `clear`)
  reject unexpected input and report it without executing the command.<br>
  For example, `help 123` reports `help takes no arguments. Unexpected input:
  "123".` Whitespace after the command is accepted, but quoted values,
  including `""`, are still arguments.

* If you are using a PDF version of this document, be careful when copying and pasting commands that span multiple lines as space characters surrounding line-breaks may be omitted when copied over to the application.
</box>

### Typing a command

The command line marks the parts of a command as you type, so a mistake is
visible before you press Enter:

* the command word is in the line's accent colour and bold
* an option such as `-p` is in mauve
* a value in double quotes is in green
* anything else is in ordinary text

If a command is refused, the whole line turns red until you start correcting
it. A command longer than the line scrolls with the caret, so you can always
see where you are typing, and selecting part of it (for example with
`Ctrl`+`A`) draws a band behind the selection.

<box type="tip" seamless>

**Tip:** The colours break a command where the parser breaks it, so if a value
you meant to quote is not coloured as a quoted value, the parser will not read
it as one either.
</box>

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

<box type="warning" seamless>

**Caution:** TAB never runs the command it suggests. Nothing is added, changed
or deleted by a mistyped command word, and what you typed stays in the command
box, so you can correct the word rather than type the command again.
</box>

### Reading the summary beside the list

The panel on the left of the window summarises the students the list is
showing, so you can see the shape of your cohort without running a command.

* **Flagged** counts the students marked as needing a follow-up.
* **Tags** lists each tag in use with the number of students carrying it,
  most used first. Tags used by the same number of students are listed
  alphabetically, ignoring case.
* The foot of the panel gives the total.

<box type="tip" seamless>

**Tip:** The summary describes the students currently listed, not the whole
student book. Run `find Alex` and it counts only the students that match;
run `list` to see the whole cohort again.
</box>

### Viewing help: `help`

Shows a message explaining how to access the help page.

![help message](images/helpMessage.png)

Format: `help`


### Adding a student: `add`

Adds a student to the student book.

Format: `add NAME -p PHONE_NUMBER [-e EMAIL] [--id NUS_STUDENT_ID] [-t TAG]... [--follow-up]`

The value options accept these equivalent spellings: `-p` and `--phone`,
`-e` and `--email`, `--id` and `-i`, and `-t` and `--tag`. The spellings may
be mixed in one command, but giving the same field twice is an error even when
the two spellings differ.

<box type="tip" seamless>

**Tip:** The email is optional. Record a student you only have a phone number
for, rather than inventing an address to satisfy the command. To remove an
email entered by mistake, use `edit INDEX -e ""`.

An address may run to 254 characters, the longest an email address is allowed
to be, so no real address is turned away. A longer value is refused and TAB
tells you how long it was — useful when a copy-and-paste has picked up more
than you meant.
</box>

<box type="tip" seamless>

**Tip:** The NUS Student ID is optional. Give it with `--id`, or
use the shorter `-i` alias. It must contain between 1 and 32 ASCII letters and
digits, such as `A01234567X` or `E0123456`. Lowercase letters are accepted and
stored as uppercase. To remove an NUS Student ID entered by mistake, use
`edit INDEX --id ""`; a value containing only spaces clears it as well.
</box>

<box type="tip" seamless>

**Tip:** A student can have any number of tags, including zero. A tag may hold
spaces, hyphens and any script, so `Lab 3`, `needs-followup` and
`AY2627 Sem 1 CS2103T T16` are all valid.
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
</box>

<box type="tip" seamless>

**Tip:** A name may hold anything you would write on a roster: slashes,
hyphens, apostrophes, full stops, and any script. It only has to contain at
least one letter or number, in any writing system.
</box>

<box type="tip" seamless>

**Tip:** The name comes first and may hold spaces as it is, so
`add Ravi s/o Kumaran -p 91234567` needs no quotes at all. Slashes, hyphens and
apostrophes are ordinary characters here.

Use double quotes for an option value holding spaces, and for any value opening
with a hyphen, so that TAB does not read it as an option:
`add Siti Nur-Aisyah -p 84420917 -t "Lab 3"` and `add "-Ahmad" -p 84001122`.

To put a double quote or a backslash inside a value, write `\"` or `\\`:
`add Dwayne \"The Rock\" Johnson -p 91234567`.
</box>

Examples:
* `add John Doe -p 98765432 -e johnd@example.com`
* `add "Betsy Crowe" --tag friend --email betsycrowe@example.com --phone 1234567 --id A01234567X -t criminal`
* `add "Ravi s/o Kumaran" -p 91234567 -e e0923841@u.nus.edu`
* `add Siti Nur-Aisyah -p 84420917 -e e1147203@u.nus.edu -i a11472030k -t T1 -t "Lab 3"`
* `add Priya Nair -p 91234567 -e priya@example.com --follow-up`
* `add 陈伟明 -p 98123344 -e e1077310@u.nus.edu`

A student's NUS Student ID appears on the detail line of their card and in successful
command feedback. A student without one has no placeholder or blank gap there.
A flagged student's card carries a `Needs follow-up` marker at its right-hand
edge, on the same line as the name, and the command result says so too.

A name too long for a card is shortened with an ellipsis in its middle, which
keeps both the start and the end of the name, so two students whose names begin
alike still read as two. Hover over any name the card cannot show in full,
whether it was shortened this way or left narrow by a crowded row, to see the
whole of it.

When a student is added, the command result displays the new record
(`New student added: NAME; Phone: PHONE...`) so you can confirm the details
immediately. Consistent with the student card, optional fields appear in the
result only when present: an unflagged student or a student without an email
or tags leaves no empty placeholder labels in the message, and tags appear
in alphabetical order (ignoring case).

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
> add John Doe -e e1234567
Missing required field(s): -p PHONE
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

Format: `edit INDEX [-n NAME] [-p PHONE_NUMBER] [-e EMAIL] [--id NUS_STUDENT_ID] [-t TAG]... [--follow-up | --no-follow-up]`

The value options accept these equivalent spellings: `-n` and `--name`, `-p`
and `--phone`, `-e` and `--email`, `--id` and `-i`, and `-t` and `--tag`.
The spellings may be mixed in one command, but giving the same field twice is
an error even when the two spellings differ.

<box type="tip" seamless>

**Tip:** `edit` reads field values the same way as `add`. A name holding slashes, hyphens or apostrophes needs nothing
done to it beyond the quotes any value with spaces takes:
`edit 1 -n "Ravi s/o Kumaran"`.
</box>

* Edits the student at the specified `INDEX`. The index refers to the index number shown in the displayed student list. The index **must be a positive integer** 1, 2, 3, ...
* The index comes first, before any option.
* At least one of the optional fields must be provided.
* Existing values will be updated to the input values.
* `--follow-up` (alias `-f`) sets follow-up; `--no-follow-up` (alias `-nf`) clears it and is
  available only with `edit`. New students are unflagged by default when added.
  Both options take no value. Repeating either request succeeds and keeps the requested state.
  Leaving both out preserves the student's follow-up status.
* Choose at most one follow-up option. Combining `--follow-up` or `-f` with
  `--no-follow-up` or `-nf` is rejected without changing any student details.
* To remove a student's email, use `-e ""`. A quoted value containing only spaces
  also removes it. Leaving out `-e` keeps the email unchanged; a bare `-e` is an
  error. Clearing an already absent email succeeds.
* A new email may run to 254 characters, the same limit `add` applies. A longer
  value is refused, TAB tells you how long it was, and the student's details
  stay as they were.
* To remove a student's NUS Student ID, use `--id ""` or `-i ""`. Leaving the
  option out keeps the NUS Student ID unchanged; a quoted value containing only
  spaces removes it, while a bare option is an error. Clearing an already absent
  NUS Student ID succeeds. Name and phone are required and cannot be cleared.
* When editing tags, all of the student's existing tags are removed; adding tags is not cumulative.
* To remove all of a student's tags, give `-t` an empty value: `-t ""`. A bare
  `-t` with nothing after it is refused, so that a mistyped option is reported
  rather than read as a request to clear.

Examples:
*  `edit 1 --phone 91234567 --email johndoe@example.com` Edits the phone number and email address of the 1st student to be `91234567` and `johndoe@example.com` respectively.
*  `edit 1 --email ""` Removes the 1st student's email while keeping all other details.
*  `edit 2 --name "Betsy Crower" --tag ""` Edits the name of the 2nd student to be `Betsy Crower` and clears all existing tags.
*  `edit 1 --id A01234567X` Records or replaces the 1st student's NUS Student ID.
*  `edit 1 -i ""` Removes the 1st student's NUS Student ID while keeping all other details.
*  `edit 1 -n "Ravi s/o Kumaran"` Edits the name of the 1st student to be `Ravi s/o Kumaran`.
*  `edit 3 -p "+65 9123 4567" -t T1 -t "Lab 3"` Replaces the phone number and both tags of the 3rd student.
*  `edit 1 --follow-up` Flags the 1st student for follow-up. Repeating it keeps the flag set.
*  `edit 1 -f` Also sets follow-up, even if the student is already flagged.
*  `edit 1 --no-follow-up` Clears the flag. Repeating it keeps the flag cleared.
*  `edit 1 -nf` Also clears follow-up, even if the student is already unflagged.
*  `edit 2 -p 91234567 --follow-up` Updates the phone number and sets follow-up in one command.

A cleared email or NUS Student ID, a cleared tag set, and an explicitly set or cleared follow-up status are
saved at once, and return as they were when you next launch TAB. The command result displays the updated
record (`Edited student: NAME; Phone: PHONE...`) with fields formatted consistently
with the student card, omitting cleared or absent optional fields and sorting tags
alphabetically (ignoring case).

### Locating students by detail: `find`

Finds students whose details contain any of the given keywords.

Format: `find KEYWORD [MORE_KEYWORDS]`

* The search is case-insensitive; for example, `hans` matches `Hans`.
* Keyword order does not matter; for example, `Hans Bo` matches `Bo Hans`.
* Enclose a phrase in double quotes to search for words with spaces together; for example, `find "Lab 3"` matches only details containing the full phrase `Lab 3` and excludes records with only separate `Lab` and `3` fragments.
* Multiple terms perform an `OR` search across individual words and quoted phrases; for example, `find Hans Bo` returns `Hans Gruber` and `Bo Yang`, while `find Alice "Lab 3"` returns students matching `Alice` or the phrase `Lab 3`.
* To include a literal double quote or backslash inside a search term, escape it with a backslash: `\"` or `\\`.
* An unclosed quote (for example, leaving off the closing quote in `"Lab 3`) or an empty search term (such as `""` or `"   "`) is refused without changing the current student list.
* Repeated spaces inside a quoted phrase are collapsed to a single space, matching how student details are stored; for example, `find "Lab  3"` matches `Lab 3`.
* The search covers all details of a student: name, NUS Student ID, phone number, email
  address, tags and follow-up status. For example, `find follow-up` can find
  flagged students, and `find a0123` can find a student with NUS Student ID `A01234567X`.
* Parts of details match; for example, `Han` matches `Hans`, and `912` matches the phone number `91234567`.
* TAB reports the number of matches found: `Found 0 students.` when none match,
  `Found 1 student.` for a single match, and `Found 3 students.` for multiple matches.

Examples:
* `find john` returns `john` and `John Doe`
* `find "Lab 3"` returns students whose details contain the exact phrase `Lab 3`
* `find Alice "Lab 3"` returns students whose details contain `Alice` or the phrase `Lab 3`
* `find alex david` returns `Alex Yeoh`, `David Li`<br>
  ![result for 'find alex david'](images/findAlexDavidResult.png)
* `find 91234 friend` returns students whose phone numbers or tags contain `91234` or `friend`
* `find a0123` returns students whose NUS Student IDs contain `A0123`, regardless of case

### Deleting a student: `delete`

Deletes the specified student from the student book.

Format: `delete INDEX`

* Deletes the student at the specified `INDEX`.
* The command result identifies the removed record (`Deleted student: NAME; Phone: PHONE...`)
  so you can verify exactly which student was deleted from the book.
* The index refers to the index number shown in the displayed student list.
* The index **must be a positive integer** 1, 2, 3, ...

Examples:
* `list` followed by `delete 2` deletes the 2nd student in the student book.
* `find Betsy` followed by `delete 1` deletes the 1st student in the results of the `find` command.

### Clearing all entries: `clear`

Asks for confirmation before clearing all entries from the student book.

Format: `clear`

TAB shows a prompt such as `Clear all 6 students? This cannot be undone. [y/N]`.
The count includes students hidden by a search. The records and saved file
stay unchanged until you reply in the command line and press Enter:

* `y` or `yes` deletes all students and saves the empty book. This cannot be undone.
* `n`, `no`, or Enter without a reply cancels without changing any records or the saved file.

Replies ignore case and surrounding whitespace: `Y`, `yEs`, and `yeS` also
confirm; `N` and `nO` also cancel. Cancellation is the `[y/N]` default.
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

TAB automatically saves changes to student records. You do not need to save manually.
Clear prompts, cancellation, and invalid replies leave the saved file unchanged.

### Editing the data file

TAB data is saved automatically as a JSON file `[JAR file location]/data/tab.json`. Advanced users are welcome to update data directly by editing that data file.

A student who needs follow-up has `"flag": true` in their JSON record. An
unflagged student normally has no `flag` key; absence, `false`, and `null` all
mean that no follow-up is needed. Data files created before this field existed
continue to load, with every record that lacks the key treated as unflagged.

A student with an NUS Student ID has a `"nusStudentId"` string such as
`"A01234567X"` in their JSON record. The key is optional: a missing key or
`null` means that no NUS Student ID has been recorded. A non-null value must
contain between 1 and 32 ASCII letters and digits. TAB accepts lowercase letters
and saves valid IDs in uppercase.

TAB still identifies duplicate records by normalized name. NUS Student IDs, phone
numbers, and emails are searchable details rather than identity keys, so two
students with different names may hold the same NUS Student ID. Check such records if
the repeated ID was unintended.

<box type="warning" seamless>

**Caution:**
If your changes make the data file unreadable, TAB shows an error at the next
run instead of opening the main window. The error identifies `data/tab.json`
and, when possible, the affected student record or field. TAB leaves the file
unchanged so that you can repair or replace it, then restart TAB.<br>
Saved email addresses longer than 254 characters are rejected before validation
because their shape can otherwise exhaust Java's stack. This is the same limit
used by commands. We still recommend backing up the file before editing it.
</box>

### Archiving data files `[coming in v2.0]`

_Details coming soon ..._

--------------------------------------------------------------------------------------------------------------------

## FAQ

**Q**: How do I transfer my data to another computer?<br>
**A**: Install TAB on the other computer and overwrite the data file it creates with the data file from your previous TAB home folder.

--------------------------------------------------------------------------------------------------------------------

## Known issues

1. **When using multiple screens**, if you move the application to a secondary screen, and later switch to using only the primary screen, the GUI will open off-screen. The remedy is to delete the `preferences.json` file created by the application before running the application again.
2. **If you minimize the Help Window** and then run the `help` command (or use the `Help` menu, or the keyboard shortcut `F1`) again, the original Help Window will remain minimized, and no new Help Window will appear. The remedy is to manually restore the minimized Help Window.

--------------------------------------------------------------------------------------------------------------------

## Command summary

Action     | Format, Examples
-----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------
**Add**    | `add NAME -p PHONE_NUMBER [-e EMAIL] [--id NUS_STUDENT_ID] [-t TAG]... [--follow-up]` <br> Alternate spellings: `-p`/`--phone`, `-e`/`--email`, `--id`/`-i`, `-t`/`--tag` <br> e.g., `add James Ho --phone 22224444 -i A01234567X --tag friend -t colleague --follow-up`
**Clear**  | `clear`, then `y`/`yes` to confirm; `n`/`no` or an empty reply to cancel (replies ignore case)
**Delete** | `delete INDEX`<br> e.g., `delete 3`
**Edit**   | `edit INDEX [-n NAME] [-p PHONE_NUMBER] [-e EMAIL] [--id NUS_STUDENT_ID] [-t TAG]... [--follow-up \| --no-follow-up]`<br> Alternate value spellings: `-n`/`--name`, `-p`/`--phone`, `-e`/`--email`, `--id`/`-i`, `-t`/`--tag` <br> Follow-up aliases: `-f` sets; `-nf` clears.<br> e.g., `edit 2 --name "James Lee" --id A01234567X`
**Find**   | `find KEYWORD [MORE_KEYWORDS]`<br> e.g., `find James "Lab 3" A0123`
**List**   | `list`
**Help**   | `help`
