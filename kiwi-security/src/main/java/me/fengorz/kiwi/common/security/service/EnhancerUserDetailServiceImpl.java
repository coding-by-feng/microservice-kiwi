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

package me.fengorz.kiwi.common.security.service;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.admin.api.dto.UserFullInfoDTO;
import me.fengorz.kiwi.admin.api.entity.SysUser;
import me.fengorz.kiwi.admin.api.feign.RemoteUserService;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.common.api.constant.SecurityConstants;
import me.fengorz.kiwi.common.api.entity.EnhancerUser;
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
 * @Description TODO
 * @Author codingByFeng
 * @Date 2019-09-25 16:50
 */
@Slf4j
@Service
@AllArgsConstructor
public class EnhancerUserDetailServiceImpl implements UserDetailsService {
    private final CacheManager cacheManager;
    private final RemoteUserService remoteUserService;

    @Override
    public UserDetails loadUserByUsername(String username) {
        Cache cache = cacheManager.getCache("user_details");
        if (cache != null && cache.get(username) != null) {
            return (EnhancerUser) cache.get(username).get();
        }
        R<UserFullInfoDTO> info = remoteUserService.info(username, SecurityConstants.FROM_IN);
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

        Collection<? extends GrantedAuthority> authorities
                = AuthorityUtils.createAuthorityList(dbAuthsSet.toArray(new String[0]));
        SysUser user = info.getSysUser();

        return new EnhancerUser(user.getUserId(), user.getDeptId(), user.getUsername(), SecurityConstants.BCRYPT + user.getPassword(),
                StrUtil.equals(user.getDelFlag(), CommonConstants.STATUS_NORMAL), true, true, true, authorities);
    }


}
