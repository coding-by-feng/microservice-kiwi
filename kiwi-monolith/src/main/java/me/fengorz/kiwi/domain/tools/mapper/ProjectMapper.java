/*
 * Copyright [2019~2025] [codingByFeng]
 */
package me.fengorz.kiwi.domain.tools.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.fengorz.kiwi.domain.tools.entity.Project;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
}
