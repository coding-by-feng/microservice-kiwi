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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.domain.upms.entity.SysRole;
import me.fengorz.kiwi.domain.upms.entity.SysUser;
import me.fengorz.kiwi.domain.upms.service.SysUserService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Custom UserDetailsService implementation
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KiwiUserDetailsService implements UserDetailsService {

    private final SysUserService sysUserService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserService.findByUsernameWithRoles(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return buildKiwiUser(user);
    }

    /**
     * Load user by email (for OAuth2 login)
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
        SysUser user = sysUserService.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return buildKiwiUser(user);
    }

    /**
     * Load user by Google OpenID
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserByGoogleOpenid(String googleOpenid) throws UsernameNotFoundException {
        SysUser user = sysUserService.findByGoogleOpenid(googleOpenid)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with Google OpenID: " + googleOpenid));

        return buildKiwiUser(user);
    }

    /**
     * Build KiwiUser from SysUser entity
     */
    private KiwiUser buildKiwiUser(SysUser user) {
        Collection<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(SysRole::getRoleCode)
                .map(code -> new SimpleGrantedAuthority("ROLE_" + code))
                .collect(Collectors.toSet());

        return KiwiUser.kiwiBuilder()
                .userId(user.getUserId())
                .deptId(user.getDeptId())
                .username(user.getUsername())
                .password(user.getPassword())
                .email(user.getEmail())
                .realName(user.getRealName())
                .avatar(user.getAvatar())
                .registerSource(user.getRegisterSource())
                .enabled(true)
                .accountNonExpired(true)
                .credentialsNonExpired(true)
                .accountNonLocked(!user.isLocked())
                .authorities(authorities)
                .build();
    }
}
