import me.fengorz.kiwi.word.crawler.component.ScheduledProducer;
import me.fengorz.kiwi.word.crawler.component.WordFetchProducer;
import me.fengorz.kiwi.word.crawler.config.QueueConfig;
import me.fengorz.kiwi.word.crawler.config.RabbitMQConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Description TODO
 * @Author codingByFeng
 * @Date 2019/10/28 10:21 AM
 */
// @RunWith(SpringRunner.class)
// @SpringBootTest(classes = {RabbitMQConfig.class, QueueConfig.class})
@Slf4j
public class QueueTest {

    // @Autowired
    private WordFetchProducer wordFetchProducer;

    // @Autowired
    private ScheduledProducer scheduledProducer;

    public void main() {
        SpringApplication.run(QueueTest.class);
    }

    // @Test
    @SneakyThrows
    // @Transactional
    public void test() {
        // Long id = 1L;
        // while (id < 100L) {
        //     Thread.sleep(1000);
        //     log.info("word.fetch sending id = " + id);
        //     this.wordFetchProducer.send(new WordMessage(id++, "test"));
        // }
        // this.scheduledProducer.fetchWord();
        System.out.println("testing");
    }

}