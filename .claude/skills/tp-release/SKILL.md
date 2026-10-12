---
name: tp-release
description: Cut a tP product release (v1.3 onward) for this repo — version bump, shadowJar, JAR smoke test, "What's New vs AB3" release notes, tag, GitHub release, milestone wrap-up. Use when asked to release, publish, tag, build the JAR, write release notes, or close a milestone.
---

# Releasing a TAB version

Deadline discipline matters: from v1.3 the "v1.\_ on time" item on the tP
dashboard turns red **and stays red** if the iteration is not properly wrapped
up by the deadline. Wrap up even if some features slipped — move them, don't
claim them.

## 1. Settle the feature set first

- Feature freeze: every PR for the milestone merged, with UG and DG updated in
  the same PRs.
- Move incomplete work to the next milestone rather than including it in the
  notes.
- Only then bump the version, so the app never claims a version while features
  are still landing.

## 2. Bump the version

`src/main/java/seedu/tab/MainApp.java` holds `VERSION`. Confirm the displayed
version, the UG, the DG and the product website all agree afterwards.

## 3. Build

```sh
./gradlew clean check coverage    # must pass before anything else
./gradlew shadowJar               # produces build/libs/tab.jar
```

Avoid drastic changes to `build.gradle`. Do not add a library just to render
the user guide as a web page.

## 4. Smoke-test the JAR

Not optional — this is where packaging bugs surface that the Gradle run hides.

```sh
mkdir -p /tmp/tab-smoke && cd /tmp/tab-smoke
cp <repo>/build/libs/tab.jar .
java -jar tab.jar
```

- An **empty, writable directory**, under **Java 25**
- Walk the documented manual-test path in the DG's
  *Instructions for manual testing* appendix
- Confirm `data/tab.json` is created, survives exit and relaunch, and that a
  correctly hand-edited file still loads
- Check the window at the supported resolutions and display scalings
- Repeat on each mainstream OS the team covers (Windows, Linux, macOS)
- Terminal output must stay presentable — no stack traces during normal
  operation

## 5. Release notes

An itemised **"What's New vs AB3"** list — compared against AB3, not against
the previous tP version — with screenshots or screen recordings of each change.
Collect those from each feature owner. Fairly detailed: a reader should be able
to tell what they can now do that they could not before.

## 6. Tag and publish

```sh
gh release create v1.3 build/libs/tab.jar \
  -R AY2627S1-CS2103T-T16-1/tp \
  --title "TAB v1.3" --notes-file <path>
```

The release must carry the executable JAR and the notes.

## 7. Wrap up the milestone

- Move unfinished issues to the next milestone
- Close the milestone
- Close the issues the release delivered

## Feature-freeze limits (v1.6)

During the `v1.6` freeze, avoid functional-code changes unless necessary. Per
contributor, `src/main` changes are capped at 15% of their functional code or
100 lines, whichever is higher. Tests, documentation and non-code files are
outside that cap.
