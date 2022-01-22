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

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * @Author zhanshifeng @Date 2019/11/26 8:50 PM
 */
public class JSR303Test {

    public void test() {
        this.subTest(new JSR303Bean());
    }

    private void subTest(JSR303Bean jsr303Bean) {
        System.out.println("kao");
    }

    @Data
    @Accessors(chain = true)
    class JSR303Bean {
        @Min(0)
        @NotNull
        private Integer i;
    }
}
