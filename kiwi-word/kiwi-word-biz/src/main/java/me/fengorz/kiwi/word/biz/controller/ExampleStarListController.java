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
package me.fengorz.kiwi.word.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.bdf.core.service.ISeqService;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.annotation.log.SysLog;
import me.fengorz.kiwi.common.api.constant.MapperConstant;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.common.sdk.web.security.SecurityUtils;
import me.fengorz.kiwi.word.api.entity.ExampleStarListDO;
import me.fengorz.kiwi.word.api.vo.ExampleStarListVO;
import me.fengorz.kiwi.word.api.vo.star.ExampleStarItemVO;
import me.fengorz.kiwi.word.biz.service.base.IExampleStarListService;
import me.fengorz.kiwi.word.biz.service.operate.IOperateService;
import org.hibernate.validator.constraints.Range;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author zhanshifeng
 * @date 2019-12-08 23:27:12
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/word/example/star/list")
@Validated
@Slf4j
public class ExampleStarListController extends BaseController {

    private final IExampleStarListService starListService;
    private final IOperateService operateService;
    private final ISeqService seqService;

    /**
     * 新增
     *
     * @param vo
     * @return R
     */
    @SysLog("新增")
    @PostMapping("/save")
    // @PreAuthorize("@pms.hasPermission('api_wordparaphraseexamplelist_add')")
    public R<Boolean> save(ExampleStarListVO vo) {
        vo.setOwner(SecurityUtils.getCurrentUserId());
        vo.setId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE));
        return R.success(starListService.save(vo));
    }

    /**
     * 修改
     *
     * @param exampleStarListDO
     * @return R
     */
    @SysLog("修改")
    @PostMapping("/updateById")
    // @PreAuthorize("@pms.hasPermission('api_wordparaphraseexamplelist_edit')")
    public R<Boolean> updateById(ExampleStarListDO exampleStarListDO) {
        return R.success(starListService.updateById(exampleStarListDO));
    }

    /**
     * 通过id删除
     *
     * @param id id
     * @return R
     */
    @SysLog("通过id删除")
    @PostMapping("/delById/{id}")
    // @PreAuthorize("@pms.hasPermission('api_wordparaphraseexamplelist_del')")
    public R<Boolean> delById(@PathVariable Integer id) {
        return R.success(starListService.removeById(id));
    }

    @GetMapping("/getCurrentUserList")
    public R<List<ExampleStarListVO>> getCurrentUserList() {
        return R.success(starListService.getCurrentUserList(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/putIntoStarList")
    public R<Boolean> putIntoStarList(@NotNull Integer exampleId, @NotNull Integer listId) {
        starListService.putIntoStarList(exampleId, listId);
        return R.success();
    }

    @PostMapping("/removeExampleStar")
    public R<Boolean> removeExampleStar(@NotNull Integer exampleId, @NotNull Integer listId) {
        starListService.removeOneRel(exampleId, listId);
        return R.success();
    }

    @PostMapping("/getListItems/{size}/{current}")
    public R<IPage<ExampleStarItemVO>> getListItems(@NotNull Integer listId, @PathVariable @Min(0) Integer current,
                                                    @PathVariable @Range(min = 1, max = 100) Integer size) {
        return R.success(starListService.getListItems(new Page(current, size), listId));
    }

}
