# Studio iOS app — scaffold

Copied from the launcher iosApp as a starting point. Before this builds in Xcode,
wire it up (deferred per "scaffold now, wire later"):

- The Gradle framework task already points at `:studioApp:shared` (StudioShared framework).
- Update the linked framework name from `Shared` to `StudioShared` in the Xcode
  target (Frameworks, Search Paths) and the Swift `import`.
- Update bundle identifier / display name so it doesn't collide with the launcher app.
- ContentView should render `MainViewController()` from `dev.azora.studio.shared`.
