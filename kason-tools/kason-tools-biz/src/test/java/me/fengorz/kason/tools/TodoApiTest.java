package me.fengorz.kason.tools;

import me.fengorz.kason.common.sdk.util.json.KasonJsonUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@SpringBootTest(
        classes = {ToolsBizTestApplication.class, ToolsBizTestOverrides.class},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TodoApiTest.DummyRedisTestConfig.class)
public class TodoApiTest {

    @Autowired
    MockMvc mockMvc;

    private String userIdHeader;

    @Before
    public void setUp() {
        // Use a random user id per test run to isolate state
        userIdHeader = String.valueOf(new Random().nextInt(1_000_000) + 1000);
    }

    @Test
    public void e2e_create_get_patch_etag_delete_and_trash_restore() throws Exception {
        // Create task
        Map<String, Object> create = new HashMap<>();
        create.put("title", "E2E-TODO-1");
        create.put("description", "first");
        create.put("successPoints", 10);
        create.put("failPoints", -5);

        MvcResult created = mockMvc.perform(post("/tools/todo/tasks")
                        .header("X-User-Id", userIdHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(KasonJsonUtils.toJsonPretty(create)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.title", is("E2E-TODO-1")))
                .andReturn();

        String etag = created.getResponse().getHeader("ETag");
        Map body = KasonJsonUtils.fromJson(created.getResponse().getContentAsString(), Map.class);
        String id = ((Map<String, Object>) body.get("data")).get("id").toString();

        // GET task and compare ETag
        MvcResult got = mockMvc.perform(get("/tools/todo/tasks/{id}", id)
                        .header("X-User-Id", userIdHeader))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.data.id", is(id)))
                .andReturn();
        String etag2 = got.getResponse().getHeader("ETag");
        org.junit.Assert.assertEquals(etag, etag2);

        // PATCH with correct If-Match
        Map<String, Object> patchBody = new HashMap<>();
        patchBody.put("description", "updated");
        MvcResult patched = mockMvc.perform(patch("/tools/todo/tasks/{id}", id)
                        .header("X-User-Id", userIdHeader)
                        .header("If-Match", etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(KasonJsonUtils.toJsonPretty(patchBody)))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.data.description", is("updated")))
                .andReturn();
        String newEtag = patched.getResponse().getHeader("ETag");
        org.junit.Assert.assertNotEquals(etag, newEtag);

        // PATCH with wrong If-Match -> 412
        mockMvc.perform(patch("/tools/todo/tasks/{id}", id)
                        .header("X-User-Id", userIdHeader)
                        .header("If-Match", "\"deadbeef\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isPreconditionFailed());

        // DELETE moves to trash
        mockMvc.perform(delete("/tools/todo/tasks/{id}", id)
                        .header("X-User-Id", userIdHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ok", is(true)));

        // List trash and restore the item
        MvcResult trashList = mockMvc.perform(get("/tools/todo/trash")
                        .header("X-User-Id", userIdHeader)
                        .param("page", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andReturn();
        Map tl = KasonJsonUtils.fromJson(trashList.getResponse().getContentAsString(), Map.class);
        java.util.List<Map<String, Object>> items = (List<Map<String, Object>>) tl.get("data");
        org.junit.Assert.assertTrue(items.size() >= 1);
        String trashId = String.valueOf(items.get(0).get("id"));

        mockMvc.perform(post("/tools/todo/trash/{id}/restore", trashId)
                        .header("X-User-Id", userIdHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.status", is("pending")));

        // Clear trash
        mockMvc.perform(delete("/tools/todo/trash")
                        .header("X-User-Id", userIdHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deletedCount", greaterThanOrEqualTo(0)));
    }

    @Test
    public void e2e_complete_task_idempotent_history_list_and_delete() throws Exception {
        // Create
        String id = createTask("E2E-TODO-2");

        // Complete success
        Map<String, Object> complete = new HashMap<>();
        complete.put("status", "success");
        MvcResult completed = mockMvc.perform(post("/tools/todo/tasks/{id}/complete", id)
                        .header("X-User-Id", userIdHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(KasonJsonUtils.toJsonPretty(complete)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.task.status", is("success")))
                .andExpect(jsonPath("$.data.history.pointsApplied", greaterThanOrEqualTo(0)))
                .andReturn();
        Map resp = KasonJsonUtils.fromJson(completed.getResponse().getContentAsString(), Map.class);
        Map<String, Object> data = (Map<String, Object>) resp.get("data");
        String histId = String.valueOf(((Map) data.get("history")).get("id"));

        // Repeat call (no idempotency)
        mockMvc.perform(post("/tools/todo/tasks/{id}/complete", id)
                        .header("X-User-Id", userIdHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(KasonJsonUtils.toJsonPretty(complete)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.task.status", is("success")));

        // List history for today
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        mockMvc.perform(get("/tools/todo/history").header("X-User-Id", userIdHeader).param("date", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.meta.date", is(today)));

        // Delete history and check meta.ranking exists
        mockMvc.perform(delete("/tools/todo/history/{id}", histId).header("X-User-Id", userIdHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ok", is(true)))
                .andExpect(jsonPath("$.meta.ranking", notNullValue()));
    }

    @Test
    public void e2e_reset_status_and_reset_all() throws Exception {
        String id1 = createTask("E2E-TODO-3");
        String id2 = createTask("E2E-TODO-4");

        // Complete second so its status != pending
        Map<String, Object> complete = new HashMap<>();
        complete.put("status", "fail");
        mockMvc.perform(post("/tools/todo/tasks/{id}/complete", id2)
                        .header("X-User-Id", userIdHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(KasonJsonUtils.toJsonPretty(complete)))
                .andExpect(status().isOk());

        // Reset single
        mockMvc.perform(post("/tools/todo/tasks/{id}/reset-status", id2).header("X-User-Id", userIdHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("pending")));

        // Complete both, then reset all
        mockMvc.perform(post("/tools/todo/tasks/{id}/complete", id1)
                        .header("X-User-Id", userIdHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(KasonJsonUtils.toJsonPretty(Collections.singletonMap("status", "success"))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/tools/todo/tasks/{id}/complete", id2)
                        .header("X-User-Id", userIdHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(KasonJsonUtils.toJsonPretty(Collections.singletonMap("status", "success"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/tools/todo/tasks/reset-statuses").header("X-User-Id", userIdHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resetCount", greaterThanOrEqualTo(1)));
    }

    @Test
    public void e2e_list_filters_and_sort() throws Exception {
        String prefix = "E2E-TODO-LIST-" + System.currentTimeMillis();
        createTask(prefix + "A");
        createTask(prefix + "B");

        mockMvc.perform(get("/tools/todo/tasks")
                        .header("X-User-Id", userIdHeader)
                        .param("search", prefix)
                        .param("page", "1").param("pageSize", "10")
                        .param("sort", "created_desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.meta.page", is(1)))
                .andExpect(jsonPath("$.meta.pageSize", is(10)))
                .andExpect(jsonPath("$.meta.total", greaterThanOrEqualTo(2)));
    }

    @Test
    public void e2e_analytics_and_ranking() throws Exception {
        String id = createTask("E2E-TODO-ANALYTICS");
        mockMvc.perform(post("/tools/todo/tasks/{id}/complete", id)
                        .header("X-User-Id", userIdHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(KasonJsonUtils.toJsonPretty(Collections.singletonMap("status", "success"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/tools/todo/analytics/monthly").header("X-User-Id", userIdHeader).param("months", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.labels", notNullValue()))
                .andExpect(jsonPath("$.data.points", notNullValue()));

        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        mockMvc.perform(get("/tools/todo/analytics/summary").header("X-User-Id", userIdHeader).param("month", month))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.month", is(month)))
                .andExpect(jsonPath("$.data.totalPoints", notNullValue()))
                .andExpect(jsonPath("$.data.completedCount", notNullValue()))
                .andExpect(jsonPath("$.data.successRatePct", greaterThanOrEqualTo(0)));

        mockMvc.perform(get("/tools/todo/ranking/current").header("X-User-Id", userIdHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPoints", notNullValue()))
                .andExpect(jsonPath("$.data.currentRank", notNullValue()));

        mockMvc.perform(get("/tools/todo/ranking/ranks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()));
    }

    @Test
    public void e2e_export_and_import_json() throws Exception {
        // Ensure some data exists
        String id = createTask("E2E-TODO-EXPORT");
        mockMvc.perform(post("/tools/todo/tasks/{id}/complete", id)
                        .header("X-User-Id", userIdHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(KasonJsonUtils.toJsonPretty(Collections.singletonMap("status", "success"))))
                .andExpect(status().isOk());

        // Export
        MvcResult exp = mockMvc.perform(get("/tools/todo/export/todo").header("X-User-Id", userIdHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version", notNullValue()))
                .andReturn();
        Map exportBody = KasonJsonUtils.fromJson(exp.getResponse().getContentAsString(), Map.class);
        Map data = (Map) exportBody.get("data");

        // Minimal import payload (JSON body)
        Map<String, Object> importBody = new LinkedHashMap<>();
        Map<String, Object> tasks = new LinkedHashMap<>();
        tasks.put(LocalDate.now().toString(), Collections.singletonList(
                new LinkedHashMap<String, Object>() {{
                    put("title", "E2E-IMPORTED-1");
                    put("successPoints", 7);
                }}
        ));
        Map<String, Object> history = new LinkedHashMap<>();
        history.put(LocalDate.now().toString(), Collections.singletonList(
                new LinkedHashMap<String, Object>() {{
                    put("title", "Imported History 1");
                    put("status", "success");
                    put("pointsApplied", 5);
                }}
        ));
        java.util.List<Map<String, Object>> trash = new ArrayList<>();
        trash.add(new LinkedHashMap<String, Object>() {{ put("title", "Imported Trash 1"); }});
        importBody.put("tasks", tasks);
        importBody.put("history", history);
        importBody.put("trash", trash);

        mockMvc.perform(post("/tools/todo/import/todo")
                        .header("X-User-Id", userIdHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(KasonJsonUtils.toJsonPretty(importBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.importedTasks", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.skippedDuplicates", greaterThanOrEqualTo(0)));
    }

    @Test
    public void e2e_seed_demo_then_rate_limited_on_second_call() throws Exception {
        mockMvc.perform(post("/tools/todo/tasks/demo").header("X-User-Id", userIdHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tasksCreated", is(3)));

        // Calling again for same user should hit ToolsException TOO_MANY_REQUESTS
        mockMvc.perform(post("/tools/todo/tasks/demo").header("X-User-Id", userIdHeader))
                .andExpect(status().isTooManyRequests());
    }

    // ---------- helpers ----------

    private String createTask(String title) throws Exception {
        Map<String, Object> create = new HashMap<>();
        create.put("title", title);
        MvcResult created = mockMvc.perform(post("/tools/todo/tasks")
                        .header("X-User-Id", userIdHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(KasonJsonUtils.toJsonPretty(create)))
                .andExpect(status().isCreated())
                .andReturn();
        Map body = KasonJsonUtils.fromJson(created.getResponse().getContentAsString(), Map.class);
        return String.valueOf(((Map) body.get("data")).get("id"));
    }

    @TestConfiguration
    static class DummyRedisTestConfig {
        @Bean
        @Primary
        public RedisConnectionFactory redisConnectionFactory() {
            RedisConnectionFactory factory = mock(RedisConnectionFactory.class, RETURNS_DEEP_STUBS);
            RedisConnection conn = mock(RedisConnection.class, RETURNS_DEEP_STUBS);
            RedisClusterConnection clusterConn = mock(RedisClusterConnection.class, RETURNS_DEEP_STUBS);
            org.mockito.Mockito.when(factory.getConnection()).thenReturn(conn);
            org.mockito.Mockito.when(factory.getClusterConnection()).thenReturn(clusterConn);
            try { org.mockito.Mockito.when(factory.getSentinelConnection()).thenReturn(null); } catch (Throwable ignored) {}
            return factory;
        }
    }
}

