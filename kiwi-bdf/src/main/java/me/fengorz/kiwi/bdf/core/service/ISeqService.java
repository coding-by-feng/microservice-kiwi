/*
 * Copyright (c) 2019, CCSSOFT All Rights Reserved.
 */
package me.fengorz.kiwi.bdf.core.service;

/**
 *
 */
public interface ISeqService {

    /**
     * 产生一个整型的序列
     *
     * @param seqTable 对应的表
     * @return 整型ID
     */
    Integer genIntSequence(String seqTable);

}
