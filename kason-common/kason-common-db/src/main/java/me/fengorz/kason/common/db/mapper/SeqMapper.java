package me.fengorz.kason.common.db.mapper;

import me.fengorz.kason.common.db.entity.Sequence;

/**
 * 描述: 序列及排序相关Mapper类，包括产生序列，产生树表的treeCode,表的排序等
 */
public interface SeqMapper {

    /**
     * 生成一个整型序列
     *
     * @param seq 序列对象
     * @return 新生成的ID
     */
    Integer genSequence(Sequence seq);

    /**
     * 删除序列
     *
     * @param seq 序列对象
     * @return 新生成的ID
     */
    Integer deleteSequence(Sequence seq);
}
