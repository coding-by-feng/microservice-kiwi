package me.fengorz.kason.tools;

import me.fengorz.kason.common.sdk.util.json.KasonJsonUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertArrayEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@SpringBootTest(classes = {ToolsBizTestApplication.class, ToolsBizTestOverrides.class},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class ProjectPhotoApiTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    public void upload_list_download_delete_photo() throws Exception {
        // 1) Create a project
        String createJson = "{\n" +
                "  \"name\": \"P-Photo-1\",\n" +
                "  \"status\": \"not_started\",\n" +
                "  \"address\": \"Kason HQ\"\n" +
                "}";
        MvcResult created = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn();
        Map<?, ?> project = KasonJsonUtils.fromJson(created.getResponse().getContentAsString(), Map.class);
        String projectId = String.valueOf(project.get("id"));

        // 2) Upload a photo
        byte[] img = new byte[]{(byte)137, 80, 78, 71, 13, 10, 26, 10}; // tiny PNG signature bytes are OK
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", img);
        MvcResult uploaded = mockMvc.perform(multipart("/api/projects/{id}/photo", projectId)
                        .file(file))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.projectId", is(projectId)))
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.contentType", is("image/png")))
                .andExpect(jsonPath("$.size", is(img.length)))
                .andReturn();
        Map<?, ?> photo = KasonJsonUtils.fromJson(uploaded.getResponse().getContentAsString(StandardCharsets.UTF_8), Map.class);
        String photoId = String.valueOf(photo.get("id"));
        String token = String.valueOf(photo.get("token"));

        // 3) List photos
        mockMvc.perform(get("/api/projects/{id}/photos", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$[0].id", is(photoId)))
                .andExpect(jsonPath("$[0].projectId", is(projectId)));

        // 4) Download the photo and assert bytes
        MvcResult dl = mockMvc.perform(get("/api/projects/{id}/photo/{token}", projectId, token))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age")))
                .andExpect(content().contentType("image/png"))
                .andReturn();
        byte[] downloaded = dl.getResponse().getContentAsByteArray();
        assertArrayEquals(img, downloaded);

        // 5) Delete the photo by token
        mockMvc.perform(delete("/api/projects/{id}/photo/{token}", projectId, token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/projects/{id}/photos", projectId))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        // 6) Upload two and delete all
        mockMvc.perform(multipart("/api/projects/{id}/photo", projectId).file(new MockMultipartFile("file", "a.png", "image/png", img)))
                .andExpect(status().isOk());
        mockMvc.perform(multipart("/api/projects/{id}/photo", projectId).file(new MockMultipartFile("file", "b.png", "image/png", img)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/projects/{id}/photos", projectId)).andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(delete("/api/projects/{id}/photos", projectId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/projects/{id}/photos", projectId)).andExpect(content().json("[]"));
    }

    @Test
    public void reject_non_image_upload() throws Exception {
        // Create a project
        String createJson = "{\n" +
                "  \"name\": \"P-Photo-2\",\n" +
                "  \"status\": \"not_started\",\n" +
                "  \"address\": \"Kason HQ\"\n" +
                "}";
        String projectId = KasonJsonUtils.fromJson(
                mockMvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(createJson))
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString(), Map.class)
                .get("id").toString();

        // Upload with non-image content type
        MockMultipartFile bad = new MockMultipartFile("file", "readme.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/projects/{id}/photo", projectId).file(bad))
                .andExpect(status().isUnsupportedMediaType());
    }
}
