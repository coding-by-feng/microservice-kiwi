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

package me.fengorz.kiwi.bdf.core.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.bdf.core.entity.Sequence;
import me.fengorz.kiwi.bdf.core.mapper.SeqMapper;
import me.fengorz.kiwi.bdf.core.service.SeqService;

/**
 * @Description 序列生成服务
 * @Author zhanshifeng
 * @Date 2020/5/31 10:57 AM
 */
@Service
@RequiredArgsConstructor
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
    public Integer genIntSequence(String seqTable) {
        Sequence seq = new Sequence();
        seq.setTableName(seqTable);
        seqMapper.genSequence(seq);
        seqMapper.deleteSequence(seq);
        return seq.getId();
    }
}
