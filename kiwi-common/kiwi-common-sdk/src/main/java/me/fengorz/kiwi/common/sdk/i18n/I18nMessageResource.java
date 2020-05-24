/*
 *
 *   Copyright [2019~2025] [codingByFeng]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *
 */

package me.fengorz.kiwi.common.sdk.i18n;

import org.springframework.context.NoSuchMessageException;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import java.text.MessageFormat;
import java.util.Locale;

/**
 * @Description 国际化消息源配置
 * @Author ZhanShiFeng
 * @Date 2020/4/8 4:14 PM
 */
public class I18nMessageResource extends AbstractMessageSource implements ResourceLoaderAware {
    private ResourceLoader resourceLoader;

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader != null ? resourceLoader : new DefaultResourceLoader();
    }

    @Override
    protected MessageFormat resolveCode(String code, Locale locale) {
        return this.createMessageFormat(this.getText(code, locale), locale);
    }

    private String getText(String code, Locale locale) {
        String text = null;
        try {
            if (this.getParentMessageSource() != null) {
                text = this.getParentMessageSource().getMessage(code, null, locale);
            }
        } catch (NoSuchMessageException var8) {
            this.logger.error("Cannot find message with code: " + code);
        }
        return text;
    }
}
