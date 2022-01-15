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

package me.fengorz.kiwi.common.sdk.constant;

import lombok.experimental.UtilityClass;

/** @Author zhanshifeng @Date 2019-09-06 15:06 */
@UtilityClass
public class SecurityConstants {

  public final String KEY_PASSWORD = "password";
  public final String KEY_ALGORITHM = "AES";
  public final String KEY_GRANT_TYPE = "grant_type";
  public final String KEY_CODE = "code";
  public final String KEY_MOBILE = "mobile";
  public final String KEY_RANDOM_STR = "randomStr";

  public final String KEY_HEADER_FROM = "from";
  public final String KEY_HEADER_BASIC_ = "Basic ";

  /** 验证码Redis key前缀 */
  public final String DEFAULT_CODE_KEY = "DEFAULT_CODE_KEY_";

  /** 默认登录URL */
  public final String URL_OAUTH_TOKEN_URL = "/oauth/token";
  /*
   * 刷新token
   */
  public final String REFRESH_TOKEN = "refresh_token";

  /** 前缀 */
  public final String PROJECT_PREFIX = "kiwi_";

  /** oauth 相关前缀 */
  public final String OAUTH_PREFIX = "oauth:";

  /** oauth 客户端信息 */
  public final String CLIENT_DETAILS_KEY = PROJECT_PREFIX + OAUTH_PREFIX + "client:details";

  public final String CLIENT_FIELDS =
      "client_id, CONCAT('{noop}',client_secret) as client_secret, resource_ids, scope, "
          + "authorized_grant_types, web_server_redirect_uri, authorities, access_token_validity, "
          + "refresh_token_validity, additional_information, autoapprove";

  /** JdbcClientDetailsService 查询语句 */
  public final String BASE_FIND_STATEMENT =
      "select " + CLIENT_FIELDS + " from sys_oauth_client_details";

  /** 默认的查询语句 */
  public final String DEFAULT_FIND_STATEMENT = BASE_FIND_STATEMENT + " order by client_id";

  /** 按条件client_id 查询 */
  public final String DEFAULT_SELECT_STATEMENT = BASE_FIND_STATEMENT + " where client_id = ?";

  /** 用户ID字段 */
  public final String DETAILS_USER_ID = "user_id";

  /** 用户名字段 */
  public final String DETAILS_USERNAME = "user_name";

  /** 用户部门字段 */
  public final String DETAILS_DEPT_ID = "dept_id";

  /** 协议字段 */
  public final String DETAILS_LICENSE = "license";

  /** 项目的license */
  public final String PROJECT_LICENSE = "made by kiwi";

  /** 内部 */
  public final String FROM_IN = "Y";

  /** 角色前缀 */
  public final String ROLE = "ROLE_";

  /** {bcrypt} 加密的特征码 */
  public final String BCRYPT = "{bcrypt}";

  /***
   * 资源服务器默认bean名称
   */
  public final String RESOURCE_SERVER_CONFIGURER = "resourceServerConfigurerAdapter";
}
