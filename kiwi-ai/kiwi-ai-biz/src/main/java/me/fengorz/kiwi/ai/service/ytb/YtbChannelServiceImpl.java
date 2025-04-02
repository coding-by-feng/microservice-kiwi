package me.fengorz.kiwi.ai.service.ytb;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.entity.*;
import me.fengorz.kiwi.ai.api.vo.ytb.YtbChannelVO;
import me.fengorz.kiwi.ai.service.AiChatService;
import me.fengorz.kiwi.ai.service.ytb.mapper.*;
import me.fengorz.kiwi.common.db.service.SeqService;
import me.fengorz.kiwi.common.sdk.enumeration.AiPromptModeEnum;
import me.fengorz.kiwi.common.sdk.enumeration.LanguageEnum;
import me.fengorz.kiwi.common.sdk.enumeration.ProcessStatusEnum;
import me.fengorz.kiwi.common.sdk.exception.ServiceException;
import me.fengorz.kiwi.common.sdk.web.WebTools;
import me.fengorz.kiwi.common.ytb.SubtitleTypeEnum;
import me.fengorz.kiwi.common.ytb.YouTuBeHelper;
import me.fengorz.kiwi.common.ytb.YtbSubtitlesResult;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class YtbChannelServiceImpl extends ServiceImpl<YtbChannelMapper, YtbChannelDO> implements YtbChannelService {

    private final YtbChannelUserMapper channelUserMapper;
    private final YtbChannelVideoMapper videoMapper;
    private final YtbVideoSubtitlesMapper subtitlesMapper;
    private final YtbVideoSubtitlesTranslationMapper subtitlesTranslationMapper;
    private final YouTuBeHelper youTuBeHelper;
    private final SeqService seqService;

    private final AiChatService grokAiService;

    private static final Pattern CHANNEL_ID_PATTERN = Pattern.compile("channel/(UC[a-zA-Z0-9_-]+)|c/([a-zA-Z0-9_-]+)|@([a-zA-Z0-9_-]+)");

    public YtbChannelServiceImpl(YtbChannelUserMapper channelUserMapper, YtbChannelVideoMapper videoMapper, YtbVideoSubtitlesMapper subtitlesMapper,
                                 YtbVideoSubtitlesTranslationMapper subtitlesTranslationMapper,
                                 YouTuBeHelper youTuBeHelper, SeqService seqService,
                                 @Qualifier("grokAiService") AiChatService grokAiService) {
        this.channelUserMapper = channelUserMapper;
        this.videoMapper = videoMapper;
        this.subtitlesMapper = subtitlesMapper;
        this.subtitlesTranslationMapper = subtitlesTranslationMapper;
        this.youTuBeHelper = youTuBeHelper;
        this.seqService = seqService;
        this.grokAiService = grokAiService;
        log.info("YtbChannelServiceImpl initialized with dependencies");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitChannel(String channelLinkOrName, Integer userId) {
        log.info("Submitting channel with linkOrName: {}, userId: {}", channelLinkOrName, userId);
        String channelLink;
        String channelName;

        if (channelLinkOrName.startsWith("http")) {
            // It's a link
            channelLink = WebTools.decode(channelLinkOrName);
            log.info("Decoded channel link: {}", channelLink);

            // Extract channel info using yt-dlp
            try {
                log.info("Attempting to extract channel name with yt-dlp for link: {}", channelLink);
                channelName = youTuBeHelper.extractChannelNameWithYtDlp(channelLink);
                log.info("Successfully extracted channel name: {}", channelName);
            } catch (Exception e) {
                log.error("Failed to extract channel name with yt-dlp, falling back to simple extraction. Error: {}", e.getMessage(), e);
                // Fallback to simple extraction
                channelName = extractChannelNameFromUrl(channelLink);
                log.info("Fallback extraction resulted in channel name: {}", channelName);
            }
        } else {
            // It's a name
            channelName = channelLinkOrName;
            channelLink = "https://www.youtube.com/channel/" + channelLinkOrName;
            log.info("Using name directly: {}, constructed link: {}", channelName, channelLink);
        }

        // Check if channel already exists
        log.info("Checking if channel already exists: {}", channelLink);
        YtbChannelDO existingChannel = this.getOne(new LambdaQueryWrapper<YtbChannelDO>()
                .eq(YtbChannelDO::getChannelLink, channelLink)
                .eq(YtbChannelDO::getIfValid, true));

        Long channelId;
        if (existingChannel == null) {
            log.info("Channel does not exist, creating new channel record");
            // Create new channel
            Long newId = Long.valueOf(seqService.genCommonIntSequence());
            log.info("Generated new channel ID: {}", newId);
            YtbChannelDO newChannel = new YtbChannelDO()
                    .setId(newId)
                    .setChannelLink(channelLink)
                    .setChannelName(channelName)
                    .setCreateTime(LocalDateTime.now())
                    .setStatus(ProcessStatusEnum.READY.getCode())
                    .setIfValid(true);
            this.save(newChannel);
            channelId = newChannel.getId();
            log.info("Saved new channel with ID: {}", channelId);
        } else {
            channelId = existingChannel.getId();
            log.info("Channel already exists with ID: {}", channelId);
        }

        // Check if user already subscribed to this channel
        log.info("Checking if user {} already subscribed to channel {}", userId, channelId);
        YtbChannelUserDO existingSubscription = channelUserMapper.selectOne(
                new LambdaQueryWrapper<YtbChannelUserDO>()
                        .eq(YtbChannelUserDO::getUserId, userId)
                        .eq(YtbChannelUserDO::getChannelId, channelId)
                        .eq(YtbChannelUserDO::getIfValid, true));

        if (existingSubscription == null) {
            log.info("User {} not subscribed to channel {}, creating subscription", userId, channelId);
            // Create subscription
            Long subscriptionId = Long.valueOf(seqService.genCommonIntSequence());
            log.info("Generated new subscription ID: {}", subscriptionId);
            YtbChannelUserDO newSubscription = new YtbChannelUserDO()
                    .setId(subscriptionId)
                    .setUserId(Long.valueOf(userId))
                    .setChannelId(channelId)
                    .setCreateTime(LocalDateTime.now())
                    .setIfValid(true);
            channelUserMapper.insert(newSubscription);
            log.info("Saved new subscription with ID: {}", subscriptionId);
        } else {
            log.info("User {} already subscribed to channel {}, subscription ID: {}",
                    userId, channelId, existingSubscription.getId());
        }

        // Trigger async process to fetch videos and subtitles
        log.info("Triggering async video synchronization for channel ID: {}", channelId);
        syncChannelVideos(channelId);

        return channelId;
    }

    /**
     * Extract channel name from URL using regex patterns as a fallback method
     *
     * @param url YouTube channel URL
     * @return Extracted channel name or ID
     */
    private String extractChannelNameFromUrl(String url) {
        log.info("Extracting channel name from URL using regex: {}", url);
        Matcher matcher = CHANNEL_ID_PATTERN.matcher(url);
        if (matcher.find()) {
            log.info("Found pattern match in URL");
            // Return the first non-null group
            for (int i = 1; i <= matcher.groupCount(); i++) {
                if (matcher.group(i) != null) {
                    log.info("Extracted channel name from group {}: {}", i, matcher.group(i));
                    return matcher.group(i);
                }
            }
        }

        // Fallback to simple extraction
        if (url.contains("/")) {
            String result = url.substring(url.lastIndexOf("/") + 1);
            log.info("Fallback extraction (last segment): {}", result);
            return result;
        }

        log.info("Using full URL as channel name: {}", url);
        return url;
    }

    private IPage<YtbChannelDO> getUserChannelPage(Page<YtbChannelDO> page, Integer userId) {
        log.info("Getting channel page for user: {}, page: {}, size: {}", userId, page.getCurrent(), page.getSize());

        // Get user's channel IDs
        log.info("Querying subscription records for user: {}", userId);
        List<YtbChannelUserDO> userSubscriptions = channelUserMapper.selectList(
                new LambdaQueryWrapper<YtbChannelUserDO>()
                        .eq(YtbChannelUserDO::getUserId, userId)
                        .eq(YtbChannelUserDO::getIfValid, true));

        log.info("Found {} subscriptions for user: {}", userSubscriptions.size(), userId);

        List<Long> channelIds = userSubscriptions.stream()
                .map(YtbChannelUserDO::getChannelId)
                .collect(Collectors.toList());

        if (channelIds.isEmpty()) {
            log.info("No channels found for user: {}, returning empty page", userId);
            return page.setRecords(null);
        }

        // Get channels by IDs
        log.info("Querying channel details for {} channel IDs", channelIds.size());
        IPage<YtbChannelDO> result = this.page(page, new LambdaQueryWrapper<YtbChannelDO>()
                .in(YtbChannelDO::getId, channelIds)
                .eq(YtbChannelDO::getIfValid, true)
                .orderByDesc(YtbChannelDO::getCreateTime));

        log.info("Returning page with {} channels for user {}",
                result.getRecords() != null ? result.getRecords().size() : 0, userId);
        return result;
    }

    /**
     * Synchronize channel videos by extracting all video links and saving relevant data
     * This method is scheduled to run asynchronously after channel submission
     *
     * @param channelId The ID of the channel to synchronize
     */
    @Async
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void syncChannelVideos(Long channelId) {
        log.info("Starting synchronization of videos for channel ID: {}", channelId);

        // Check if channel is already being processed by looking at the status in DB
        YtbChannelDO channel = this.getById(channelId);
        if (channel == null) {
            log.error("Channel not found with ID: {}", channelId);
            return;
        }

        // Check if channel is already in PROCESSING state
        if (ProcessStatusEnum.PROCESSING.getCode().equals(channel.getStatus())) {
            log.info("Channel ID: {} is already being processed (status: {}), skipping",
                    channelId, channel.getStatus());
            return;
        }

        try {
            log.info("Found channel: ID={}, name={}, link={}", channelId, channel.getChannelName(), channel.getChannelLink());

            updateChannelStatus("Updating channel status to PROCESSING for channel ID: {}", channelId, channel, ProcessStatusEnum.PROCESSING, "Channel status updated to PROCESSING for channel ID: {}");

            // Extract all video links from the channel
            log.info("Extracting video links from channel: {}", channel.getChannelLink());
            List<String> videoLinks = youTuBeHelper.extractAllVideoLinks(channel.getChannelLink());
            log.info("Extracted {} video links from channel: {}", videoLinks.size(), channel.getChannelName());

            int processedCount = 0;
            int totalVideos = videoLinks.size();
            int successCount = 0;
            int failureCount = 0;

            // Process each video link
            for (String videoLink : videoLinks) {
                try {
                    log.info("Processing video {}/{}: {}", processedCount + 1, totalVideos, videoLink);
                    processVideoLink(channelId, videoLink);
                    processedCount++;
                    successCount++;

                    // Log progress periodically
                    if (processedCount % 10 == 0 || processedCount == totalVideos) {
                        log.info("Processed {}/{} videos for channel ID: {}, Success: {}, Failure: {}",
                                processedCount, totalVideos, channelId, successCount, failureCount);
                    }
                } catch (Exception e) {
                    failureCount++;
                    log.error("Error processing video link: {}, Error: {}", videoLink, e.getMessage(), e);
                    log.info("Failed videos count: {}/{}", failureCount, totalVideos);
                    // Continue with next video even if current one fails
                }
            }

            // Update channel status to FINISH
            updateChannelStatus("Updating channel status to FINISH for channel ID: {}", channelId, channel, ProcessStatusEnum.FINISHED, "Channel status updated to FINISH for channel ID: {}");

            log.info("Completed synchronization of videos for channel ID: {}, Total: {}, Success: {}, Failure: {}",
                    channelId, totalVideos, successCount, failureCount);
        } catch (Exception e) {
            log.error("Failed to synchronize videos for channel ID: {}, Error: {}", channelId, e.getMessage(), e);
            // Update channel status to indicate failure
            YtbChannelDO channelToUpdate = this.getById(channelId);
            if (channelToUpdate != null) {
                updateChannelStatus("Resetting channel status to FAILED for retry, channel ID: {}",
                        channelId, channelToUpdate, ProcessStatusEnum.FAILED,
                        "Channel status reset to FAILED for channel ID: {}");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void updateChannelStatus(String previousLogFormat, Long channelId, YtbChannelDO channel, ProcessStatusEnum status, String postLogFormat) {
        // Update channel status to PROCESSING
        log.info(previousLogFormat, channelId);
        channel.setStatus(status.getCode());
        this.updateById(channel);
        log.info(postLogFormat, channelId);
    }

    /**
     * Process a single video link - extract information and save to database
     *
     * @param channelId The channel ID that owns this video
     * @param videoLink The video link to process
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    private void processVideoLink(Long channelId, String videoLink) {
        log.info("Processing video link for channel {}: {}", channelId, videoLink);

        Long videoId = null;
        try {
            // Check if video already exists
            log.info("Checking if video already exists: {}", videoLink);
            YtbChannelVideoDO existingVideo = videoMapper.selectOne(
                    new LambdaQueryWrapper<YtbChannelVideoDO>()
                            .eq(YtbChannelVideoDO::getVideoLink, videoLink)
                            .eq(YtbChannelVideoDO::getIfValid, true));

            if (existingVideo != null && existingVideo.getStatus().equals(ProcessStatusEnum.FINISHED.getCode())) {
                log.info("Video already exists with ID: {}, title: {}", existingVideo.getId(), existingVideo.getVideoTitle());
                return;
            }

            // 1. Get video title
            log.info("Fetching video title for: {}", videoLink);
            String videoTitle = youTuBeHelper.getVideoTitle(videoLink);
            log.info("Retrieved video title: {}", videoTitle);

            // 2. Save video to ytb_channel_video with PROCESSING status
            videoId = Long.valueOf(seqService.genCommonIntSequence());
            log.info("Generated new video ID: {}", videoId);

            if (existingVideo == null) {
                YtbChannelVideoDO videoDO = new YtbChannelVideoDO()
                        .setId(videoId)
                        .setChannelId(channelId)
                        .setVideoTitle(videoTitle)
                        .setVideoLink(videoLink)
                        .setStatus(ProcessStatusEnum.PROCESSING.getCode()) // Set to PROCESSING initially
                        .setCreateTime(LocalDateTime.now())
                        .setIfValid(true);

                log.info("Saving video record to database with PROCESSING status: ID={}, title={}", videoId, videoTitle);
                videoMapper.insert(videoDO);
            }

            log.info("Video record saved successfully: {}", videoId);

            // 3. Download and process subtitles
            log.info("Downloading subtitles for video: {}", videoLink);
            YtbSubtitlesResult subtitlesResult = youTuBeHelper.downloadSubtitles(videoLink);

            if (subtitlesResult != null) {
                log.info("Successfully downloaded subtitles for video: {}, type: {}", videoId, subtitlesResult.getType());
                processSubtitles(videoId, videoLink, subtitlesResult);
            } else {
                log.info("No subtitles available for video: {}", videoId);
            }

            // Update video status to FINISH after processing
            log.info("Updating video status to FINISH for video ID: {}", videoId);
            YtbChannelVideoDO videoToUpdate = videoMapper.selectById(videoId);
            if (videoToUpdate != null) {
                videoToUpdate.setStatus(ProcessStatusEnum.FINISHED.getCode());
                videoMapper.updateById(videoToUpdate);
                log.info("Video status updated to FINISH for video ID: {}", videoId);
            }

            log.info("Completed processing video: {}", videoId);
        } catch (Exception e) {
            log.error("Error processing video: {}, Error details: {}", videoLink, e.getMessage(), e);

            // Update video status to indicate failure
            if (videoId != null) {
                log.info("Setting video status to READY for retry, video ID: {}", videoId);
                YtbChannelVideoDO videoToUpdate = videoMapper.selectById(videoId);
                if (videoToUpdate != null) {
                    videoToUpdate.setStatus(ProcessStatusEnum.READY.getCode()); // Reset to READY for retry
                    videoMapper.updateById(videoToUpdate);
                    log.info("Video status reset to READY for video ID: {}", videoId);
                }
            }

            throw new ServiceException("Failed to process video: " + e.getMessage(), e);
        }
    }

    /**
     * Process subtitles for a video and save translations
     *
     * @param videoId         The video ID
     * @param videoLink       The video link
     * @param subtitlesResult The subtitles result from YouTuBeHelper
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    private void processSubtitles(Long videoId, String videoLink, YtbSubtitlesResult subtitlesResult) {
        log.info("Processing subtitles for video ID: {}, type: {}", videoId, subtitlesResult.getType());

        Long subtitlesId = null;
        try {
            // 1. Save subtitles to ytb_video_subtitles with PROCESSING status
            subtitlesId = Long.valueOf(seqService.genCommonIntSequence());
            log.info("Generated new subtitles ID: {}", subtitlesId);

            Integer subtitleType = getSubtitleType(subtitlesResult.getType());
            log.info("Mapped subtitle type from {} to {}", subtitlesResult.getType(), subtitleType);

            YtbVideoSubtitlesDO subtitlesDO = new YtbVideoSubtitlesDO()
                    .setId(subtitlesId)
                    .setVideoId(videoId)
                    .setType(subtitleType)
                    .setSubtitlesText(subtitlesResult.getScrollingSubtitles())
                    .setStatus(ProcessStatusEnum.PROCESSING.getCode()) // Set to PROCESSING initially
                    .setCreateTime(LocalDateTime.now())
                    .setIfValid(true);

            log.info("Saving subtitles record to database with PROCESSING status: ID={}, video ID={}", subtitlesId, videoId);
            subtitlesMapper.insert(subtitlesDO);
            log.info("Subtitles record saved successfully: {}", subtitlesId);

            // Process English original - no translation needed but save as a record
            log.info("Saving English original subtitles as translation record");
            saveSubtitleTranslation(
                    subtitlesId,
                    LanguageEnum.EN,
                    subtitlesResult.getScrollingSubtitles(),
                    ProcessStatusEnum.FINISHED.getCode(), // Already completed
                    false // Not from AI
            );
            log.info("English subtitles saved successfully for subtitles ID: {}", subtitlesId);

            // 2. Generate Chinese translation using AI
            try {
                log.info("Starting Chinese translation for subtitles ID: {}", subtitlesId);

                // Create translation record with PROCESSING status first
                Long translationId = saveSubtitleTranslation(
                        subtitlesId,
                        LanguageEnum.ZH_CN,
                        "", // Empty initially
                        ProcessStatusEnum.PROCESSING.getCode(), // Set to PROCESSING
                        true // From AI
                );

                // Perform the translation
                String translatedContent = translateSubtitles(videoLink, subtitlesResult, LanguageEnum.ZH_CN);
                log.info("Successfully generated Chinese translation, length: {} characters",
                        translatedContent != null ? translatedContent.length() : 0);

                // Update the translation with content and FINISH status
                updateSubtitleTranslation(
                        translationId,
                        translatedContent,
                        ProcessStatusEnum.FINISHED.getCode()
                );
                log.info("Chinese translation updated with content and FINISH status: {}", translationId);
            } catch (Exception e) {
                log.error("Error translating subtitles to Chinese for video ID: {}, Error: {}",
                        videoId, e.getMessage(), e);

                // Save translation record with READY status for later processing
                log.info("Saving Chinese translation record with READY status for subtitles ID: {}", subtitlesId);
                saveSubtitleTranslation(
                        subtitlesId,
                        LanguageEnum.ZH_CN,
                        "", // Empty translation
                        ProcessStatusEnum.READY.getCode(), // Ready for processing
                        true // Will be from AI when processed
                );
                log.info("Chinese translation record saved with READY status for subtitles ID: {}", subtitlesId);
            }

            // Update subtitles status to FINISH
            log.info("Updating subtitles status to FINISH for subtitles ID: {}", subtitlesId);
            YtbVideoSubtitlesDO subtitlesToUpdate = subtitlesMapper.selectById(subtitlesId);
            if (subtitlesToUpdate != null) {
                subtitlesToUpdate.setStatus(ProcessStatusEnum.FINISHED.getCode());
                subtitlesMapper.updateById(subtitlesToUpdate);
                log.info("Subtitles status updated to FINISH for subtitles ID: {}", subtitlesId);
            }

            log.info("Completed processing subtitles for video ID: {}", videoId);
        } catch (Exception e) {
            log.error("Error processing subtitles: video ID={}, Error: {}", videoId, e.getMessage(), e);

            // Update subtitles status to indicate failure
            if (subtitlesId != null) {
                log.info("Setting subtitles status to READY for retry, subtitles ID: {}", subtitlesId);
                YtbVideoSubtitlesDO subtitlesToUpdate = subtitlesMapper.selectById(subtitlesId);
                if (subtitlesToUpdate != null) {
                    subtitlesToUpdate.setStatus(ProcessStatusEnum.READY.getCode()); // Reset to READY for retry
                    subtitlesMapper.updateById(subtitlesToUpdate);
                    log.info("Subtitles status reset to READY for subtitles ID: {}", subtitlesId);
                }
            }

            throw new ServiceException("Failed to process subtitles: " + e.getMessage(), e);
        }
    }

    /**
     * Save a subtitle translation record
     *
     * @return The ID of the saved translation record
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    private Long saveSubtitleTranslation(Long subtitlesId, LanguageEnum language, String translation,
                                         Integer status, boolean fromAi) {
        log.info("Saving subtitle translation: subtitlesId={}, language={}, status={}, fromAi={}, translation length={}",
                subtitlesId, language.getCode(), status, fromAi, translation != null ? translation.length() : 0);

        Long translationId = Long.valueOf(seqService.genCommonIntSequence());
        log.info("Generated new translation ID: {}", translationId);

        YtbVideoSubtitlesTranslationDO translationDO = new YtbVideoSubtitlesTranslationDO()
                .setId(translationId)
                .setSubtitlesId(subtitlesId)
                .setLang(language.getCode())
                .setTranslation(translation)
                .setType(1) // Default to not proofread
                .setStatus(status)
                .setFromAi(fromAi)
                .setCreateTime(LocalDateTime.now())
                .setIfValid(true);

        log.info("Inserting translation record: ID={}, subtitlesId={}, language={}",
                translationId, subtitlesId, language.getCode());
        subtitlesTranslationMapper.insert(translationDO);
        log.info("Translation record saved successfully: {}", translationId);

        return translationId;
    }

    /**
     * Update an existing subtitle translation with content and status
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    private void updateSubtitleTranslation(Long translationId, String translation, Integer status) {
        log.info("Updating subtitle translation: ID={}, status={}, translation length={}",
                translationId, status, translation != null ? translation.length() : 0);

        YtbVideoSubtitlesTranslationDO translationDO = subtitlesTranslationMapper.selectById(translationId);
        if (translationDO != null) {
            translationDO.setTranslation(translation)
                    .setStatus(status);

            subtitlesTranslationMapper.updateById(translationDO);
            log.info("Translation record updated successfully: {}", translationId);
        } else {
            log.warn("Translation record not found for ID: {}", translationId);
        }
    }

    /**
     * Translate subtitles using AI service
     */
    @SuppressWarnings({"unchecked", "SameParameterValue"})
    private String translateSubtitles(String videoLink, YtbSubtitlesResult subtitlesResult, LanguageEnum targetLanguage) {
        log.info("Translating subtitles to language: {}, for video: {}", targetLanguage.getCode(), videoLink);

        if (subtitlesResult == null) {
            log.info("No subtitles result provided, returning empty translation");
            return "";
        }

        // Different handling based on subtitle type
        switch (subtitlesResult.getType()) {
            case SMALL_AUTO_GENERATED_RETURN_STRING:
            case SMALL_PROFESSIONAL_RETURN_STRING:
                log.info("Processing small subtitle with single-string translation, type: {}", subtitlesResult.getType());
                String subtitlesText = (String) subtitlesResult.getPendingToBeTranslatedOrRetouchedSubtitles();
                log.info("Calling AI service for small subtitle translation, text length: {}",
                        subtitlesText != null ? subtitlesText.length() : 0);
                String result = grokAiService.callForYtbAndCache(
                        videoLink,
                        subtitlesText,
                        AiPromptModeEnum.SUBTITLE_TRANSLATOR,
                        targetLanguage
                );
                log.info("AI translation completed, result length: {}", result != null ? result.length() : 0);
                return result;

            case LARGE_AUTO_GENERATED_RETURN_LIST:
            case LARGE_PROFESSIONAL_RETURN_LIST:
                log.info("Processing large subtitle with batch translation, type: {}", subtitlesResult.getType());
                List<String> subtitlesList = (List<String>) subtitlesResult.getPendingToBeTranslatedOrRetouchedSubtitles();
                log.info("Calling AI service for batch subtitle translation, list size: {}",
                        subtitlesList != null ? subtitlesList.size() : 0);
                String batchResult = grokAiService.batchCallForYtbAndCache(
                        videoLink,
                        subtitlesList,
                        AiPromptModeEnum.SUBTITLE_TRANSLATOR,
                        targetLanguage
                );
                log.info("AI batch translation completed, result length: {}", batchResult != null ? batchResult.length() : 0);
                return batchResult;

            default:
                log.warn("Unknown subtitle type: {}, returning empty translation", subtitlesResult.getType());
                return "";
        }
    }

    /**
     * Get subtitle type value from YtbSubtitlesResult type
     */
    private Integer getSubtitleType(SubtitleTypeEnum subtitleType) {
        log.info("Mapping subtitle enum type to database type: {}", subtitleType);

        // SubtitleTypeEnum contains LARGE/SMALL_AUTO_GENERATED/PROFESSIONAL types
        // We need to map to our internal type: 1-professional, 2-auto-generated
        if (subtitleType == SubtitleTypeEnum.SMALL_PROFESSIONAL_RETURN_STRING ||
                subtitleType == SubtitleTypeEnum.LARGE_PROFESSIONAL_RETURN_LIST) {
            log.info("Mapped to professional subtitle type (1)");
            return 1; // Professional
        } else {
            log.info("Mapped to auto-generated subtitle type (2)");
            return 2;
        }
    }

    @Override
    public IPage<YtbChannelVO> getUserChannelPageVO(Page<YtbChannelDO> page, Integer userId) {
        // 1. Get the channel data entities from database
        IPage<YtbChannelDO> channelPage = getUserChannelPage(page, userId);

        // 2. Map the results to VO objects
        IPage<YtbChannelVO> voPage = new Page<>(
                channelPage.getCurrent(),
                channelPage.getSize(),
                channelPage.getTotal()
        );

        // 3. Convert each DO to VO with additional data
        List<YtbChannelVO> records = ListUtils.emptyIfNull(channelPage.getRecords()).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        voPage.setRecords(records);
        return voPage;
    }

    /**
     * Converts a YtbChannelDO entity to YtbChannelVO view object
     * Adding additional computed data like total videos count
     */
    private YtbChannelVO convertToVO(YtbChannelDO channelDO) {
        // Get the total videos count for this channel
        // Create query wrapper to count videos for specific channel
        LambdaQueryWrapper<YtbChannelVideoDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(YtbChannelVideoDO::getChannelId, channelDO.getId());

        // Build and return the VO
        return YtbChannelVO.builder()
                .channelId(channelDO.getId())
                .channelName(channelDO.getChannelName())
                .status(channelDO.getStatus())
                .build();
    }

}
