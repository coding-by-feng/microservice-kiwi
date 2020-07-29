import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.fengorz.kiwi.word.crawler.component.MqSender;

/**
 * @Description TODO
 * @Author zhanshifeng
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
    private MqSender mqSender;

    @Override
    public void run() {
        // FetchWordMqDTO wordMessage = new FetchWordMqDTO("test");
        // System.out.println("mq sending a message is " + wordMessage);
        // this.mqSender.fetchWord(wordMessage);
        // latch.countDown();
    }

    // @Test
    public void test() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        // for (int i = 0; i < 1000; i++) executorService.submit(this::test);

        latch.await();

        System.out.println("all sending complete!");

        executorService.shutdown();
    }
}
