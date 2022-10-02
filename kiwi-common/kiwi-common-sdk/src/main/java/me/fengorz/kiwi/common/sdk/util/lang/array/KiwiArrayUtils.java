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

package me.fengorz.kiwi.common.sdk.util.lang.array;

import org.apache.commons.lang3.ArrayUtils;

/**
 * @Author zhanshifeng @Date 2020/5/17 12:31 PM
 */
public class KiwiArrayUtils extends ArrayUtils {

    public static byte[] merge(byte[] left, byte[] right) {
        byte[] merged = new byte[left.length + right.length];
        System.arraycopy(left, 0, merged, 0, left.length);
        System.arraycopy(right, 0, merged, left.length, right.length);
        return merged;
    }

    public static byte[] merge(byte[]... bytes) {
        int bufferSize = 0;
        for (byte[] perBytes : bytes) {
            bufferSize += perBytes.length;
        }
        byte[] merged = new byte[bufferSize];
        for (int i = 0; i < bytes.length - 1; i++) {
            // FileUtil.writeBytes(bytes[i], String.format("/Users/zhanshifeng/Documents/temp/test_all_%d.mp3", i));
            System.arraycopy(bytes[i], 0, merged, 0, bytes[i].length);
            System.arraycopy(bytes[i + 1], 0, merged, bytes[i].length, bytes[i + 1].length);
        }
        // FileUtil.writeBytes(bytes[bytes.length - 1],
        // String.format("/Users/zhanshifeng/Documents/temp/test_all_%d.mp3", bytes.length - 1));
        return merged;
    }

}
