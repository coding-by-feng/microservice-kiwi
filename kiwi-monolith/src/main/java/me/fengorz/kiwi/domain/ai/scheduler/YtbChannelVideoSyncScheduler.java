/*
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.fengorz.kiwi.domain.ai.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.domain.ai.entity.YtbChannel;
import me.fengorz.kiwi.domain.ai.entity.YtbChannelVideo;
import me.fengorz.kiwi.domain.ai.entity.YtbVideoSubtitles;
import me.fengorz.kiwi.domain.ai.service.YtbChannelService;
import me.fengorz.kiwi.domain.ai.service.YtbChannelVideoService;
import me.fengorz.kiwi.domain.ai.service.YtbVideoSubtitlesService;
import me.fengorz.kiwi.domain.ai.ytb.SubtitleTypeEnum;
import me.fengorz.kiwi.domain.ai.ytb.YouTuBeHelper;
import me.fengorz.kiwi.domain.ai.ytb.YtbSubtitlesResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduled job for synchronizing YouTube channel videos and caching subtitles.
 *
 * This scheduler:
 * 1. Fetches the newest video list from each enabled channel
 * 2. Saves new videos to the database
 * 3. Downloads and caches scrolling subtitles for each video (if available)
 * 4. Does NOT call AI API for translation/enhancement (manual user action required)
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kiwi.youtube.sync.enabled", havingValue = "true", matchIfMissing = false)
public class YtbChannelVideoSyncScheduler {

    private final YtbChannelService ytbChannelService;
    private final YtbChannelVideoService ytbChannelVideoService;
    private final YtbVideoSubtitlesService ytbVideoSubtitlesService;
    private final YouTuBeHelper youTuBeHelper;

    @Value("${kiwi.youtube.sync.fetch-subtitles:true}")
    private boolean fetchSubtitles;

    @Value("${kiwi.youtube.sync.max-new-videos-per-channel:50}")
    private int maxNewVideosPerChannel;

    @Value("${kiwi.youtube.sync.consecutive-existing-threshold:10}")
    private int consecutiveExistingThreshold;

    @Value("${kiwi.youtube.sync.run-on-startup:false}")
    private boolean runOnStartup;

    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Run sync job on application startup if configured.
     * Runs in a separate thread to avoid blocking Spring Boot startup (Tomcat must bind first).
     */
    @PostConstruct
    public void onStartup() {
        log.info("[YTB-SYNC] Scheduler initialized - fetchSubtitles={}, maxNewVideosPerChannel={}, consecutiveExistingThreshold={}, runOnStartup={}",
                fetchSubtitles, maxNewVideosPerChannel, consecutiveExistingThreshold, runOnStartup);
        if (runOnStartup) {
            log.info("[YTB-SYNC] run-on-startup is enabled, triggering initial sync in background thread...");
            Thread startupSyncThread = new Thread(this::syncChannelVideos, "ytb-sync-startup");
            startupSyncThread.setDaemon(true);
            startupSyncThread.start();
        }
    }

    /**
     * Scheduled task to sync channel videos.
     * Default: runs every 6 hours (configurable via cron expression)
     */
    @Scheduled(cron = "${kiwi.youtube.sync.cron:0 0 */6 * * *}")
    public void syncChannelVideos() {
        if (!running.compareAndSet(false, true)) {
            log.warn("[YTB-SYNC] Previous sync job is still running, skipping this execution");
            return;
        }

        String jobId = String.valueOf(System.currentTimeMillis());
        log.info("[YTB-SYNC] ========== Job {} started at {} ==========", jobId, LocalDateTime.now());
        log.info("[YTB-SYNC] Job config - fetchSubtitles={}, maxNewVideosPerChannel={}, consecutiveExistingThreshold={}",
                fetchSubtitles, maxNewVideosPerChannel, consecutiveExistingThreshold);

        try {
            long startTime = System.currentTimeMillis();

            List<YtbChannel> enabledChannels = ytbChannelService.findEnabledChannels();
            if (enabledChannels.isEmpty()) {
                log.warn("[YTB-SYNC] No enabled channels found, job {} finished early", jobId);
                return;
            }
            log.info("[YTB-SYNC] Found {} enabled channels to sync: {}", enabledChannels.size(),
                    enabledChannels.stream().map(YtbChannel::getChannelName).toList());

            int totalNewVideos = 0;
            int totalSubtitlesCached = 0;
            int successfulChannels = 0;
            int failedChannels = 0;

            for (int i = 0; i < enabledChannels.size(); i++) {
                YtbChannel channel = enabledChannels.get(i);
                log.info("[YTB-SYNC] Processing channel {}/{}: {} (ID: {})",
                        i + 1, enabledChannels.size(), channel.getChannelName(), channel.getId());
                try {
                    SyncResult result = syncChannel(channel);
                    totalNewVideos += result.newVideosCount;
                    totalSubtitlesCached += result.subtitlesCachedCount;
                    successfulChannels++;
                    log.info("[YTB-SYNC] Channel {} completed - newVideos={}, subtitlesCached={}",
                            channel.getChannelName(), result.newVideosCount, result.subtitlesCachedCount);
                } catch (Exception e) {
                    failedChannels++;
                    log.error("[YTB-SYNC] Error syncing channel {} (ID: {}): {}",
                            channel.getChannelName(), channel.getId(), e.getMessage(), e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("[YTB-SYNC] ========== Job {} completed at {} ==========", jobId, LocalDateTime.now());
            log.info("[YTB-SYNC] Job {} summary - Duration: {}ms ({}s), Channels: {}/{} successful, New videos: {}, Subtitles cached: {}",
                    jobId, duration, duration / 1000, successfulChannels, enabledChannels.size(), totalNewVideos, totalSubtitlesCached);
            if (failedChannels > 0) {
                log.warn("[YTB-SYNC] Job {} had {} failed channel(s)", jobId, failedChannels);
            }
        } catch (Exception e) {
            log.error("[YTB-SYNC] Job {} failed with unexpected error: {}", jobId, e.getMessage(), e);
        } finally {
            running.set(false);
            log.info("[YTB-SYNC] Job {} lock released", jobId);
        }
    }

    /**
     * Sync a single channel - fetch videos and cache subtitles
     */
    private SyncResult syncChannel(YtbChannel channel) {
        log.info("[YTB-SYNC] Syncing channel: {} (ID: {}, link: {})",
                channel.getChannelName(), channel.getId(), channel.getChannelLink());
        long channelStartTime = System.currentTimeMillis();

        int newVideosCount = 0;
        int subtitlesCachedCount = 0;
        int skippedExistingCount = 0;
        int failedVideosCount = 0;

        try {
            log.debug("[YTB-SYNC] Fetching video links from channel: {}", channel.getChannelLink());
            List<String> videoLinks = youTuBeHelper.extractAllVideoLinks(channel.getChannelLink());
            log.info("[YTB-SYNC] Found {} total videos in channel: {}", videoLinks.size(), channel.getChannelName());

            int consecutiveExisting = 0;
            int scannedCount = 0;
            for (String videoLink : videoLinks) {
                // Stop if we've found enough new videos
                if (newVideosCount >= maxNewVideosPerChannel) {
                    log.info("[YTB-SYNC] Reached max new videos limit ({}) for channel: {}",
                            maxNewVideosPerChannel, channel.getChannelName());
                    break;
                }

                // Early exit: N consecutive existing videos means we've reached already-synced territory
                if (consecutiveExisting >= consecutiveExistingThreshold) {
                    log.info("[YTB-SYNC] Hit {} consecutive existing videos for channel: {}, assuming all new videos found (scanned {}/{})",
                            consecutiveExistingThreshold, channel.getChannelName(), scannedCount, videoLinks.size());
                    break;
                }

                scannedCount++;
                try {
                    // Check if video already exists
                    if (ytbChannelVideoService.existsByVideoLink(videoLink)) {
                        log.debug("[YTB-SYNC] Video already exists, skipping: {}", videoLink);
                        skippedExistingCount++;
                        consecutiveExisting++;
                        continue;
                    }

                    // New video found - reset consecutive counter
                    consecutiveExisting = 0;

                    // Create new video record
                    log.debug("[YTB-SYNC] Creating new video record for: {}", videoLink);
                    YtbChannelVideo video = createVideoRecord(channel.getId(), videoLink);
                    if (video != null) {
                        newVideosCount++;
                        log.info("[YTB-SYNC] Added new video #{}: \"{}\" (ID: {}) - {}",
                                newVideosCount, video.getVideoTitle(), video.getId(), videoLink);

                        // Cache subtitles if enabled
                        if (fetchSubtitles) {
                            log.debug("[YTB-SYNC] Attempting to cache subtitles for video ID: {}", video.getId());
                            boolean cached = cacheSubtitles(video);
                            if (cached) {
                                subtitlesCachedCount++;
                                log.info("[YTB-SYNC] Subtitles cached for video: {}", video.getVideoTitle());
                            } else {
                                log.debug("[YTB-SYNC] No subtitles available or already cached for video: {}",
                                        video.getVideoTitle());
                            }
                        }
                    } else {
                        failedVideosCount++;
                        log.warn("[YTB-SYNC] Failed to create video record for: {}", videoLink);
                    }
                } catch (Exception e) {
                    failedVideosCount++;
                    log.warn("[YTB-SYNC] Error processing video {}: {}", videoLink, e.getMessage(), e);
                }
            }

            long channelDuration = System.currentTimeMillis() - channelStartTime;
            log.info("[YTB-SYNC] Channel {} sync completed in {}ms - scanned={}, new={}, skipped={}, failed={}, subtitles={}",
                    channel.getChannelName(), channelDuration, scannedCount, newVideosCount,
                    skippedExistingCount, failedVideosCount, subtitlesCachedCount);
        } catch (Exception e) {
            long channelDuration = System.currentTimeMillis() - channelStartTime;
            log.error("[YTB-SYNC] Error extracting videos from channel {} after {}ms: {}",
                    channel.getChannelLink(), channelDuration, e.getMessage(), e);
        }

        return new SyncResult(newVideosCount, subtitlesCachedCount);
    }

    /**
     * Create a new video record in the database
     */
    private YtbChannelVideo createVideoRecord(Long channelId, String videoLink) {
        log.debug("[YTB-SYNC] Creating video record - channelId={}, videoLink={}", channelId, videoLink);
        try {
            log.debug("[YTB-SYNC] Fetching video title for: {}", videoLink);
            String videoTitle = youTuBeHelper.getVideoTitle(videoLink);
            log.debug("[YTB-SYNC] Fetching video published date for: {}", videoLink);
            LocalDateTime publishedAt = youTuBeHelper.getVideoPublishedAt(videoLink);

            YtbChannelVideo video = new YtbChannelVideo();
            video.setChannelId(channelId);
            video.setVideoLink(videoLink);
            video.setVideoTitle(videoTitle != null ? videoTitle : "Unknown Title");
            video.setPublishedAt(publishedAt);
            video.setStatus(YtbChannelVideo.STATUS_READY);
            video.setIfValid(true);
            video.setCreateTime(LocalDateTime.now());

            YtbChannelVideo savedVideo = ytbChannelVideoService.saveVideo(video);
            log.debug("[YTB-SYNC] Video record created - ID={}, title=\"{}\", publishedAt={}",
                    savedVideo.getId(), savedVideo.getVideoTitle(), savedVideo.getPublishedAt());
            return savedVideo;
        } catch (Exception e) {
            log.error("[YTB-SYNC] Error creating video record for {}: {}", videoLink, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Download and cache subtitles for a video (raw scrolling subtitles only, no AI processing)
     */
    private boolean cacheSubtitles(YtbChannelVideo video) {
        log.debug("[YTB-SYNC] Caching subtitles - videoId={}, videoLink={}", video.getId(), video.getVideoLink());
        try {
            log.debug("[YTB-SYNC] Downloading subtitles for video: {}", video.getVideoLink());

            YtbSubtitlesResult result = youTuBeHelper.downloadSubtitles(video.getVideoLink());
            if (result == null) {
                log.debug("[YTB-SYNC] No subtitle result returned for video: {}", video.getVideoLink());
                return false;
            }
            if (result.getScrollingSubtitles() == null) {
                log.debug("[YTB-SYNC] Subtitle result has no scrolling subtitles for video: {} (type: {})",
                        video.getVideoLink(), result.getType());
                return false;
            }

            // Determine subtitle type (professional or auto-generated)
            Integer subtitleType = determineSubtitleType(result.getType());
            String subtitleTypeName = subtitleType == YtbVideoSubtitles.TYPE_PROFESSIONAL ? "professional" : "auto-generated";
            log.debug("[YTB-SYNC] Subtitle type determined: {} ({}) for video: {}",
                    subtitleTypeName, result.getType(), video.getVideoLink());

            // Check if subtitles already exist for this video
            List<YtbVideoSubtitles> existingSubtitles = ytbVideoSubtitlesService.findByVideoId(video.getId());
            if (!existingSubtitles.isEmpty()) {
                log.debug("[YTB-SYNC] Subtitles already cached for video ID: {} (found {} existing records)",
                        video.getId(), existingSubtitles.size());
                return false;
            }

            // Save subtitles to database
            int subtitleLength = result.getScrollingSubtitles().length();
            log.debug("[YTB-SYNC] Saving subtitles to database - videoId={}, type={}, textLength={}",
                    video.getId(), subtitleTypeName, subtitleLength);

            YtbVideoSubtitles subtitles = new YtbVideoSubtitles();
            subtitles.setVideoId(video.getId());
            subtitles.setType(subtitleType);
            subtitles.setStatus(YtbVideoSubtitles.STATUS_READY);
            subtitles.setSubtitlesText(result.getScrollingSubtitles());
            subtitles.setIfValid(true);
            subtitles.setCreateTime(LocalDateTime.now());

            YtbVideoSubtitles savedSubtitles = ytbVideoSubtitlesService.saveSubtitles(subtitles);
            log.info("[YTB-SYNC] Cached subtitles for video: \"{}\" (ID: {}, subtitleId: {}, type: {}, length: {} chars)",
                    video.getVideoTitle(), video.getId(), savedSubtitles.getId(), subtitleTypeName, subtitleLength);
            return true;
        } catch (Exception e) {
            log.warn("[YTB-SYNC] Failed to cache subtitles for video \"{}\" (ID: {}, link: {}): {}",
                    video.getVideoTitle(), video.getId(), video.getVideoLink(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Determine subtitle type from SubtitleTypeEnum
     */
    private Integer determineSubtitleType(SubtitleTypeEnum type) {
        if (type == null) {
            return YtbVideoSubtitles.TYPE_AUTO_GENERATED;
        }
        switch (type) {
            case SMALL_PROFESSIONAL_RETURN_STRING:
            case LARGE_PROFESSIONAL_RETURN_LIST:
                return YtbVideoSubtitles.TYPE_PROFESSIONAL;
            default:
                return YtbVideoSubtitles.TYPE_AUTO_GENERATED;
        }
    }

    /**
     * Check if the scheduler is currently running
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Result holder for sync operation
     */
    private static class SyncResult {
        final int newVideosCount;
        final int subtitlesCachedCount;

        SyncResult(int newVideosCount, int subtitlesCachedCount) {
            this.newVideosCount = newVideosCount;
            this.subtitlesCachedCount = subtitlesCachedCount;
        }
    }
}
