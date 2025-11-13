package me.fengorz.kason.common.db.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.fengorz.kason.common.sdk.util.bean.KasonBeanUtils;

import java.util.function.Consumer;

public class DbUtils {

    public static <T, E> IPage<T> convertFrom(IPage<E> sourcePage, Class<T> requiredType) {
        if (sourcePage == null) {
            return null;
        } else {
            IPage<T> page = new Page(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal());
            page.setRecords(KasonBeanUtils.convertFrom(sourcePage.getRecords(), requiredType));
            return page;
        }
    }

    public static <T, E> IPage<T> convertFrom(IPage<E> sourcePage, Class<T> requiredType, Consumer<T> consumer) {
        if (sourcePage == null) {
            return null;
        } else {
            IPage<T> page = new Page(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal());
            page.setRecords(KasonBeanUtils.convertFrom(sourcePage.getRecords(), requiredType, consumer));
            return page;
        }
    }


}
