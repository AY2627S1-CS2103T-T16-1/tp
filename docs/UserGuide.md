---
  layout: default.md
  title: "User Guide"
  pageNav: 3
---

# TAB User Guide

TAB (Teaching Assistant's Book) is a **desktop application for teaching assistants to manage student records through typed commands**. You can keep contact details, group students with tags and track who needs follow-up in one graphical window.

<!-- Adapted from the inherited AddressBook-Level3 User Guide by SE-EDU. -->
<!-- https://github.com/NUS-CS2103-AY2627-S1/tp/blob/master/docs/UserGuide.md -->
<page-nav-print />

## Quick start

1. Install Java `25`, using an x64 JDK without bundled JavaFX on Windows or Linux.

   On a Mac, use the JDK+FX version in the
   [course setup guide](https://se-education.org/guides/tutorials/javaInstallationMac.html).
   If you choose a plain JDK on Apple Silicon, use an x64 JDK under Rosetta because TAB's bundled JavaFX targets Intel Macs.
   The course's native JDK+FX setup does not need Rosetta.

1. Download `tab.jar` from the [releases page](https://github.com/AY2627S1-CS2103T-T16-1/tp/releases)
   and place it in a writable folder, which will be TAB's home folder.

1. Open a terminal in that folder and run `java -jar tab.jar`.
   Double-clicking `tab.jar` may not work on some systems.
   TAB opens with six sample students when no saved data exists.

   ![TAB showing student records and the student summary](images/Ui.png)

1. Type a command in the command box and press `Enter`.
   Try `list` to show every student, then `help` to read the built-in command reference.

1. Try `add John Doe -p 98765432 -e johnd@example.com --id A05678901K` to add a student.
   You can then use `find -n John` to locate the record.

1. Refer to [Features](#features) for command details or the [Command summary](#command-summary) for a quick reference.

## General reference

### Command format

TAB follows common CLI conventions for options and quoted values, using short options such as `-p` and long options such as `--phone`.

* Words in `UPPER_CASE` are values you supply.<br>
  For example, `add NAME` allows `add John Doe`.

* Square brackets mark optional parts.<br>
  For example, `add John Doe [-p PHONE]` allows `add John Doe`.

* `...` allows repetition.<br>
  For example, `[-t TAG]...` allows `-t T1 -t "Lab 3"`.

* `|` separates alternatives.<br>
  For example, `[--follow-up | --no-follow-up]` allows either option or neither.

* Options can appear in any order after a leading name or target.<br>
  For example, both `add John Doe -e john@example.com -p 91234567` and `add John Doe -p 91234567 -e john@example.com` work.

* Put values containing spaces or beginning with a hyphen in double quotes.<br>
  For example, `tag 1 -a "Lab 3"` adds one tag containing a space.

* Curly (`“”`) and full-width (`＂`) double quotes work like straight double quotes.

* Use a backslash (`\`) to escape a double quote (`\"`) or another backslash (`\\`).<br>
  For example, `add "Baz \"Q\" Lee"` stores the name `Baz "Q" Lee`.

* Separate an option from its quoted value with a space or `=`.<br>
  For example, `-t "Lab 3"` and `-t="Lab 3"` work; `-t"Lab 3"` is rejected as an unknown option.

* Command words ignore case, but option names are case-sensitive.<br>
  For example, `LIST` works, but `-P` does not replace `-p`.

* A dash your editor wrote for a typed hyphen opens an option, and each dash stands for one `-`.<br>
  For example, `add Amy Lim –p 91234567` records the phone number, but one em dash before `phone` reads as `-phone`, which is refused with `Did you mean --phone?`

* A dash within a value stays part of it, and quoting keeps one at the start.<br>
  For example, `add Jean–Luc Picard` keeps the dash in the name, and `add "–Mystery"` names a student `–Mystery`.

* Give each option once unless the format shows `...`. Short and long spellings count as the same option.

Options also accept `OPTION=VALUE`, such as `--phone=91234567`. TAB suggests corrections for mistyped commands, and for options given with the wrong case or the wrong number of dashes, but you must correct and resubmit them.

### Student fields

The field options below are used by `add`, `edit` and `find`.

| Field | Options | Accepted values |
| --- | --- | --- |
| Name | `add`: first argument, no flag needed<br> `edit`, `find`: `-n` or `--name` | At least one letter or number in any script, with punctuation allowed |
| Phone | `-p`, `--phone` | At least three digits (formatting, extensions and notes allowed) |
| Email | `-e`, `--email` | Up to 254 characters |
| NUS Student ID | `--id`, `-i` | 1 to 32 ASCII letters or digits, stored in uppercase |
| Tag | `-t`, `--tag` | At least one letter or number in any script |
| Follow-up | `--follow-up`, `-f`<br> `--no-follow-up`, `-nf` | Accepts no value |

#### Additional field rules

##### General

Spaces at the ends of values are removed, and repeated spaces become one space.

##### Email

TAB accepts the following subset of standard email formats:

* Before `@`, use ASCII letters, digits and <code>!#$%&amp;'*+/=?^_&#96;{|}~-</code>. Dots are allowed between these characters, but not at either end or consecutively; for example, `sean.o'brien@example.com`.
* After `@`, use ASCII letters and digits, with hyphens inside each dot-separated part. The last part needs at least two adjacent letters or digits.
* An address without a dot after `@`, such as `john@gmail`, is saved because [RFC 5322's address syntax](https://www.rfc-editor.org/rfc/rfc5322.html#section-3.4.1) allows single-part domains. TAB warns in case an ending such as `.com` is missing.

##### Tags

* Spaces, punctuation and symbols are allowed, such as `Lab 3 (Mon)` or `#urgent`.
* Tags differing only in case or extra spaces are treated as the same tag.
* The tag `Needs follow-up` is reserved for the follow-up flag, as are its spacing, hyphen and trailing-punctuation variants such as `Needs follow up`. A tag differing by other punctuation, such as `Needs_follow_up`, is saved with a warning instead and does not set the flag.

##### NUS Student IDs

IDs must be unique across the book, ignoring case.

##### Duplicate warnings

After `add` or `edit`, TAB may warn about another student with a matching name, phone or email. The record is still saved. These warnings may miss duplicates; ignore a warning if you know the records describe different students.

### Choosing a student

`edit`, `tag` and `delete` accept one student target. This can be specified by the displayed index or NUS Student ID.

| Target | Meaning | Example |
| --- | --- | --- |
| Displayed index | Position in the **currently displayed list**, starting at 1 | `edit 1 -p 91234567` |
| NUS Student ID | ID beginning with a letter, matched across the whole book regardless of case | `edit A01234567X -p 91234567` |
| Explicit ID option for `edit` | `--target-id` selects any ID, including one beginning with a digit | `edit --target-id 1234 -p 91234567` |
| Explicit ID option for `tag` and `delete` | `--id` or `-i` selects any ID, including one beginning with a digit | `tag --id 1234 -a T2` or `delete --id 1234` |

* For example: after `find -n Charlotte`, Charlotte is displayed as student `1`, so `delete 1` would delete Charlotte. Subsequently, running `list` will then show the whole book, where index `1` may refer to someone else.

* Selecting a student by ID requires the complete ID. <br>
  For example `A0123` cannot target the student with ID `A01234567X`.

* A target that does not begin with a letter is always read as an index. To target an ID beginning with a digit, use the explicit option above. <br>
  For example, `delete 1234` deletes the student at index 1234, not the one whose ID is `1234`.

* For `edit`, as `--id` is used to edit the student's ID, `--target-id` is used for explicitly selecting the student by NUS Student ID (including one beginning with a digit) instead.

For example:

1. Run `add Numeric Example --id 1234` to add a student with ID `1234`.
1. Run `edit --target-id 1234 -p 91234567` to update the student.
1. Run `tag --id 1234 -a T2` to add a tag to the same student.
1. Run `edit --target-id 1234 --id E7654321` to change their ID.
1. Run `delete --id E7654321` to remove this example student.

**TIP**: `delete`, `edit` and `tag` keep the currently active search and filter, so students may disappear when they no longer match. To avoid acting on the wrong student, you may want to check the displayed indices after each change, or use NUS Student ID as the target.

## Features

### Viewing help with `help`

Opens a reference for the commands inside TAB. Shows syntax, option aliases and quoting rules.

**Format** `help`

You can also open it with `F1` or by clicking the Help menu.

![TAB help showing current command syntax and navigation keys](images/helpMessage.png)

The footer shows your reading progress and navigation keys, including `q` or `Esc` to return to commands.

| Key | Action in help |
| --- | --- |
| `Space`, `f`, `Page Down` | Move forward one screen |
| `b`, `Page Up` | Move backward one screen |
| `Enter`, `j`, `Down` | Move down one line |
| `k`, `Up` | Move up one line |
| `d`, `u` | Move forward or backward half a screen |
| `g`, `Home` / `G`, `End` | Go to the beginning or end |
| `c` | Copy the User Guide link |
| `q`, `Esc` | Return to commands |

### Adding a student with `add`

Adds a student to the student book.

**Format** `add NAME [-p PHONE] [-e EMAIL] [--id NUS_STUDENT_ID] [-t TAG]... [--follow-up]`

* Only the name is required.
* Students start without a follow-up flag unless you supply `--follow-up`.
* If a search would hide the new student, TAB shows all students so the addition is visible.

**Examples**

* `add Ravi s/o Kumaran --id E1234567 -t "Lab 3" --follow-up` adds a student needing follow-up.
* `add Mei Tan -p "+65 9123 4567" -e mei__tan@hotmail.com` adds a student with contact details.

### Listing all students with `list`

Shows a list of all students and reports their count.

**Format** `list`

### Editing a student with `edit`

Edits an existing student selected by [displayed index or NUS Student ID](#choosing-a-student).

**Format** `edit TARGET [-n NAME] [-p PHONE] [-e EMAIL] [--id NUS_STUDENT_ID] [-t TAG]... [--follow-up | --no-follow-up]`

* Supply at least one change, using the [student field rules](#student-fields).
* Omitted fields stay unchanged.
* Use `-p ""`, `-e ""` or `--id ""` to clear an optional detail. A value holding only spaces, of any kind, clears it too.
* Supplied tags **replace all existing tags**. Using `-t ""` alone will also clear every tag. To change one tag while keeping the others, consider using the `tag` command instead.
* Use `--follow-up` or `-f` to set the flag, or `--no-follow-up` or `-nf` to clear it.

**Examples**

* `edit 1 -p 91234567 -e johnd@example.com` changes two contact details.
* `edit 2 -n "Betsy Crower" -t ""` changes a name and clears all tags.
* `edit A01234567X --follow-up` flags Alex Yeoh in the sample data.

### Adding or removing a tag with `tag`

Adds or removes **one** tag for an existing student selected by [displayed index or NUS Student ID](#choosing-a-student).

**Format** `tag TARGET -a TAG` or `tag TARGET -r TAG`

* You may use `--add` in place of `-a`, or `--remove` in place of `-r`.
* Other tags and details remain unchanged.

**Examples**

* `tag 1 -a "Lab 3"` adds one tag.
* `tag 1 -r "lab 3"` removes that tag regardless of case.
* `tag --id A02345678G --add T2` adds a tag to Charlotte Oliveiro in the sample data.

### Locating students with `find`

Searches the whole book using the [student field options](#student-fields).

**Format** `find [FIELD_OPTION TERM]... [--follow-up | --no-follow-up]`

* Supply at least one field term or follow-up option.
* Each term needs a field option, such as `-n` for names or `-e` for emails.
* The matching ignores case and accepts partial values. <br>
  For example, `john` matches `Johnathan`. <br>
  For tags, `find -t T1` also lists students tagged `T10`, `T11`, or `T12`, so this search cannot list only students tagged `T1`.
* **Every condition must match**, including repeated options for the same field (an `AND` search).
* To search an entire phrase, use double quotes.
* Name searches ignore Latin accents, strokes and apostrophe styles (but tags and other fields do not). <br>
  For example, `dang` matches `Đặng` in a name search.
* Name searches treat hyphens and the longer dashes as spaces, because the same name may be punctuated differently from one roster to the next. Other fields, such as tags, do not. <br>
  For example, `find -n "Mary Jane"` matches the names `Mary-Jane` and `Mary–Jane`, but `find -t "Mary Jane"` does not match the tag `Mary-Jane`.
* Phone searches match digits across formatting. <br>
  For example, `91234567` matches `+65 9123 4567`.
* Use `--follow-up` for students with the follow-up flag, or `--no-follow-up` for students without it. The two cannot be used together.

**Examples**

* `find -n Jason -n Tan` requires both name fragments, regardless of their order.
* `find -t "Lab 3"` requires that phrase within one tag.
* `find -n Alice -t "Lab 3" -f` requires the name, tag and follow-up status.

### Deleting a student with `delete`

Deletes one student immediately with no undo command. The student to delete is selected by [displayed index or NUS Student ID](#choosing-a-student).

**Format** `delete TARGET`

**Examples**

* `find -n Charlotte` followed by `delete 1` removes the first displayed match.
* `delete --id A02345678G` removes Charlotte Oliveiro in the sample data, even when hidden by a search.

### Clearing all entries with `clear`

Deletes every student record, including those not currently displayed.

**Format** `clear`

`clear` asks for confirmation because deletion cannot be undone.

* Reply `y` or `yes` and press `Enter` to confirm.
* Reply `n`, `no` or an empty line to cancel.
* Replies ignore case.
* An unrecognized reply keeps confirmation pending, so cancel before entering another command.

### Exiting with `exit`

Closes TAB.

**Format** `exit`

* Current window size and position are remembered for the next launch.

### Reading and navigating the window

![Student summary beside cards showing a follow-up flag and hidden tags](images/cardSummary.png)

In this example, the student summary counts three students and one follow-up flag. The first card shows the follow-up marker beside its name, while `2 more...` indicates hidden tags.

* The **student summary** on the left shows the displayed students' total, follow-up count and tag counts.
* Tags differing only in case share a summary row.
* A flagged student has a `Needs follow-up` marker on their card.
* Hover over shortened details or a hidden-tag count to read the full text.

| Key | Action outside help |
| --- | --- |
| `Up`, `Down` in the command box | Access previously submitted commands |
| `Tab`, `Shift`+`Tab` | Move forward or backward through the command box, student list, tag list in the student summary and visible reply |
| Arrow keys, `Page Up`, `Page Down`, `Home`, `End` | Navigate the focused list or reply |

Command history lasts until TAB closes.

### Saving and editing data

TAB automatically saves student changes in `data/tab.json` under its home folder. The data file's path is shown in the status bar.

If a save fails, the records from before the command are restored.

Use one TAB window per data file when editing. If another window has saved changes, restart the older window to load them before retrying.

Advanced users can edit the JSON file after closing every TAB window and making a backup. Save as UTF-8 and preserve the existing structure and field names.

The file contains a `students` array. Each student uses these fields:

| JSON fields | Value type |
| --- | --- |
| `name`, `phone`, `email`, `nusStudentId` | Text strings following the [student field rules](#student-fields); only `name` is required |
| `tags` | Array of strings, such as `["Lab 3", "T1"]` |
| `flag` | Boolean `true` or `false`; use this for follow-up instead of a `Needs follow-up` tag |

You may add an optional file description outside the `students` array, such as `"_comment": "Tutorial T16"`. TAB keeps it when saving.

If a file is invalid or unreadable, TAB reports the problem and leaves it unchanged without opening the main window. Repair the file before restarting.

## FAQ

**How do I transfer my records to another computer?**

Install TAB there, then copy `data/tab.json` into the new home folder's `data` folder while TAB is closed. Create that folder if needed, and back up any existing records before replacing them.

**What if TAB cannot save?**

Check that TAB's home folder is writable and has free disk space, then retry the command.

TAB uses a lock file, `data/.tab.json.lock`, to coordinate saves between windows. When an error names it, close all TAB windows and restore write access to that file before restarting. Leave the file in place.

## Command summary

For `TARGET`, see [Choosing a student](#choosing-a-student).

| Action | Format or example |
| --- | --- |
| Add | `add NAME [-p PHONE] [-e EMAIL] [--id NUS_STUDENT_ID] [-t TAG]... [--follow-up]` |
| List | `list` |
| Edit | `edit TARGET [-n NAME] [-p PHONE] [-e EMAIL] [--id NUS_STUDENT_ID] [-t TAG]... [--follow-up \| --no-follow-up]` |
| Add a tag | `tag TARGET -a TAG` |
| Remove a tag | `tag TARGET -r TAG` |
| Find | `find [FIELD_OPTION TERM]... [--follow-up \| --no-follow-up]`, such as `find -n Alex -t T1` |
| Delete | `delete TARGET` |
| Clear | `clear`, then confirm or cancel |
| Help | `help`, then `q` or `Esc` to return |
| Exit | `exit` |
