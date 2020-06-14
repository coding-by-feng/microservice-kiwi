/*
 * Copyright (c) 2020, CCSSOFT All Rights Reserved.
 */
package me.fengorz.kiwi.common.es.annotation;

/**
 * ES查询类型
 */
public enum QueryType {
    /**
     * ES
     */
    TERM, MATCH, WILDCARD, PREFIX, BETWEEN, LT, LTE, GT, GTE;
}
