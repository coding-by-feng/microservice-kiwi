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
package me.fengorz.kiwi.word.biz.service.base.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.util.bean.KiwiBeanUtils;
import me.fengorz.kiwi.common.sdk.util.validate.KiwiAssertUtils;
import me.fengorz.kiwi.common.sdk.web.security.SecurityUtils;
import me.fengorz.kiwi.word.api.entity.ExampleStarListDO;
import me.fengorz.kiwi.word.api.entity.ExampleStarRelDO;
import me.fengorz.kiwi.word.api.entity.column.WordParaphraseExampleListColumn;
import me.fengorz.kiwi.word.api.vo.ExampleStarListVO;
import me.fengorz.kiwi.word.api.vo.star.ExampleStarItemVO;
import me.fengorz.kiwi.word.biz.mapper.ExampleStarListMapper;
import me.fengorz.kiwi.word.biz.service.base.IExampleStarListService;
import me.fengorz.kiwi.word.biz.service.base.IWordExampleStarRelService;
import me.fengorz.kiwi.word.biz.service.operate.IAsyncArchiveService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author zhanshifeng
 * @date 2019-12-08 23:27:12
 */
@Service()
@RequiredArgsConstructor
public class ExampleStarListServiceImpl
        extends ServiceImpl<ExampleStarListMapper, ExampleStarListDO>
        implements IExampleStarListService {

    private final ExampleStarListMapper mapper;
    private final IWordExampleStarRelService relService;
    private final IAsyncArchiveService archiveService;

    @Override
    public Integer countById(Integer id) {
        return this.count(new QueryWrapper<>(new ExampleStarListDO().setId(id)));
    }

    @Override
    public List<ExampleStarListVO> getCurrentUserList(Integer userId) {
        QueryWrapper<ExampleStarListDO> queryWrapper =
                new QueryWrapper<>(
                        new ExampleStarListDO().setOwner(userId).setIsDel(GlobalConstants.FLAG_N))
                        .select(
                                ExampleStarListDO.class,
                                tableFieldInfo ->
                                        WordParaphraseExampleListColumn.ID.equals(tableFieldInfo.getColumn())
                                                || WordParaphraseExampleListColumn.LIST_NAME.equals(
                                                tableFieldInfo.getColumn())
                                                || WordParaphraseExampleListColumn.REMARK.equals(
                                                tableFieldInfo.getColumn()));

        return KiwiBeanUtils.convertFrom(mapper.selectList(queryWrapper), ExampleStarListVO.class);
    }

    @Override
    public IPage<ExampleStarItemVO> getListItems(Page<ExampleStarListDO> page, Integer listId) {
        return this.mapper.selectListItems(page, listId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void putIntoStarList(Integer exampleId, Integer listId) {
        LambdaQueryWrapper<ExampleStarRelDO> queryWrapper =
                Wrappers.<ExampleStarRelDO>lambdaQuery()
                        .eq(ExampleStarRelDO::getListId, listId)
                        .eq(ExampleStarRelDO::getExampleId, exampleId);
        if (relService.count(queryWrapper) > 0) {
            return;
        }
        relService.save(new ExampleStarRelDO().setListId(listId).setExampleId(exampleId));
        archiveService.archiveExampleRel(exampleId, listId, SecurityUtils.getCurrentUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeOneRel(Integer exampleId, Integer listId) {
        LambdaQueryWrapper<ExampleStarRelDO> queryWrapper =
                new LambdaQueryWrapper<ExampleStarRelDO>()
                        .eq(ExampleStarRelDO::getListId, listId)
                        .eq(ExampleStarRelDO::getExampleId, exampleId);
        int count = relService.count(queryWrapper);
        KiwiAssertUtils.serviceNotEmpty(count, "example is not exists!");
        relService.remove(queryWrapper);
        archiveService.invalidArchiveExampleRel(exampleId, listId, SecurityUtils.getCurrentUserId());
    }
}
