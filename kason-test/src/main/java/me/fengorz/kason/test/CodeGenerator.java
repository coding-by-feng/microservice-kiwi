package me.fengorz.kason.test;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class CodeGenerator {
    public static void main(String[] args) {
        // 创建 Velocity 引擎
        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.init();

        // 创建 Velocity 上下文
        VelocityContext context = new VelocityContext();

        // 根据当前使用的 DTO 类选择代码模板和获取字段
        Class<?> dtoClass = AaDTO.class;
        List<String> fields = getDtoFields(dtoClass);

        // 将 DTO 类和字段放入上下文中
        context.put("dtoClass", dtoClass);
        context.put("fields", fields);

        // 加载并渲染代码模板
        Template template = velocityEngine.getTemplate("code_template.vm");
        StringWriter writer = new StringWriter();
        template.merge(context, writer);

        // 输出生成的代码
        String generatedCode = writer.toString();
        System.out.println(generatedCode);
    }

    private static List<String> getDtoFields(Class<?> dtoClass) {
        List<String> fields = new ArrayList<>();
        Field[] declaredFields = dtoClass.getDeclaredFields();
        for (Field field : declaredFields) {
            fields.add(field.getName());
        }
        return fields;
    }
}

class AaDTO {
    private String name;
    private int age;

    // Getter 和 Setter 省略...
}
