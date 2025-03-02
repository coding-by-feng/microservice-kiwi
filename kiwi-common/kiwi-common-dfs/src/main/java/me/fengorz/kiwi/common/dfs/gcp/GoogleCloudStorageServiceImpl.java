package me.fengorz.kiwi.common.dfs.gcp;

import com.github.tobato.fastdfs.domain.fdfs.MetaData;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.dfs.DfsService;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.exception.dfs.DfsOperateDeleteException;
import me.fengorz.kiwi.common.sdk.exception.dfs.DfsOperateException;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @Description Google Cloud Storage implementation of DfsService
 * @Author Kason Zhan
 * @Date 26/02/2025 3:04pm
 */
@Slf4j
@Primary
@Service("googleCloudStorageService")
public class GoogleCloudStorageServiceImpl implements DfsService {

    @Autowired
    private Storage storage;
    @Value("${dfs.gcp.bucket-name:kiwidict-bucket}")
    private String defaultBucketName;

    @Override
    public String uploadFile(InputStream inputStream, long size, String extName) throws DfsOperateException {
        try {
            // Generate a unique object name with a prefix
            String fileName = buildFileName(extName);
            // Define the blob metadata
            BlobInfo blobInfo = BlobInfo.newBuilder(defaultBucketName, fileName).build();
            // Upload the file directly from the InputStream
            storage.create(blobInfo, IOUtils.toByteArray(inputStream));
            // Return the object name for later use in delete/download
            return buildUploadedPath(fileName);
        } catch (Exception e) {
            throw new DfsOperateException("Failed to upload file to Google Cloud Storage", e);
        }
    }

    private String buildUploadedPath(String fileName) {
        return defaultBucketName + GlobalConstants.SYMBOL_FORWARD_SLASH + fileName;
    }

    private static String buildFileName(String extName) {
        return "uploads/" + System.currentTimeMillis() + "." + extName;
    }

    @Override
    public String uploadFile(InputStream inputStream, long size, String extName, Set<MetaData> metaDataSet) throws DfsOperateException {
        try {
            // Generate a unique object name with a prefix
            String fileName = buildFileName(extName);
            // Define the blob metadata
            BlobInfo.Builder blobInfoBuilder = BlobInfo.newBuilder(defaultBucketName, fileName);
            // Add metadata if provided
            if (metaDataSet != null && !metaDataSet.isEmpty()) {
                Map<String, String> metadataMap = new HashMap<>();
                for (MetaData metaData : metaDataSet) {
                    metadataMap.put(metaData.getName(), metaData.getValue());
                }
                blobInfoBuilder.setMetadata(metadataMap);
            }
            // Upload the file directly from the InputStream
            storage.create(blobInfoBuilder.build(), IOUtils.toByteArray(inputStream));
            // Return the object name for later use in delete/download
            return buildUploadedPath(fileName);
        } catch (Exception e) {
            throw new DfsOperateException("Failed to upload file with metadata to Google Cloud Storage", e);
        }
    }

    @Override
    public void deleteFile(String groupName, String path) throws DfsOperateDeleteException {
        try {
            // Use groupName as the bucket name and path as the object name
            BlobId blobId = BlobId.of(groupName, path);
            // Attempt to delete the blob
            boolean deleted = storage.delete(blobId);
            if (!deleted) {
                throw new DfsOperateDeleteException("Failed to delete file from Google Cloud Storage: " + path);
            }
        } catch (Exception e) {
            throw new DfsOperateDeleteException("Error deleting file from Google Cloud Storage", e);
        }
    }

    @Override
    public InputStream downloadStream(String groupName, String path) throws DfsOperateException {
        try {
            // Use groupName as the bucket name and path as the object name
            Blob blob = storage.get(BlobId.of(groupName, path));
            if (blob == null) {
                throw new DfsOperateException("File not found in Google Cloud Storage: " + path);
            }
            // Return a stream to read the file content
            return new ByteArrayInputStream(blob.getContent());
        } catch (Exception e) {
            throw new DfsOperateException("Failed to download file stream from Google Cloud Storage", e);
        }
    }

    @Override
    public byte[] downloadFile(String groupName, String path) throws DfsOperateException {
        try {
            // Use groupName as the bucket name and path as the object name
            Blob blob = storage.get(BlobId.of(groupName, path));
            if (blob == null) {
                throw new DfsOperateException("File not found in Google Cloud Storage: " + path);
            }
            // Return the file content as a byte array
            return blob.getContent();
        } catch (Exception e) {
            throw new DfsOperateException("Failed to download file from Google Cloud Storage", e);
        }
    }
}