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
package me.fengorz.kiwi.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.fengorz.kiwi.admin.api.entity.SysUserRoleRel;
import me.fengorz.kiwi.admin.mapper.SysUserRoleRelMapper;
import me.fengorz.kiwi.admin.service.SysUserRoleRelService;
import org.springframework.stereotype.Service;

/**
 * 用户角色表
 *
 * @author codingByFeng
 * @date 2019-09-26 14:39:35
 */
@Service("sysUserRoleRelService")
public class SysUserRoleRelServiceImpl extends ServiceImpl<SysUserRoleRelMapper, SysUserRoleRel> implements SysUserRoleRelService {

}