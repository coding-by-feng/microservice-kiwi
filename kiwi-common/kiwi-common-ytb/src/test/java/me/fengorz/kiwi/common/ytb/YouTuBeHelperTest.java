package me.fengorz.kiwi.common.ytb;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class YouTuBeHelperTest {

    // Example usage
    @Test
    void testProcessSubtitles() {
        String subtitles = "WEBVTT\n" +
                "Kind: captions\n" +
                "Language: en\n" +
                "00:00:00.040 --> 00:00:03.669\n" +
                "studio and they are from the Independent\n" +
                "00:00:03.679 --> 00:00:06.670\n" +
                "00:00:06.670 --> 00:00:06.680\n" +
                "examinations board which basically has\n" +
                "00:00:06.680 --> 00:00:08.470\n" +
                "achieved\n" +
                "00:00:08.470 --> 00:00:08.480\n" +
                "00:00:08.480 --> 00:00:13.190\n" +
                "00:00:13.190 --> 00:00:13.200\n" +
                "98.4 s pass rate in studio with me are\n" +
                "00:00:13.200 --> 00:00:17.670\n" +
                "00:00:17.670 --> 00:00:17.680\n" +
                "three top Achievers from Rodin this is a\n" +
                "00:00:17.680 --> 00:00:20.790\n" +
                "00:00:20.790 --> 00:00:20.800\n" +
                "private school and guess how much that\n" +
                "00:00:20.800 --> 00:00:22.830\n" +
                "00:00:22.830 --> 00:00:22.840\n" +
                "school has achieved in terms of its\n" +
                "00:00:22.840 --> 00:00:26.230\n" +
                "00:00:26.230 --> 00:00:26.240\n" +
                "Bachelor passes\n" +
                "00:00:26.240 --> 00:00:29.109\n" +
                "00:00:29.109 --> 00:00:29.119\n" +
                "100% yeah all right let me introduce\n" +
                "00:00:29.119 --> 00:00:33.389\n" +
                "00:00:33.389 --> 00:00:33.399\n" +
                "them one by one and seated right in\n" +
                "00:00:33.399 --> 00:00:38.990\n" +
                "00:00:38.990 --> 00:00:39.000\n" +
                "front of me is Maya oay she has achieved\n" +
                "00:00:39.000 --> 00:00:40.069\n" +
                "eight\n" +
                "00:00:40.069 --> 00:00:40.079\n" +
                "00:00:40.079 --> 00:00:44.310\n" +
                "00:00:44.310 --> 00:00:44.320\n" +
                "distinctions seated next to Maya is rid\n" +
                "00:00:44.320 --> 00:00:49.709\n" +
                "00:00:49.709 --> 00:00:49.719\n" +
                "day Romy day she's achieved seven\n" +
                "00:00:49.719 --> 00:00:51.830\n" +
                "00:00:51.830 --> 00:00:51.840\n" +
                "distinctions we then\n" +
                "00:00:51.840 --> 00:00:57.709\n" +
                "00:00:57.709 --> 00:00:57.719\n" +
                "have Mia so there's a Maya and a Mia Mia\n" +
                "00:00:57.719 --> 00:01:01.910\n" +
                "00:01:01.910 --> 00:01:01.920\n" +
                "Lis she is the top achiever I am told\n" +
                "00:01:01.920 --> 00:01:05.350\n" +
                "00:01:05.350 --> 00:01:05.360\n" +
                "with about eight distinctions I'm not\n" +
                "00:01:05.360 --> 00:01:07.590\n" +
                "00:01:07.590 --> 00:01:07.600\n" +
                "going to ask you guys boring questions\n" +
                "00:01:07.600 --> 00:01:10.910\n" +
                "00:01:10.910 --> 00:01:10.920\n" +
                "oh oh I can't forget how can I do this\n" +
                "00:01:10.920 --> 00:01:14.070\n" +
                "00:01:14.070 --> 00:01:14.080\n" +
                "the head of senior School Annabelle\n" +
                "00:01:14.080 --> 00:01:17.109\n" +
                "00:01:17.109 --> 00:01:17.119\n" +
                "Roberts welcome thank to all of you such\n" +
                "00:01:17.119 --> 00:01:19.830\n" +
                "00:01:19.830 --> 00:01:19.840\n" +
                "a pleasure to have you here and so let's\n" +
                "00:01:19.840 --> 00:01:23.149\n" +
                "00:01:23.149 --> 00:01:23.159\n" +
                "begin this conversation and Mia let's\n" +
                "00:01:23.159 --> 00:01:25.630\n" +
                "00:01:25.630 --> 00:01:25.640\n" +
                "start with you I don't know what to ask\n" +
                "00:01:25.640 --> 00:01:27.910\n" +
                "00:01:27.910 --> 00:01:27.920\n" +
                "you actually because I don't want to ask\n" +
                "00:01:27.920 --> 00:01:30.910\n" +
                "00:01:30.910 --> 00:01:30.920\n" +
                "you boring questions so h ah how did you\n" +
                "00:01:30.920 --> 00:01:31.990\n" +
                "do\n" +
                "00:01:31.990 --> 00:01:32.000\n" +
                "00:01:32.000 --> 00:01:34.950\n" +
                "00:01:34.950 --> 00:01:34.960\n" +
                "this well it's a long story it started";

//        String retouched = VttFileCleaner.cleanTimestamp(Arrays.stream(subtitles.split("\n")).collect(Collectors.toList()));
//        System.out.println(retouched);
    }

}