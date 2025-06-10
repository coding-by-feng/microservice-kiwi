package me.fengorz.kiwi.ai.api.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatHttpRequest {

    @JsonProperty("messages")
    private List<Message> messages;  // List of Message objects

    @JsonProperty("model")
    private String model;  // e.g., "grok-2-latest"

}