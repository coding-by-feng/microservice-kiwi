package me.fengorz.kason.tools.config;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kason.tools.model.project.Project;
import me.fengorz.kason.tools.repository.mapper.ProjectMapper;
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
            // Map legacy textual statuses to new codes
            // 未开始/legacy not_started -> glass_ordered
            mapper.update(new Project(), new UpdateWrapper<Project>().eq("status", "未开始").set("status", "glass_ordered"));
            mapper.update(new Project(), new UpdateWrapper<Project>().eq("status", "not_started").set("status", "glass_ordered"));

            // 进行中/施工中/legacy in_progress -> doors_windows_produced
            mapper.update(new Project(), new UpdateWrapper<Project>().eq("status", "进行中").set("status", "doors_windows_produced"));
            mapper.update(new Project(), new UpdateWrapper<Project>().eq("status", "施工中").set("status", "doors_windows_produced"));
            mapper.update(new Project(), new UpdateWrapper<Project>().eq("status", "in_progress").set("status", "doors_windows_produced"));

            // 已完成/完成/legacy completed -> final_payment_received
            mapper.update(new Project(), new UpdateWrapper<Project>().eq("status", "已完成").set("status", "final_payment_received"));
            mapper.update(new Project(), new UpdateWrapper<Project>().eq("status", "完成").set("status", "final_payment_received"));
            mapper.update(new Project(), new UpdateWrapper<Project>().eq("status", "completed").set("status", "final_payment_received"));
        } catch (Exception e) {
            log.warn("Project status normalization skipped ({}).", e.toString());
        }
    }
}
