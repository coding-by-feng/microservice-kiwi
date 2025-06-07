package me.fengorz.kiwi.common.tts.service;

import me.fengorz.kiwi.common.sdk.exception.ServiceException;
import me.fengorz.kiwi.common.sdk.exception.tts.TtsException;

/**
 * @Author Kason Zhan
 * @Date 13/03/2025
 */
public interface TtsBaseService {

    default byte[] speechEnglish(String text) throws TtsException {
        throw new ServiceException("Method speechEnglish hasn't implemented yet.");
    }

    default byte[] speechChinese(String text) throws TtsException {
        throw new ServiceException("Method speechChinese hasn't implemented yet.");
    }

}
