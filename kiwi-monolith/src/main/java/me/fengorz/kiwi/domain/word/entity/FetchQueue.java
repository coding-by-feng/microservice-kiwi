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
package me.fengorz.kiwi.domain.word.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * Fetch Queue Entity - stores words pending to be fetched
 *
 * @author codingByFeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("word_fetch_queue")
@Accessors(chain = true)
public class FetchQueue extends Model<FetchQueue> {

    private static final long serialVersionUID = 1L;

    public static final Integer STATUS_WAITING = 0;
    public static final Integer STATUS_FETCHING = 1;
    public static final Integer STATUS_SUCCESS = 2;
    public static final Integer STATUS_FAIL = 3;

    public static final String VALID_YES = "Y";
    public static final String VALID_NO = "N";

    public static final Integer LOCK_YES = 1;
    public static final Integer LOCK_NO = 0;

    @TableId(type = IdType.AUTO)
    private Integer queueId;

    private String wordName;

    private Integer wordId;

    private String derivation;

    private Integer fetchPriority;

    private Integer fetchStatus;

    private String isValid;

    private String fetchResult;

    private Integer isLock;

    private LocalDateTime inTime;

    private LocalDateTime operateTime;

    private Integer fetchTime;

    private Integer isIntoCache;

    private Integer infoType;

    @JsonIgnore
    public boolean isWaiting() {
        return STATUS_WAITING.equals(this.fetchStatus);
    }

    @JsonIgnore
    public boolean isFetching() {
        return STATUS_FETCHING.equals(this.fetchStatus);
    }

    @JsonIgnore
    public boolean isSuccess() {
        return STATUS_SUCCESS.equals(this.fetchStatus);
    }

    @JsonIgnore
    public boolean isFail() {
        return STATUS_FAIL.equals(this.fetchStatus);
    }

    @JsonIgnore
    public boolean isValid() {
        return VALID_YES.equals(this.isValid);
    }

    @JsonIgnore
    public boolean isLocked() {
        return LOCK_YES.equals(this.isLock);
    }

    public void lock() {
        this.isLock = LOCK_YES;
    }

    public void unlock() {
        this.isLock = LOCK_NO;
    }
}
