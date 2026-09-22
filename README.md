Open the project in Android Studio and run the `app` configuration on an Android emulator or device.

# Plan

## Requirement Analysis

- feed:Articles、Weather card、Saved
- Dark theme

## Feature

### feed

- view: vertical、swiperefreshlayout、if offline show view on top、scrolling smooth(like Instagram APP)
- Articles:
  - view: vertical、save(unsave)、thumb、data is loading / empty / error
  - pagination
    - load next page when reaching near bottom
    - no more data
    - pagination error
  - online:
    - call api、click → detail
  - offline:
    - empty state

### Weather card

- Q:why pick this source?
  - ANS:
    1. Time Saver: Free API with no key registration required.
    2. Clean Data: Structured weather data requires less image handling compared to movies.
    3. Core Focus: Allowed me to focus on caching, freshness policy, offline support, pagination, and UI states within the time limit.
- source: Open-Meteo
- detect user current location and distance
- view: place weather card at the top of feed、data is loading / empty / error
- online:
  - call api
- offline:
  - Hide Weather

### saved

- view: vertical
- show saved articles
- click → detail
- empty state
- offline readable
- Saved records are stored locally only and are not synced to the server. Clearing the app data removes all saved records.

### Detai

Saved articles use the downloaded HTML content instead of calling the API, both online and offline. This reduces unnecessary network usage and allows saved articles to be read offline.

**Trade-off:**

Images may not be available if the WebView cache has been cleared or the cached images are no longer available.

# Deferred

- Tablet UI
- Search functionality
- Adjusting API request frequency based on network type (Wi-Fi / 5G)
- Complete offline article browsing, including locally stored images

## freshness policy

Things to Consider & Ranking.

1. Weather and articles should use different rules.
2. Cache
3. App in background?
4. When should we refresh?
5. Pagination.
4. Images.
5. Network usage.(*Deferred*,because I do not think they are the main factors in reducing mobile data usage. I will focus on the main requirements and use the refresh policy, cache, and pagination to reduce data usage.)

### Articles

Use cached data if it is less than 1 hour old. Otherwise, call the API.I chose 1 hour as a balance between freshness and network usage. Based on my research, 15 minutes to 1 hour is commonly used for feed updates, so I think 1 hour is reasonable for this project.

### Weather card

- Change the weather API request frequency to every 15 minutes (aligned with Google Weather API recommendations) or when the distance moved is ≥ 1 km; otherwise, use local DB data. This keeps the weather reasonably fresh without making too many network requests

## Flow

```text
Article page
     │
     ▼
  Online?
┌─────┴─────┐
No           Yes
│             │
▼             ▼
Offline view   Load Page
                 │
                 ▼
           Initial Load?
            ┌────┴────┐
           Yes        No (Scroll / Page > 1)
            │         │
            ▼         ▼
   Cached & < 1h?    Fetch Next Page (API)
        ┌───┴───┐
       Yes      No (> 1h or Empty)
        │        │
        ▼        ▼
    Use Cache  Call API (Spaceflight)
                 │
                 ▼
            Clear & Update DB


Weather Load
     │
     ▼
  Online?
┌─────┴─────┐
No           Yes
│             │
▼             ▼
Hide Weather  Location Permission
                 │
            ┌────┴────┐
         Denied    Granted
            │         │
            ▼         ▼
      Status Notice Lat/Lng
      (Permission)    │
                      ▼
             Distance < 1km & < 15m?
                   ┌──┴──┐
                  Yes    No
                   │      │
                   ▼      ▼
            Use Cache   Call Open-Meteo API
                          │
                          ▼
                      Update DB

Saved
  │
  ▼
Observe DB
  │
  ┌────┴────┐
  ▼         ▼
Empty    Has Saved
  │         │
  ▼         ▼
Empty View  Saved List
              │
       ┌────────┴────────┐
       ▼                 ▼
  Click Item        Click Unsave
       │                 │
       ▼                 ▼
Article Detail    Delete HTML File
       │                 │
  Local HTML?            ▼
  ┌────┴────┐      Delete Local DB
 Yes        No           │
  │          │           ▼
  ▼          ▼     Notify Reading (Sync)
Load HTML  Load URL
```

# Testing

### Weather:

- online:
  1. The weather is successfully displayed after obtaining the latitude and longitude. If the latitude and longitude     cannot be obtained, an error screen is displayed. (OK)
  2. Updated every 15 minutes. (OK)
- offline:The online screen is displayed. (OK)

### Articles:

- online:
  1. Smooth scrolling. (OK)
  2. Thumbnails and content are displayed. A default icon is used when a thumbnail cannot be displayed. (OK)
  3. After clicking the Saved button, the article is displayed on the Saved page. Confirmed that it is the selected article, and clicking the button again removes it from Saved. (OK)
  4. Clicking an item navigates to the detail page. (OK)
  5. Swipe-to-refresh. (OK)
  6. Updated every 15 minutes. (OK)
- offline:The online screen is displayed. (OK)

### saved:

- online/offline:
  1. Clicking the Saved button removes the article. (OK)
  2. Clicking an item navigates to the detail page. (OK)

### detail:

- online/offline:
  - The website is displayed. (OK)
