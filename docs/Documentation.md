---
  layout: default.md
  title: "Documentation guide"
  pageNav: 3
---

# Documentation Guide

* We use [**MarkBind**](https://markbind.org/) to manage documentation.
* The `docs/` folder contains the source files for the documentation website.
* To learn how set it up and maintain the project website, follow the guide [[se-edu/guides] Working with Forked MarkBind sites](https://se-education.org/guides/tutorials/markbind-forked-sites.html) for project documentation.

## Local setup and preview

Install Node.js 22 with npm, Java 25, and Graphviz (including the `dot` command).
From the repository root, run:

```sh
cd docs
npm ci
npm run serve
```

Use `npm run build` for a static build in `docs/_site/`. These commands use the
project's local MarkBind dependency rather than a separately installed global CLI.
The exact CLI version is declared in `docs/package.json` and resolved with its
dependencies in `docs/package-lock.json`. The publishing workflow reads that locked
CLI version instead of maintaining a second version setting. Local previews and
the published site therefore use the same MarkBind release.

To update MarkBind, run `npm install --save-dev --save-exact markbind-cli@VERSION`
in `docs/`, commit both package files, and verify the build and the guides' diagrams
and boxes. Run `npm ci` after pulling a changed lockfile. Do not add a separate
version to `.github/workflows/docs.yml`.

**Style guidance:**

* Follow the [**_Google developer documentation style guide_**](https://developers.google.com/style).
* Also relevant is the [_se-edu/guides **Markdown coding standard**_](https://se-education.org/guides/conventions/markdown.html).


**Converting to PDF**

* See the guide [_se-edu/guides **Saving web documents as PDF files**_](https://se-education.org/guides/tutorials/savingPdf.html).
