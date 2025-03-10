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

import java.io.*;

/**
 * @Author Kason Zhan @Date 2019/11/27 4:32 PM
 */
public class WordTxtReader {

    public void readWords() {
        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader br = null; // 用于包装InputStreamReader,提高处理性能。因为BufferedReader有缓冲的，而InputStreamReader没有。
        try {
            fis = new FileInputStream("/Users/zhanshifeng/Downloads/雅思词汇表.txt"); // FileInputStream
            // 从文件系统中的某个文件中获取字节
            isr = new InputStreamReader(fis); // InputStreamReader 是字节流通向字符流的桥梁,
            br = new BufferedReader(isr); // 从字符输入流中读取文件中的内容,封装了一个new InputStreamReader的对象
            String line = null;
            while ((line = br.readLine()) != null) {
                System.out.println(line.substring(0, line.indexOf("\t")));
            }
        } catch (FileNotFoundException e) {
            System.out.println("找不到指定文件");
        } catch (IOException e) {
            System.out.println("读取文件失败");
        } finally {
            try {
                br.close();
                isr.close();
                fis.close();
                // 关闭的时候最好按照先后顺序关闭最后开的先关闭所以先关s,再关n,最后关m
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
