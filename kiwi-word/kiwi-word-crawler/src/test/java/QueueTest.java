import org.springframework.boot.SpringApplication;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.word.crawler.component.producer.MqSender;
import me.fengorz.kiwi.word.crawler.component.producer.ScheduledChiefProducer;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2019/10/28 10:21 AM
 */
// @RunWith(SpringRunner.class)
// @SpringBootTest(classes = {RabbitMQConfig.class, QueueConfig.class})
@Slf4j
public class QueueTest {

    // @Autowired
    private MqSender mqSender;

    // @Autowired
    private ScheduledChiefProducer scheduledChiefProducer;

    public void main() {
        SpringApplication.run(QueueTest.class);
    }

    // @Test
    // @Transactional
    public void test() {
        // Long id = 1L;
        // while (id < 100L) {
        // Thread.sleep(1000);
        // log.info("word.fetch sending id = " + id);
        // this.wordFetchProducer.send(new WordMessage(id++, "test"));
        // }
        // this.scheduledChiefProducer.fetchWord();
        System.out.println("testing");
    }

}
