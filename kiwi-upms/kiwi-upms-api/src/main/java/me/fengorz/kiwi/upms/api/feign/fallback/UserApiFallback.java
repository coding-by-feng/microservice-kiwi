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

package me.fengorz.kiwi.upms.api.feign.fallback;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.feign.AbstractFallback;
import me.fengorz.kiwi.upms.api.dto.UserFullInfoDTO;
import me.fengorz.kiwi.upms.api.entity.SysUser;
import me.fengorz.kiwi.upms.api.feign.UserApi;

/**
 * @Description TODO
 * @Author Kason Zhan
 * @Date 2022/9/18 10:25
 */
@Slf4j
public class UserApiFallback extends AbstractFallback implements UserApi {

    @Override
    public R<UserFullInfoDTO> info(String username) {
        return handleError();
    }
/* <<<<<<<<<<<<<<  ✨ Windsurf Command ⭐ >>>>>>>>>>>>>>>> */
    /**
     * feign查询用户信息失败时的回调
     *
     * @param username 用户名
     * @param from     来源
     * @return {@link R#failed()}
     */
/* <<<<<<<<<<  3b13a914-a6d4-4c2d-ae53-ed686fdd3473  >>>>>>>>>>> */
    @Override
    public R<UserFullInfoDTO> info(String username, String from) {
        log.error("feign 查询用户信息失败:{}", username, throwable);
        return null;
    }

    @Override
    public R<Boolean> save(SysUser sysUser) {
        log.error("feign 保存用户失败:{}", sysUser != null ? sysUser.getUsername() : "null", throwable);
        return handleError();
    }

    @Override
    public R<Boolean> save(SysUser sysUser, String from) {
        log.error("feign 保存用户失败:{}, from:{}", sysUser != null ? sysUser.getUsername() : "null", from, throwable);
        return handleError();
    }

}