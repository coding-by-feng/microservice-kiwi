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

package me.fengorz.kiwi.generator.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import me.fengorz.kiwi.generator.common.ToolConstants;

/**
 * @Author zhanshifeng
 * @Date 2020/2/24 1:16 PM
 */
public class ToolIOUtils {

    /**
     * 将多部分内容写到流中，自动转换为字符串
     *
     * @param out
     *            输出流
     * @param charsetName
     *            写出的内容的字符集
     * @param isCloseOut
     *            写入完毕是否关闭输出流
     * @param contents
     *            写入的内容，调用toString()方法，不包括不会自动换行
     */
    public static void write(OutputStream out, String charsetName, boolean isCloseOut, Object... contents) {
        try {
            write(out, Charset.forName(ToolConstants.UTF_8), isCloseOut, contents);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void write(OutputStream out, Charset charset, boolean isCloseOut, Object... contents)
        throws IOException {
        OutputStreamWriter osw = null;
        try {
            osw = getWriter(out, charset);
            for (Object content : contents) {
                if (content != null) {
                    osw.write(content.toString());
                    osw.flush();
                }
            }
        } finally {
            if (isCloseOut) {
                osw.close();
            }
        }
    }

    public static OutputStreamWriter getWriter(OutputStream out, Charset charset) {
        if (null == out) {
            return null;
        }

        if (null == charset) {
            return new OutputStreamWriter(out);
        } else {
            return new OutputStreamWriter(out, charset);
        }
    }

    public static void close(AutoCloseable closeable) {
        if (null != closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                // 静默关闭
            }
        }
    }

    public static void close(Closeable closeable) {
        if (null != closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                // 静默关闭
            }
        }
    }
}
