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

import me.fengorz.kiwi.common.api.ResultCode;
import me.fengorz.kiwi.common.api.exception.BaseException;

/**
 * @Description Dfs文件操作异常
 * @Author zhanshifeng
 * @Date 2019/11/7 11:38 PM
 */
public class DfsOperateException extends BaseException {

    public DfsOperateException() {
        super();
    }

    public DfsOperateException(Throwable cause) {
        super(cause);
    }

    public DfsOperateException(String msg, Object... args) {
        super(msg, args);
    }

    public DfsOperateException(String msg, Throwable throwable, Object... args) {
        super(msg, throwable, args);
    }

    public DfsOperateException(ResultCode resultCode, Object... args) {
        super(resultCode, args);
    }

    public DfsOperateException(String msg, ResultCode resultCode, Object... args) {
        super(msg, resultCode, args);
    }

    public DfsOperateException(String msg, ResultCode resultCode, Throwable throwable, Object... args) {
        super(msg, resultCode, throwable, args);
    }
}
