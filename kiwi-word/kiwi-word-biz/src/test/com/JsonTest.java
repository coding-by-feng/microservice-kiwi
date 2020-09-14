package com;

import me.fengorz.kiwi.word.api.entity.WordFetchQueueDO;

/**
 * @Author zhanshifeng
 * @Date 2019/10/31 10:21 AM
 */
public class JsonTest {
    public static void main(String[] args) {
        WordFetchQueueDO wordFetchQueue = new WordFetchQueueDO();
        wordFetchQueue.setFetchPriority(100);
        wordFetchQueue.setFetchStatus(0);
        wordFetchQueue.setWordName("test");
    }
}
