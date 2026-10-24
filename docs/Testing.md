---
  layout: default.md
  title: "Testing guide"
  pageNav: 3
---

# Testing guide

<!-- * Table of Contents -->
<page-nav-print />

<!-- -------------------------------------------------------------------------------------------------------------------- -->

## Running tests

Use an **x64 JDK 25 without bundled JavaFX** for both the IntelliJ project SDK and
Gradle JVM to test the JavaFX 17 baseline. The three primary CI jobs use Zulu
JDK 25 with the `jdk` package, rather than `jdk+fx`.
Gradle supplies the supported JavaFX version, **17.0.7**, from `build.gradle`.
A JDK that bundles JavaFX can override these dependencies and produce different
UI behavior.

The JavaFX native libraries bundled by this project target x64. On Apple
Silicon, use the x64 JDK under Rosetta rather than an ARM64 JDK; CI explicitly
selects x64 for its JavaFX 17 jobs even when macOS uses Apple Silicon.

CI also tests the [course's Mac setup](https://se-education.org/guides/tutorials/javaInstallationMac.html)
in a separate job: Zulu JDK+FX **25.0.3**, with its bundled JavaFX **25**, on
native ARM64 macOS. To reproduce that job, follow the linked installation
guide, select that JDK as both the IntelliJ SDK and Gradle JVM, and run the
same Gradle command below. This setup does not need Rosetta.

UI tests allow up to 60 seconds for work on the JavaFX thread. Cold window
initialization under Rosetta can exceed ten seconds on hosted macOS runners;
these tests verify behavior rather than enforce a startup performance target.
Once a window is shown, the oversized-window test retries its geometry checks
for up to ten seconds and reports the failing edge with actual bounds.
CI limits the complete Gradle build step to fifteen minutes to catch a
stalled native toolkit while allowing cold downloads and slower runners.
Failed tests print full exception traces without requiring Gradle's verbose
`--info` logging.

To reproduce CI locally, set `JAVA_HOME` to this JDK and put its `bin` directory
first on `PATH`, then run `./gradlew clean check coverage` (Windows:
`gradlew clean check coverage`). On Linux, use a graphical session or run under
a virtual display, for example `xvfb-run -a ./gradlew clean check coverage`.

You can run tests in two ways.

* **Method 1: Using IntelliJ JUnit test runner**
  * To run all tests, right-click on the `src/test/java` folder and choose `Run 'All Tests'`
  * To run a subset of tests, you can right-click on a test package,
    test class, or a test and choose `Run 'ABC'`
* **Method 2: Using Gradle**
  * Open a console and run the command `gradlew clean test` (Mac/Linux: `./gradlew clean test`)

<box type="info" seamless>

**Link**: Read [this Gradle Tutorial from the se-edu/guides](https://se-education.org/guides/tutorials/gradle.html) to learn more about using Gradle.
</box>

--------------------------------------------------------------------------------------------------------------------

## Types of tests

This project has three types of tests:

1. *Unit tests* target the lowest-level methods and classes.<br>
   For example: `seedu.tab.commons.util.StringUtilTest`
1. *Integration tests* check how multiple code units work together; the individual units are assumed to work.<br>
   For example: `seedu.tab.storage.StorageManagerTest`
1. *Hybrid tests* combine unit and integration testing. These tests check both the individual units and how they work together.<br>
   For example: `seedu.tab.logic.LogicManagerTest`
