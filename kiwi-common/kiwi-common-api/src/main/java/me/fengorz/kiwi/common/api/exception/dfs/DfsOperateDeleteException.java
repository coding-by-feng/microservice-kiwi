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

package me.fengorz.kiwi.common.api.exception.dfs;

/**
 * @Description Dfs文件删除异常
 * @Author ZhanShiFeng
 * @Date 2019/11/7 11:38 PM
 */
public class DfsOperateDeleteException extends DfsOperateException {

    public DfsOperateDeleteException() {
        super();
    }

    public DfsOperateDeleteException(String message) {
        super(message);
    }

    public DfsOperateDeleteException(String message, Throwable cause) {
        super(message, cause);
    }

    public DfsOperateDeleteException(Throwable cause) {
        super(cause);
    }

    public DfsOperateDeleteException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}