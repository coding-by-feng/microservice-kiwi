/*
 *
 * Copyright [2019~2025] [codingByFeng]
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

package me.fengorz.kiwi.admin.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import me.fengorz.kiwi.admin.api.dto.UserFullInfoDTO;
import me.fengorz.kiwi.admin.api.feign.factory.UserApiFallbackFactory;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.constant.EnvConstants;
import me.fengorz.kiwi.common.sdk.constant.SecurityConstants;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2022/9/18 10:15
 */
@FeignClient(contextId = "userApi", value = EnvConstants.APPLICATION_NAME_KIWI_UPMS,
    fallbackFactory = UserApiFallbackFactory.class)
public interface UserApi {

    String SYS_USER = "/sys/user";

    @GetMapping(SYS_USER + "/info/{username}")
    R<UserFullInfoDTO> info(@PathVariable("username") String username);

    @GetMapping(SYS_USER + "/info/{username}")
    R<UserFullInfoDTO> info(@PathVariable("username") String username,
        @RequestHeader(SecurityConstants.KEY_HEADER_FROM) String from);

}
