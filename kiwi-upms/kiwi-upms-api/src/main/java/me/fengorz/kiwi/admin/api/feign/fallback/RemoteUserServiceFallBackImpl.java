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

package me.fengorz.kiwi.admin.api.feign.fallback;

import me.fengorz.kiwi.admin.api.dto.UserFullInfoDTO;
import me.fengorz.kiwi.admin.api.feign.RemoteUserService;
import me.fengorz.kiwi.common.api.R;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @Description TODO
 * @Author codingByFeng
 * @Date 2019-09-26 16:59
 */
@Slf4j
@Component
public class RemoteUserServiceFallBackImpl implements RemoteUserService {

    @Setter
    private Throwable throwable;

    @Override
    public R<UserFullInfoDTO> info(String username, String from) {
        log.error("feign 查询用户信息失败:{}", username, throwable);
        return null;
    }
}
