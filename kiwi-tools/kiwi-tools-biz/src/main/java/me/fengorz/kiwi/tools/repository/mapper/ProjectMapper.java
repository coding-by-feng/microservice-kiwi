package me.fengorz.kiwi.tools.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.fengorz.kiwi.tools.model.project.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
    /**
     * Join query with project_stage_status for search with optional stage filters and pagination.
     */
    List<Project> searchJoined(
            Page<Project> page,
            @Param("q") String q,
            @Param("stageGlass") Boolean stageGlass,
            @Param("stageFrame") Boolean stageFrame,
            @Param("stagePurchase") Boolean stagePurchase,
            @Param("stageTransport") Boolean stageTransport,
            @Param("stageInstall") Boolean stageInstall,
            @Param("stageRepair") Boolean stageRepair,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("archived") Boolean archived,
            @Param("includeArchived") Boolean includeArchived,
            @Param("sortColumn") String sortColumn,
            @Param("sortOrder") String sortOrder
    );

    long countJoined(
            @Param("q") String q,
            @Param("stageGlass") Boolean stageGlass,
            @Param("stageFrame") Boolean stageFrame,
            @Param("stagePurchase") Boolean stagePurchase,
            @Param("stageTransport") Boolean stageTransport,
            @Param("stageInstall") Boolean stageInstall,
            @Param("stageRepair") Boolean stageRepair,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("archived") Boolean archived,
            @Param("includeArchived") Boolean includeArchived
    );
}
