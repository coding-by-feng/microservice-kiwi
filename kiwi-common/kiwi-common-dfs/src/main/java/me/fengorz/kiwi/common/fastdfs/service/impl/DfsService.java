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

package me.fengorz.kiwi.common.fastdfs.service.impl;

import com.github.tobato.fastdfs.domain.fdfs.MetaData;
import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.domain.proto.storage.DownloadByteArray;
import com.github.tobato.fastdfs.service.AppendFileStorageClient;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.fastdfs.constant.DfsConstants;
import me.fengorz.kiwi.common.fastdfs.service.IDfsService;
import me.fengorz.kiwi.common.sdk.exception.dfs.DfsOperateDeleteException;
import me.fengorz.kiwi.common.sdk.exception.dfs.DfsOperateException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Set;

/**
 * @Description Dfs分布式文件服务类
 * @Author zhanshifeng
 * @Date 2019/11/4 10:58 AM
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DfsService implements IDfsService {

    /**
     * 面向普通应用的文件操作接口
     */
    private final FastFileStorageClient fastFileStorageClient;

    /**
     * 支持断点续传的文件服务接口
     */
    private final AppendFileStorageClient appendFileStorageClient;

    @Override
    public String uploadFile(InputStream inputStream, long size, String extName) throws DfsOperateException {
        return uploadFile(inputStream, size, extName, null);
    }

    @Override
    public String uploadFile(InputStream inputStream, long size, String extName, Set<MetaData> metaDataSet)
        throws DfsOperateException {
        try {
            log.info("uploading file size = {}，name suffix = {}", size, extName);
            StorePath storePath = fastFileStorageClient.uploadFile(inputStream, size, extName, metaDataSet);
            log.info("upload file success，group：{}，path：{}", storePath.getGroup(), storePath.getPath());
            return storePath.getFullPath();
        } catch (Exception e) {
            // e.printStackTrace();
            log.error(DfsConstants.UPLOAD_FILE_EXCEPTION, e);
            throw new DfsOperateException(DfsConstants.UPLOAD_FILE_EXCEPTION);
        }
    }

    @Override
    public void deleteFile(String groupName, String path) throws DfsOperateDeleteException {
        if (path.startsWith(groupName + "/")) {
            path = path.split(groupName + "/")[1];
        }
        try {
            fastFileStorageClient.deleteFile(groupName, path);
            log.info("delete file success，group：{}，path：{}", groupName, path);
        } catch (Exception e) {
            log.error(DfsConstants.DELETE_FILE_EXCEPTION);
            throw new DfsOperateDeleteException(DfsConstants.DELETE_FILE_EXCEPTION);
        }
    }

    @Override
    public InputStream downloadStream(String groupName, String path) throws DfsOperateException {
        try {
            byte[] content = downloadFile(groupName, path);
            return new ByteArrayInputStream(content);
        } catch (Exception e) {
            log.error(DfsConstants.DOWNLOAD_STREAM_FILE_EXCEPTION, e);
            throw new DfsOperateException(DfsConstants.DOWNLOAD_STREAM_FILE_EXCEPTION);
        }
    }

    @Override
    public byte[] downloadFile(String groupName, String path) throws DfsOperateException {
        if (path.startsWith(groupName + "/")) {
            path = path.split(groupName + "/")[1];
        }
        try {
            log.info("download file success，group：{}，path：{}", groupName, path);
            return fastFileStorageClient.downloadFile(groupName, path, new DownloadByteArray());
        } catch (Exception e) {
            log.error(DfsConstants.DOWNLOAD_FILE_EXCEPTION, e);
            throw new DfsOperateException(DfsConstants.DOWNLOAD_FILE_EXCEPTION, e);
        }
    }

}
