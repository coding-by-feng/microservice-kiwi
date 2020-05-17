import lombok.SneakyThrows;
import me.fengorz.kiwi.word.api.dto.fetch.WordMessageDTO;
import me.fengorz.kiwi.word.crawler.component.WordFetchProducer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Description TODO
 * @Author codingByFeng
 * @Date 2019/10/29 3:19 PM
 */
// @RunWith(SpringRunner.class)
// @SpringBootTest(classes = {RabbitMQConfig.class, QueueConfig.class})
public class CountDownLatchTest implements Runnable {

    private final static CountDownLatch latch = new CountDownLatch(10);
    private final static CountDownLatchTest test = new CountDownLatchTest();
    private Long id;

    // @PostConstruct
    public void init() {
        this.id = 1L;
    }

    // @Autowired
    private WordFetchProducer wordFetchProducer;

    @Override
    public void run() {
        WordMessageDTO wordMessage = new WordMessageDTO("test");
        System.out.println("mq sending a message is " + wordMessage);
        this.wordFetchProducer.send(wordMessage);
        latch.countDown();
    }

    @SneakyThrows
    // @Test
    public void test() {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 1000; i++) {
            executorService.submit(this::test);
        }

        latch.await();

        System.out.println("all sending complete!");

        executorService.shutdown();
    }
}
