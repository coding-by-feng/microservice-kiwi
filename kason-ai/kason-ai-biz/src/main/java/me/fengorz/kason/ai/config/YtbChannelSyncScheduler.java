package me.fengorz.kason.ai.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kason.ai.api.entity.YtbChannelDO;
import me.fengorz.kason.ai.service.ytb.YtbChannelService;
import me.fengorz.kason.common.sdk.enumeration.ProcessStatusEnum;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduler for synchronizing YouTube channel videos
 * Periodically checks for channels that aren't fully processed and initiates synchronization
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "youtube.video.batch.enabled", havingValue = "true")
public class YtbChannelSyncScheduler {

    private final YtbChannelService ytbChannelService;

    // Concurrency guards to avoid overlapping runs
    private final AtomicBoolean processingUnfinishedRunning = new AtomicBoolean(false);
    private final AtomicBoolean resetFinishedRunning = new AtomicBoolean(false);

    /**
     * Scheduled task that runs every 1 minute to find channels to process
     * Criteria:
     *  - FAILED (no time condition)
     *  - FINISHED (to fetch new videos as lists grow)
     *  - PROCESSING but stuck for more than 12 hours
     */
    @Scheduled(fixedDelay = 60 * 1000)
    public void processUnfinishedChannels() {
        if (!processingUnfinishedRunning.compareAndSet(false, true)) {
            log.info("processUnfinishedChannels is still running, skipping this schedule tick at {}", LocalDateTime.now());
            return;
        }

        log.info("Starting scheduled channel synchronization check at {}", LocalDateTime.now());

        try {
            // Find channels matching the criteria described above
            List<YtbChannelDO> unfinishedChannels = ytbChannelService.list(
                    new LambdaQueryWrapper<YtbChannelDO>()
                            .eq(YtbChannelDO::getIfValid, true)
                            .and(wrapper -> wrapper
                                    // READY, FAILED, or FINISHED without time constraint
                                    .in(YtbChannelDO::getStatus, Collections.singletonList(
                                            ProcessStatusEnum.READY.getCode()
                                    ))
                                    // Or PROCESSING but stuck for a long time
                                    .or(w -> w
                                            .eq(YtbChannelDO::getStatus, ProcessStatusEnum.PROCESSING.getCode())
                                            .lt(YtbChannelDO::getCreateTime, LocalDateTime.now().minusHours(12))
                                    )
                            )
            );

            log.info("Found {} unfinished channels to process", unfinishedChannels.size());

            // Process each unfinished channel
            for (YtbChannelDO channel : unfinishedChannels) {
                log.info("Scheduling sync for channel ID: {}, name: {}, current status: {}",
                        channel.getId(), channel.getChannelName(), channel.getStatus());

                // Call the async method to process this channel (load newest video list and update DB; subtitles are handled inside service)
                ytbChannelService.syncChannelVideos(channel.getId());
            }

            log.info("Completed scheduling channel synchronization for {} channels", unfinishedChannels.size());
        } catch (Exception e) {
            log.error("Error in channel synchronization scheduler: {}", e.getMessage(), e);
        } finally {
            processingUnfinishedRunning.set(false);
        }
    }

    /**
     * Scheduled task that runs every 1 hour to find finished/failed channels and reset them to READY state
     * Skips the current trigger if a previous run is still executing.
     */
    @Scheduled(fixedDelay = 60 * 60 * 1000)
    public void resetFinishedChannels() {
        if (!resetFinishedRunning.compareAndSet(false, true)) {
            log.info("resetFinishedChannels is still running, skipping this schedule tick at {}", LocalDateTime.now());
            return;
        }

        log.info("Starting scheduled reset of FINISHED/FAILED channels at {}", LocalDateTime.now());

        try {
            // Find all channels that are valid and in FINISHED or FAILED state
            List<YtbChannelDO> finishedChannels = ytbChannelService.list(
                    new LambdaQueryWrapper<YtbChannelDO>()
                            .eq(YtbChannelDO::getIfValid, true)
                            .in(YtbChannelDO::getStatus, Arrays.asList(
                                    ProcessStatusEnum.FINISHED.getCode(),
                                    ProcessStatusEnum.FAILED.getCode()
                            ))
            );

            log.info("Found {} FINISHED/FAILED channels to reset", finishedChannels.size());

            // Process each finished/failed channel and reset to READY
            for (YtbChannelDO channel : finishedChannels) {
                log.info("Resetting status for channel ID: {}, name: {} from {} to READY",
                        channel.getId(), channel.getChannelName(), channel.getStatus());

                // Call the updateChannelStatus method to update the channel status
                ytbChannelService.updateChannelStatus(
                        "Preparing to reset channel ID: {} to READY",
                        channel.getId(),
                        channel,
                        ProcessStatusEnum.READY,
                        "Successfully reset channel ID: {} to READY state"
                );
            }

            log.info("Completed resetting {} channels to READY state", finishedChannels.size());
        } catch (Exception e) {
            log.error("Error in channel reset scheduler: {}", e.getMessage(), e);
        } finally {
            resetFinishedRunning.set(false);
        }
    }

}