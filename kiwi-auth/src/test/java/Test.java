/**
 * @Author zhanshifeng @Date 2019-09-03 10:48
 */
public class Test {
    public static void main(String[] args) {
        System.out.println(System.getenv("KIWI_ENC_PASSWORD"));
        System.getenv().forEach((k, v) -> System.out.println(k + "=" + v));
    }
}
