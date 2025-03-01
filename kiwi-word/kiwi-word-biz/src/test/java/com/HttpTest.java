package com;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpUtil;
import me.fengorz.kiwi.common.dfs.DfsUtils;
import me.fengorz.kiwi.word.api.common.ApiCrawlerConstants;

/**
 * @Author Kason Zhan @Date 2019/11/4 4:37 PM
 */
public class HttpTest {

    public static void main(String[] args) {
        String voiceFileUrl = ApiCrawlerConstants.URL_CAMBRIDGE_BASE
            + URLUtil.decode("/zhs/media/%E8%8B%B1%E8%AF%AD/uk_pron_ogg/u/ukb/ukbp0/ukbp0573.ogg");
        long voiceSize =
            HttpUtil.downloadFile(voiceFileUrl, FileUtil.file(ApiCrawlerConstants.URL_CAMBRIDGE_FETCH_CHINESE));
        System.out.println(DfsUtils.getFileName(voiceFileUrl));
    }
}
