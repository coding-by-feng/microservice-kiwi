package me.fengorz.kiwi.tools.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.fengorz.kiwi.tools.model.todo.TodoTrash;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TodoTrashMapper extends BaseMapper<TodoTrash> {
}

