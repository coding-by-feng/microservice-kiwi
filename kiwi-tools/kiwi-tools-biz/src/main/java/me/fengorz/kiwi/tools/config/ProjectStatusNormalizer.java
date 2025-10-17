package me.fengorz.kiwi.tools.config;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.tools.model.Project;
import me.fengorz.kiwi.tools.repository.mapper.ProjectMapper;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectStatusNormalizer {

    private final ProjectMapper mapper;

    @PostConstruct
    public void normalize() {
        try {
            // 未开始 -> not_started
            mapper.update(new Project(), new UpdateWrapper<Project>().eq("status", "未开始").set("status", "not_started"));
            // 进行中/施工中 -> in_progress
            mapper.update(new Project(), new UpdateWrapper<Project>().eq("status", "进行中").set("status", "in_progress"));
            mapper.update(new Project(), new UpdateWrapper<Project>().eq("status", "施工中").set("status", "in_progress"));
            // 已完成/完成 -> completed
            mapper.update(new Project(), new UpdateWrapper<Project>().eq("status", "已完成").set("status", "completed"));
            mapper.update(new Project(), new UpdateWrapper<Project>().eq("status", "完成").set("status", "completed"));
        } catch (Exception e) {
            log.warn("Project status normalization skipped ({}).", e.toString());
        }
    }
}

