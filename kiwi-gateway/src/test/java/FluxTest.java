/*
 *
 *   Copyright [2019~2025] [zhanshifeng]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *
 */

import io.netty.handler.codec.http.HttpMethod;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.NettyPipeline;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import reactor.netty.resources.ConnectionProvider;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2019/12/16 4:27 PM
 */
public class FluxTest {

    // @Test
    public void Test1() {
        ConnectableFlux<Object> publish = Flux.create(fluxSink -> {
            while (true) {
                fluxSink.next(System.currentTimeMillis());
            }
        }).publish();
    }

    // @Test
    public void Test2() {
        Flux<Integer> ints = Flux.range(1, 3);
        ints.subscribe(System.out::println);
    }

    // @Test
    public void Test3() {
        Flux<Integer> ints = Flux.range(1, 4)
                .map(i -> {
                    if (i <= 3) return i;
                    throw new RuntimeException("Got to 4");
                });
        ints.subscribe(i -> System.out.println(i),
                error -> System.err.println("Error: " + error));
    }

    // @Test
    public void Test4() {

        final HttpClient httpClient = HttpClient.create(ConnectionProvider.newConnection());
        System.out.println("Test4----------->");


        Flux<HttpClientResponse> responseFlux = httpClient.request(HttpMethod.GET).uri("https://www.baidu.com/s?wd=test")
                .send((request, nettyOutbound) -> {
                    System.out.println("sending...");
                    return nettyOutbound.options(NettyPipeline.SendOptions::flushOnEach);
                }).responseConnection((httpClientResponse, connection) -> {
                    System.out.println("response...");
                    return Mono.just(httpClientResponse);
                });

        Mono<Object> mono = responseFlux.then(Mono.defer(() -> {
            System.out.println("mono");
            return Mono.empty();
        }));

        System.out.println("Test4----------->subscribe");
        mono.subscribe(o -> {
            HttpClientResponse response = (HttpClientResponse) o;
            System.out.println(response.toString());
        }, throwable -> System.out.println(throwable));

        System.out.println("Test4----------->end");
    }

    // @Test
    public void test5() {
        Flux.just("a", "b", "c").log().subscribe(new Subscriber<String>() {

            @Override
            public void onSubscribe(Subscription s) {
                System.out.println("onSubscribe");
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(String s) {
                System.out.println("onNext");
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("onError");
            }

            @Override
            public void onComplete() {
                System.out.println("onComplete");
            }
        });
    }

    // @Test
    public void test() {
        FluxTest fluxTest = new FluxTest();
        fluxTest.getClass().getName();
    }

}
