package me.fengorz.kiwi.ai.grok.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usage {
    @JsonProperty("prompt_tokens")
    private int promptTokens;  // e.g., 38

    @JsonProperty("completion_tokens")
    private int completionTokens;  // e.g., 110

    @JsonProperty("reasoning_tokens")
    private int reasoningTokens;  // e.g., 0

    @JsonProperty("total_tokens")
    private int totalTokens;  // e.g., 148

    @JsonProperty("prompt_tokens_details")
    private PromptTokensDetails promptTokensDetails;  // Nested PromptTokensDetails object
}