package me.fengorz.kiwi.ai.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Helper class to keep track of batch results with their original index
 */
@AllArgsConstructor
@Getter
public class BatchResult {

    private final int batchIndex;
    private final String content;

}