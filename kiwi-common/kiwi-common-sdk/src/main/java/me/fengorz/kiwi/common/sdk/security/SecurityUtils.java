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

package me.fengorz.kiwi.common.sdk.security;

import me.fengorz.kiwi.common.api.entity.EnhancerUser;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

/**
 * @Author codingByFeng
 * @Date 2019-09-26 10:19
 */
@UtilityClass
public class SecurityUtils {

    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public EnhancerUser getUser(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof EnhancerUser) {
            return (EnhancerUser) principal;
        }
        return null;
    }

    /**
     * 获取当前登录的用户
     * @return
     */
    public EnhancerUser getCurrentUser() {
        Authentication authentication = getAuthentication();
        if (Objects.isNull(authentication)) {
            return null;
        }
        return getUser(authentication);
    }

    /**
     * 获取当前登录的用户ID
     * @return
     */
    public Integer getCurrentUserId() {
        EnhancerUser currentUser = getCurrentUser();
        if (currentUser != null) {
            currentUser.getId();
        }
        return null;
    }

}