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
import me.fengorz.kiwi.word.api.entity.ParaphraseExampleDO;
import me.fengorz.kiwi.word.api.vo.ParaphraseExampleVO;
import me.fengorz.kiwi.word.biz.mapper.ParaphraseExampleMapper;
import me.fengorz.kiwi.word.biz.service.base.ParaphraseExampleService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 单词例句表
 *
 * @author zhanshifeng
 * @date 2019-10-31 20:40:38
 */
@Service
@RequiredArgsConstructor
public class ParaphraseExampleServiceImpl extends ServiceImpl<ParaphraseExampleMapper, ParaphraseExampleDO>
    implements ParaphraseExampleService {

    private final ParaphraseExampleMapper mapper;

    @Override
    public Integer countById(Integer id) {
        return this.count(new QueryWrapper<>(new ParaphraseExampleDO().setExampleId(id)));
    }

    @Override
    public List<ParaphraseExampleVO> listExamples(Integer paraphraseId) {
        return KiwiBeanUtils.convertFrom(mapper.selectList(
            Wrappers.<ParaphraseExampleDO>lambdaQuery().eq(ParaphraseExampleDO::getParaphraseId, paraphraseId)
                .eq(ParaphraseExampleDO::getIsDel, GlobalConstants.FLAG_NO)),
            ParaphraseExampleVO.class);
    }

    @Override
    public List<ParaphraseExampleVO> selectExampleAndIsCollect(Integer owner, Integer paraphraseId) {
        return this.mapper
            .selectExampleAndIsCollect(new SelectEntityIsCollectDTO().setOwner(owner).setEntityId(paraphraseId));
    }
}
