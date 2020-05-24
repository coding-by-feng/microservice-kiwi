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

package me.fengorz.kiwi.word.api.common;

/**
 * @Description word服务静态变量
 * @Author ZhanShiFeng
 * @Date 2020/5/17 11:19 PM
 */
public interface WordConstants {

    String KIWI_WORD_BIZ = "kiwi-word-biz";

    String CACHE_NAMES = "kiwi";

    String CACHE_KEY_PREFIX_CLASS_OPERATE_WORD = "word";

    String CACHE_KEY_PREFIX_CLASS_WORD_MAIN = "word_main";
    String CACHE_KEY_PREFIX_METHOD_ID = "id";
    String CACHE_KEY_PREFIX_METHOD_NAME = "name";

    String CACHE_KEY_PREFIX_CLASS_WORD_VARIANT = "variant";
    String CACHE_KEY_PREFIX_METHOD_VARIANT_NAME = "name";
    String CACHE_KEY_PREFIX_METHOD_ID_NAME = "id_name";
    String CACHE_KEY_PREFIX_METHOD_ID_NAME_TYPE = "id_name_type";

    int VARIANT_TYPE_UNKNOWN = 0;

}
