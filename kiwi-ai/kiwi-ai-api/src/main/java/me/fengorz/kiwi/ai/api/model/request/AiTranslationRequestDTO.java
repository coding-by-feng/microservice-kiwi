package me.fengorz.kiwi.ai.api.model.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * Request DTO for AI translation operations
 * @Author Kason Zhan
 * @Date 06/03/2025
 */
@Data
public class AiTranslationRequestDTO {
    
    @NotBlank(message = "Original text cannot be blank")
    @Size(max = 10000, message = "Text length cannot exceed 10000 characters")
    private String originalText;
    
    @NotBlank(message = "Language cannot be blank")
    private String language;
    
    @NotBlank(message = "Mode cannot be blank")
    private String mode; // directly_translation, translation_and_explanation, etc.
    
    // Optional parameters for future extensions
    private String context;
    private String style;
}