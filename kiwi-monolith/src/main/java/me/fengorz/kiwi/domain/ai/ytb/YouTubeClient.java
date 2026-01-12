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
package me.fengorz.kiwi.domain.ai.ytb;

import java.io.InputStream;
import java.time.LocalDateTime;

/**
 * YouTube Client Interface
 * Common abstraction for accessing YouTube resources either via yt-dlp CLI or YouTube Data API.
 *
 * @author codingByFeng
 */
public interface YouTubeClient {

    /**
     * Download raw video content as InputStream.
     */
    InputStream downloadVideo(String videoUrl);

    /**
     * Download (or fetch) subtitles for a video URL.
     */
    YtbSubtitlesResult downloadSubtitles(String videoUrl);

    /**
     * Get the human-readable title for a video.
     */
    String getVideoTitle(String videoUrl);

    /**
     * Attempt to retrieve video publish datetime.
     */
    default LocalDateTime getVideoPublishedAt(String videoUrl) {
        return null;
    }
}
