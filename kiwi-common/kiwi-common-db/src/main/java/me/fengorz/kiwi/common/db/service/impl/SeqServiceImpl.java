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

package me.fengorz.kiwi.common.db.service.impl;

import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.common.db.MapperConstant;
import me.fengorz.kiwi.common.db.entity.Sequence;
import me.fengorz.kiwi.common.db.mapper.SeqMapper;
import me.fengorz.kiwi.common.db.service.SeqService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Description 序列生成服务
 * @Author Kason Zhan
 * @Date 2020/5/31 10:57 AM
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "ms.config.exclude-db", havingValue = "false")
public class SeqServiceImpl implements SeqService {

    private final SeqMapper seqMapper;

    /**
     * 传入产生序列对应的表，产生一个整型序列ID
     *
     * @param seqTable 对应的表
     * @return 新生成的序列ID
     */
    @Override
    @Transactional(rollbackFor = RuntimeException.class, propagation = Propagation.REQUIRES_NEW)
    public synchronized Integer genIntSequence(String seqTable) {
        // TODO ZSF refactor, not use synchronized on method.
        Sequence seq = new Sequence();
        seq.setTableName(seqTable);
        seqMapper.genSequence(seq);
        seqMapper.deleteSequence(seq);
        return seq.getId();
    }

    @Override
    public Integer genCommonIntSequence() {
        return this.genIntSequence(MapperConstant.T_INS_SEQUENCE);
    }
}
