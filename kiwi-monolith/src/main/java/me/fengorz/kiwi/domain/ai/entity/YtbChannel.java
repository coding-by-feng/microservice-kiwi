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
package me.fengorz.kiwi.domain.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * YouTube Channel Entity
 *
 * @author codingByFeng
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ytb_channel")
@Accessors(chain = true)
public class YtbChannel extends Model<YtbChannel> {

    private static final long serialVersionUID = 1L;

    public static final Integer STATUS_READY = 0;
    public static final Integer STATUS_PROCESSING = 1;
    public static final Integer STATUS_FINISH = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String channelLink;

    private String channelName;

    private Integer status;

    private LocalDateTime createTime;

    private Boolean ifValid;

    @JsonIgnore
    public boolean isReady() {
        return STATUS_READY.equals(this.status);
    }

    @JsonIgnore
    public boolean isProcessing() {
        return STATUS_PROCESSING.equals(this.status);
    }

    @JsonIgnore
    public boolean isFinished() {
        return STATUS_FINISH.equals(this.status);
    }
}
