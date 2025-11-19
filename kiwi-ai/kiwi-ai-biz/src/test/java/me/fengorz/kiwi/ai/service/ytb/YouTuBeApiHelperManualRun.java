package me.fengorz.kiwi.ai.service.ytb;

import me.fengorz.kiwi.ai.api.vo.ytb.CaptionResponse;
import me.fengorz.kiwi.common.ytb.YtbSubtitlesResult;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

/** Simple manual harness to verify helper logic without Maven surefire reports. */
public class YouTuBeApiHelperManualRun {
    public static void main(String[] args) throws Exception {
        YouTubeApiService mockService = org.mockito.Mockito.mock(YouTubeApiService.class);
        YouTuBeApiHelper helper = new YouTuBeApiHelper(mockService);
        Field f1 = helper.getClass().getDeclaredField("largeSubtitlesThreshold");
        f1.setAccessible(true);
        f1.set(helper, 200);
        Field f2 = helper.getClass().getDeclaredField("subtitlesLangs");
        f2.setAccessible(true);
        f2.set(helper, "en,en-US");
        List<CaptionResponse> captions = Collections.singletonList(CaptionResponse.builder().id("c1").language("en").trackKind("standard").isAutoSynced(false).build());
        org.mockito.Mockito.when(mockService.getVideoCaptions("https://youtu.be/ZBqIh_hBKAY?si=faSySwWwY53OVPYy")).thenReturn(captions);
        String content = "WEBVTT\n\n1\n00:00:00.000 --> 00:00:00.500\nHello world\n\n";
        org.mockito.Mockito.when(mockService.downloadCaption("c1")).thenReturn(content);
        YtbSubtitlesResult result = helper.downloadSubtitles("https://youtu.be/ZBqIh_hBKAY?si=faSySwWwY53OVPYy");
        System.out.println("Result type=" + result.getType());
        System.out.println("Lang=" + result.getLangCode());
        System.out.println("Scrolling=" + result.getScrollingSubtitles());
        System.out.println("Pending=" + result.getPendingToBeTranslatedOrRetouchedSubtitles());
    }
}

