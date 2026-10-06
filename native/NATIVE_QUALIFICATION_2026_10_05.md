# Native Studio qualification — 2026-10-05

Executed on macOS 27 arm64 using the actual compiler at
`azora-lang/compiler/build/bin/macosArm64/debugExecutable/azora.kexe` and Apple
Clang 21.0.0 (`/usr/bin/clang`). No JVM invocation participates in these runs.

Passing:

- Native compiler builds Studio services from `.az` sources and the C OS bridge.
- Apple AddressSanitizer/UndefinedBehaviorSanitizer checked host persistence,
  stale/double/forged handles, symlink/traversal refusal, UTF-8 byte preservation,
  output failure/truncation, process cancellation and explicit destruction.
- Emitted services perform create/edit/undo/redo/save/reopen with zero live
  checked host handles. Structural intelligence verifies exact spans.
- Real native compiler inspect/build/play run through Studio's process boundary;
  valid source emits no semantic diagnostics, invalid source emits the real
  AZ-SYM-0001 diagnostic with exact source and ASCII span/byte coordinates.
- Native template creation reads the actual Engine ECS fixture through confined
  descriptors, writes a project/manifest, then native compiler build/play executes
  it successfully through Studio services. No development Python builder is used.
- Native Engine ECS scene select/edit/undo/save/reopen, invalid-file rollback,
  play/pause/stop and destruction pass under Apple ASan/UBSan. Probe source:
  `src/model-probe.az`; generated binary `build/model-probe/azora-model-probe`.

Open:

- `src/workspace.az` is the actual constructor DSL tree. Headless retained tree
  probe `src/workspace-probe.az` links but initially detects a heap use after free
  in Engine `storageIndexOf`; trace `build/workspace-probe/asan-workspace.log`.
- Graphical Studio typechecks but initially failed LLVM linking with an undefined
  future local `@d2` in guarded GPU initialization defer cleanup. Compiler repair
  is in progress; original working native source window remains visible.
- CUA screenshot confirmed the original native window renders. A Save click was
  not executed because automatic approval review reached a usage limit. No UI
  interaction, richer rendering, frame baseline or graphical shutdown gate is
  claimed complete.
- The selected Homebrew Clang21.1.4 ASan runtime deadlocks before main on this
  macOS host; a one-second process sample isolated reentrant ASan initialization
  through dyld text iteration. Apple Clang runs the same C probe successfully.
- Full editor semantic features, Unicode native text input, installed project/
  package/compiler distribution, arbitrary scene schemas, assets, Room migration,
  plugin ABI and additional supported platforms remain unqualified.

The foundation F07 readiness gate remains open.
