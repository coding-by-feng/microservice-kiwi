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

package me.fengorz.kiwi.admin.api.feign;

import me.fengorz.kiwi.admin.api.dto.UserFullInfoDTO;
import me.fengorz.kiwi.admin.api.feign.factory.RemoteUserServiceFallBackFactory;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.constant.SecurityConstants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * @Description 用户信息服务
 * @Author codingByFeng
 * @Date 2019-09-26 09:25
 */
@FeignClient(contextId = "remoteUserService", value = "kiwi-upms", fallbackFactory = RemoteUserServiceFallBackFactory.class)
public interface RemoteUserService {

    @GetMapping("/sys/user/info/{username}")
    R<UserFullInfoDTO> info(@PathVariable("username") String username, @RequestHeader(SecurityConstants.KEY_HEADER_FROM) String from);

}
