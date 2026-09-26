# Implementation Plan - Fix Stability, CRUD, and Navigation Issues

This plan addresses the critical issues reported: native crashes, duplicate packets, navigation resets during deletion, and state consistency.

## Proposed Changes

### 1. Stability & Navigation Fixes
- **Investigate SIGABRT**: Ensure `ListDetailPaneScaffoldNavigator` is used correctly. Use a proper type instead of `Nothing` if possible, although `Nothing` is often used when no extra state is needed.
- **Navigation Invariants**: Refine `MainActivity` navigation logic to be more robust. Avoid `clear()` if possible, or ensure it's safe.
- **Edge-to-Edge & Lifecycle**: Ensure `dropUnlessResumed` is used consistently for all navigation actions.

### 2. CRUD Bug Fixes
- **Duplicate Packets**: In `MainViewModel`, add checks to prevent adding duplicate nodes, packets, or tags with the same name under the same parent.
- **Idempotency**: Use `OnConflictStrategy.IGNORE` or explicit checks to ensure operations don't create unexpected duplicates if triggered multiple times.

### 3. Navigation & State Consistency
- **Selection Handling**: Update `MainViewModel` to clear `selectedItem` when the corresponding entity is deleted.
- **UI State**: Ensure `ConfigureScreen` reacts gracefully to `selectedItem` becoming null.
- **Thread Safety**: Ensure all database operations in `MainViewModel` are sequential or properly synchronized if necessary (though `viewModelScope` handles most of this).

## Proposed Changes

### [MainViewModel](file:///C:/Users/bocke/AndroidStudioProjects/AiListTest/app/src/main/java/com/example/ailisttest/ui/MainViewModel.kt)
#### [MODIFY]
- Update `addNode`, `addPacket`, `addTag` to check for existing items with the same name.
- Update `deleteNode`, `deletePacket`, `deleteTag` to clear `selectedItem` if it matches the deleted item.

### [ConfigureScreen](file:///C:/Users/bocke/AndroidStudioProjects/AiListTest/app/src/main/java/com/example/ailisttest/ui/screens/ConfigureScreen.kt)
#### [MODIFY]
- Handle potential race conditions or state resets during deletion.
- Ensure the navigator navigates back to the list if the selected item is deleted.

### [DAOs](file:///C:/Users/bocke/AndroidStudioProjects/AiListTest/app/src/main/java/com/example/ailisttest/data/local/PacketDao.kt)
#### [MODIFY]
- Change `OnConflictStrategy.REPLACE` to `OnConflictStrategy.ABORT` or `IGNORE` where appropriate to prevent accidental overrides, and handle conflicts in the ViewModel/Repository.

## Verification Plan
### Automated Tests
- Run existing unit tests (if any) and add new ones for CRUD idempotency.
- `./gradlew :app:testDebugUnitTest`

### Manual Verification
- Manually trigger rapid "Add" clicks to verify no duplicates.
- Delete the currently selected item and verify the UI transitions back to the list pane without crashing or resetting unintendedly.
- Check logs for any native crashes.
