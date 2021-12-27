/*
 *
 * Copyright [2019~2025] [zhanshifeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 *
 */
package me.fengorz.kiwi.word.biz.service.base.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.util.bean.KiwiBeanUtils;
import me.fengorz.kiwi.word.api.dto.mapper.in.SelectEntityIsCollectDTO;
import me.fengorz.kiwi.word.api.entity.ParaphraseDO;
import me.fengorz.kiwi.word.api.request.ParaphraseRequest;
import me.fengorz.kiwi.word.api.vo.detail.ParaphraseVO;
import me.fengorz.kiwi.word.biz.mapper.ParaphraseMapper;
import me.fengorz.kiwi.word.biz.service.base.IParaphraseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 单词释义表
 *
 * @author zhanshifeng
 * @date 2019-10-31 20:39:48
 */
@Service()
@RequiredArgsConstructor
public class ParaphraseServiceImpl extends ServiceImpl<ParaphraseMapper, ParaphraseDO>
        implements IParaphraseService {

    private final ParaphraseMapper mapper;

    @Override
    public Integer countById(Integer id) {
        return this
                .count(new QueryWrapper<>(new ParaphraseDO().setParaphraseId(id).setIsDel(GlobalConstants.FLAG_DEL_NO)));
    }

    @Override
    public List<ParaphraseVO> selectParaphraseAndIsCollect(Integer characterId, Integer currentUserId) {
        return mapper.selectParaphraseAndIsCollect(
                new SelectEntityIsCollectDTO().setEntityId(characterId).setOwner(currentUserId));
    }

    @Override
    public List<ParaphraseVO> listPhrase(Integer wordId) {
        return KiwiBeanUtils.convertFrom(mapper.selectList(Wrappers.<ParaphraseDO>lambdaQuery().eq(ParaphraseDO::getWordId, wordId).eq(ParaphraseDO::getIsDel, GlobalConstants.FLAG_NO)), ParaphraseVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delByWordId(Integer wordId) {
        mapper.delete(Wrappers.<ParaphraseDO>lambdaQuery().eq(ParaphraseDO::getWordId, wordId));
    }

    @Override
    public boolean modifyMeaningChinese(ParaphraseRequest request) {
        ParaphraseDO paraphraseDO = new ParaphraseDO();
        KiwiBeanUtils.copyProperties(request, paraphraseDO);
        return mapper.updateById(paraphraseDO) > 0;
    }

}
