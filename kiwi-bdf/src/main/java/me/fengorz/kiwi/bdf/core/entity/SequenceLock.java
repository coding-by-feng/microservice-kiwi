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
package me.fengorz.kiwi.bdf.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 业务锁
 */
@Data
@ToString
@Accessors(chain = true)
@TableName("t_sequence_lock")
public class SequenceLock implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 锁id
     */
    @TableId(value = "lock_id", type = IdType.AUTO)
    private Long lockId;

    /**
     * 锁名称
     */
    private String lockName;

    /**
     * 状态 : 1:未分配；2：已分配
     */
    private Integer status;

    /**
     * 锁来源
     */
    private String lockSrc;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 锁释放时间
     */
    private LocalDateTime releaseTime;

    /**
     * 更新时间
     */
    private LocalDateTime lockTime;
}
