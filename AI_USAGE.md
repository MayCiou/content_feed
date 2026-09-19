# AI tool: Gemini in Android Studio、chatGPT、Gemini。
Q:Is anything in the PDF unclear? Should I check with them?
AI:Ask "Should heterogeneous content also support the Detail/ Save?"
Me:Reject.The main focus is on articles. The weather card already shows the key weather information.

Q:They gave a hint on the freshness strategy. Hourly updates just follow their idea, but there is more to consider. What other factors should we think about?
AI:Things to Consider.
Me:Rank the Freshness Policy factors by importance.
AI:Things to Consider & Ranking.
Me:Why isn't the cache we discussed earlier on the priority list? 
AI:
    1. Weather and articles should use different rules.
    2. Cache
    3. App in background?
    4. When should we refresh?
    5. Pagination.
    4. Images.
    5. Network usage.

Q:To get the exact refresh timing, I think we can do some market research.
AI:I looked into a few practical examples for reference:
Microsoft Outlook (RSS): Maximum polling frequency of 60 minutes.
RSS.app: Notes that most consumer apps poll every 15 minutes to 1 hour, depending on the use case.
Me: I see, 1 hour makes sense.

Q:Weather should detect the user's current location.
AI:The requirements don't strictly call for the user's current location, as it adds extra complexity like location permissions and 'permission denied' states.I suggest fixing the location to a default city, like Taipei.
Me:
    1. Hardcoding Taipei isn't a general solution for users in different locations. 
    2. The general implementation: Request location permission, retrieve current latitude/longitude, and pass coordinates to the weather API for location-relevant data.
    

