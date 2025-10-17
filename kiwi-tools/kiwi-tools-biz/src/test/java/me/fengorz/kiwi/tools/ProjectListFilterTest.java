package me.fengorz.kiwi.tools;

import me.fengorz.kiwi.common.sdk.util.json.KiwiJsonUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@SpringBootTest(classes = {ToolsBizTestApplication.class, ToolsBizTestOverrides.class},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class ProjectListFilterTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    public void includeArchived_returns_only_archived() throws Exception {
        // Create two projects
        String p1 = mockMvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A1\",\"status\":\"not_started\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String p2 = mockMvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A2\",\"status\":\"not_started\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id2 = (String) KiwiJsonUtils.fromJson(p2, Map.class).get("id");

        // Archive the second
        mockMvc.perform(post("/api/projects/{id}/archive", id2).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.archived", is(true)));

        // Default listing (active only)
        mockMvc.perform(get("/api/projects").param("page", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].archived", everyItem(is(false))));

        // includeArchived=true should return ONLY archived
        mockMvc.perform(get("/api/projects").param("includeArchived", "true").param("page", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", notNullValue()))
                .andExpect(jsonPath("$.items", everyItem(hasEntry("archived", true))));
    }
}

