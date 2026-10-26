# Keyboard navigation review evidence

These JavaFX scene captures compare PR #545's base (`f1654aea`) with its
implementation (`233edd05`). Both use the real `MainWindow`, 30 synthetic
students with distinct tags, an 820 by 600 pixel scene, Java 25, and bundled
JavaFX 25 on macOS. Window activation is controlled off-screen as in the
navigation tests. No graphical overlays or image edits are applied.

Each sequence starts with focus in the command box and the reply hidden:

| Capture | Keys | Before | After |
| --- | --- | --- | --- |
| Student focus | Tab | Student list has focus without a visible outline | Student list has an accent outline |
| Tag focus | Tab twice | Focus returns to the command box; tags are skipped | Tag list has focus and an accent outline |

The PNG files preserve the full application scene so the surrounding
controls and overflow scrollbars remain visible. These captures demonstrate
focus indicators and traversal; they are not a cross-platform manual test.
