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

package me.fengorz.kiwi.word.api.exception;

import me.fengorz.kiwi.common.sdk.exception.BaseException;

/** @Author zhanshifeng @Date 2019/11/1 2:54 PM */
public class WordResultStoreException extends BaseException {

  private static final long serialVersionUID = 4431850064509376871L;

  public WordResultStoreException() {}

  public WordResultStoreException(String message) {
    super(message);
  }

  public WordResultStoreException(String message, Throwable cause) {
    super(message, cause);
  }

  public WordResultStoreException(Throwable cause) {
    super(cause);
  }

  public WordResultStoreException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
