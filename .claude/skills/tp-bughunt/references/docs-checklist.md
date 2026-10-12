# UG and DG checks (PE Part II)

Judge the **rendered HTML** pages (the team's GitHub Pages site), not the `.md`
source; the source is fine for grepping. Only the UG and DG are in scope.

## UG versus product (highest value)

For every command section:

- Run **every example** exactly as printed (copy from the HTML, so any smart
  quotes, en dashes or non-breaking spaces that the page introduced come along —
  a copy-paste that fails because the page rendered `--` as `–` is a real
  DocumentationBug).
- Compare the stated format, prefixes, optional/required markers, and
  constraints (lengths, allowed characters, ranges) with the actual behaviour
  and with `MESSAGE_USAGE` in the code.
- Compare screenshots with the current UI.
- The command summary table: every command present, formats identical to the
  sections, no stale commands from AB3.
- Behaviour the UG does not mention that a user would need (e.g. duplicate
  rules, case sensitivity, what `find` matches) — a missing explanation is a
  DocumentationBug if users cannot learn it from the app's messages.
- Leftover AB3 text: "AddressBook", "persons", `addressbook.json`, the AB3
  logo, AB3 sample names, links to se-edu.
- Quick start: the download link, the folder/launch instructions, the Java
  version. Follow it literally.
- FAQ / known issues / data-file section: the file path it names matches what
  the app writes.

## DG

- Architecture and component diagrams match the code's real class names and
  packages (grep for each class named in a diagram).
- Sequence diagrams: correct UML notation (activation bars, return arrows
  dashed, object naming `:Class`, lifelines ending with an X only when
  destroyed). Notation errors are Low or higher.
- Class diagrams: multiplicities, association vs composition, navigability
  matching the fields in the code.
- Implementation sections for the team's features (not just AB3's undo/redo)
  describe what the code does now.
- "Instructions for manual testing": every listed step works and gives the
  stated result; every user-testable feature changed from AB3 is covered.
- User stories, use cases and NFRs: use cases' main success scenarios match the
  real commands; NFRs are measurable and not contradicted by the product.
- Glossary covers the domain terms the UG and DG use.
- Broken links and anchors (`lychee` or a quick crawl of `href`s), missing
  images, diagrams rendering as raw PlantUML text.

## Severity hints

- Wrong command format or example that fails: Low–Medium (a user following the
  UG is blocked).
- Missing feature documentation: Medium if users cannot discover it.
- Typos, minor grammar: VeryLow (and only a few of them).
- Broken link: Low or Medium, by inconvenience.
- Cosmetic layout: VeryLow unless it hinders reading.
