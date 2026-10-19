---
  layout: default.md
  title: "Logging guide"
---

# Logging guide

* The project uses the `java.util.logging` package for logging.
* `LogsCenter` manages logging levels and destinations.
* A class obtains a `Logger` with `LogsCenter.getLogger(Class)`, which logs messages at the configured level.
* Log messages are written to the console and a `.log` file. If the log file
  cannot be opened, `LogsCenter` warns through its already-configured base
  logger and continues with console logging. File logging is retried on the
  next launch.
* The output logging level can be controlled by changing the `LOG_LEVEL` constant in the `LogsCenter` class.
* **When choosing a level for a log message**, follow the conventions given in [_[se-edu/guides] Java: Logging conventions_](https://se-education.org/guides/conventions/java/logging.html).

## What TAB logs

`LogicManager` writes the command as it was typed, and `StudentBookParser` writes
how it read at `FINE`. Neither says what came of it, so a command that changes or
selects records logs its own effect:

* `add`, `delete` and `edit` at `INFO`, with how many records the book then holds.
* `clear` at `INFO`, with how many it emptied, once the confirmation is given.
* `find` at `FINE`, with how many matched, since it changes only what is on screen.

`list`, `exit` and `help` change nothing worth recording and stay quiet.

An entry counts records and names the index the User saw rather than writing the
student out. The command text in the log already holds whatever the User typed,
and repeating a name or an address in the entries after it puts more of someone's
details in the file for nothing.
