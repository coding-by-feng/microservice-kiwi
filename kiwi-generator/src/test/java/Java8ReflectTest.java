import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * @Author zhanshifeng
 * @Date 2019-09-11 11:11
 */
public class Java8ReflectTest {
    // @Test
    public void test1() throws NoSuchMethodException {
        Method method = GeneratorTest.class.getMethod("main", String[].class);
        for (Parameter parameter : method.getParameters()) {
            System.out.println(parameter.getName());
        }
    }
}
