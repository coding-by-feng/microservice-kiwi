import lombok.SneakyThrows;
import me.fengorz.kiwi.common.sdk.util.json.KiwiJsonUtils;
import me.fengorz.kiwi.word.api.dto.fetch.FetchWordResultDTO;
import me.fengorz.kiwi.word.api.dto.fetch.WordMessageDTO;
import me.fengorz.kiwi.word.crawler.service.IJsoupService;
import me.fengorz.kiwi.word.crawler.service.impl.JsoupService;

/**
 * @Description TODO
 * @Author codingByFeng
 * @Date 2019/11/4 3:01 PM
 */
public class FetchTest {

    // @Test
    @SneakyThrows
    public void test() {
        IJsoupService jsoupService = new JsoupService();
        FetchWordResultDTO test = jsoupService.fetchWordInfo(new WordMessageDTO().setWord("mandatory"));
        System.out.println(KiwiJsonUtils.toJsonStr(test));
    }

}
