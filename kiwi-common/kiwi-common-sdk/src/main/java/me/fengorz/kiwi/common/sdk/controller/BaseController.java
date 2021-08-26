/*
 *
 * Copyright [2019~2025] [zhanshifeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 *
 */

package me.fengorz.kiwi.common.sdk.controller;

import me.fengorz.kiwi.common.api.entity.EnhancerUser;
import me.fengorz.kiwi.common.sdk.web.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * @Description 抽象控制层基类
 * @Author zhanshifeng
 * @Date 2020/4/21 7:28 PM
 */
public abstract class BaseController {

    @Autowired
    protected MessageSource messageSource;
    @Autowired
    protected MessageSource propertiesMessageSource;

    /**
     * 获取当前登录用户
     *
     * @return
     */
    protected EnhancerUser getCurrentUser() {
        return SecurityUtils.getCurrentUser();
    }

    protected String getMessage(String code, Object... args) {
        return this.messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }

    protected String getMessageFromProperties(String code, Object... args) {
        return this.propertiesMessageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }

}
