package me.fengorz.kiwi.test;

import com.github.fppt.jedismock.RedisServer;

import java.io.IOException;

public class EmbeddedRedisInitiator {

    public static RedisServer redisServer;

    public static void setUp() throws IOException {
        redisServer = RedisServer
                .newRedisServer()
                .start();
    }

    public static void tearDown() throws IOException {
        // Stop Redis server
        if (redisServer != null) {
            redisServer.stop();
        }
    }

}