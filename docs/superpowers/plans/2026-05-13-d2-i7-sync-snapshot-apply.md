# D2-I7 Sync Snapshot Apply Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make iOS D2-I7 bootstrap/pull apply `profile / notes / collections / favorites` snapshot data immediately after sync, instead of only persisting messages and relying on extra network refreshes.

**Architecture:** Keep the existing sync transport (`SyncRepository` + `SyncCoordinator`) intact and add a narrow snapshot-application layer at the existing UI/cache boundary. `SyncCoordinator` will continue to persist `messages_local`, while `AppDependencies` will forward the `SyncPullResponse` snapshot into existing view models/repositories through explicit apply methods, avoiding a broad repository rewrite.

**Tech Stack:** Swift 5.10, SwiftUI, Combine, XCTest, custom sqlite layer (`TNDatabase`), existing `SyncCoordinator` / `SyncRepository` / view models.

---

### Task 1: Add failing regression tests for sync snapshot application

**Files:**
- Modify: `thunder_note_ios/ThunderNoteTests/SyncCoordinatorPullPersistTests.swift`
- Modify: `thunder_note_ios/ThunderNoteTests/ProfileViewModelTests.swift`
- Modify: `thunder_note_ios/ThunderNoteTests/FlashNoteListViewModelTests.swift`
- Modify: `thunder_note_ios/ThunderNoteTests/CollectionsViewModelTests.swift`
- Modify: `thunder_note_ios/ThunderNoteTests/FavoritesViewModelTests.swift`

- [ ] **Step 1: Write failing sync snapshot tests**

Add tests that assert:

```swift
@MainActor
func test_manualSync_appliesNonMessageSnapshotThroughHook() async {
    let repo = StubSyncRepositoryPull()
    repo.pullResponse = SyncPullResponse(
        profile: UserProfile(nickname: "Alice"),
        notes: [FlashNote(id: -1, inbox: true), FlashNote(id: 7, title: "工作")],
        collections: [Collection(id: 1, name: "项目")],
        favorites: [FavoriteItem(id: 9, messageId: 77, favoritedAt: "2026-05-13T10:00:00")],
        serverTime: "S"
    )

    let hit = SnapshotHookBox()
    let coord = SyncCoordinator(
        syncRepository: repo,
        onPullSucceeded: { response in
            hit.profile = response.profile?.nickname
            hit.noteCount = response.notes.count
            hit.collectionCount = response.collections.count
            hit.favoriteCount = response.favorites.count
        }
    )

    await coord.manualSync()

    XCTAssertEqual(hit.profile, "Alice")
    XCTAssertEqual(hit.noteCount, 2)
    XCTAssertEqual(hit.collectionCount, 1)
    XCTAssertEqual(hit.favoriteCount, 1)
}
```

And view-model-level tests such as:

```swift
@MainActor
func test_applySyncSnapshot_overridesLoadedNotesAndResorts() async {
    let vm = FlashNoteListViewModel(repository: StubFlashNoteRepository(notes: []))
    vm.applySyncSnapshot([
        FlashNote(id: 7, title: "普通", updatedAt: "2026-05-13T10:00:00"),
        FlashNote(id: -1, inbox: true),
    ])
    XCTAssertEqual(vm.notes.map(\.id), [-1, 7])
}
```

- [ ] **Step 2: Run targeted tests and verify they fail**

Run:

```bash
xcodebuild -project ThunderNote.xcodeproj -scheme ThunderNote -destination 'platform=iOS Simulator,name=iPhone 15' -only-testing:ThunderNoteTests/SyncCoordinatorPullPersistTests -only-testing:ThunderNoteTests/ProfileViewModelTests -only-testing:ThunderNoteTests/FlashNoteListViewModelTests -only-testing:ThunderNoteTests/CollectionsViewModelTests -only-testing:ThunderNoteTests/FavoritesViewModelTests test
```

Expected: build/test failure because `applySyncSnapshot` / equivalent snapshot hooks do not exist yet.

---

### Task 2: Implement snapshot apply hooks in existing UI/cache holders

**Files:**
- Modify: `thunder_note_ios/ThunderNote/Features/Profile/ProfileViewModel.swift`
- Modify: `thunder_note_ios/ThunderNote/Features/FlashNote/FlashNoteListViewModel.swift`
- Modify: `thunder_note_ios/ThunderNote/Features/Collections/CollectionsViewModel.swift`
- Modify: `thunder_note_ios/ThunderNote/Features/Favorites/FavoritesViewModel.swift`

- [ ] **Step 1: Add minimal snapshot apply entry points**

Implement focused methods like:

```swift
@MainActor
public func applySyncSnapshot(_ profile: UserProfile) {
    self.profile = profile
}
```

```swift
@MainActor
public func applySyncSnapshot(_ notes: [FlashNote]) {
    self.notes = Self.sort(notes)
    if case .loading = state {
        state = .loaded
    }
}
```

```swift
@MainActor
public func applySyncSnapshot(collections: [Collection]) {
    self.collections = collections.sorted { $0.displayName < $1.displayName }
    recomputeGroups()
    if case .loading = state {
        state = .loaded
    }
}
```

```swift
@MainActor
public func applySyncSnapshot(_ favorites: [FavoriteItem]) {
    items = favorites.sorted { ($0.favoritedAt ?? "") > ($1.favoritedAt ?? "") }
    registry.replaceAll(items.compactMap { $0.messageId })
    if case .loading = state {
        state = .loaded
    }
}
```

- [ ] **Step 2: Preserve existing behavior and avoid unsafe overwrites**

Keep these rules in code:

```swift
// Do not clear transientMessage here.
// Do not issue network requests from applySyncSnapshot.
// Only apply when sync response actually contains data for this slice.
```

That means the methods should be pure local state updates and should not trigger `repository.list()` / `fetchProfile()` inside the apply path.

---

### Task 3: Wire SyncCoordinator snapshot response into AppDependencies

**Files:**
- Modify: `thunder_note_ios/ThunderNote/App/AppDependencies.swift`
- Test: `thunder_note_ios/ThunderNoteTests/SyncCoordinatorPullPersistTests.swift`

- [ ] **Step 1: Replace broad post-sync refresh with snapshot-aware application**

Update the `onPullSucceeded` closure so it applies sync payload first, then only does lightweight follow-up refresh where still necessary:

```swift
onPullSucceeded: { [weak flashNoteListViewModelRef, weak profileViewModel, weak collectionsViewModel, weak favoritesViewModel, weak profileStatsViewModel] response in
    if let profile = response.profile {
        await profileViewModel?.applySyncSnapshot(profile)
    }
    if !response.notes.isEmpty {
        await flashNoteListViewModelRef?.applySyncSnapshot(response.notes)
    }
    if !response.collections.isEmpty {
        await collectionsViewModel?.applySyncSnapshot(collections: response.collections)
    }
    if !response.favorites.isEmpty {
        await favoritesViewModel?.applySyncSnapshot(response.favorites)
    }
    await profileStatsViewModel?.refresh()
}
```

- [ ] **Step 2: Keep message sync path unchanged**

Do not alter the existing message-local persistence path:

```swift
messageRepository.bindConversationsChanged(syncCoordinator.conversationsChangedPublisher)
```

The implementation should continue to rely on `SyncCoordinator.persistPulledMessages(_:)` for `messages_local` and only extend handling for the other four slices.

---

### Task 4: Re-run targeted tests, then broader sync/iOS verification

**Files:**
- Test: `thunder_note_ios/ThunderNoteTests/SyncCoordinatorPullPersistTests.swift`
- Test: `thunder_note_ios/ThunderNoteTests/ProfileViewModelTests.swift`
- Test: `thunder_note_ios/ThunderNoteTests/FlashNoteListViewModelTests.swift`
- Test: `thunder_note_ios/ThunderNoteTests/CollectionsViewModelTests.swift`
- Test: `thunder_note_ios/ThunderNoteTests/FavoritesViewModelTests.swift`
- Test: `thunder_note_ios/ThunderNoteTests/SyncCoordinatorTests.swift`
- Test: `thunder_note_ios/ThunderNoteTests/SyncCoordinatorPendingTests.swift`

- [ ] **Step 1: Re-run the focused tests**

Run:

```bash
xcodebuild -project ThunderNote.xcodeproj -scheme ThunderNote -destination 'platform=iOS Simulator,name=iPhone 15' -only-testing:ThunderNoteTests/SyncCoordinatorPullPersistTests -only-testing:ThunderNoteTests/ProfileViewModelTests -only-testing:ThunderNoteTests/FlashNoteListViewModelTests -only-testing:ThunderNoteTests/CollectionsViewModelTests -only-testing:ThunderNoteTests/FavoritesViewModelTests test
```

Expected: PASS.

- [ ] **Step 2: Run related sync regression tests**

Run:

```bash
xcodebuild -project ThunderNote.xcodeproj -scheme ThunderNote -destination 'platform=iOS Simulator,name=iPhone 15' -only-testing:ThunderNoteTests/SyncRepositoryTests -only-testing:ThunderNoteTests/SyncCoordinatorTests -only-testing:ThunderNoteTests/SyncCoordinatorPendingTests test
```

Expected: PASS.

- [ ] **Step 3: Run diagnostics/build-quality checks**

Run local diagnostics/build checks for modified files, then:

```bash
xcodebuild -project ThunderNote.xcodeproj -scheme ThunderNote -destination 'platform=iOS Simulator,name=iPhone 15' build
```

Expected: BUILD SUCCEEDED.

---

### Task 5: Update planning docs to reflect the new D2-I7 current fact

**Files:**
- Modify: `docs/完整开发计划.md`
- Modify: `docs/开发测试部署经验库.md` (only if a reusable sync pitfall surfaced during implementation)

- [ ] **Step 1: Update D2-I7 wording to reflect the new current state**

Revise the D2-I7 notes so they no longer say that iOS only applies `messages` after pull/bootstrap if this implementation is complete. Keep any remaining gap language focused on what is still truly missing (for example: unified `/api/sync/push` payload assembly or SSE).

- [ ] **Step 2: Verify docs do not over-claim**

Ensure the updated text says exactly what is now true:

```md
- bootstrap / pull 已能把 profile / notes / collections / favorites 快照直接应用到客户端当前状态；messages 继续走 messages_local + conversation refresh。
- push 仍属于最小可用实现，SSE 仍未接入。
```
