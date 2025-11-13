package me.fengorz.kason.common.db.service;

/**
 *
 */
public interface SeqService {

    /**
     * 产生一个整型的序列
     *
     * @param seqTable 对应的表
     * @return 整型ID
     */
    Integer genIntSequence(String seqTable);

    Integer genCommonIntSequence();
}
