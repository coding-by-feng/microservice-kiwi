import me.fengorz.kiwi.common.sdk.log.EnhancedLogUtils;
import me.fengorz.kiwi.word.api.dto.fetch.FetchWordResultDTO;
import me.fengorz.kiwi.word.api.entity.WordFetchQueueDO;
import me.fengorz.kiwi.word.api.util.CrawlerUtils;
import org.apache.commons.lang3.StringUtils;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2019/11/6 9:01 PM
 */
public class TempTest {

    public void test1() {
        System.out.println(CrawlerUtils.getGroupName("group1/M00/00/00/rBpA2l3CvDKAYW2GAAAiLukT7YE158.ogg"));
        System.out.println(CrawlerUtils.getUploadVoiceFilePath("group1/M00/00/00/rBpA2l3CvDKAYW2GAAAiLukT7YE158.ogg"));
    }

    public void test2() {
        List list = null;
        for (Object o : list) {
            System.out.println(o);
        }
        System.out.println("success");
    }

    // @Test
    public void test3() {
        WordFetchQueueDO wordFetchQueue = new WordFetchQueueDO();
        this.subTest3(wordFetchQueue, 100);
        System.out.println(wordFetchQueue);
    }

    private void subTest3(WordFetchQueueDO wordFetchQueue, int status) {
        wordFetchQueue.setFetchStatus(status);
    }

    // @Test
    public void test4() {
        System.out.println(StringUtils.repeat("a", 10));
    }

    // @Test
    public void test5() {
        // System.out.println(ClassUtil.getClassPath());
        // System.out.println(ThreadUtil.getStackTrace());
        System.out.println(EnhancedLogUtils.getClassName());
        System.out.println(EnhancedLogUtils.getMethodName());
        System.out.println(EnhancedLogUtils.getFileName());
        System.out.println(EnhancedLogUtils.getLineNumber());
    }

    // @Test
    public void test6() {
        FetchWordResultDTO fetchWordResultDTO = new FetchWordResultDTO();
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<FetchWordResultDTO>> results = validator.validate(fetchWordResultDTO);
        for (ConstraintViolation<FetchWordResultDTO> result : results) {
            System.out.println(result.getPropertyPath());
            System.out.println(result.getMessage());
            System.out.println(result.getInvalidValue());
        }
    }
}
