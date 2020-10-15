import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpUtil;

/**
 * @Author zhanshifeng
 * @Date 2019/11/4 4:37 PM
 */
public class HttpTest {

    public static void main(String[] args) {
        // String voiceFileUrl =
        //         URLUtil.decode("https://dictionary.cambridge.org/" + "M00/1E/F2/rBAQCV9hmDyAVpIYAAAb7aU0miQ759.ogg");
        String voiceFileUrl =
                URLUtil.decode("http://kiwidict.com/wordBiz/word/pronunciation/downloadVoice/5858984");
        long voiceSize = HttpUtil.downloadFile(voiceFileUrl,
                FileUtil.file("/Users/zhanshifeng/Documents/myDocument/temp/20201015/test.mp3"));
        System.out.println(voiceFileUrl.substring(voiceFileUrl.lastIndexOf("/")));
    }

}
