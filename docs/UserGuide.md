---
  layout: default.md
  title: "User Guide"
  pageNav: 3
---

# TAB User Guide

TAB is a **desktop application for teaching assistants to manage student records through typed commands**. You can keep contact details, group students with tags and track who needs follow-up, while viewing everything in a graphical window.

<!-- Adapted from the inherited AddressBook-Level3 User Guide by SE-EDU. -->
<!-- https://github.com/NUS-CS2103-AY2627-S1/tp/blob/master/docs/UserGuide.md -->
<page-nav-print />

## Quick start

1. Install Java `25` or later, using an x64 JDK without bundled JavaFX on Windows or Linux.
   On a Mac, use the JDK+FX version in the
   [course setup guide](https://se-education.org/guides/tutorials/javaInstallationMac.html).

1. Download `tab.jar` from the [releases page](https://github.com/AY2627S1-CS2103T-T16-1/tp/releases)
   and place it in a writable folder, which will be TAB's home folder.

1. Open a terminal in that folder and run `java -jar tab.jar`.
   TAB opens with six sample students when no saved data exists.

   ![TAB showing student records and the cohort summary](images/Ui.png)

1. Type a command in the command box and press `Enter`.
   Try `list` to show every student, then `help` to read the built-in command reference.

1. Try `add John Doe -p 98765432 -e johnd@example.com --id A05678901K` to add a student.
   You can then use `find -n John` to locate the record.

1. Refer to [Features](#features) for command details or the [Command summary](#command-summary) for a quick reference.

If you use a plain JDK on an Apple Silicon Mac, choose an x64 JDK and run it under Rosetta because TAB's bundled JavaFX libraries target Intel Macs. The course's native JDK+FX setup does not need Rosetta.

## General reference

### Command format

TAB follows common CLI conventions for options and quoted values, using short options such as `-p` and long options such as `--phone`.

* Words in `UPPER_CASE` are values you supply.<br>
  For example, `add NAME` allows `add John Doe`.

* Square brackets mark optional parts.<br>
  For example, `add John Doe [-p PHONE]` allows `add John Doe`.

* `...` allows repetition.<br>
  For example, `[-t TAG]...` allows `-t T1 -t Lab3`.

* `|` separates alternatives.<br>
  For example, `[--follow-up | --no-follow-up]` allows either option or neither.

* Options can appear in any order after a leading name or target.<br>
  For example, both `add John Doe -e john@example.com -p 91234567` and `add John Doe -p 91234567 -e john@example.com` work.

* Put values containing spaces or beginning with a hyphen in double quotes.<br>
  For example, `tag 1 -a "Lab 3"` adds one tag containing a space.

* Command words ignore case, while option names are case-sensitive.<br>
  For example, `LIST` works, but `-P` cannot replace `-p`.

The built-in `help` reference covers additional syntax, including `OPTION=VALUE` and escaping quotation marks. TAB suggests corrections for mistyped commands or capitalized options, but you must correct and resubmit them.

### Student fields

The field options below are used by `add`, `edit` and `find`.

| Field | Options | Accepted values |
| --- | --- | --- |
| Name | `add`: first argument, no flag needed<br> `edit`, `find`: `-n` or `--name` | At least one letter or number in any script, with punctuation allowed |
| Phone | `-p`, `--phone` | At least three digits, with formatting, extensions and notes allowed |
| Email | `-e`, `--email` | ASCII address such as `sean.o'brien@example.com`, up to 254 characters |
| NUS Student ID | `--id`, `-i` | 1 to 32 ASCII letters or digits, stored in uppercase |
| Tag | `-t`, `--tag` | Non-empty text, including spaces and symbols |
| Follow-up | `--follow-up`, `-f` | No value, marks the student as needing follow-up |

* Spaces at the ends of values are removed, and repeated spaces become one space.
* Tags differing only in case or extra spaces are treated as the same tag.
* `Needs follow-up` is reserved for the flag, while similar tags such as `Needs follow up` are saved with a warning and do not set the flag.
* NUS Student IDs must be unique across the book, ignoring case.
* Matching names, phones or emails produce possible-duplicate warnings after saving an addition or edit.
* Users are encouraged to exercise their own discretion regarding duplicate warnings. You can ignore them if you know that the records describe different students.

### Choosing a student

`edit`, `tag` and `delete` accept one student target. This can be specified by the displayed index or NUS student ID.

| Target | Meaning | Example |
| --- | --- | --- |
| Displayed index | Position in the **currently displayed list**, starting at 1 | `edit 1 -p 91234567` |
| NUS Student ID | ID beginning with a letter, matched across the whole book regardless of case | `edit A01234567X -p 91234567` |
| Explicit ID option | Any ID, including one beginning with a digit | `edit --target-id A01234567X -p 91234567` |

* Targeting by index uses currently displayed index. <br>
  For example: after `find -n Charlotte`, Charlotte is displayed as student `1`, so `delete 1` would delete Charlotte. Subsequently, running `list` will then show the whole book, where index `1` may refer to someone else.

* Selecting a student by ID requires the complete ID. <br>
  For example `A0123` cannot target the student with ID `A01234567X`.

* Use `--target-id ID` with `edit` to select an ID explicitly, including one beginning with a digit.

* With `edit`, `--id` changes the student's ID, so `--target-id` is used for selecting students instead.

For example, first run `add Numeric Example --id 1234`, then `edit --target-id 1234 -p 91234567` to update that student. To change their ID, run `edit --target-id 1234 --id E7654321`.

**TIP**: `delete`, `edit` and `tag` keep the currently active search and filter, so students may disappear when they no longer match. To prevent errors, you may want to check the displayed indices after each change, or use NUS Student ID as the target to avoid selecting a different student.

## Features

### Viewing help with `help`

Opens a command reference inside TAB, showing syntax, option aliases and quoting rules.

Format: `help`

You can also open it with `F1` or by clicking the Help menu.

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
* If a search hides the new student, TAB shows all students so you can see the addition.

Examples

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
* Use `-p ""`, `-e ""` or `--id ""` to clear an optional detail.
* Supplied tags **replace all existing tags**. Using `-t ""` alone will also clear every tag. To change one tag while keeping the others, consider using the `tag` command instead.
* Use `--follow-up` or `-f` to set the flag, or `--no-follow-up` or `-nf` to clear it.

Examples

* `edit 1 -p 91234567 -e johnd@example.com` changes two contact details.
* `edit 2 -n "Betsy Crower" -t ""` changes a name and clears all tags.
* `edit A01234567X --follow-up` flags Alex Yeoh in the sample data.

### Adding or removing a tag with `tag`

Adds or removes **one** tag for an existing student selected by [displayed index or NUS Student ID](#choosing-a-student).

**Format** `tag TARGET -a TAG` or `tag TARGET -r TAG`

* You may use `--add` in place of `-a`, or `--remove` in place of `-r`.
* Other tags and details will remain unchanged.

Examples

* `tag 1 -a "Lab 3"` adds one tag.
* `tag 1 -r "lab 3"` removes that tag regardless of case.
* `tag --id A02345678G --add T2` adds a tag to Charlotte Oliveiro in the sample data.

### Locating students with `find`

Searches the whole book using the [student field options](#student-fields).

**Format** `find [FIELD_OPTION TERM]... [-f | -nf]`

* Supply at least one field term or follow-up option.
* Each term needs a field option, such as `-n` for names or `-e` for emails.
* The matching ignores case and accepts partial values <br>
  For example,  `john` matches `Johnathan`.
* **Every condition must match**, including repeated options for the same field (an `AND` search).
* To search an entire phrase, use double quotes.
* Name searches ignore Latin accents, strokes and apostrophe styles. <br>
  For example, `dang` matches `Đặng`.
* Name searches treat hyphens as spaces, but other fields do not. <br>
  For example, `find -n "Mary Jane"` matches the name `Mary-Jane`, but `find -t "Mary Jane"` does not match the tag `Mary-Jane`.
* Phone searches match digits across formatting. <br>
  For example, `91234567` matches `+65 9123 4567`.
* Use `-f` for students with the follow-up flag, or `-nf` for students without it. `-f` and `-nf` cannot be used together.

Examples

* `find -n Jason -n Tan` requires both name fragments, regardless of their order.
* `find -t "Lab 3"` requires that phrase within one tag.
* `find -n Alice -t "Lab 3" -f` requires the name, tag and follow-up status.

### Deleting a student with `delete`

Deletes one student immediately, with no undo command. The student to delete is selected by [displayed index or NUS Student ID](#choosing-a-student).

**Format** `delete INDEX` or `delete NUS_STUDENT_ID` or `delete --id NUS_STUDENT_ID`

Examples

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

* The left panel summarizes the **displayed students**, including their total, follow-up count and tag counts.
* Tags differing only in case share a summary row.
* A flagged student has a `Needs follow-up` marker on their card.
* Hover over shortened details or a hidden-tag count to read the full text.

| Key | Action outside help |
| --- | --- |
| `Up`, `Down` in the command box | Access previously submitted commands |
| `Tab`, `Shift`+`Tab` | Move forward or backward through the command box, student list, tag summary and visible reply |
| Arrow keys, `Page Up`, `Page Down`, `Home`, `End` | Navigate the focused list or reply |

Command history lasts until TAB closes.

### Saving and editing data

TAB automatically saves student changes in `data/tab.json` under its home folder. The status bar shows the data file's path, and a failed save restores the records from before the command.

Use one TAB window per data file when editing. If another window has saved changes, restart the older window to load them before retrying.

Advanced users can edit the JSON file after closing every TAB window and making a backup. Save as UTF-8 and preserve the existing structure and field names.

| JSON fields | Value type |
| --- | --- |
| `name`, `phone`, `email`, `nusStudentId` | Text strings following the [student field rules](#student-fields) |
| `tags` | Array of strings, such as `["Lab 3", "T1"]` |
| `flag` | Boolean `true` or `false` |

If a file is invalid or unreadable, TAB reports the problem and leaves it unchanged without opening the main window. Repair the file before restarting.

## FAQ

**How do I transfer my records to another computer?**

Install TAB there, then copy `data/tab.json` into the new home folder's `data` folder while TAB is closed. Create that folder if needed, and back up any existing records before replacing them.

**What if TAB cannot save?**

Check that TAB's home folder is writable and has free disk space, then retry the command.

If the error names `data/.tab.json.lock`, close all TAB windows and restore write access to that file before restarting. TAB uses it to coordinate saves between windows, so leave it in place.

## Command summary

| Action | Format or example |
| --- | --- |
| Add | `add NAME [-p PHONE] [-e EMAIL] [--id ID] [-t TAG]... [--follow-up]` |
| List | `list` |
| Edit | `edit TARGET [-n NAME] [-p PHONE] [-e EMAIL] [--id ID] [-t TAG]... [--follow-up \| --no-follow-up]` |
| Add a tag | `tag TARGET -a TAG` |
| Remove a tag | `tag TARGET -r TAG` |
| Find | `find [FIELD_OPTION TERM]... [-f \| -nf]`, such as `find -n Alex -t T1` |
| Delete | `delete TARGET`, with `--id ID` for digit-leading IDs |
| Clear | `clear`, then confirm or cancel |
| Help | `help`, then `q` or `Esc` to return |
| Exit | `exit` |
