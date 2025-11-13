package me.fengorz.kason.tools.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.fengorz.kason.tools.model.todo.TodoHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TodoHistoryMapper extends BaseMapper<TodoHistory> {
}

