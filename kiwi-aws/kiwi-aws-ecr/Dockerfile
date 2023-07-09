FROM amazoncorretto:17
RUN echo $' \
public class Hello { \
public static void main(String[] args) { \
System.out.println("Welcome to Amazon Corretto!"); \
} \
}' > Hello.java
RUN pwd
RUN /bin/javac Hello.java
ENTRYPOINT ["java", "Hello"]

