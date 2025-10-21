package me.fengorz.kiwi.ai.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.entity.YtbChannelDO;
import me.fengorz.kiwi.ai.service.ytb.YtbChannelService;
import me.fengorz.kiwi.common.sdk.enumeration.ProcessStatusEnum;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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
     * Scheduled task that runs every 1 minute to find unfinished channels and process them
     * Uses fixed delay to ensure intervals between executions; if a previous run is still executing,
     * the current trigger will be skipped.
     */
    @Scheduled(fixedDelay = 60 * 1000)
    public void processUnfinishedChannels() {
        if (!processingUnfinishedRunning.compareAndSet(false, true)) {
            log.info("processUnfinishedChannels is still running, skipping this schedule tick at {}", LocalDateTime.now());
            return;
        }

        log.info("Starting scheduled channel synchronization check at {}", LocalDateTime.now());

        try {
            // Find all channels that are valid and either in READY state or have been in PROCESSING/FAILED state for too long
            List<YtbChannelDO> unfinishedChannels = ytbChannelService.list(
                    new LambdaQueryWrapper<YtbChannelDO>()
                            .eq(YtbChannelDO::getIfValid, true)
                            .and(wrapper -> wrapper
                                    .eq(YtbChannelDO::getStatus, ProcessStatusEnum.READY.getCode())
                                    // Or in FAILED state but stuck (more than 2 hours old)
                                    .or(w -> w
                                            .in(YtbChannelDO::getStatus, Collections.singletonList(ProcessStatusEnum.FAILED.getCode()))
                                            .lt(YtbChannelDO::getCreateTime, LocalDateTime.now().minusHours(2))
                                    )
                                    // Or in PROCESSING state but stuck (more than 12 hours old)
                                    .or(w -> w
                                            .in(YtbChannelDO::getStatus, Collections.singletonList(ProcessStatusEnum.PROCESSING.getCode()))
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
     * Scheduled task that runs every 1 hour to find finished channels and reset them to READY state
     * Skips the current trigger if a previous run is still executing.
     */
    @Scheduled(fixedDelay = 60 * 60 * 1000)
    public void resetFinishedChannels() {
        if (!resetFinishedRunning.compareAndSet(false, true)) {
            log.info("resetFinishedChannels is still running, skipping this schedule tick at {}", LocalDateTime.now());
            return;
        }

        log.info("Starting scheduled reset of finished channels at {}", LocalDateTime.now());

        try {
            // Find all channels that are valid and in FINISHED state
            List<YtbChannelDO> finishedChannels = ytbChannelService.list(
                    new LambdaQueryWrapper<YtbChannelDO>()
                            .eq(YtbChannelDO::getIfValid, true)
                            .eq(YtbChannelDO::getStatus, ProcessStatusEnum.FINISHED.getCode())
            );

            log.info("Found {} finished channels to reset", finishedChannels.size());

            // Process each finished channel and reset to READY
            for (YtbChannelDO channel : finishedChannels) {
                log.info("Resetting status for channel ID: {}, name: {} from FINISHED to READY",
                        channel.getId(), channel.getChannelName());

                // Call the updateChannelStatus method to update the channel status
                ytbChannelService.updateChannelStatus(
                        "Preparing to reset channel ID: {} from FINISHED to READY",
                        channel.getId(),
                        channel,
                        ProcessStatusEnum.READY,
                        "Successfully reset channel ID: {} to READY state"
                );
            }

            log.info("Completed resetting {} finished channels to READY state", finishedChannels.size());
        } catch (Exception e) {
            log.error("Error in channel reset scheduler: {}", e.getMessage(), e);
        } finally {
            resetFinishedRunning.set(false);
        }
    }

}