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

package me.fengorz.kiwi.common.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 响应体
 *
 * @param <T> @Author zhanshifeng
 */
@ToString
@Data
@Accessors(chain = true)
public class R<T> implements Serializable {
  private static final long serialVersionUID = -1845782287674831578L;
  private static final String FEIGN_CALL_FAILED = "feign call failed!";

  private Integer code;

  private String msg;

  private T data;

  public static <T> R<T> auto(boolean flag) {
    return auto(flag, ApiContants.EMPTY);
  }

  public static <T> R<T> auto(boolean flag, String msg) {
    if (flag) {
      return R.success();
    } else {
      return R.failed(msg);
    }
  }

  public static <T> R<T> success() {
    return restResult(null, ResultCode.SUCCESS, null);
  }

  public static <T> R<T> success(T data) {
    return restResult(data, ResultCode.SUCCESS, null);
  }

  public static <T> R<T> success(T data, String msg) {
    return restResult(data, ResultCode.SUCCESS, msg);
  }

  public static <T> R<T> failed() {
    return restResult(null, ResultCode.FAIL, null);
  }

  public static <T> R<T> feignCallFailed() {
    return restResult(null, ResultCode.MICROSERVICE_INVOCATION_ERROR, FEIGN_CALL_FAILED);
  }

  public static <T> R<T> feignCallFailed(String msg) {
    return restResult(null, ResultCode.MICROSERVICE_INVOCATION_ERROR, msg);
  }

  public static <T> R<T> failed(String msg) {
    return restResult(null, ResultCode.FAIL, msg);
  }

  public static <T> R<T> failed(ResultCode resultCode, String msg) {
    return restResult(null, resultCode, msg);
  }

  public static <T> R<T> failed(T data) {
    return restResult(data, ResultCode.FAIL, null);
  }

  public static <T> R<T> failed(T data, String msg) {
    return restResult(data, ResultCode.FAIL, msg);
  }

  public static <T> R<T> error() {
    return restResult(null, ResultCode.ERROR, null);
  }

  public static <T> R<T> error(T data) {
    return restResult(data, ResultCode.ERROR, null);
  }

  public static <T> R<T> error(T data, String msg) {
    return restResult(data, ResultCode.ERROR, msg);
  }

  private static <T> R<T> restResult(T data, ResultCode resultCode, String msg) {
    R<T> apiResult = new R<>();
    apiResult.setCode(resultCode.getCode());
    apiResult.setData(data);
    apiResult.setMsg(msg);
    return apiResult;
  }

  public boolean isSuccess() {
    return ResultCode.SUCCESS.getCode().equals(this.code);
  }

  @JsonIgnore
  public boolean isFail() {
    return !isSuccess();
  }
}
