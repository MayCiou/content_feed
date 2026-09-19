Open the project in Android Studio and run the `app` configuration on an Android emulator or device.

# Plan:
## Requirement Analysis:　
    - feed:Articles、Weather card
    - Dark theme
## Feature:
    - Network；Check the internet connection
        - online: call api
        - offline: show alert, loading offline view
    -feed: 
        - view: vertical、swiperefreshlayout、if offline show fixed message on top、scrolling smooth(like Instagram APP)
        - Articles: 
            - view: vertical、save(unsave)、thumb、data is loading / empty / error
            - online: 
                - call api、detail
            - offline: 
                - show save
        - Weather card: 
            - view: horizontal、data is loading / empty / error
            - online: 
                - call api
            - offline:
                - show last update time

## freshness policy:
    Things to Consider & Ranking.
    1. Weather and articles should use different rules.
    2. Cache
    3. App in background?
    4. When should we refresh?
    5. Pagination.
    4. Images.
    5. Network usage.(Deferred,because I do not think they are the main factors in reducing mobile data usage. I will focus on the main requirements and use the refresh policy, cache, and pagination to reduce data usage.)

    # Articles:
            Use cached data if it is less than 1 hour old. Otherwise, call the API.I chose 1 hour as a balance between freshness and network usage. Based on my research, 15 minutes to 1 hour is commonly used for feed updates, so I think 1 hour is reasonable for this project.
    # Weather card:
            - Use cached data if it is less than 10 minutes old. Otherwise, call the API. This keeps the weather reasonably fresh without making too many network requests.
    #Flow
                          Feed
                           │
                           ▼
                         Cache
                           │
                ┌──────────┴──────────┐
                │                     │
                ▼                     ▼
            Weather                Article
          Fresh: < 10 min        Fresh: < 1 hour
                │                     │
          ┌─────┴─────┐         ┌─────┴─────┐
          │           │         │           │
          ▼           ▼         ▼           ▼
       Use Cache   Call API   Use Cache   Call API
                      │                     │
                      └──────────┬──────────┘
                                 ▼
                            Update Cache
  