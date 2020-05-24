import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import org.junit.Assert;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;

/**
 * @Author codingByFeng
 * @Date 2019-09-11 10:32
 */
public class Java8OptionalTest {
    /*
     * isPresent()返回是否为empty
     */
    // @Test
    @SneakyThrows
    public void whenCreatesEmptyOptional_thenCorrect() {
        Optional<String> empty = Optional.empty();
        // 断言是否为false，是的话抛出异常
        Assert.assertFalse(empty.isPresent());
        System.out.println(empty);
    }

    // @Test
    @SneakyThrows
    public void givenNonNull_whenCreatesOptional_thenCorrect() {
        String test = "test" ;
        Optional<String> opt = Optional.of(test);
        Assert.assertEquals("Optional[test]" , opt);
        System.out.println(opt);
        System.out.println(opt.get());
    }

    /*
     * of()不允许传入null
     */
    // @Test(expected = NullPointerException.class)
    public void givenNull_whenThrowsErrorOnCreate_thenCorrect() {
        String name = null;
        Optional<String> opt = Optional.of(name);
    }

    /*
     * 允许null转为Optional
     */
    // @Test
    public void givenNonNull_whenCreatesNullable_thenCorrect() {
        String name = null;
        Optional<String> opt = Optional.ofNullable(name);
        Assert.assertEquals("Optional.empty" , opt.toString());
        System.out.println(opt);
    }

    /*
     * 检查非空新写法
     */
    // @Test
    public void givenOptional_whenIfPresentWorks_thenCorrect() {
        Optional<String> opt = Optional.of("baeldung");

        opt.ifPresent(name -> System.out.println(name.length()));
        // 老的写法
        // if(name != null){
        //     System.out.println(name.length);
        // }
    }

    /*
     * 三目运算符通过Optional的两种写法的区别
     * 当值存在时，orElse相比于orElseGet，多创建了一个对象,使用orElse时照常执行。
     */
    // @Test
    public void whenOrElseWorks_thenCorrect() {
        String notEmpty = "ne" ;
        System.out.println(Optional.ofNullable(notEmpty).orElse(getNewValue()));
        System.out.println("------");
        System.out.println(Optional.ofNullable(notEmpty).orElseGet(this::getNewValue));
    }

    /*
     * null的话抛出自定义异常
     */
    // @Test
    public void whenOrElseThrowWorks_thenCorrect() {
        String nullValue = null;
        Optional.ofNullable(nullValue).orElseThrow(IllegalArgumentException::new);
    }

    /*
     * null的时候调用get会抛异常，感觉有点违背Optional的设计初衷
     */
    // @Test
    public void givenOptionalWithNull_whenGetThrowsException_thenCorrect() {
        System.out.println(Optional.ofNullable(null).get());
    }

    /*
     * 接收一个函数式接口，当符合接口时，则返回一个Optional对象，否则返回一个空的Optional对象。
     */
    // @Test
    public void filterTest() {
        Assert.assertTrue(isStandard(new Peri(88)));
        Assert.assertTrue(isStandard(new Peri(90)));
    }

    /*
     * 值存在的话，map可以将其值转换为另外一个值
     */
    // @Test
    public void mapTest() {
        Optional.ofNullable("33").map(o -> "3").ifPresent(e -> {
                    System.out.println(e);
                }
        );
    }

    /*
     * map()加filter()判断是否登录成功
     */
    // @Test
    public void mapAndFilterDealLogin() {
        String username = "admin" ;
        String inputPassword = "123456" ;

        User user = new User();// 代表从数据库查出User对象
        Optional.ofNullable(user).filter(u -> username.equals(u.getUsername()))
                .filter(u -> inputPassword.equals(u.getPassword()))
                .ifPresent(u -> {
                    System.out.println(u.getUsername() + "登录成功");
                });
    }

    /*
     * map不会自动多层Optionnal封装解包，flatmap可以自动解包
     */
    // @Test
    @SneakyThrows
    public void mapAndflatmap() {

        Person codingByFeng = new Person("codingByFeng");
        Optional<Person> person = Optional.of(codingByFeng);
        Optional<Optional<String>> stringOptional = person.map(Person::getName);
        Optional<String> optional = stringOptional.orElseThrow(Exception::new);
        String s = optional.orElse(null);
        Assert.assertEquals("codingByFeng" , s);

        String s1 = person.flatMap(Person::getName).orElseThrow(Exception::new);
        Assert.assertEquals(s1, "codingByFeng");

    }

    @Data
    @AllArgsConstructor
    private class Person {
        private String name;

        public Optional<String> getName() {
            return Optional.ofNullable(this.name);
        }
    }

    @Data
    private class User {
        private String username;
        private String password;

        public User() {
            this.username = "admin" ;
            this.password = "123456" ;
        }
    }


    /*
     * 判断美女的颜值是否达标，同时也不能满分，满分
     */
    private boolean isStandard(Peri peri) {
        return Optional.ofNullable(peri).map(Peri::getFaceScore).filter(s -> s > 70).filter(s -> s < 100).isPresent();
    }

    @Data
    public class Peri {
        // 颜值
        private int faceScore;

        public Peri(int faceScore) {
            this.faceScore = faceScore;
        }
    }

    private String getNewValue() {
        System.out.println("getNewValue");
        return "new" ;
    }


    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    public @interface NonEmpty {
    }
}
