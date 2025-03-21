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

package me.fengorz.kiwi.bdf.security.component;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import me.fengorz.kiwi.bdf.security.exception.KiwiAuth2Exception;

import java.io.IOException;

import static me.fengorz.kiwi.common.api.ApiContants.RESULT_CODE_SERVICE_ERROR;

/**
 * OAuth2 异常格式化 @Author Kason Zhan
 */
public class KiwiAuth2ExceptionSerializer extends StdSerializer<KiwiAuth2Exception> {
    private static final long serialVersionUID = 6776883623606657402L;

    public KiwiAuth2ExceptionSerializer() {
        super(KiwiAuth2Exception.class);
    }

    @Override
    public void serialize(KiwiAuth2Exception value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeObjectField("code", RESULT_CODE_SERVICE_ERROR);
        gen.writeStringField("msg", value.getMessage());
        gen.writeStringField("data", value.getErrorCode());
        gen.writeEndObject();
    }
}
