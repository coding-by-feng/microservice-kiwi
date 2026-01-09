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
package me.fengorz.kiwi.common.constant;

/**
 * Security Constants
 *
 * @author codingByFeng
 */
public final class SecurityConstants {

    private SecurityConstants() {}

    public static final String KEY_PASSWORD = "password";
    public static final String KEY_ALGORITHM = "AES";
    public static final String KEY_GRANT_TYPE = "grant_type";
    public static final String KEY_CODE = "code";
    public static final String KEY_MOBILE = "mobile";
    public static final String KEY_RANDOM_STR = "randomStr";

    public static final String KEY_HEADER_FROM = "from";
    public static final String KEY_HEADER_BASIC_ = "Basic ";

    /** Verification code Redis key prefix */
    public static final String DEFAULT_CODE_KEY = "DEFAULT_CODE_KEY_";

    /** Default login URL */
    public static final String URL_OAUTH_TOKEN_URL = "/oauth/token";

    /** Refresh token */
    public static final String REFRESH_TOKEN = "refresh_token";

    /** Project prefix */
    public static final String PROJECT_PREFIX = "kiwi_";

    /** OAuth prefix */
    public static final String OAUTH_PREFIX = "oauth:";

    /** OAuth client details key */
    public static final String CLIENT_DETAILS_KEY = PROJECT_PREFIX + OAUTH_PREFIX + "client:details";

    public static final String CLIENT_FIELDS =
            "client_id, client_secret, resource_ids, scope, "
                    + "authorized_grant_types, web_server_redirect_uri, authorities, access_token_validity, "
                    + "refresh_token_validity, additional_information, autoapprove";

    /** JdbcClientDetailsService query statement */
    public static final String BASE_FIND_STATEMENT = "select " + CLIENT_FIELDS + " from sys_oauth_client_details";

    /** Default query statement */
    public static final String DEFAULT_FIND_STATEMENT = BASE_FIND_STATEMENT + " order by client_id";

    /** Query by client_id */
    public static final String DEFAULT_SELECT_STATEMENT = BASE_FIND_STATEMENT + " where client_id = ?";

    /** User ID field */
    public static final String DETAILS_USER_ID = "user_id";

    /** Username field */
    public static final String DETAILS_USERNAME = "user_name";

    /** Department ID field */
    public static final String DETAILS_DEPT_ID = "dept_id";

    /** License field */
    public static final String DETAILS_LICENSE = "license";

    /** Project license */
    public static final String PROJECT_LICENSE = "made by kiwi";

    /** Internal flag */
    public static final String FROM_IN = "Y";

    /** Role prefix */
    public static final String ROLE = "ROLE_";

    /** Bcrypt prefix */
    public static final String BCRYPT = "{bcrypt}";

    /** Resource server bean name */
    public static final String RESOURCE_SERVER_CONFIGURER = "resourceServerConfigurerAdapter";
}
