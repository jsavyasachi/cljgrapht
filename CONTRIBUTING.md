# Contributing to cljgrapht

We accept bug reports, fixes, and focused feature contributions.

## Before you start

- **Open an issue first** for a change that is more than a small fix. This lets
  us agree on the approach before you do the work.
- Read the open issues and pull requests. This prevents duplicate work.

## Development

This is a Clojure library. You need a JDK and [Leiningen](https://leiningen.org/).
Projects that use `deps.edn` use the Clojure CLI instead: see the README.

```bash
lein test     # run the test suite
lein check    # AOT-compile; must be free of reflection warnings
```

Requirements for a change that we can merge:

- **Tests first.** Add or update the tests for the behavior that you change. For
  a bug fix, add a regression test. It must fail before your fix and pass after
  it.
- **Build passes.** `lein test` passes and `lein check` reports **zero**
  reflection warnings.
- **One change.** Keep each pull request to one logical change.

## Commits and pull requests

- Follow [Conventional Commits](https://www.conventionalcommits.org/)
  (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:` …).
- Keep the subject in the imperative mood. Limit it to about 72 characters.
- Update `CHANGES.md` / `CHANGELOG.md` when your change is user-visible.
- Rebase on the latest `main` before opening the pull request.

## License

When you contribute, you agree that your contribution has the same license as
this project (see `LICENSE` / the README).
