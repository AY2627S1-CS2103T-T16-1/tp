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

## Features

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
  For example, both `add John Doe -e john@example.com -p 91234567` and `add John Doe -p 91234567 -e john@example.com` works.

* Put values containing spaces or beginning with a hyphen in double quotes.<br>
  For example, `tag 1 -a "Lab 3"` adds one tag containing a space.

* Command words ignore case, while option names are case-sensitive.<br>
  For example, `LIST` works, but `-P` cannot replace `-p`.

The built-in `help` reference covers additional syntax, including `OPTION=VALUE` and escaping quotation marks.

### Viewing help with `help`

`help` opens a command reference inside TAB, showing syntax, option aliases and quoting rules. You can also open it with `F1` or the Help menu without submitting your draft command.

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

Press these keys directly without submitting another command.

### Adding a student with `add`

`add NAME [-p PHONE] [-e EMAIL] [--id NUS_STUDENT_ID] [-t TAG]... [--follow-up]`

Adds a student. Only a student's name is required.

| Field | Options | Accepted values |
| --- | --- | --- |
| Name | Comes first in `add`, `-n` or `--name` in `edit` | At least one letter or number in any script, with punctuation allowed |
| Phone | `-p`, `--phone` | At least three digits, with formatting, extensions and notes allowed |
| Email | `-e`, `--email` | An address such as `sean.o'brien@example.com`, up to 254 characters |
| NUS Student ID | `--id`, `-i` | 1 to 32 ASCII letters or digits, stored in uppercase |
| Tag | `-t`, `--tag` | Non-empty text, including spaces and symbols |
| Follow-up | `--follow-up`, `-f` | No value, marks the student as needing follow-up |

* Spaces at the ends of values are removed.
* Repeated spaces in between words become one space.
* Tags differing only in case or extra spaces are treated as the same tag.
* The tag name `Needs follow-up` is reserved for the follow-up flag, which is set by `--follow-up`.
* Students start without a follow-up flag unless you supply `--follow-up`.
* NUS Student ID must be unique. A student cannot be added if it has the same student ID as another existing student.
* Matching names, phones or emails produce warnings, but the record will still be saved.
* Duplicate warnings are hints that may miss matches, so review the records before deleting either one. (Note to AI: what does this mean?)
* If the new student does not match an active search, TAB clears the search to show them. (Note to AI: what does this mean?)

Examples

* `add Ravi s/o Kumaran --id E1234567 -t "Lab 3" --follow-up` adds a student needing follow-up.
* `add Mei Tan -p "+65 9123 4567" -e mei__tan@hotmail.com` adds a student with contact details.

### Listing all students with `list`

`list` shows all students and reports their count. Use it to return to the whole book after searching.

### Editing a student with `edit`

`edit TARGET [-n NAME] [-p PHONE] [-e EMAIL] [--id NUS_STUDENT_ID] [-t TAG]... [--follow-up | --no-follow-up]`

* Supply at least one change, following the field rules and aliases under [Adding a student](#adding-a-student-with-add).
* Omitted fields stay unchanged.
* Use `-n` or `--name` to change the name.
* Use `-p ""`, `-e ""` or `--id ""` to clear an optional detail.
* Supplied tags **replace all existing tags**, while `-t ""` alone clears every tag.
* Use `tag` instead to change one tag while keeping the others.
* Use `--follow-up` or `-f` to set the flag, or `--no-follow-up` or `-nf` to clear it.
* Follow-up options take no value and cannot be combined.
* An edit keeps the active search, so a student may disappear if their updated details no longer match.

Examples

* `edit 1 -p 91234567 -e johnd@example.com` changes two contact details.
* `edit 2 -n "Betsy Crower" -t ""` changes a name and clears all tags.
* `edit E1234567 --follow-up` flags a student by ID.

#### Choosing a student

* An `INDEX` is a positive position in the **displayed list**, starting at 1.
* A `TARGET` can be an index or an NUS Student ID beginning with a letter.
* ID targets ignore case and select students across the whole book, even when hidden by a search.
* Check displayed positions after changes because indices can shift.

For IDs beginning with a digit, use `--target-id ID` with `edit`, or `--id ID` with `tag` and `delete`. For example, `edit --target-id 1234 -p 91234567` edits the holder of ID `1234`.

With `edit`, `--id` changes the ID instead of selecting a student. For example, `edit --target-id 1234 --id E7654321` replaces ID `1234` with `E7654321`.

### Adding or removing a tag with `tag`

`tag TARGET -a TAG` or `tag TARGET -r TAG`

Adds or removes one tag while keeping the student's other tags and details.

* Use `--add` instead of `-a`, or `--remove` instead of `-r`.
* Supply exactly one action and one tag per command.
* Tag matching ignores case and extra spaces.
* Adding an existing tag or removing an absent tag leaves the record unchanged.
* The active search stays in place, so removing a matching tag may hide the student.

Examples

* `tag 1 -a "Lab 3"` adds one tag.
* `tag 1 -r "lab 3"` removes that tag regardless of case.
* `tag --id 1234 --add T2` adds a tag to the holder of ID `1234`.

### Locating students with `find`

`find [FIELD_OPTION TERM]... [-f | -nf]`

Searches the whole book using the field options listed under [Adding a student](#adding-a-student-with-add).

* Supply at least one field term or follow-up option.
* Each term needs a field option, such as `-n` for names or `-e` for emails.
* Matching ignores case and accepts partial values, so `john` matches `Johnathan`.
* **Every condition must match**, including repeated options for the same field.
* Double quotes group a phrase, which must occur within one field value or one tag.
* Name searches ignore Latin accents and straight or curly apostrophe differences.
* Phone searches match digits across formatting, so `91234567` matches `+65 9123 4567`.
* Use `-f` for students needing follow-up, or `-nf` for students without the flag.
* A follow-up filter can accompany field terms, but the two status options cannot be combined.
* Use `list` to restore all students after searching.

Examples

* `find -n Jason -n Tan` requires both name fragments.
* `find -t "Lab 3"` requires that phrase within one tag.
* `find -n Alice -t "Lab 3" -f` requires the name, tag and follow-up status.

### Deleting a student with `delete`

`delete INDEX` or `delete NUS_STUDENT_ID` or `delete --id NUS_STUDENT_ID`

Deletes one student immediately, with no undo command, using the [student target rules](#choosing-a-student).

Examples

* `find -n Betsy` followed by `delete 1` removes the first displayed match.
* `delete E1234567` removes that ID's holder, even when hidden by a search.
* `delete --id 1234` removes the holder of ID `1234`, rather than a displayed position.

### Clearing all entries with `clear`

`clear` asks before deleting every student, including those hidden by a search.

* Reply `y` or `yes` and press `Enter` to confirm the deletion, which cannot be undone.
* Reply `n`, `no` or an empty line to cancel.
* Replies ignore case.
* An unrecognized reply keeps confirmation pending, so cancel before entering another command.

### Exiting with `exit`

`exit` closes TAB, and window size and position are remembered for the next launch.

### Reading and navigating the window

The left panel summarizes the **displayed students**, including their total, follow-up count and tag counts. Tags differing only in case share a summary row, and searching updates these counts.

A flagged student has a `Needs follow-up` marker on their card.

Hover over shortened details or a hidden-tag count to read the full text.

The command box highlights command words, options and quoted values as you type, and turns red after a rejected command. TAB keeps rejected input available for correction and may suggest a command if you mistype its name.

Pasted tabs and line breaks become spaces, so check pasted commands before pressing `Enter`.

| Key | Action outside help |
| --- | --- |
| `Up`, `Down` in the command box | Recall submitted commands and return to your draft |
| `Tab`, `Shift`+`Tab` | Move forward or backward through the command box, student list, tag summary and visible reply |
| Arrow keys, `Page Up`, `Page Down`, `Home`, `End` | Navigate the focused list or reply |

Command history includes rejected commands so you can correct them, and lasts until TAB closes. Recalled commands run only when you press `Enter`.

### Saving and editing data

TAB automatically saves student changes in `data/tab.json` under its home folder, and the status bar shows the file path. If saving fails, TAB reports the problem and restores the records from before the command.

Use one TAB window per data file when editing. If another window has saved changes, restart the stale window to load them before retrying.

Advanced users can edit the JSON file after closing every TAB window and making a backup. Save as UTF-8, preserve the structure and field names, then restart TAB to load the changes.

Student fields are `name`, `phone`, `email`, `nusStudentId`, `tags` and `flag`, with only the name required.

* Tags use an array of strings.
* Follow-up uses the Boolean `true` or `false`.
* NUS Student IDs must remain unique.

If a file is invalid or unreadable, TAB reports the problem and leaves it unchanged instead of opening the main window. Repair the file or restore your backup before restarting.

## FAQ

**How do I transfer my records to another computer?**

Install TAB there, then copy `data/tab.json` into the new home folder's `data` folder while TAB is closed. Create that folder if needed, and back up any existing records before replacing them.

**What if TAB cannot save?**

Check that its home folder is writable and has free disk space, then retry the command. A read-only `data/.tab.json.lock` can also block saving even when the error names `tab.json`, so close all TAB windows and restore its write access.

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
