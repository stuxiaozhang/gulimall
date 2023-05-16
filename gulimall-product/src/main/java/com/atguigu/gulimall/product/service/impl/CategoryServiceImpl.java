package com.atguigu.gulimall.product.service.impl;

import com.atguigu.gulimall.product.entity.AttrGroupEntity;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );
        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        // 1. 查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);
        // 2. 组装成父子的树形结构
        //  找到所有一级分类（一级分类的父分类是0）；给一级菜单递归设置子菜单；对菜单进行排序；将流中的所有元素导出到一个列表(List)中
        List<CategoryEntity> level1Menus = entities.stream()
                .filter(categoryEntity -> categoryEntity.getParentCid() == 0)  // 说明是一级分类
                .peek(menu -> menu.setChildren(getChildren(menu, entities))) // 处理，给一级菜单递归设置子菜单
                // 按sort属性排序(改进写法)
                .sorted(Comparator.comparingInt(menu -> (menu.getSort() == null ? 0 : menu.getSort())))
                .collect(Collectors.toList());

        return level1Menus;
    }


    // 递归查找当前菜单的子菜单（教程原写法）
    private List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> entities) {
        List<CategoryEntity> children = entities.stream()
                .filter(categoryEntity -> {  // categoryEntity 就是指当前的这个菜单
                    return categoryEntity.getParentCid().longValue() == root.getCatId().longValue();  // 注意此处应该用longValue()来比较，否则会出先bug，因为parentCid和catId是long类型
                })
                .map(categoryEntity -> {
                    categoryEntity.setChildren(getChildren(categoryEntity, entities));
                    return categoryEntity;
                })
                .sorted((menu1, menu2) -> {
                    return (menu1.getSort() == null? 0: menu1.getSort()) - (menu2.getSort() == null? 0: menu2.getSort());
                })
                .collect(Collectors.toList());
        return children;
    }

    /**
     * 给一个第三级路径，返回完整的三级分类路径
     * @param catelogId (e.g. 225)
     * @return (e.g. [2, 234, 225])
     */
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> wholePath = findParentPath(catelogId, paths);

        return wholePath.toArray(new Long[wholePath.size()]);
    }
    /*
        递归查找id=225的父节点id  [2, 234, 225]
     */
    private List<Long> findParentPath(Long catelogId, List<Long> paths) {
        /* 1. 获取当前节点的id */
        CategoryEntity byId = this.getById(catelogId);
        /* 2. 递归查找父节点，直到当前节点的父节点=0，即找到最顶端节点，停 */
        if(byId.getParentCid() != 0) {
            findParentPath(byId.getParentCid(), paths);
        }
        paths.add(catelogId);
        return paths;
    }
}