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

package me.fengorz.kiwi.common.dfs;

import com.github.tobato.fastdfs.exception.FdfsServerException;
import me.fengorz.kiwi.common.api.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @Description 全局的异常处理器 @Author Kason Zhan @Date 2019/11/26 3:28 PM
 */
@RestControllerAdvice
public class GlobalDfsExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalDfsExceptionHandler.class);

    @ExceptionHandler({FdfsServerException.class})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public R<String> handleFdfsServerException(FdfsServerException e) {
        log.error("global FdfsServerException:{}", e.getMessage(), e);
        return R.failed(e.getMessage());
    }

}
