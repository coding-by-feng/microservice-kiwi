import me.fengorz.kiwi.common.sdk.util.json.KiwiJsonUtils;
import me.fengorz.kiwi.vocabulary.crawler.service.IJsoupService;
import me.fengorz.kiwi.vocabulary.crawler.service.impl.JsoupServiceImpl;
import me.fengorz.kiwi.word.api.dto.queue.FetchPhraseRunUpMqDTO;
import me.fengorz.kiwi.word.api.dto.queue.result.FetchPhraseRunUpResultDTO;
import me.fengorz.kiwi.word.api.exception.JsoupFetchConnectException;
import me.fengorz.kiwi.word.api.exception.JsoupFetchPronunciationException;
import me.fengorz.kiwi.word.api.exception.JsoupFetchResultException;

/**
 * @Author zhanshifeng @Date 2019/11/4 3:01 PM
 */
public class FetchTest {

    // @Test
    public void test() throws JsoupFetchResultException, JsoupFetchConnectException, JsoupFetchPronunciationException {
        IJsoupService jsoupService = new JsoupServiceImpl();
        // FetchWordResultDTO test = jsoupService.fetchWordInfo(new
        // FetchWordMqDTO().setWord("mandatory"));
        FetchPhraseRunUpResultDTO test = jsoupService.fetchPhraseRunUp(new FetchPhraseRunUpMqDTO().setWord("start"));
        System.out.println(KiwiJsonUtils.toJsonStr(test));
    }

    // @Test
    public void testSuper() {
        // System.out.println(consumer.getSuperObj());
        // System.out.println(consumer.getThisObj());
        // System.out.println(pronunciationConsumer.getSuperObj());
        // System.out.println(pronunciationConsumer.getThisObj());
    }
}
