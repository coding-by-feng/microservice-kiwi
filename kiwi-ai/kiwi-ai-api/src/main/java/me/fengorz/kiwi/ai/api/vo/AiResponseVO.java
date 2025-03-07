package me.fengorz.kiwi.ai.api.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author Kason Zhan
 * @Date 06/03/2025
 */
@Data
public class AiResponseVO implements Serializable {

    private static final long serialVersionUID = 8399270332034259225L;

    private String originalText;
    private String languageCode;
    private String responseText;

}
