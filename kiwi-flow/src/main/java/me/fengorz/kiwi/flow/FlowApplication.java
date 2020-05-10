/*
 *
 *   Copyright [2019~2025] [codingByFeng]
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

package me.fengorz.kiwi.flow;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;

/**
 * @Author codingByFeng
 * @Date 2019-09-02 14:56
 */
@SpringCloudApplication
public class FlowApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(FlowApplication.class);
        // 关闭SpringBoot的经典Banner
        application.setBannerMode(Banner.Mode.OFF);
        application.run(args);
    }

    public CommandLineRunner init(final RepositoryService repositoryService,
                                  final RuntimeService runtimeService,
                                  final TaskService taskService) {

        return new CommandLineRunner() {
            @Override
            public void run(String... strings) throws Exception {
                System.out.println("-----> Number of process definitions : "
                        + repositoryService.createProcessDefinitionQuery().count());
                System.out.println("-----> Number of tasks : " + taskService.createTaskQuery().count());
                runtimeService.startProcessInstanceByKey("Expense");
                System.out.println("-----> Number of tasks after process start: "
                        + taskService.createTaskQuery().count());
            }
        };
    }

}
