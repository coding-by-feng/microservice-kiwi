package me.fengorz.kiwi.common.video;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

@Slf4j
class YouTuBeHelperTest {


    @Test
    void testProcessSubtitleFile() throws IOException {

        // Process the file
        String result = new YouTuBeHelper().processSubtitleFile(new File("/Users/zhanshifeng/test.vtt"));

        // Expected result after processing
        log.info(result);
    }

}