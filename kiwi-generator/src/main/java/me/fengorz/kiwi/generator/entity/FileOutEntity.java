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

package me.fengorz.kiwi.generator.entity;

import java.io.File;

import lombok.Data;

/**
 * @Author Kason Zhan @Date 2019-09-12 16:50
 */
@Data
public class FileOutEntity {

    private String templatePath;

    private String entityType;

    private String projectPath;

    private String packagePrefix;

    private String modulePackage;

    private String moduleName;

    private String filePrefix;

    private String entityName;

    private String fileSuffix;

    private String fileType;

    @Override
    public String toString() {
        return projectPath + File.pathSeparator + packagePrefix + File.pathSeparator + moduleName + File.pathSeparator
            + entityName + fileSuffix;
    }
}
