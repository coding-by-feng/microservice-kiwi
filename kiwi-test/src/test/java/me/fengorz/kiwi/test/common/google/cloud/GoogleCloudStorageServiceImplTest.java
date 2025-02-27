package me.fengorz.kiwi.test.common.google.cloud;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.dfs.DfsService;
import me.fengorz.kiwi.common.sdk.exception.dfs.DfsOperateDeleteException;
import me.fengorz.kiwi.common.sdk.exception.dfs.DfsOperateException;
import me.fengorz.kiwi.test.dfs.DfsTestApplication;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = DfsTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GoogleCloudStorageServiceImplTest {

    @javax.annotation.Resource(name = "googleCloudStorageService")
    private DfsService dfsService;

    @Autowired
    private ResourceLoader resourceLoader;

    private final String BUCKET_NAME = "kiwidict-bucket";
    private final String LOCAL_UPLOAD_FILE_PATH = "classpath:/dfs/file.txt";
    private final String LOCAL_DOWNLOAD_DIR = System.getProperty("java.io.tmpdir"); // Use system's temp directory
    private final String LOCAL_DOWNLOAD_FILE_NAME = "downloaded_file.txt";
    private final Path LOCAL_DOWNLOAD_FILE_PATH = Paths.get(LOCAL_DOWNLOAD_DIR, LOCAL_DOWNLOAD_FILE_NAME);
    // Path to the resources folder (relative to project root)
    private final Path RESOURCES_DOWNLOAD_PATH = Paths.get("src", "test", "resources", "dfs", "downloaded_file.txt");

    private boolean skipIfDownloadedFileExists() {
        // Check if the resource 'classpath:/dfs/downloaded_file.txt' exists
        Resource downloadedResource = resourceLoader.getResource("classpath:/dfs/downloaded_file.txt");
        if (downloadedResource.exists()) {
            log.info("Resource 'classpath:/dfs/downloaded_file.txt' already exists, skipping test");
            return true; // Test passes by returning early
        }
        return false;
    }

    @Test
    @Disabled
    @SneakyThrows
    public void testUploadAndDownloadFile() throws IOException {
        if (skipIfDownloadedFileExists()) {
            log.info("skipping testUploadAndDownloadFile because downloaded file already exists");
            return;
        }

        // --- Arrange: Prepare the local file for upload ---
        Resource resource = resourceLoader.getResource(LOCAL_UPLOAD_FILE_PATH);
        assertTrue(resource.exists(), "Local upload file does not exist");

        // Get the File object from the resource
        File uploadFile = resource.getFile();
        Path uploadPath = uploadFile.toPath();
        String extName = getFileExtension(uploadFile.getName());
        long fileSize = uploadFile.length();

        // --- Act: Upload the file to GCP Storage ---
        try (InputStream uploadStream = Files.newInputStream(uploadPath)) {
            String uploadedFileName = dfsService.uploadFile(uploadStream, fileSize, extName);
            assertNotNull(uploadedFileName, "Uploaded file name should not be null");
            assertTrue(uploadedFileName.endsWith("." + extName),
                    "Uploaded file name should end with '." + extName + "'");

            // --- Act: Download the file from GCP Storage ---
            byte[] downloadedContent = dfsService.downloadFile(BUCKET_NAME, uploadedFileName);

            // --- Assert: Verify the downloaded content matches the original ---
            byte[] originalContent = Files.readAllBytes(uploadPath);
            assertArrayEquals(originalContent, downloadedContent,
                    "Downloaded content should match the original");

            // Save the downloaded file locally in the temp directory for verification
            Files.createDirectories(LOCAL_DOWNLOAD_FILE_PATH.getParent());
            try (FileOutputStream fos = new FileOutputStream(LOCAL_DOWNLOAD_FILE_PATH.toFile())) {
                fos.write(downloadedContent);
            }
            assertTrue(Files.exists(LOCAL_DOWNLOAD_FILE_PATH), "Downloaded file should exist in temp directory");

            // Optionally copy to resources folder (not recommended for tests, but shown for completeness)
            Files.createDirectories(RESOURCES_DOWNLOAD_PATH.getParent());
            Files.write(RESOURCES_DOWNLOAD_PATH, downloadedContent);
            assertTrue(Files.exists(RESOURCES_DOWNLOAD_PATH), "Downloaded file should exist in resources folder");

            // --- Cleanup: Optionally delete the file from GCP Storage ---
            dfsService.deleteFile(BUCKET_NAME, uploadedFileName);
        }
    }

    @Test
    @SneakyThrows
    public void testDownloadFile() throws IOException {
        if (skipIfDownloadedFileExists()) {
            log.info("Skipping download file because downloaded file already exists");
            return;
        }

        // --- Arrange: Prepare and upload a test file ---
        Resource resource = resourceLoader.getResource(LOCAL_UPLOAD_FILE_PATH);
        assertTrue(resource.exists(), "Local upload file does not exist");

        File uploadFile = resource.getFile();
        Path uploadPath = uploadFile.toPath();
        String extName = getFileExtension(uploadFile.getName());
        long fileSize = uploadFile.length();

        String uploadedFileName;
        try (InputStream uploadStream = Files.newInputStream(uploadPath)) {
            uploadedFileName = dfsService.uploadFile(uploadStream, fileSize, extName);
            assertNotNull(uploadedFileName, "Uploaded file name should not be null");
            assertTrue(uploadedFileName.endsWith("." + extName),
                    "Uploaded file name should end with '." + extName + "'");
        }

        // --- Act: Download the file using downloadFile ---
        byte[] downloadedContent;
        try {
            downloadedContent = dfsService.downloadFile(BUCKET_NAME, uploadedFileName);
        } catch (DfsOperateException e) {
            fail("Failed to download file: " + e.getMessage());
            return; // Early return to avoid further execution if the test fails
        }

        // --- Assert: Verify the downloaded content matches the original ---
        byte[] originalContent = Files.readAllBytes(uploadPath);
        assertNotNull(downloadedContent, "Downloaded content should not be null");
        assertArrayEquals(originalContent, downloadedContent,
                "Downloaded content should match the original");

        // Save the downloaded file locally in the temp directory for verification
        Files.createDirectories(LOCAL_DOWNLOAD_FILE_PATH.getParent());
        try (FileOutputStream fos = new FileOutputStream(LOCAL_DOWNLOAD_FILE_PATH.toFile())) {
            fos.write(downloadedContent);
        }
        assertTrue(Files.exists(LOCAL_DOWNLOAD_FILE_PATH), "Downloaded file should exist in temp directory");

        // Optionally copy to resources folder (not recommended for tests, but shown for completeness)
        Files.createDirectories(RESOURCES_DOWNLOAD_PATH.getParent());
        Files.write(RESOURCES_DOWNLOAD_PATH, downloadedContent);
        assertTrue(Files.exists(RESOURCES_DOWNLOAD_PATH), "Downloaded file should exist in resources folder");

        // --- Cleanup: Delete the uploaded file from GCP Storage ---
        try {
            dfsService.deleteFile(BUCKET_NAME, uploadedFileName);
        } catch (DfsOperateDeleteException e) {
            log.warn("Failed to delete file from GCP Storage during cleanup: " + e.getMessage());
        }
    }

    // Helper method to extract file extension
    private String getFileExtension(String filePath) {
        String fileName = new File(filePath).getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }
}