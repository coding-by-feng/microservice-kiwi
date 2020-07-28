import me.fengorz.kiwi.common.sdk.util.json.KiwiJsonUtils;
import me.fengorz.kiwi.word.api.dto.queue.WordFetchMessageDTO;
import me.fengorz.kiwi.word.api.dto.queue.fetch.FetchWordResultDTO;
import me.fengorz.kiwi.word.api.exception.JsoupFetchConnectException;
import me.fengorz.kiwi.word.api.exception.JsoupFetchPronunciationException;
import me.fengorz.kiwi.word.api.exception.JsoupFetchResultException;
import me.fengorz.kiwi.word.crawler.service.IJsoupService;
import me.fengorz.kiwi.word.crawler.service.impl.JsoupServiceImpl;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2019/11/4 3:01 PM
 */
public class FetchTest {

    // @Test
    public void test() throws JsoupFetchResultException, JsoupFetchConnectException, JsoupFetchPronunciationException {
        IJsoupService jsoupService = new JsoupServiceImpl();
        FetchWordResultDTO test = jsoupService.fetchWordInfo(new WordFetchMessageDTO().setWord("mandatory"));
        System.out.println(KiwiJsonUtils.toJsonStr(test));
    }

}
