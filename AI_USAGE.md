# AI tool: Gemini in Android Studio、chatGPT、Gemini。
**Q:**Is anything in the PDF unclear? Should I check with them?
**AI:**Ask "Should heterogeneous content also support the Detail/ Save?"
**Me:**Reject.The main focus is on articles. The weather card already shows the key weather information.

**Q:**They gave a hint on the freshness strategy. Hourly updates just follow their idea, but there is more to consider. What other factors should we think about?
**AI:**Things to Consider.
**Me:**Rank the Freshness Policy factors by importance.
**AI:**Things to Consider & Ranking.
**Me:**Why isn't the cache we discussed earlier on the priority list?
**AI:**
1. Weather and articles should use different rules.
2. Cache
3. App in background?
4. When should we refresh?
5. Pagination.
4. Images.
5. Network usage.

**Q:**To get the exact refresh timing, I think we can do some market research.
**AI:**I looked into a few practical examples for reference:
Microsoft Outlook (RSS): Maximum polling frequency of 60 minutes.
RSS.app: Notes that most consumer apps poll every 15 minutes to 1 hour, depending on the use case.
**Me:** I see, 1 hour makes sense.

**Q:**Weather should detect the user's current location.
**AI:**The requirements don't strictly call for the user's current location, as it adds extra complexity like location permissions and 'permission denied' states.I suggest fixing the location to a default city, like Taipei.
**Me:**
1. Hardcoding Taipei isn't a general solution for users in different locations.
2. The general implementation: Request location permission, retrieve current latitude/longitude, and pass coordinates to the weather API for location-relevant data.

**Q:** Does `switchFragment(SavedFragment())` create a new `SavedFragment` every time the button is clicked?
**AI:** Yes.
**Me:** Reject. It creates unnecessary Fragment instances and may cause unnecessary resource usage.

**Q:** Displaying `city = "Taipei"` feels a bit slow. Is this normal?
**AI:** Use the last known location (`lastLocation`) to display the location immediately. Retrieving it only takes a few milliseconds.
**Me:** Reject. The latitude and longitude would not update after the user moves.

**Q:** If the latitude and longitude cannot be obtained, is this a simulator issue? Would it work normally on a physical device?
**AI:** `PRIORITY_HIGH_ACCURACY` is suitable for apps like Google Maps, while `BALANCED_POWER_ACCURACY` is more suitable for weather apps.
**Me:** I tested on an emulator. Since the evaluator may use either an emulator or a physical device, I decided to use `PRIORITY_HIGH_ACCURACY`.

**Q:** Scrolling felt a bit laggy. How could it be optimized?
**AI:** Recommended optimizing the Paging configuration, including `prefetchDistance = 4`.
**Me:** Why 4 instead of 10?
**AI:**
- The downside of 4 is that if the user scrolls very quickly, the next page may not load in time, causing a loading state.
- The downside of 10 is that if the user does not scroll that far, it may trigger an unnecessary network request.
**Me:** Based on the number of cells visible on the phone screen, there are approximately 4–5 cells visible at a time. Therefore, I considered 4 to be a reasonable value.
