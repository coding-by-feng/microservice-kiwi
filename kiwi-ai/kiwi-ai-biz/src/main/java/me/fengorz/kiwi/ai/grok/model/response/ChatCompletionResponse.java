package me.fengorz.kiwi.ai.grok.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatCompletionResponse implements Serializable {

    private static final long serialVersionUID = 1136341226694243240L;

    @JsonProperty("id")
    private String id;  // e.g., "07892db8-42d9-4da6-af95-76d8e4942290"

    @JsonProperty("object")
    private String object;  // e.g., "chat.completion"

    @JsonProperty("created")
    private long created;  // e.g., 1741195161 (Unix timestamp)

    @JsonProperty("model")
    private String model;  // e.g., "grok-2-1212"

    @JsonProperty("choices")
    private List<Choice> choices;  // List of Choice objects

    @JsonProperty("usage")
    private Usage usage;  // Usage object

    @JsonProperty("system_fingerprint")
    private String systemFingerprint;  // e.g., "fp_5c0c5bd9d9"
}