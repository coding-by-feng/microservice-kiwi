package me.fengorz.kiwi.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.vo.DirectlyTranslationVO;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.annotation.log.LogMarker;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.common.sdk.enumeration.AiPromptModeEnum;
import me.fengorz.kiwi.common.sdk.enumeration.LanguageEnum;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    private final AiService aiService;

    @LogMarker(isPrintParameter = true)
    @GetMapping("/directly-translate/{language}/{originalText}")
    public R<DirectlyTranslationVO> directlyTranslate(@PathVariable(value = "originalText") String originalText,
                                                      @PathVariable(value = "language") String language) {
        return R.success(PojoBuilder.buildDirectlyTranslationVO(originalText, LanguageConvertor.convertLanguageToEnum(language),
                aiService.translate(originalText, AiPromptModeEnum.DIRECTLY_TRANSLATION, LanguageEnum.LANGUAGE_MAP.get(language))));
    }

}
