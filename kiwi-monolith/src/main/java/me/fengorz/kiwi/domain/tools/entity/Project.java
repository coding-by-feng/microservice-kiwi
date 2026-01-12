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
package me.fengorz.kiwi.domain.tools.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Project Entity
 *
 * @author codingByFeng
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tools_project")
@Accessors(chain = true)
public class Project extends Model<Project> {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String projectCode;

    private String name;

    private String address;

    private String contactName;

    private String contactPhone;

    private String notes;

    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean glass;

    private Boolean frame;

    private Boolean purchase;

    private Boolean transport;

    private Boolean install;

    private Boolean repair;

    private Boolean archived;

    private LocalDate createdAt;

    private LocalDateTime updatedAt;
}
