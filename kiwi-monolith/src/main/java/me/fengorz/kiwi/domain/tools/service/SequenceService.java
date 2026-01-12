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
package me.fengorz.kiwi.domain.tools.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.domain.tools.entity.Sequence;
import me.fengorz.kiwi.domain.tools.mapper.SequenceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sequence Service - generates unique integer sequences
 *
 * @author codingByFeng
 */
@Slf4j
@Service
public class SequenceService extends ServiceImpl<SequenceMapper, Sequence> {

    /**
     * Generate a new unique integer sequence
     * Uses separate transaction to ensure sequence is committed
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized Integer generateSequence() {
        Sequence sequence = Sequence.builder()
                .stub("a")
                .build();
        save(sequence);
        Integer id = sequence.getId();
        // Clean up the generated row to keep the table small
        removeById(id);
        return id;
    }
}
