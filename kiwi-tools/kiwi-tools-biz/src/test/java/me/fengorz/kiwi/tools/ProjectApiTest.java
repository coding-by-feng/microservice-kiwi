package me.fengorz.kiwi.tools;

import me.fengorz.kiwi.common.sdk.util.json.KiwiJsonUtils;
import me.fengorz.kiwi.tools.model.project.Project;
import org.junit.After;
import org.junit.Assert;
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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@SpringBootTest(
        classes = {ToolsBizTestApplication.class, ToolsBizTestOverrides.class},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(ProjectApiTest.DummyRedisTestConfig.class)
public class ProjectApiTest {
    static {
        // Workaround for JAXB optimization incompatibility with JDK 24 used by Spring Security OAuth2
        System.setProperty("com.sun.xml.bind.v2.bytecode.ClassTailor.noOptimize", "true");
    }

    @Autowired
    MockMvc mockMvc;


    private static final String PREFIX = "E2E-AKL-";

    @Before
    public void beforeEach() throws Exception {
        cleanupByPrefix(PREFIX);
    }

    @After
    public void afterEach() throws Exception {
        cleanupByPrefix(PREFIX);
    }

    @Test
    public void testCreateAndList() throws Exception {
        // UTF-8 JSON MediaType
        MediaType JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON.getType(),
                MediaType.APPLICATION_JSON.getSubtype(), StandardCharsets.UTF_8);

        Map<String, Object> body = new HashMap<>();
        body.put("id", "101"); // dummy if required by your controller
        body.put("projectCode", "P-101"); // dummy if server generates, it will be ignored
        body.put("name", "Kitchen remodel");
        // now use English status code
        body.put("status", "not_started");
        // dummy required fields (adapt to your actual request model validation)
        body.put("clientName", "测试客户");
        body.put("address", "上海市浦东新区世纪大道1号");
        body.put("salesPerson", "Alice");
        body.put("installer", "Bob");
        body.put("teamMembers", "Alice,Bob,Charlie");
        body.put("startDate", "2025-10-14");
        body.put("endDate", "2025-10-20");
        body.put("todayTask", "安装橱柜");
        body.put("progressNote", "初始创建记录");
        // photoUrl removed; photos are managed via /api/projects/{id}/photo endpoints

        mockMvc.perform(post("/api/projects")
                        .characterEncoding(StandardCharsets.UTF_8.name())
                        .contentType(JSON_UTF8)
                        .accept(JSON_UTF8)
                        .content(KiwiJsonUtils.toJsonPretty(body)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.projectCode", startsWith("P-")))
                // API returns English code now
                .andExpect(jsonPath("$.status", is("not_started")));

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

    @Test
    public void e2e_create100_list_then_delete50_and_verify_totals() throws Exception {
        long baseline = getTotal(true, null);

        // create 100
        java.util.List<Project> created = new java.util.ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            Project p = createProject(PREFIX + i, "not_started");
            Assert.assertNotNull(p.getId());
            Assert.assertNotNull(p.getProjectCode());
            Assert.assertFalse(Boolean.TRUE.equals(p.getArchived()));
            created.add(p);
        }

        long afterCreate = getTotal(true, null);
        Assert.assertEquals(baseline + 100, afterCreate);

        // delete first 50
        for (int i = 0; i < 50; i++) {
            deleteProject(created.get(i).getId());
        }

        long afterDelete = getTotal(true, null);
        Assert.assertEquals(baseline + 50, afterDelete);

        // simple pagination check (defaults exclude archived)
        MvcResult page1 = mockMvc.perform(get("/api/projects")
                        .param("page", "1")
                        .param("pageSize", "20")
                        .param("sortBy", "created_at")
                        .param("sortOrder", "desc"))
                .andExpect(status().isOk())
                .andReturn();
        java.util.Map<?, ?> body = KiwiJsonUtils.fromJson(page1.getResponse().getContentAsString(), java.util.Map.class);
        Assert.assertEquals(20, ((java.util.List<?>) body.get("items")).size());

        // cleanup remaining 50
        for (int i = 50; i < 100; i++) {
            deleteProject(created.get(i).getId());
        }
        long finalTotal = getTotal(true, null);
        Assert.assertEquals(baseline, finalTotal);
    }

    @Test
    public void e2e_archive_post_api_and_filters() throws Exception {
        // create 10
        java.util.List<Project> created = new java.util.ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            created.add(createProject(PREFIX + "ARCH-" + i, "not_started"));
        }

        // archive 4
        for (int i = 0; i < 4; i++) {
            Project p = archiveProject(created.get(i).getId(), true);
            Assert.assertTrue(Boolean.TRUE.equals(p.getArchived()));
        }

        // idempotent
        Project again = archiveProject(created.get(0).getId(), true);
        Assert.assertTrue(Boolean.TRUE.equals(again.getArchived()));

        // counts using q filter
        long defaultTotal = getTotal(false, PREFIX + "ARCH-");
        long allTotal = getTotal(true, PREFIX + "ARCH-");
        long archivedOnly = getTotalWithArchivedFilter(true, PREFIX + "ARCH-");
        long activeOnly = getTotalWithArchivedFilter(false, PREFIX + "ARCH-");

        Assert.assertEquals(6, defaultTotal);
        Assert.assertEquals(10, allTotal);
        Assert.assertEquals(4, archivedOnly);
        Assert.assertEquals(6, activeOnly);

        // unarchive 2
        for (int i = 0; i < 2; i++) {
            Project p = archiveProject(created.get(i).getId(), false);
            Assert.assertFalse(Boolean.TRUE.equals(p.getArchived()));
        }
        Assert.assertEquals(2, getTotalWithArchivedFilter(true, PREFIX + "ARCH-"));
        Assert.assertEquals(8, getTotalWithArchivedFilter(false, PREFIX + "ARCH-"));
    }

    @Test
    public void e2e_validation_and_patch_update() throws Exception {
        // invalid end before start
        java.util.Map<String, Object> bad = new java.util.HashMap<>();
        bad.put("name", PREFIX + "BAD");
        bad.put("startDate", "2025-03-10");
        bad.put("endDate", "2025-03-28");
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(KiwiJsonUtils.toJsonPretty(bad)))
                .andExpect(status().isBadRequest());

        // create valid
        Project p = createProject(PREFIX + "PATCH", "not_started");

        // patch fields and archived
        java.util.Map<String, Object> patchBody = new java.util.HashMap<>();
        patchBody.put("clientName", "Kiwi Ltd");
        patchBody.put("status", "not_started");
        patchBody.put("archived", true);

        MvcResult patchedRes = mockMvc.perform(patch("/api/projects/{id}", p.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(KiwiJsonUtils.toJsonPretty(patchBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientName", is("Kiwi Ltd")))
                .andExpect(jsonPath("$.archived", is(true)))
                .andReturn();

        Project patched = KiwiJsonUtils.fromJson(patchedRes.getResponse().getContentAsString(), Project.class);

        // get by id
        mockMvc.perform(get("/api/projects/{id}", patched.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(patched.getId())));

        // delete
        deleteProject(patched.getId());

        mockMvc.perform(get("/api/projects/{id}", patched.getId()))
                .andExpect(status().isNotFound());
    }

    // ---------- Helpers ----------

    private Project createProject(String name, String status) throws Exception {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("name", name);
        body.put("status", status);
        body.put("address", "Auckland CBD, NZ");

        MvcResult res = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(KiwiJsonUtils.toJsonPretty(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.projectCode", startsWith("P-")))
                .andReturn();

        return KiwiJsonUtils.fromJson(res.getResponse().getContentAsString(), Project.class);
    }

    private Project archiveProject(String id, Boolean archived) throws Exception {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        if (archived != null) body.put("archived", archived);
        MvcResult res = mockMvc.perform(post("/api/projects/{id}/archive", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(KiwiJsonUtils.toJsonPretty(body)))
                .andExpect(status().isOk())
                .andReturn();
        return KiwiJsonUtils.fromJson(res.getResponse().getContentAsString(), Project.class);
    }

    private void deleteProject(String id) throws Exception {
        mockMvc.perform(delete("/api/projects/{id}", id))
                .andExpect(status().isNoContent());
    }

    private long getTotal(boolean includeArchived, String q) throws Exception {
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req = get("/api/projects")
                .param("page", "1")
                .param("pageSize", "1")
                .param("sortBy", "created_at")
                .param("sortOrder", "desc");
        if (includeArchived) req.param("includeArchived", "true");
        if (q != null) req.param("q", q);

        MvcResult res = mockMvc.perform(req)
                .andExpect(status().isOk())
                .andReturn();
        java.util.Map<?, ?> body = KiwiJsonUtils.fromJson(res.getResponse().getContentAsString(), java.util.Map.class);
        return ((Number) body.get("total")).longValue();
    }

    private long getTotalWithArchivedFilter(boolean archived, String q) throws Exception {
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req = get("/api/projects")
                .param("page", "1")
                .param("pageSize", "1")
                .param("sortBy", "created_at")
                .param("sortOrder", "desc")
                .param("archived", String.valueOf(archived));
        if (q != null) req.param("q", q);

        MvcResult res = mockMvc.perform(req)
                .andExpect(status().isOk())
                .andReturn();
        java.util.Map<?, ?> body = KiwiJsonUtils.fromJson(res.getResponse().getContentAsString(), java.util.Map.class);
        return ((Number) body.get("total")).longValue();
    }

    private void cleanupByPrefix(String prefix) throws Exception {
        // list with includeArchived=true and q=prefix, then delete all
        int page = 1;
        int pageSize = 200;
        long total;
        java.util.List<java.util.Map<String, Object>> collected = new java.util.ArrayList<>();
        do {
            MvcResult res = mockMvc.perform(get("/api/projects")
                            .param("q", prefix)
                            .param("page", String.valueOf(page))
                            .param("pageSize", String.valueOf(pageSize))
                            .param("includeArchived", "true")
                            .param("sortBy", "created_at")
                            .param("sortOrder", "desc"))
                    .andExpect(status().isOk())
                    .andReturn();
            java.util.Map body = KiwiJsonUtils.fromJson(res.getResponse().getContentAsString(), java.util.Map.class);
            total = ((Number) body.get("total")).longValue();
            java.util.List items = (java.util.List) body.get("items");
            if (items != null) collected.addAll(items);
            page++;
        } while ((page - 1) * pageSize < total);

        for (java.util.Map<String, Object> m : collected) {
            Object id = m.get("id");
            if (id != null) {
                try {
                    deleteProject(String.valueOf(id));
                } catch (Exception ignored) {
                }
            }
        }
    }

    @TestConfiguration
    static class DummyRedisTestConfig {
        @Bean
        @Primary
        public RedisConnectionFactory redisConnectionFactory() {
            RedisConnectionFactory factory = mock(RedisConnectionFactory.class, RETURNS_DEEP_STUBS);
            RedisConnection conn = mock(RedisConnection.class, RETURNS_DEEP_STUBS);
            RedisClusterConnection clusterConn = mock(RedisClusterConnection.class, RETURNS_DEEP_STUBS);
            when(factory.getConnection()).thenReturn(conn);
            when(factory.getClusterConnection()).thenReturn(clusterConn);
            // reactive methods are rarely used; return null to avoid bringing reactor into tests
            try {
                // method may not exist on older Spring Data; ignore if reflective failures occur
                when(factory.getSentinelConnection()).thenReturn(null);
            } catch (Throwable ignored) { }
            return factory;
        }
    }
}
