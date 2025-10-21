package me.fengorz.kiwi.ai.service.ytb;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.entity.*;
import me.fengorz.kiwi.ai.api.vo.ytb.*;
import me.fengorz.kiwi.ai.service.AiChatService;
import me.fengorz.kiwi.ai.service.ytb.mapper.*;
import me.fengorz.kiwi.common.db.service.SeqService;
import me.fengorz.kiwi.common.sdk.enumeration.LanguageEnum;
import me.fengorz.kiwi.common.sdk.enumeration.ProcessStatusEnum;
import me.fengorz.kiwi.common.sdk.exception.ServiceException;
import me.fengorz.kiwi.common.sdk.web.WebTools;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service("ytbChannelServiceV2")
public class YtbChannelServiceImplV2 extends ServiceImpl<YtbChannelMapper, YtbChannelDO> implements YtbChannelService {

    private final YtbChannelUserMapper channelUserMapper;
    private final YtbChannelVideoMapper videoMapper;
    private final YtbVideoSubtitlesMapper subtitlesMapper;
    private final YtbVideoSubtitlesTranslationMapper subtitlesTranslationMapper;
    private final YouTubeApiService youTubeApiService;
    private final SeqService seqService;

    public YtbChannelServiceImplV2(YtbChannelUserMapper channelUserMapper,
                                   YtbChannelVideoMapper videoMapper,
                                   YtbVideoSubtitlesMapper subtitlesMapper,
                                   YtbVideoSubtitlesTranslationMapper subtitlesTranslationMapper,
                                   YouTubeApiService youTubeApiService,
                                   SeqService seqService,
                                   @Qualifier("grokAiService") AiChatService grokAiService) {
        this.channelUserMapper = channelUserMapper;
        this.videoMapper = videoMapper;
        this.subtitlesMapper = subtitlesMapper;
        this.subtitlesTranslationMapper = subtitlesTranslationMapper;
        this.youTubeApiService = youTubeApiService;
        this.seqService = seqService;
        log.info("YtbChannelServiceImplV2 initialized with YouTube API service");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitChannel(String channelLinkOrName, Integer userId) {
        log.info("Submitting channel with linkOrName: {}, userId: {}", channelLinkOrName, userId);

        String channelLink;
        String channelName;
        String channelId;

        try {
            // Decode the input
            String decodedInput = channelLinkOrName.startsWith("http") ?
                    WebTools.decode(channelLinkOrName) : channelLinkOrName;

            log.info("Decoded input: {}", decodedInput);

            // Get channel details using YouTube API
            ChannelDetailsResponse channelDetails = youTubeApiService.getChannelDetails(decodedInput);

            channelId = channelDetails.getChannelId();
            channelName = channelDetails.getTitle();
            channelLink = "https://www.youtube.com/channel/" + channelId;

            log.info("Retrieved channel details - ID: {}, Name: {}, Link: {}", channelId, channelName, channelLink);

        } catch (Exception e) {
            log.error("Failed to get channel details using YouTube API: {}", e.getMessage(), e);
            throw new ServiceException("Failed to retrieve channel information: " + e.getMessage(), e);
        }

        // Check if channel already exists
        log.info("Checking if channel already exists: {}", channelLink);
        YtbChannelDO existingChannel = this.getOne(new LambdaQueryWrapper<YtbChannelDO>()
                .eq(YtbChannelDO::getChannelLink, channelLink)
                .eq(YtbChannelDO::getIfValid, true));

        Long finalChannelId;
        if (existingChannel == null) {
            log.info("Channel does not exist, creating new channel record");
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
            finalChannelId = newChannel.getId();
            log.info("Saved new channel with ID: {}", finalChannelId);
        } else {
            finalChannelId = existingChannel.getId();
            log.info("Channel already exists with ID: {}", finalChannelId);
        }

        // Handle user subscription
        handleUserSubscription(userId, finalChannelId);

        return finalChannelId;
    }

    @Override
    @Async
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void syncChannelVideos(Long channelId) {
        log.info("Starting synchronization of videos for channel ID: {}", channelId);

        YtbChannelDO channel = this.getById(channelId);
        if (channel == null) {
            log.error("Channel not found with ID: {}", channelId);
            return;
        }

        // Check if already processing
        if (ProcessStatusEnum.PROCESSING.getCode().equals(channel.getStatus()) &&
                channel.getCreateTime().isAfter(LocalDateTime.now().minusMinutes(10))) {
            log.info("Channel ID: {} is already being processed, skipping", channelId);
            return;
        }

        try {
            log.info("Found channel: ID={}, name={}, link={}", channelId, channel.getChannelName(), channel.getChannelLink());

            updateChannelStatus("Updating channel status to PROCESSING for channel ID: {}", channelId, channel,
                    ProcessStatusEnum.PROCESSING, "Channel status updated to PROCESSING for channel ID: {}");

            // Get all videos from channel using YouTube API
            log.info("Extracting video links from channel: {}", channel.getChannelLink());
            List<ChannelVideoResponse> videos = youTubeApiService.getChannelVideos(channel.getChannelLink());
            log.info("Retrieved {} videos from channel: {}", videos.size(), channel.getChannelName());

            int processedCount = 0;
            int successCount = 0;
            int failureCount = 0;

            // Process each video
            for (ChannelVideoResponse video : videos) {
                try {
                    String videoUrl = "https://www.youtube.com/watch?v=" + video.getVideoId();
                    log.info("Processing video {}/{}: {}", processedCount + 1, videos.size(), videoUrl);

                    Optional.ofNullable(processVideoWithApi(channelId, video))
                            .ifPresent(videoId -> {
                                // Update video status to FINISH after processing
                                log.info("Updating video status to FINISH for video ID: {}", videoId);
                                YtbChannelVideoDO videoToUpdate = videoMapper.selectById(videoId);
                                if (videoToUpdate != null) {
                                    videoToUpdate.setStatus(ProcessStatusEnum.FINISHED.getCode());
                                    videoMapper.updateById(videoToUpdate);
                                    log.info("Video status updated to FINISH for video ID: {}", videoId);
                                }
                            });

                    processedCount++;
                    successCount++;

                    // Log progress periodically
                    if (processedCount % 10 == 0 || processedCount == videos.size()) {
                        log.info("Processed {}/{} videos for channel ID: {}, Success: {}, Failure: {}",
                                processedCount, videos.size(), channelId, successCount, failureCount);
                    }
                } catch (Exception e) {
                    failureCount++;
                    log.error("Error processing video: {}, Error: {}", video.getVideoId(), e.getMessage(), e);
                }
            }

            // Update channel status to FINISH
            updateChannelStatus("Updating channel status to FINISH for channel ID: {}", channelId, channel,
                    ProcessStatusEnum.FINISHED, "Channel status updated to FINISH for channel ID: {}");

            log.info("Completed synchronization of videos for channel ID: {}, Total: {}, Success: {}, Failure: {}",
                    channelId, videos.size(), successCount, failureCount);

        } catch (Exception e) {
            log.error("Failed to synchronize videos for channel ID: {}, Error: {}", channelId, e.getMessage(), e);
            YtbChannelDO channelToUpdate = this.getById(channelId);
            if (channelToUpdate != null) {
                updateChannelStatus("Resetting channel status to FAILED for retry, channel ID: {}",
                        channelId, channelToUpdate, ProcessStatusEnum.FAILED,
                        "Channel status reset to FAILED for channel ID: {}");
            }
        }
    }

    // Private helper methods

    private void handleUserSubscription(Integer userId, Long channelId) {
        log.info("Checking if user {} already subscribed to channel {}", userId, channelId);
        YtbChannelUserDO existingSubscription = channelUserMapper.selectOne(
                new LambdaQueryWrapper<YtbChannelUserDO>()
                        .eq(YtbChannelUserDO::getUserId, userId)
                        .eq(YtbChannelUserDO::getChannelId, channelId)
                        .eq(YtbChannelUserDO::getIfValid, true));

        if (existingSubscription == null) {
            log.info("User {} not subscribed to channel {}, creating subscription", userId, channelId);
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
            log.info("User {} already subscribed to channel {}", userId, channelId);
        }
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    private Long processVideoWithApi(Long channelId, ChannelVideoResponse video) {
        String videoUrl = "https://www.youtube.com/watch?v=" + video.getVideoId();
        log.info("Processing video for channel {}: {}", channelId, videoUrl);

        try {
            // Check if video already exists
            YtbChannelVideoDO existingVideo = videoMapper.selectOne(
                    new LambdaQueryWrapper<YtbChannelVideoDO>()
                            .eq(YtbChannelVideoDO::getVideoLink, videoUrl)
                            .eq(YtbChannelVideoDO::getIfValid, true));

            if (existingVideo != null && existingVideo.getStatus().equals(ProcessStatusEnum.FINISHED.getCode())) {
                log.info("Video already exists and is finished: {}", existingVideo.getId());
                return null;
            }

            // Get additional video details
            VideoDetailsResponse videoDetails = youTubeApiService.getVideoDetails(video.getVideoId());

            // Save video to database
            Long videoId = Long.valueOf(seqService.genCommonIntSequence());

            if (existingVideo == null) {
                YtbChannelVideoDO videoDO = new YtbChannelVideoDO()
                        .setId(videoId)
                        .setChannelId(channelId)
                        .setVideoTitle(videoDetails.getTitle())
                        .setVideoLink(videoUrl)
                        .setStatus(ProcessStatusEnum.PROCESSING.getCode())
                        .setCreateTime(LocalDateTime.now())
                        .setIfValid(true);

                videoMapper.insert(videoDO);
                log.info("Saved video record: {}", videoId);
            }

            // Process captions
            processVideoCaptions(videoId, videoUrl);

            return videoId;

        } catch (Exception e) {
            log.error("Error processing video: {}", videoUrl, e);
            throw new ServiceException("Failed to process video: " + e.getMessage(), e);
        }
    }

    private void processVideoCaptions(Long videoId, String videoUrl) {
        log.info("Processing captions for video ID: {}", videoId);

        try {
            List<CaptionResponse> captions = youTubeApiService.getVideoCaptions(videoUrl);

            if (captions.isEmpty()) {
                log.info("No captions available for video: {}", videoId);
                return;
            }

            // Find English captions (prefer manual over auto-generated)
            CaptionResponse selectedCaption = captions.stream()
                    .filter(c -> c.getLanguage().startsWith("en") && !c.getIsAutoSynced())
                    .findFirst()
                    .orElse(captions.stream()
                            .filter(c -> c.getLanguage().startsWith("en"))
                            .findFirst()
                            .orElse(captions.get(0)));

            log.info("Selected caption: language={}, isAutoSynced={}",
                    selectedCaption.getLanguage(), selectedCaption.getIsAutoSynced());

            // Save subtitle record
            Long subtitlesId = Long.valueOf(seqService.genCommonIntSequence());

            Integer subtitleType = selectedCaption.getIsAutoSynced() ? 2 : 1; // 1=professional, 2=auto-generated

            YtbVideoSubtitlesDO subtitlesDO = new YtbVideoSubtitlesDO()
                    .setId(subtitlesId)
                    .setVideoId(videoId)
                    .setType(subtitleType)
                    .setSubtitlesText("Caption available - ID: " + selectedCaption.getId()) // Placeholder
                    .setStatus(ProcessStatusEnum.PROCESSING.getCode())
                    .setCreateTime(LocalDateTime.now())
                    .setIfValid(true);

            subtitlesMapper.insert(subtitlesDO);
            log.info("Saved subtitles record: {}", subtitlesId);

            // Process translations
            processSubtitleTranslations(subtitlesId, videoUrl, selectedCaption);

            // Update subtitles status to FINISH
            subtitlesDO.setStatus(ProcessStatusEnum.FINISHED.getCode());
            subtitlesMapper.updateById(subtitlesDO);

        } catch (Exception e) {
            log.error("Error processing captions for video: {}", videoId, e);
        }
    }

    private void processSubtitleTranslations(Long subtitlesId, String videoUrl, CaptionResponse caption) {
        // Save English original
        saveSubtitleTranslation(
                subtitlesId,
                LanguageEnum.EN,
                "Original caption - ID: " + caption.getId(),
                ProcessStatusEnum.FINISHED.getCode(),
                false
        );

        // Generate Chinese translation
        try {
            Long translationId = saveSubtitleTranslation(
                    subtitlesId,
                    LanguageEnum.ZH_CN,
                    "",
                    ProcessStatusEnum.PROCESSING.getCode(),
                    true
            );

            // For now, we'll use a placeholder since we can't download caption content without OAuth
            String translatedContent = "Translation requires OAuth authentication for caption download";

            updateSubtitleTranslation(translationId, translatedContent, ProcessStatusEnum.FINISHED.getCode());

        } catch (Exception e) {
            log.error("Error creating Chinese translation: {}", e.getMessage(), e);
            saveSubtitleTranslation(
                    subtitlesId,
                    LanguageEnum.ZH_CN,
                    "",
                    ProcessStatusEnum.READY.getCode(),
                    true
            );
        }
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    private Long saveSubtitleTranslation(Long subtitlesId, LanguageEnum language, String translation,
                                         Integer status, boolean fromAi) {
        Long translationId = Long.valueOf(seqService.genCommonIntSequence());

        YtbVideoSubtitlesTranslationDO translationDO = new YtbVideoSubtitlesTranslationDO()
                .setId(translationId)
                .setSubtitlesId(subtitlesId)
                .setLang(language.getCode())
                .setTranslation(translation)
                .setType(1)
                .setStatus(status)
                .setFromAi(fromAi)
                .setCreateTime(LocalDateTime.now())
                .setIfValid(true);

        subtitlesTranslationMapper.insert(translationDO);
        return translationId;
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    private void updateSubtitleTranslation(Long translationId, String translation, Integer status) {
        YtbVideoSubtitlesTranslationDO translationDO = subtitlesTranslationMapper.selectById(translationId);
        if (translationDO != null) {
            translationDO.setTranslation(translation).setStatus(status);
            subtitlesTranslationMapper.updateById(translationDO);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void updateChannelStatus(String previousLogFormat, Long channelId, YtbChannelDO channel,
                                    ProcessStatusEnum status, String postLogFormat) {
        log.info(previousLogFormat, channelId);
        channel.setStatus(status.getCode());
        this.updateById(channel);
        log.info(postLogFormat, channelId);
    }

    @Override
    public IPage<YtbChannelVO> getUserChannelPageVO(Page<YtbChannelDO> page, Integer userId) {
        IPage<YtbChannelDO> channelPage = getUserChannelPage(page, userId);

        IPage<YtbChannelVO> voPage = new Page<>(
                channelPage.getCurrent(),
                channelPage.getSize(),
                channelPage.getTotal()
        );

        List<YtbChannelVO> records = ListUtils.emptyIfNull(channelPage.getRecords()).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        voPage.setRecords(records);
        return voPage;
    }

    private IPage<YtbChannelDO> getUserChannelPage(Page<YtbChannelDO> page, Integer userId) {
        List<YtbChannelUserDO> userSubscriptions = channelUserMapper.selectList(
                new LambdaQueryWrapper<YtbChannelUserDO>()
                        .eq(YtbChannelUserDO::getUserId, userId)
                        .eq(YtbChannelUserDO::getIfValid, true));

        List<Long> channelIds = userSubscriptions.stream()
                .map(YtbChannelUserDO::getChannelId)
                .collect(Collectors.toList());

        if (channelIds.isEmpty()) {
            return page.setRecords(null);
        }

        return this.page(page, new LambdaQueryWrapper<YtbChannelDO>()
                .in(YtbChannelDO::getId, channelIds)
                .eq(YtbChannelDO::getIfValid, true)
                .orderByDesc(YtbChannelDO::getCreateTime));
    }

    private YtbChannelVO convertToVO(YtbChannelDO channelDO) {
        return YtbChannelVO.builder()
                .channelId(channelDO.getId())
                .channelName(channelDO.getChannelName())
                .status(channelDO.getStatus())
                .build();
    }
}