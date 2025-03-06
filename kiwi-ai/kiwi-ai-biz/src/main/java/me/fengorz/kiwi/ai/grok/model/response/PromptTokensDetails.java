package me.fengorz.kiwi.ai.grok.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromptTokensDetails {
    @JsonProperty("text_tokens")
    private int textTokens;  // e.g., 38

    @JsonProperty("audio_tokens")
    private int audioTokens;  // e.g., 0

    @JsonProperty("image_tokens")
    private int imageTokens;  // e.g., 0

    @JsonProperty("cached_tokens")
    private int cachedTokens;  // e.g., 0
}