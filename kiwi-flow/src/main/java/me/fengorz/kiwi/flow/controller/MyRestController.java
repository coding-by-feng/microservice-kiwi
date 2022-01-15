/*
 *
 * Copyright [2019~2025] [zhanshifeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 *
 */

package me.fengorz.kiwi.flow.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import me.fengorz.kiwi.flow.service.MyService;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/** @Author zhanshifeng @Date 2019/12/4 3:24 PM */
@RestController
public class MyRestController {

  @Autowired private MyService myService;

  @RequestMapping(value = "/process", method = RequestMethod.POST)
  public void startProcessInstance() {
    myService.startProcess();
  }

  @RequestMapping(
      value = "/tasks",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public List<TaskRepresentation> getTasks(@RequestParam String assignee) {
    List<Task> tasks = myService.getTasks(assignee);
    List<TaskRepresentation> dtos = new ArrayList<TaskRepresentation>();
    for (Task task : tasks) {
      dtos.add(new TaskRepresentation(task.getId(), task.getName()));
    }
    return dtos;
  }

  @Data
  @AllArgsConstructor
  static class TaskRepresentation {

    private String id;
    private String name;
  }
}
