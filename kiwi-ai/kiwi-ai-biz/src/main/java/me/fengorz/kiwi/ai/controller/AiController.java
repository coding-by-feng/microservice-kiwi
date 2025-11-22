package me.fengorz.kiwi.ai.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.entity.AiCallHistoryDO;
import me.fengorz.kiwi.ai.api.enums.HistoryFilterEnum;
import me.fengorz.kiwi.ai.api.model.request.Favorite;
import me.fengorz.kiwi.ai.api.vo.AiCallHistoryVO;
import me.fengorz.kiwi.ai.api.vo.AiResponseVO;
import me.fengorz.kiwi.ai.service.AiChatService;
import me.fengorz.kiwi.ai.service.history.AiCallHistoryService;
import me.fengorz.kiwi.ai.util.LanguageConvertor;
import me.fengorz.kiwi.ai.util.PojoBuilder;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.annotation.log.LogMarker;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.common.sdk.enumeration.AiPromptModeEnum;
import me.fengorz.kiwi.common.sdk.enumeration.LanguageEnum;
import me.fengorz.kiwi.common.sdk.web.WebTools;
import me.fengorz.kiwi.common.sdk.web.security.SecurityUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @Author Kason Zhan
 * @Date 06/03/2025
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class AiController extends BaseController {

    private final AiChatService aiService;
    private final AiCallHistoryService aiCallHistoryService;

    @LogMarker(isPrintParameter = true)
    @GetMapping("/directly-translation/{language}/{originalText}")
    public R<AiResponseVO> directlyTranslation(@PathVariable(value = "originalText") String originalText,
            @PathVariable(value = "language") String language) {
        String decodedOriginalText = WebTools.decode(originalText);
        return R.success(PojoBuilder.buildDirectlyTranslationVO(decodedOriginalText,
                LanguageConvertor.convertLanguageToEnum(language),
                aiService.call(decodedOriginalText, AiPromptModeEnum.DIRECTLY_TRANSLATION,
                        LanguageEnum.LANGUAGE_MAP.get(language))));
    }

    @LogMarker(isPrintParameter = true)
    @GetMapping("/translation-and-explanation/{language}/{originalText}")
    public R<AiResponseVO> translationAndExplanation(@PathVariable(value = "originalText") String originalText,
            @PathVariable(value = "language") String language) {
        String decodedOriginalText = WebTools.decode(originalText);
        return R.success(PojoBuilder.buildDirectlyTranslationVO(decodedOriginalText,
                LanguageConvertor.convertLanguageToEnum(language),
                aiService.call(decodedOriginalText, AiPromptModeEnum.TRANSLATION_AND_EXPLANATION,
                        LanguageEnum.LANGUAGE_MAP.get(language))));
    }

    @LogMarker(isPrintParameter = true)
    @GetMapping("/grammar-explanation/{language}/{originalText}")
    public R<AiResponseVO> grammarExplanation(@PathVariable(value = "originalText") String originalText,
            @PathVariable(value = "language") String language) {
        String decodedOriginalText = WebTools.decode(originalText);
        return R.success(PojoBuilder.buildDirectlyTranslationVO(decodedOriginalText,
                LanguageConvertor.convertLanguageToEnum(language),
                aiService.call(decodedOriginalText, AiPromptModeEnum.GRAMMAR_EXPLANATION,
                        LanguageEnum.LANGUAGE_MAP.get(language))));
    }

    @LogMarker(isPrintParameter = true)
    @GetMapping("/grammar-correction/{language}/{originalText}")
    public R<AiResponseVO> grammarCorrection(@PathVariable(value = "originalText") String originalText,
            @PathVariable(value = "language") String language) {
        String decodedOriginalText = WebTools.decode(originalText);
        return R.success(PojoBuilder.buildDirectlyTranslationVO(decodedOriginalText,
                LanguageConvertor.convertLanguageToEnum(language),
                aiService.call(decodedOriginalText, AiPromptModeEnum.GRAMMAR_CORRECTION,
                        LanguageEnum.LANGUAGE_MAP.get(language))));
    }

    @LogMarker(isPrintParameter = true)
    @GetMapping("/vocabulary-explanation/{language}/{originalText}")
    public R<AiResponseVO> vocabularyExplanation(@PathVariable(value = "originalText") String originalText,
            @PathVariable(value = "language") String language) {
        String decodedOriginalText = WebTools.decode(originalText);
        return R.success(PojoBuilder.buildDirectlyTranslationVO(decodedOriginalText,
                LanguageConvertor.convertLanguageToEnum(language),
                aiService.call(decodedOriginalText, AiPromptModeEnum.VOCABULARY_EXPLANATION,
                        LanguageEnum.LANGUAGE_MAP.get(language))));
    }

    @LogMarker(isPrintParameter = true)
    @GetMapping("/synonym/{language}/{originalText}")
    public R<AiResponseVO> synonym(@PathVariable(value = "originalText") String originalText,
            @PathVariable(value = "language") String language) {
        String decodedOriginalText = WebTools.decode(originalText);
        return R.success(PojoBuilder.buildDirectlyTranslationVO(decodedOriginalText,
                LanguageConvertor.convertLanguageToEnum(language),
                aiService.call(decodedOriginalText, AiPromptModeEnum.SYNONYM,
                        LanguageEnum.LANGUAGE_MAP.get(language))));
    }

    @LogMarker(isPrintParameter = true)
    @GetMapping("/antonym/{language}/{originalText}")
    public R<AiResponseVO> antonym(@PathVariable(value = "originalText") String originalText,
            @PathVariable(value = "language") String language) {
        String decodedOriginalText = WebTools.decode(originalText);
        return R.success(PojoBuilder.buildDirectlyTranslationVO(decodedOriginalText,
                LanguageConvertor.convertLanguageToEnum(language),
                aiService.call(decodedOriginalText, AiPromptModeEnum.ANTONYM,
                        LanguageEnum.LANGUAGE_MAP.get(language))));
    }

    /**
     * Get user's AI call history with pagination
     *
     * @param current Page number (1-based indexing)
     * @param size    Number of items per page
     * @param filter  Filter type: "normal" (default), "archived", or "all"
     * @return Paginated list of user's AI call history ordered by timestamp desc
     */
    @LogMarker
    @GetMapping("/history")
    public R<IPage<AiCallHistoryVO>> getCallHistory(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "filter", defaultValue = "normal") String filter) {

        // Validate pagination parameters
        if (current < 1) {
            return R.failed("Page number must be greater than 0");
        }

        if (size < 1 || size > 100) {
            return R.failed("Page size must be between 1 and 100");
        }

        try {
            // Create pagination object
            Page<AiCallHistoryDO> page = new Page<>(current, size);

            // Get current user and retrieve their call history
            Integer userId = getCurrentUserId();
            if (userId == null) {
                return R.failed("User not authenticated");
            }

            // Convert filter string to enum
            HistoryFilterEnum filterEnum = HistoryFilterEnum.fromCode(filter);

            IPage<AiCallHistoryVO> resultPage = aiCallHistoryService.getUserCallHistory(page, Long.valueOf(userId),
                    filterEnum);

            return R.success(resultPage);
        } catch (Exception e) {
            log.error("Error retrieving user call history: {}", e.getMessage(), e);
            return R.failed("Failed to retrieve call history");
        }
    }

    /**
     * Archive AI call history item by ID
     *
     * @param id History item ID
     * @return Success or failure response
     */
    @LogMarker
    @PutMapping("/history/{id}/archive")
    public R<String> archiveCallHistory(@PathVariable("id") Long id) {
        try {
            // Get current user ID
            Integer userId = getCurrentUserId();
            if (userId == null) {
                return R.failed("User not authenticated");
            }

            // Validate ID parameter
            if (id == null || id <= 0) {
                return R.failed("Invalid history item ID");
            }

            // Archive the history item
            boolean result = aiCallHistoryService.archiveCallHistory(id, Long.valueOf(userId));

            if (result) {
                return R.success("History item archived successfully");
            } else {
                return R.failed(
                        "Failed to archive history item. Item may not exist or doesn't belong to current user.");
            }
        } catch (Exception e) {
            log.error("Error archiving history item with ID {}: {}", id, e.getMessage(), e);
            return R.failed("Failed to archive history item");
        }
    }

    /**
     * Delete AI call history item by ID
     *
     * @param id History item ID
     * @return Success or failure response
     */
    @LogMarker
    @DeleteMapping("/history/{id}")
    public R<String> deleteCallHistory(@PathVariable("id") Long id) {
        try {
            // Get current user ID
            Integer userId = getCurrentUserId();
            if (userId == null) {
                return R.failed("User not authenticated");
            }

            // Validate ID parameter
            if (id == null || id <= 0) {
                return R.failed("Invalid history item ID");
            }

            // Delete the history item
            boolean result = aiCallHistoryService.deleteCallHistory(id, Long.valueOf(userId));

            if (result) {
                return R.success("History item deleted successfully");
            } else {
                return R.failed("Failed to delete history item. Item may not exist or doesn't belong to current user.");
            }
        } catch (Exception e) {
            log.error("Error deleting history item with ID {}: {}", id, e.getMessage(), e);
            return R.failed("Failed to delete history item");
        }
    }

    /**
     * Set favorite status for an AI call history item
     *
     * @param id       History item ID
     * @param favorite Favorite request body
     * @return Success or failure response
     */
    @LogMarker
    @PutMapping("/history/{id}/favorite")
    public R<String> setFavorite(@PathVariable("id") Long id, @RequestBody Favorite favorite) {
        try {
            Integer userId = getCurrentUserId();
            if (userId == null) {
                return R.failed("User not authenticated");
            }
            if (id == null || id <= 0) {
                return R.failed("Invalid history item ID");
            }
            if (favorite == null || favorite.getFavorite() == null) {
                return R.failed("Favorite status is required");
            }
            boolean result = aiCallHistoryService.setFavoriteStatus(id, Long.valueOf(userId), favorite.getFavorite());
            if (result) {
                return R.success(favorite.getFavorite() ? "Marked as favorite" : "Unmarked as favorite");
            }
            return R.failed("Failed to update favorite status. Item may not exist or doesn't belong to current user.");
        } catch (Exception e) {
            log.error("Error setting favorite status for ID {}: {}", id, e.getMessage(), e);
            return R.failed("Failed to update favorite status");
        }
    }

    /**
     * Get current user ID from security context
     */
    private static Integer getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }
}