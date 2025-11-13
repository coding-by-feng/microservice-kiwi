package me.fengorz.kason.tools.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.fengorz.kason.tools.model.todo.TodoTrash;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TodoTrashMapper extends BaseMapper<TodoTrash> {
}

