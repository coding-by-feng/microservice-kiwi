/*
 *
 *   Copyright [2019~2025] [codingByFeng]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *
 */

package me.fengorz.kiwi.common.fastdfs.component;

import com.github.tobato.fastdfs.domain.fdfs.MetaData;
import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.domain.proto.storage.DownloadByteArray;
import com.github.tobato.fastdfs.service.AppendFileStorageClient;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import me.fengorz.kiwi.common.fastdfs.exception.DfsOperateDeleteException;
import me.fengorz.kiwi.common.fastdfs.exception.DfsOperateException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Set;

/**
 * @Description TODO
 * @Author codingByFeng
 * @Date 2019/11/4 10:58 AM
 */
@Service
@Slf4j
@AllArgsConstructor
public class DfsService {

    public static final String DELETE_FILE_EXCEPTION = "dfsService delete file exception!";
    public static final String UPLOAD_FILE_EXCEPTION = "dfsService upload file exception";
    public static final String DOWNLOAD_STREAM_FILE_EXCEPTION = "dfsService download stream of file exception";
    public static final String DOWNLOAD_FILE_EXCEPTION = "dfsService download of file exception";
    /**
     * 面向普通应用的文件操作接口
     */
    private final FastFileStorageClient fastFileStorageClient;

    /**
     * 支持断点续传的文件服务接口
     */
    private final AppendFileStorageClient appendFileStorageClient;

    public String uploadFile(InputStream inputStream, long size, String extName) throws DfsOperateException {
        return uploadFile(inputStream, size, extName, null);
    }

    public String uploadFile(InputStream inputStream, long size, String extName, Set<MetaData> metaDataSet) throws DfsOperateException {
        try {
            log.info("uploading file size = {}，name suffix = {}", size, extName);
            StorePath storePath = fastFileStorageClient.uploadFile(inputStream, size, extName, metaDataSet);
            log.info("upload file success，group：{}，path：{}", storePath.getGroup(), storePath.getPath());
            return storePath.getFullPath();
        } catch (Exception e) {
            log.error(UPLOAD_FILE_EXCEPTION, e);
            throw new DfsOperateException(UPLOAD_FILE_EXCEPTION);
        }
    }

    public void deleteFile(String groupName, String path) throws DfsOperateDeleteException {
        if (path.startsWith(groupName + "/")) {
            path = path.split(groupName + "/")[1];
        }
        try {
            fastFileStorageClient.deleteFile(groupName, path);
            log.info("delete file success，group：{}，path：{}", groupName, path);
        } catch (Exception e) {
            log.error(DELETE_FILE_EXCEPTION, e);
            throw new DfsOperateDeleteException(DELETE_FILE_EXCEPTION);
        }
    }

    public InputStream downloadStream(String groupName, String path) throws DfsOperateDeleteException {
        try {
            byte[] content = downloadFile(groupName, path);
            return new ByteArrayInputStream(content);
        } catch (Exception e) {
            log.error(DOWNLOAD_STREAM_FILE_EXCEPTION, e);
            throw new DfsOperateDeleteException(DOWNLOAD_STREAM_FILE_EXCEPTION);
        }
    }

    public byte[] downloadFile(String groupName, String path) throws DfsOperateDeleteException {
        if (path.startsWith(groupName + "/")) {
            path = path.split(groupName + "/")[1];
        }
        try {
            log.info("download file success，group：{}，path：{}", groupName, path);
            return fastFileStorageClient.downloadFile(groupName, path, new DownloadByteArray());
        } catch (Exception e) {
            log.error(DOWNLOAD_FILE_EXCEPTION, e);
            throw new DfsOperateDeleteException(DOWNLOAD_FILE_EXCEPTION, e);
        }
    }

}