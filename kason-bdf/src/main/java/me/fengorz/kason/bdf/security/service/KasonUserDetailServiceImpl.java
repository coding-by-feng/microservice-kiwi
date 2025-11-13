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

package me.fengorz.kason.bdf.security.service;

import cn.hutool.core.util.ArrayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kason.common.api.R;
import me.fengorz.kason.common.api.entity.EnhancerUser;
import me.fengorz.kason.common.sdk.constant.GlobalConstants;
import me.fengorz.kason.common.sdk.constant.SecurityConstants;
import me.fengorz.kason.upms.api.dto.UserFullInfoDTO;
import me.fengorz.kason.upms.api.entity.SysUser;
import me.fengorz.kason.upms.api.feign.UserApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @Author Kason Zhan
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ms.config.exclude-cache", havingValue = "false")
public class KasonUserDetailServiceImpl implements UserDetailsService {
    private final CacheManager cacheManager;
    private final UserApi userApi;

    @Override
    public UserDetails loadUserByUsername(String username) {
        Cache cache = cacheManager.getCache("user_details");
        if (cache != null && cache.get(username) != null) {
            return (EnhancerUser) cache.get(username).get();
        }
        R<UserFullInfoDTO> info = userApi.info(username, SecurityConstants.FROM_IN);
        UserDetails userDetails = getUserDetails(info);
        cache.put(username, userDetails);
        return userDetails;
    }

    private UserDetails getUserDetails(R<UserFullInfoDTO> userFullInfoDTO) {
        if (userFullInfoDTO == null || userFullInfoDTO.getData() == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        UserFullInfoDTO info = userFullInfoDTO.getData();

        Set<String> dbAuthsSet = new HashSet<>();
        if (ArrayUtil.isNotEmpty(info.getRoles())) {
            Arrays.stream(info.getRoles()).forEach(role -> {
                dbAuthsSet.add(SecurityConstants.ROLE + role);
            });

            dbAuthsSet.addAll(Arrays.asList(info.getPermissions()));
        }

        Collection<? extends GrantedAuthority> authorities =
                AuthorityUtils.createAuthorityList(dbAuthsSet.toArray(new String[0]));
        SysUser user = info.getSysUser();

        return new EnhancerUser(user.getUserId(), user.getDeptId(), user.getUsername(),
                SecurityConstants.BCRYPT + user.getPassword(), GlobalConstants.FLAG_DEL_NO == user.getDelFlag(), true, true,
                true, authorities);
    }
}
