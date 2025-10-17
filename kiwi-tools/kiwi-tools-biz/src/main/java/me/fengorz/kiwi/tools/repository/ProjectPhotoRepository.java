package me.fengorz.kiwi.tools.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.tools.model.ProjectPhoto;
import me.fengorz.kiwi.tools.repository.mapper.ProjectPhotoMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProjectPhotoRepository {
    private final ProjectPhotoMapper mapper;

    public ProjectPhoto save(ProjectPhoto photo) {
        if (photo.getId() == null || photo.getId().isEmpty()) {
            photo.setId(String.valueOf(IdWorker.getId()));
        }
        if (photo.getCreatedAt() == null) {
            photo.setCreatedAt(LocalDateTime.now());
        }
        if (photo.getSortOrder() == null) {
            Integer max = mapper.selectCount(new LambdaQueryWrapper<ProjectPhoto>().eq(ProjectPhoto::getProjectId, photo.getProjectId())) > 0
                    ? mapper.selectList(new LambdaQueryWrapper<ProjectPhoto>().eq(ProjectPhoto::getProjectId, photo.getProjectId()).orderByDesc(ProjectPhoto::getSortOrder).last("limit 1")).stream().findFirst().map(ProjectPhoto::getSortOrder).orElse(0)
                    : 0;
            photo.setSortOrder(max + 1);
        }
        mapper.insert(photo);
        return photo;
    }

    public Optional<ProjectPhoto> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    public List<ProjectPhoto> findByProjectId(String projectId) {
        LambdaQueryWrapper<ProjectPhoto> qw = new LambdaQueryWrapper<ProjectPhoto>()
                .eq(ProjectPhoto::getProjectId, projectId)
                .orderByAsc(ProjectPhoto::getSortOrder)
                .orderByAsc(ProjectPhoto::getCreatedAt);
        return mapper.selectList(qw);
    }

    public void deleteById(String id) {
        mapper.deleteById(id);
    }
}

