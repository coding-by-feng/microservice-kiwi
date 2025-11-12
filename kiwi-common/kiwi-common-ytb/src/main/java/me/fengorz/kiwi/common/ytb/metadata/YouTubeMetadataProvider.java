package me.fengorz.kiwi.common.ytb.metadata;

import me.fengorz.kiwi.common.sdk.exception.ServiceException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Strategy interface for retrieving YouTube metadata (titles, publish time, etc.).
 */
public interface YouTubeMetadataProvider {

    /**
     * Resolve the display title for a video.
     *
     * @param videoUrlOrId canonical video URL or ID
     * @return resolved video title
     */
    String getVideoTitle(String videoUrlOrId);

    /**
     * Resolve the canonical channel name for a channel reference.
     *
     * @param channelUrlOrId channel URL, handle or ID
     * @return channel display name
     */
    String getChannelName(String channelUrlOrId) throws ServiceException;

    /**
     * List all watch URLs for the channel uploads playlist.
     *
     * @param channelUrlOrId channel URL, handle or ID
     * @return list of fully-qualified video URLs
     */
    List<String> listChannelVideoLinks(String channelUrlOrId) throws ServiceException;

    /**
     * Resolve the publication time for a video.
     *
     * @param videoUrlOrId canonical video URL or ID
     * @return publication time or {@code null} if unavailable
     */
    LocalDateTime getVideoPublishedAt(String videoUrlOrId);
}
