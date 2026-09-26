# Implementation Plan - Setup Navigation and ViewModel

Implement Navigation 3 state-driven logic and `MainViewModel` for hierarchical data management and Room interaction.

## Proposed Changes

### [Data Layer]

#### [NEW] [MainRepository](file:///C:/Users/bocke/AndroidStudioProjects/AiListTest/app/src/main/java/com/example/ailisttest/data/MainRepository.kt)
Create a repository to abstract Room DAO operations and provide a unified interface for the ViewModel.

### [UI Layer - ViewModel]

#### [NEW] [MainViewModel](file:///C:/Users/bocke/AndroidStudioProjects/AiListTest/app/src/main/java/com/example/ailisttest/ui/MainViewModel.kt)
- Expose hierarchical state using `StateFlow`.
- Implement CRUD operations for Nodes, Packets, and Tags.
- Manage "Selected Item" state for the Configure screen's split view.

### [UI Layer - Navigation]

#### [NEW] [NavKey](file:///C:/Users/bocke/AndroidStudioProjects/AiListTest/app/src/main/java/com/example/ailisttest/ui/navigation/NavKey.kt)
Define `@Serializable` keys for `Main`, `Configure`, and `Display` screens.

#### [MODIFY] [MainActivity](file:///C:/Users/bocke/AndroidStudioProjects/AiListTest/app/src/main/java/com/example/ailisttest/MainActivity.kt)
- Integrate Navigation 3 `NavDisplay`.
- Initialize `MainViewModel`.

### [UI Layer - Screens (Placeholders)]

#### [NEW] [MainScreen](file:///C:/Users/bocke/AndroidStudioProjects/AiListTest/app/src/main/java/com/example/ailisttest/ui/screens/MainScreen.kt)
#### [NEW] [ConfigureScreen](file:///C:/Users/bocke/AndroidStudioProjects/AiListTest/app/src/main/java/com/example/ailisttest/ui/screens/ConfigureScreen.kt)
#### [NEW] [DisplayScreen](file:///C:/Users/bocke/AndroidStudioProjects/AiListTest/app/src/main/java/com/example/ailisttest/ui/screens/DisplayScreen.kt)

These will be basic placeholders to verify navigation works.

## Verification Plan

### Automated Tests
- Run Room DAO tests (if available) or basic unit tests for `MainRepository`.
- Verify `MainViewModel` state transitions.

### Manual Verification
- Launch the app and navigate between Main, Configure, and Display screens.
- Verify the hierarchical data is correctly fetched and displayed (once UI is implemented in next steps).
