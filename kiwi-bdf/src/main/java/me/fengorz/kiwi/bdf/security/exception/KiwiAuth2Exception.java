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

package me.fengorz.kiwi.bdf.security.exception;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import me.fengorz.kiwi.bdf.security.component.KiwiAuth2ExceptionSerializer;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;

/** 自定义OAuth2Exception @Author zhanshifeng */
@JsonSerialize(using = KiwiAuth2ExceptionSerializer.class)
public class KiwiAuth2Exception extends OAuth2Exception {
  private static final long serialVersionUID = -6965054177137505144L;
  @Getter private String errorCode;

  public KiwiAuth2Exception(String msg) {
    super(msg);
  }

  public KiwiAuth2Exception(String msg, String errorCode) {
    super(msg);
    this.errorCode = errorCode;
  }
}
