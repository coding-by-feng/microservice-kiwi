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

package me.fengorz.kiwi.common.api.entity;

import lombok.Getter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

import static me.fengorz.kiwi.common.api.ApiContants.ADMIN_USERNAME;

/**
 * @Author Kason Zhan @Date 2019-09-25 10:41
 */
public class EnhancerUser extends User implements InitializingBean {

    private static final long serialVersionUID = -3345914637714509878L;

    @Getter
    private final Integer id;

    @Getter
    private final Integer deptId;

    @Getter
    private Boolean isAdmin;

    public EnhancerUser(Integer id, Integer deptId, String username, String password, boolean enabled,
        boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked,
        Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.id = id;
        this.deptId = deptId;
    }

    @Override
    public void afterPropertiesSet() {
        this.isAdmin = ADMIN_USERNAME.equals(getUsername());
    }
}
