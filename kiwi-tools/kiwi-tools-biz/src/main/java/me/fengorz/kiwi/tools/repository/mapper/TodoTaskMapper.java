package me.fengorz.kiwi.tools.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.fengorz.kiwi.tools.model.todo.TodoTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TodoTaskMapper extends BaseMapper<TodoTask> {
}

