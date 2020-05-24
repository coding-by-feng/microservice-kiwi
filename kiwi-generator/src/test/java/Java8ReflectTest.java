import lombok.SneakyThrows;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * @Description TODO
 * @Author codingByFeng
 * @Date 2019-09-11 11:11
 */
public class Java8ReflectTest {
    // @Test
    @SneakyThrows
    public void test1() {
        Method method = GeneratorTest.class.getMethod("main" , String[].class);
        for (Parameter parameter : method.getParameters()) {
            System.out.println(parameter.getName());
        }
    }
}
