package me.fengorz.kiwi.ai.api.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.fengorz.kiwi.ai.api.model.request.Message;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Choice {

    @JsonProperty("index")
    private Integer index;  // e.g., 0

    @JsonProperty("message")
    private Message message;  // Message object

    @JsonProperty("finish_reason")
    private String finishReason;  // e.g., "stop"

}