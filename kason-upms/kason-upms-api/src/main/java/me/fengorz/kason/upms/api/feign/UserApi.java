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

package me.fengorz.kason.upms.api.feign;

import me.fengorz.kason.common.api.R;
import me.fengorz.kason.common.sdk.constant.EnvConstants;
import me.fengorz.kason.common.sdk.constant.SecurityConstants;
import me.fengorz.kason.upms.api.dto.UserFullInfoDTO;
import me.fengorz.kason.upms.api.entity.SysUser;
import me.fengorz.kason.upms.api.feign.factory.UserApiFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * @Description TODO
 * @Author Kason Zhan
 * @Date 2022/9/18 10:15
 */
@FeignClient(contextId = "userApi", value = EnvConstants.APPLICATION_NAME_KASON_UPMS,
        fallbackFactory = UserApiFallbackFactory.class)
public interface UserApi {

    String SYS_USER = "/sys/user";

    @GetMapping(SYS_USER + "/info/{username}")
    R<UserFullInfoDTO> info(@PathVariable("username") String username);

    @GetMapping(SYS_USER + "/info/{username}")
    R<UserFullInfoDTO> info(@PathVariable("username") String username,
                            @RequestHeader(SecurityConstants.KEY_HEADER_FROM) String from);

    @PostMapping(SYS_USER)
    R<Boolean> save(@RequestBody SysUser sysUser);

    @PostMapping(SYS_USER)
    R<Boolean> save(@RequestBody SysUser sysUser,
                    @RequestHeader(SecurityConstants.KEY_HEADER_FROM) String from);

}