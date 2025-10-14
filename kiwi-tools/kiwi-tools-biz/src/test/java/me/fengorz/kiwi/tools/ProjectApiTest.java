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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc(addFilters = false) // disable security filters in MockMvc tests
@ActiveProfiles("test")
@SpringBootTest(
        classes = ToolsBizTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class ProjectApiTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    public void testCreateAndList() throws Exception {
        // UTF-8 JSON MediaType
        MediaType JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON.getType(),
                MediaType.APPLICATION_JSON.getSubtype(), StandardCharsets.UTF_8);

        Map<String, Object> body = new HashMap<>();
        body.put("id", "101"); // dummy if required by your controller
        body.put("projectCode", "P-101"); // dummy if server generates, it will be ignored
        body.put("name", "Kitchen remodel");
        // keep Chinese text to validate encoding correctness
        body.put("status", "未开始");
        // dummy required fields (adapt to your actual request model validation)
        body.put("clientName", "测试客户");
        body.put("clientPhone", "13800000000");
        body.put("address", "上海市浦东新区世纪大道1号");
        body.put("salesPerson", "Alice");
        body.put("installer", "Bob");
        body.put("teamMembers", "Alice,Bob,Charlie");
        body.put("startDate", "2025-10-14");
        body.put("endDate", "2025-10-20");
        body.put("todayTask", "安装橱柜");
        body.put("progressNote", "初始创建记录");
        body.put("photoUrl", "https://example.com/photo.jpg");

        mockMvc.perform(post("/api/projects")
                        .characterEncoding(StandardCharsets.UTF_8.name())
                        .contentType(JSON_UTF8)
                        .accept(JSON_UTF8)
                        .content(KiwiJsonUtils.toJsonPretty(body)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.projectCode", startsWith("P-")))
                // verify UTF-8 survives round-trip
                .andExpect(jsonPath("$.status", is("未开始")));

        mockMvc.perform(get("/api/projects")
                        .characterEncoding(StandardCharsets.UTF_8.name())
                        .accept(JSON_UTF8)
                        .param("page", "1")
                        .param("pageSize", "50"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", notNullValue()))
                .andExpect(jsonPath("$.page", is(1)))
                .andExpect(jsonPath("$.pageSize", is(50)))
                .andExpect(jsonPath("$.total", greaterThanOrEqualTo(1)));
    }
}
