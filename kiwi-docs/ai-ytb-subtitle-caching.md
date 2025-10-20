# AI YouTube Subtitle Caching (Normalized Video ID)

Context
- Controller: `kiwi-ai-biz/.../YouTuBeController.java`
- Service: `kiwi-ai-biz/.../YtbSubtitleStreamingService.java`

Goals
- Cache raw YouTube subtitles and translated subtitles.
- Ensure different URL forms for the same video map to a single cache entry by using the normalized videoId as the key.

Key points
- Video ID normalization: supports `youtu.be/<id>`, `youtube.com/watch?v=<id>`, `youtube.com/shorts/<id>`, `youtube.com/embed/<id>` and more. Fallback attempts to parse `v=` and trims query/fragment.
- Cache names: `AiConstants.CACHE_NAMES`
- Key composition: via custom key generator (bean name `cacheKeyGenerator`) and the `@KiwiCacheKeyPrefix` values. Keys are structured roughly as:
  - `grok:subtitle:scrolling:<videoId>`
  - `grok:video:title:<videoId>`
  - `grok:subtitle:streaming:<videoId>:<language>`

Cached methods
- Original subtitles
  - Public wrapper: `getScrollingSubtitles(String videoUrl)`
    - Internally calls `getScrollingSubtitlesInternal(@KiwiCacheKey(1) String videoId, String decodedUrl)`
    - `@Cacheable` on `getScrollingSubtitlesInternal` using `videoId` as cache key.
- Video title
  - Public wrapper: `getVideoTitle(String videoUrl)`
    - Internally calls `getVideoTitleInternal(@KiwiCacheKey(1) String videoId, String decodedUrl)`
    - `@Cacheable` on `getVideoTitleInternal` using `videoId` as cache key.
- Translated subtitles (streaming)
  - First tries cache: `getCachedStreamingSubtitleTranslationById(@KiwiCacheKey(1) String videoId, @KiwiCacheKey(2) String language)` with `@Cacheable` (returns null on miss, not cached).
  - On first streaming, accumulates full content and persists via `@CachePut` method:
    - `cacheStreamingSubtitleTranslation(@KiwiCacheKey(1) String videoId, @KiwiCacheKey(2) String language, String translatedContent)`

Eviction
- Controller endpoint `DELETE /ai/ytb/video/subtitles` calls `cleanAllCaches(videoUrl, language)`.
- The service normalizes to `videoId` and evicts:
  - `cleanScrollingSubtitlesCacheById(@KiwiCacheKey(1) String videoId)`
  - `cleanVideoTitleCacheById(@KiwiCacheKey(1) String videoId)`
  - `cleanStreamingSubtitleTranslationCacheById(@KiwiCacheKey(1) String videoId, @KiwiCacheKey(2) String language)` when language provided
- Additionally cleans AI chat translation caches that use decoded URL + prompt mode.

Frontend notes
- Repeated requests for the same videoId will hit cache regardless of different URL variants.
- When language parameter is provided for translation, cache is distinct per `(videoId, language)`.
- To force refresh, call the `DELETE /ai/ytb/video/subtitles` endpoint.

