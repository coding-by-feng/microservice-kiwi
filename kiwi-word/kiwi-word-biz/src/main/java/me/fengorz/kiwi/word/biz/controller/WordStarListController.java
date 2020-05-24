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
package me.fengorz.kiwi.word.biz.controller;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.annotation.log.SysLog;
import me.fengorz.kiwi.common.api.entity.EnhancerUser;
import me.fengorz.kiwi.common.api.exception.ServiceException;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.word.api.entity.WordStarListDO;
import me.fengorz.kiwi.word.api.entity.column.WordStarListColumn;
import me.fengorz.kiwi.word.biz.service.IWordStarListService;
import me.fengorz.kiwi.word.biz.service.operate.IWordOperateService;
import org.hibernate.validator.constraints.Range;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


/**
 * 单词本
 *
 * @author codingByFeng
 * @date 2019-12-08 23:26:57
 */
@RestController
@RequestMapping("/word/star/list")
@AllArgsConstructor
@Slf4j
@Validated
public class WordStarListController extends BaseController {

    private final IWordStarListService wordStarListService;
    private final IWordOperateService wordOperateService;

    /**
     * 分页查询
     *
     * @param page           分页对象
     * @param wordStarListDO 单词本
     * @return
     */
    @PostMapping("/page")
    public R getWordStarListPage(Page page, WordStarListDO wordStarListDO) {
        return R.success(wordStarListService.page(page, Wrappers.query(wordStarListDO)));
    }

    /**
     * 根据条件查询单个实体
     *
     * @param condition
     * @return
     */
    @PostMapping("/getOne")
    public R getOne(@RequestBody WordStarListDO condition) {
        return R.success(wordStarListService.getOne(new QueryWrapper<>(condition)));
    }


    /**
     * 通过id查询单词本
     *
     * @param id id
     * @return R
     */
    @GetMapping("/getById/{id}")
    public R getById(@PathVariable("id") Integer id) {
        return R.success(wordStarListService.getById(id));
    }

    /**
     * 新增单词本
     *
     * @param wordStarListDO 单词本
     * @return R
     */
    @SysLog("新增单词本")
    @PostMapping("/save")
    // @PreAuthorize("@pms.hasPermission('api_wordstarlist_add')")
    public R save(WordStarListDO wordStarListDO) {
        wordStarListDO.setOwner(1);
        return R.success(wordStarListService.save(wordStarListDO));
    }

    /**
     * 修改单词本
     *
     * @param wordStarListDO 单词本
     * @return R
     */
    @SysLog("修改单词本")
    @PostMapping("/updateById")
    // @PreAuthorize("@pms.hasPermission('api_wordstarlist_edit')")
    public R updateById(WordStarListDO wordStarListDO) {
        return R.success(wordStarListService.updateById(wordStarListDO));
    }

    /**
     * 通过id删除单词本
     *
     * @param id id
     * @return R
     */
    @SysLog("通过id删除单词本")
    @PostMapping("/del/{id}")
    // @PreAuthorize("@pms.hasPermission('api_wordstarlist_del')")
    public R del(@PathVariable Integer id) {
        return R.success(wordStarListService.removeById(id));
    }

    @GetMapping("/getCurrentUserList")
    public R getCurrentUserList() {
        EnhancerUser currentUser = this.getCurrentUser();
        return R.success(
                Stream.of(
                        wordStarListService.getCurrentUserList(1)
                ).flatMap(list -> {
                    List<Map> convertedList = new ArrayList<>();
                    list.forEach(wordStarListDO -> {
                        HashMap<String, Object> map = CollUtil.newHashMap();
                        map.put(WordStarListColumn.ID, wordStarListDO.getId());
                        map.put("listName", wordStarListDO.getListName());
                        convertedList.add(map);
                    });
                    return Stream.of(convertedList.toArray());
                })
        );
    }

    @PostMapping("/getListItems/{size}/{current}")
    public R getListItems(@NotNull Integer listId,
                          @PathVariable @Min(1) Integer current,
                          @PathVariable @Range(min = 1, max = 100) Integer size) {
        return R.success(wordStarListService.getListItems(new Page(current, size), listId));
    }

    @PostMapping("/putWordStarList")
    public R putWordStarList(@NotNull Integer wordId, @NotNull Integer listId) throws ServiceException {
        return R.success(this.wordOperateService.putWordIntoStarList(wordId, listId));
    }

    @PostMapping("/removeWordStarList")
    public R removeWordStarList(@NotNull Integer wordId, @NotNull Integer listId) throws ServiceException {
        return R.success(this.wordOperateService.removeWordStarList(wordId, listId));
    }
}
