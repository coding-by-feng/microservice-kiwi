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
package me.fengorz.kiwi.domain.upms.entity;

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
 * System Department Entity
 *
 * @author codingByFeng
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
@Accessors(chain = true)
public class SysDept extends Model<SysDept> {

    private static final long serialVersionUID = 1L;

    public static final String VALID_YES = "Y";
    public static final String VALID_NO = "N";

    @TableId(type = IdType.AUTO)
    private Integer deptId;

    private String deptName;

    private Integer sort;

    private String isValid;

    private Integer parentId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @JsonIgnore
    public boolean isValid() {
        return VALID_YES.equals(this.isValid);
    }

    public void markInvalid() {
        this.isValid = VALID_NO;
    }

    @JsonIgnore
    public boolean isRoot() {
        return this.parentId == null || this.parentId == 0;
    }
}
