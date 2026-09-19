## 1. State Management

Chosen: ViewModel + StateFlow

Alternative: LiveData

Trade-off:
I chose LiveData because it is natively lifecycle-aware.
    - LiveData: Automatically avoids delivering updates when the UI is inactive (e.g., in a STOPPED state), requiring no extra lifecycle handling.
    - StateFlow: Lacks native lifecycle awareness, so it requires additional lifecycle handling (e.g., using repeatOnLifecycle) to prevent unnecessary UI updates when the view is not visible.

## 2. Dependency Injection

Chosen: Hilt

Alternative: Manual dependency injection

Trade-off:
I chose Hilt for its lifecycle integration and standardized dependency management, which improves maintainability as the project grows.  
The trade-off is additional framework and build complexity compared with Manual dependency injection.

## 3. Persistence

Chosen: Room

Alternative: DataStore

Trade-off:
First, I analyzed the API response and the app requirements. The API provides structured article data, and the app needs to persist saved articles. So I chose Room for persistence.
I chose Room because it makes database operations easier and can map database data to Kotlin objects.
If I used SQLite directly, I would need to manually read the Cursor and map each row to an Article object, which would require more code.

## 4. UI

Chosen: XML Views

Alternative: Jetpack Compose

Trade-off:
I chose XML Views because I wanted to consider the team's familiarity and long-term maintenance. 
But if the team is already experienced with Compose, I don't see a strong reason to avoid it.

## 5. Concurrency

Chosen: Kotlin Coroutines

Alternative: RxJava

Trade-off:
Coroutines are simpler for this project and work well with Kotlin.
The project mainly uses one-time API calls, so I chose Coroutines
instead of using Flow for the API response.



Offline Weather page:
    - show offline message
    - show last update time and last Weather info (*Deferred*,2 reasions:
            - 1.High Development Cost: It requires extra time to implement history storage and track the last updated timestamp.
            - 2.User Experience Focus: Weather information emphasizes real-time accuracy rather than historical data. I also checked Apple's Weather app, which does not display weather info when offline.)

Offline Article page: show empty state because offline browsing is not required and caching all articles would consume unnecessary storage.

# Saved page:Saved records are stored locally only and are not synced to the server. Clearing the app data removes all saved records.

# Article page: Used Glide for simple image loading, efficient caching, and memory management.

# Supports portrait mode only; tablet support is deferred due to time constraints.

##Task flow
    API
    ↓
    Test / Verify API
    ↓
    Cache
    ↓
    Test / Verify Cache
    ↓
    Model
    ↓
    Test / Verify Model
    ↓
    UI
    ↓
    Test / Verify UI
    ↓
    All Functions Testing