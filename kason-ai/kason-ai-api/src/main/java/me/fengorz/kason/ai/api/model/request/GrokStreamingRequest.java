package me.fengorz.kason.ai.api.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrokStreamingRequest {
    
    private List<Message> messages;
    
    private String model;
    
    @JsonProperty("stream")
    private boolean stream = false;
    
    @JsonProperty("max_tokens")
    private Integer maxTokens;
    
    private Double temperature;

    // Constructor for backward compatibility
    public GrokStreamingRequest(List<Message> messages, String model) {
        this.messages = messages;
        this.model = model;
        this.stream = false;
    }

    // Constructor with streaming support
    public GrokStreamingRequest(List<Message> messages, String model, boolean stream) {
        this.messages = messages;
        this.model = model;
        this.stream = stream;
    }
}