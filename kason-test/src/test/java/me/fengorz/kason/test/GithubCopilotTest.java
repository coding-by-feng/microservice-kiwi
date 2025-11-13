package me.fengorz.kason.test;

/**
 * @Description TODO
 * @Author Kason Zhan
 * @Date 2023/3/24 09:01
 */
public class GithubCopilotTest {

    // 写一个主方法
    public static void main(String[] args) {
        // 打印一句话
        System.out.println("Hello, Copilot!");
        // 写一个人类对象，名字叫做臭范范，她会唱歌
        Human fff = new Human("臭范范", "唱歌");
    }

    // 定义一个人类内部类
    static class Human {
        // 定义一个人类的名字
        private final String name;
        // 定义一个人类的爱好
        private final String hobby;

        // 定义一个构造方法
        public Human(String name, String hobby) {
            this.name = name;
            this.hobby = hobby;
        }

        // 定义一个人类的自我介绍方法
        public void introduce() {
            System.out.println("大家好，我叫" + name + "，我的爱好是" + hobby);
        }
    }

}
