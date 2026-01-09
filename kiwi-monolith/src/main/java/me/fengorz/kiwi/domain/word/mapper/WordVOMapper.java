/*
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.fengorz.kiwi.domain.word.mapper;

import me.fengorz.kiwi.domain.word.dto.FuzzyQueryResultDTO;
import me.fengorz.kiwi.domain.word.dto.WordCreateRequest;
import me.fengorz.kiwi.domain.word.dto.WordStarListRequest;
import me.fengorz.kiwi.domain.word.entity.Paraphrase;
import me.fengorz.kiwi.domain.word.entity.Pronunciation;
import me.fengorz.kiwi.domain.word.entity.WordMain;
import me.fengorz.kiwi.domain.word.entity.WordStarList;
import me.fengorz.kiwi.domain.word.vo.ParaphraseVO;
import me.fengorz.kiwi.domain.word.vo.PronunciationVO;
import me.fengorz.kiwi.domain.word.vo.WordMainVO;
import me.fengorz.kiwi.domain.word.vo.WordStarListVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between entities and VOs/DTOs
 *
 * @author codingByFeng
 */
@Component
public class WordVOMapper {

    /**
     * Convert WordMain entity to WordMainVO
     */
    public WordMainVO toWordMainVO(WordMain entity) {
        if (entity == null) {
            return null;
        }
        return new WordMainVO()
                .setWordId(entity.getWordId())
                .setWordName(entity.getWordName())
                .setInfoType(entity.getInfoType())
                .setInTime(entity.getInTime())
                .setLastUpdateTime(entity.getLastUpdateTime());
    }

    /**
     * Convert list of WordMain entities to list of WordMainVOs
     */
    public List<WordMainVO> toWordMainVOList(List<WordMain> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toWordMainVO)
                .collect(Collectors.toList());
    }

    /**
     * Convert WordCreateRequest to WordMain entity
     */
    public WordMain toWordMain(WordCreateRequest request) {
        if (request == null) {
            return null;
        }
        return WordMain.builder()
                .wordName(request.getWordName())
                .infoType(request.getInfoType() != null ? request.getInfoType() : 1)
                .build();
    }

    /**
     * Convert Paraphrase entity to ParaphraseVO
     */
    public ParaphraseVO toParaphraseVO(Paraphrase entity) {
        if (entity == null) {
            return null;
        }
        return new ParaphraseVO()
                .setParaphraseId(entity.getParaphraseId())
                .setWordId(entity.getWordId())
                .setCharacterId(entity.getCharacterId())
                .setCodes(entity.getCodes())
                .setParaphraseEnglish(entity.getParaphraseEnglish())
                .setParaphraseEnglishTranslate(entity.getParaphraseEnglishTranslate())
                .setMeaningChinese(entity.getMeaningChinese());
    }

    /**
     * Convert list of Paraphrase entities to list of ParaphraseVOs
     */
    public List<ParaphraseVO> toParaphraseVOList(List<Paraphrase> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toParaphraseVO)
                .collect(Collectors.toList());
    }

    /**
     * Convert WordStarList entity to WordStarListVO
     */
    public WordStarListVO toWordStarListVO(WordStarList entity) {
        if (entity == null) {
            return null;
        }
        return new WordStarListVO()
                .setId(entity.getId())
                .setListName(entity.getListName())
                .setRemark(entity.getRemark())
                .setOwner(entity.getOwner())
                .setSort(entity.getSort())
                .setCreateTime(entity.getCreateTime());
    }

    /**
     * Convert list of WordStarList entities to list of WordStarListVOs
     */
    public List<WordStarListVO> toWordStarListVOList(List<WordStarList> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toWordStarListVO)
                .collect(Collectors.toList());
    }

    /**
     * Convert WordStarListRequest to WordStarList entity
     */
    public WordStarList toWordStarList(WordStarListRequest request) {
        if (request == null) {
            return null;
        }
        return WordStarList.builder()
                .listName(request.getListName())
                .remark(request.getRemark())
                .sort(request.getSort())
                .build();
    }

    /**
     * Convert FuzzyQueryResult to FuzzyQueryResultDTO
     */
    public FuzzyQueryResultDTO toFuzzyQueryResultDTO(String wordName) {
        return new FuzzyQueryResultDTO().setValue(wordName);
    }

    /**
     * Convert Pronunciation entity to PronunciationVO
     */
    public PronunciationVO toPronunciationVO(Pronunciation entity) {
        if (entity == null) {
            return null;
        }
        return new PronunciationVO()
                .setPronunciationId(entity.getPronunciationId())
                .setWordId(entity.getWordId())
                .setSoundmark(entity.getSoundmark())
                .setSoundmarkType(entity.getSoundmarkType())
                .setCharacterId(entity.getCharacterId())
                .setSourceUrl(entity.getSourceUrl());
    }

    /**
     * Convert list of Pronunciation entities to list of PronunciationVOs
     */
    public List<PronunciationVO> toPronunciationVOList(List<Pronunciation> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toPronunciationVO)
                .collect(Collectors.toList());
    }
}
