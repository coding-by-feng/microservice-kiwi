package me.fengorz.kiwi.ai.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.entity.YtbChannelDO;
import me.fengorz.kiwi.ai.service.ytb.YtbChannelService;
import me.fengorz.kiwi.common.sdk.enumeration.ProcessStatusEnum;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Scheduler for synchronizing YouTube channel videos
 * Periodically checks for channels that aren't fully processed and initiates synchronization
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class YtbChannelSyncScheduler {

    private final YtbChannelService ytbChannelService;
    
    /**
     * Scheduled task that runs every 10 minutes to find unfinished channels and process them
     * Uses fixed delay to ensure 10-minute intervals between executions
     */
    @Scheduled(fixedDelay = 10 * 60 * 60)
    public void processUnfinishedChannels() {
        log.info("Starting scheduled channel synchronization check at {}", LocalDateTime.now());
        
        try {
            // Find all channels that are valid and either in READY state or have been in PROCESSING state for too long
            List<YtbChannelDO> unfinishedChannels = ytbChannelService.list(
                    new LambdaQueryWrapper<YtbChannelDO>()
                            .eq(YtbChannelDO::getIfValid, true)
                            .and(wrapper -> wrapper
                                    .eq(YtbChannelDO::getStatus, ProcessStatusEnum.READY.getCode())
                                    // Or in PROCESSING state but stuck (more than 1 hour old)
                                    .or(w -> w
                                            .in(YtbChannelDO::getStatus, Arrays.asList(ProcessStatusEnum.PROCESSING.getCode(), ProcessStatusEnum.READY.getCode()))
                                            .lt(YtbChannelDO::getCreateTime, LocalDateTime.now().minusHours(1))
                                    )
                            )
            );
            
            log.info("Found {} unfinished channels to process", unfinishedChannels.size());
            
            // Process each unfinished channel
            for (YtbChannelDO channel : unfinishedChannels) {
                log.info("Scheduling sync for channel ID: {}, name: {}, current status: {}",
                        channel.getId(), channel.getChannelName(), channel.getStatus());
                
                // Call the async method to process this channel
                ytbChannelService.syncChannelVideos(channel.getId());
                
                // Add a small delay between processing different channels to avoid overwhelming the system
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Thread interrupted while waiting between channel processing");
                }
            }
            
            log.info("Completed scheduling channel synchronization for {} channels", unfinishedChannels.size());
        } catch (Exception e) {
            log.error("Error in channel synchronization scheduler: {}", e.getMessage(), e);
        }
    }
}