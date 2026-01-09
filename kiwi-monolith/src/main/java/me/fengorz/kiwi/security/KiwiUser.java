/*
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.fengorz.kiwi.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Custom user details class extending Spring Security's User.
 * Contains additional user information beyond username/password.
 */
@Getter
public class KiwiUser extends User {

    private static final long serialVersionUID = 1L;

    /**
     * User ID from database
     */
    private final Integer userId;

    /**
     * Department ID
     */
    private final Integer deptId;

    /**
     * User's email address
     */
    private final String email;

    /**
     * User's real name
     */
    private final String realName;

    /**
     * User's avatar URL
     */
    private final String avatar;

    /**
     * Registration source (local, google, wechat, qq)
     */
    private final String registerSource;

    public KiwiUser(Integer userId,
                    Integer deptId,
                    String username,
                    String password,
                    String email,
                    String realName,
                    String avatar,
                    String registerSource,
                    boolean enabled,
                    boolean accountNonExpired,
                    boolean credentialsNonExpired,
                    boolean accountNonLocked,
                    Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.userId = userId;
        this.deptId = deptId;
        this.email = email;
        this.realName = realName;
        this.avatar = avatar;
        this.registerSource = registerSource;
    }

    /**
     * Builder for creating KiwiUser instances
     */
    public static KiwiUserBuilder kiwiBuilder() {
        return new KiwiUserBuilder();
    }

    public static class KiwiUserBuilder {
        private Integer userId;
        private Integer deptId;
        private String username;
        private String password;
        private String email;
        private String realName;
        private String avatar;
        private String registerSource;
        private boolean enabled = true;
        private boolean accountNonExpired = true;
        private boolean credentialsNonExpired = true;
        private boolean accountNonLocked = true;
        private Collection<? extends GrantedAuthority> authorities;

        public KiwiUserBuilder userId(Integer userId) {
            this.userId = userId;
            return this;
        }

        public KiwiUserBuilder deptId(Integer deptId) {
            this.deptId = deptId;
            return this;
        }

        public KiwiUserBuilder username(String username) {
            this.username = username;
            return this;
        }

        public KiwiUserBuilder password(String password) {
            this.password = password;
            return this;
        }

        public KiwiUserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public KiwiUserBuilder realName(String realName) {
            this.realName = realName;
            return this;
        }

        public KiwiUserBuilder avatar(String avatar) {
            this.avatar = avatar;
            return this;
        }

        public KiwiUserBuilder registerSource(String registerSource) {
            this.registerSource = registerSource;
            return this;
        }

        public KiwiUserBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public KiwiUserBuilder accountNonExpired(boolean accountNonExpired) {
            this.accountNonExpired = accountNonExpired;
            return this;
        }

        public KiwiUserBuilder credentialsNonExpired(boolean credentialsNonExpired) {
            this.credentialsNonExpired = credentialsNonExpired;
            return this;
        }

        public KiwiUserBuilder accountNonLocked(boolean accountNonLocked) {
            this.accountNonLocked = accountNonLocked;
            return this;
        }

        public KiwiUserBuilder authorities(Collection<? extends GrantedAuthority> authorities) {
            this.authorities = authorities;
            return this;
        }

        public KiwiUser build() {
            return new KiwiUser(userId, deptId, username, password, email, realName, avatar,
                    registerSource, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        }
    }
}
