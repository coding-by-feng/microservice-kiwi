import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import cn.hutool.core.util.ArrayUtil;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Author zhanshifeng @Date 2019-09-12 14:29
 */
public class Java8StreamTest {

    /*
     * of()传入null会抛异常
     * 其生成的Stream是有限长度的，Stream的长度为其内的元素个数
     */
    // @Test
    public void baseTest() {
        Stream<Integer> integerStream = Stream.of(1, 2, 3, 4);
        Stream.of(null);
    }

    /*
     * 生成无限长度的流
     */
    // @Test
    public void generateTest() {
        Stream.generate(() -> Math.random()).forEach(System.out::println);
    }

    /*
     * 同样生成一个无限长的流，生成规则是根据初始种子，以及迭代生成下一个种子的计算公式生成的。
     */
    // @Test
    public void iterateTest() {
        Stream.iterate(2D, item -> item * item).limit(20).forEach(System.out::println);
    }

    /*
     * 连接不同的流
     */
    // @Test
    public void concatTest() {
        Stream.concat(Stream.of(1, 2, 3), Stream.of(4, 5)).forEach(System.out::println);
    }

    /*
     * 去除重复元素
     */
    // @Test
    public void distinctTest() {
        Stream.of(1, 2, 3, 4, 5, 4, 3, 2, 34, 4).distinct().forEach(System.out::println);
    }

    // @Test
    public void filterTest() {
        Stream.of(1, 2, 3, 4, 5, 4, 3, 2, 34, 4).distinct().filter(e -> e < 5).forEach(System.out::println);
    }

    // @Test
    public void mapAndFlatmapTest() {
        List<Action> actionList = new ArrayList<>();
        actionList.add(new Action("Left Hook"));
        actionList.add(new Action("Right Hook"));
        actionList.add(new Action("Up Hook"));

        Stream<Person> personStream1 =
            Stream.of(new Person("jack", Arrays.stream(ArrayUtil.toArray(actionList, Action.class))),
                new Person("mark", Arrays.stream(ArrayUtil.toArray(actionList, Action.class))));

        Stream<Person> personStream2 =
            Stream.of(new Person("jack", Arrays.stream(ArrayUtil.toArray(actionList, Action.class))),
                new Person("mark", Arrays.stream(ArrayUtil.toArray(actionList, Action.class))));

        Stream<Stream<Action>> streamStream =
            personStream1.map(Person::getActionStream).filter(actionStream -> actionStream != null);
        Stream<Action> actionStream = personStream2.flatMap(Person::getActionStream)
            .filter(action -> StringUtils.isNotBlank(action.getActionName()));
    }

    /*
     * 给Stream每个元素绑定一个消费函数，当foreach消费每个元素之前，预定义的消费函数会被先执行
     */
    // @Test
    public void peekTest() {
        Stream.of(1, 2, 3, 4).peek(integer -> {
            System.out.println("pre" + integer);
        }).forEach(System.out::println);
    }

    @Data
    @AllArgsConstructor
    private class Person {
        private String name;
        private Stream<Action> actionStream;
    }

    @Data
    @AllArgsConstructor
    private class Action {
        private String actionName;

        @Override
        public String toString() {
            return this.actionName;
        }
    }
}
