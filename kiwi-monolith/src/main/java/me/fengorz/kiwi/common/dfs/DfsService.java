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
package me.fengorz.kiwi.common.dfs;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;

/**
 * Distributed File System Service Interface
 *
 * @author codingByFeng
 */
public interface DfsService {

    String uploadFile(InputStream inputStream, long size, String extName);

    String uploadFile(InputStream inputStream, long size, String extName, Set<Map<String, String>> metaDataSet);

    void deleteFile(String groupName, String path);

    InputStream downloadStream(String groupName, String path);

    byte[] downloadFile(String groupName, String path);
}
