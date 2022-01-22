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

package me.fengorz.kiwi.admin.api.feign.fallback;

import lombok.Setter;
import me.fengorz.kiwi.admin.api.dto.UserFullInfoDTO;
import me.fengorz.kiwi.admin.api.feign.IUserAPI;
import me.fengorz.kiwi.common.api.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @Author zhanshifeng @Date 2019-09-26 16:59
 */
@Component
public class IUserAPIFallBackImpl implements IUserAPI {

    private static final Logger logger = LoggerFactory.getLogger(IUserAPIFallBackImpl.class);
    @Setter
    private Throwable throwable;

    @Override
    public R<UserFullInfoDTO> info(String username, String from) {
        logger.error("feign 查询用户信息失败:{}", username, throwable);
        return null;
    }
}
