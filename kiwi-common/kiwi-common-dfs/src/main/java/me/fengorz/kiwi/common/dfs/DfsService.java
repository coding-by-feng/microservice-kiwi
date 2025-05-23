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

package me.fengorz.kiwi.common.dfs;

import com.github.tobato.fastdfs.domain.fdfs.MetaData;
import me.fengorz.kiwi.common.sdk.exception.dfs.DfsOperateDeleteException;
import me.fengorz.kiwi.common.sdk.exception.dfs.DfsOperateException;

import java.io.InputStream;
import java.util.Set;

/**
 * @author zhanshifeng
 */
public interface DfsService {

    String uploadFile(InputStream inputStream, long size, String extName) throws DfsOperateException;

    String uploadFile(InputStream inputStream, long size, String extName, Set<MetaData> metaDataSet)
        throws DfsOperateException;

    void deleteFile(String groupName, String path) throws DfsOperateDeleteException;

    InputStream downloadStream(String groupName, String path) throws DfsOperateException;

    byte[] downloadFile(String groupName, String path) throws DfsOperateException;
}
