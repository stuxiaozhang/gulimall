package com.atguigu.gulimall.product.service.impl;

import com.atguigu.gulimall.product.dao.BrandDao;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.BrandService;
import com.atguigu.gulimall.product.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryBrandRelationDao;
import com.atguigu.gulimall.product.entity.CategoryBrandRelationEntity;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;


@Service("categoryBrandRelationService")
public class CategoryBrandRelationServiceImpl extends ServiceImpl<CategoryBrandRelationDao, CategoryBrandRelationEntity> implements CategoryBrandRelationService {

    @Autowired
    private CategoryDao categoryDao;

    @Autowired
    private BrandDao brandDao;

    @Autowired
    private CategoryBrandRelationDao categoryBrandRelationDao;

    @Autowired
    private BrandService brandService;

    /**
     * 关联分类的brand信息更新。（法一）
     * @param brandId
     * @param name
     */
    @Override
    public void updateBrand(Long brandId, String name) {
        CategoryBrandRelationEntity relationEntity = new CategoryBrandRelationEntity();
        relationEntity.setBrandId(brandId);
        relationEntity.setBrandName(name);

        QueryWrapper<CategoryBrandRelationEntity> queryWrapper = new QueryWrapper<CategoryBrandRelationEntity>().eq("brand_id", brandId);
        this.update(relationEntity, queryWrapper);
    }

    /**
     * 关联分类的category信息更新（法二）
     * @param catId
     * @param name
     */
    @Override
    public void updateCategory(Long catId, String name) {
        this.baseMapper.updateCategroy(catId, name);
    }

    /**
     * 13.2 获取分类关联的品牌（先在CategoryBrandRelation表中找catelog_id对应的实体，再获得其brandid，再去brand表查）
     * @param catId
     * @return
     */
    @Override
    public List<BrandEntity> getBrandsByCatId(Long catId) {

        List<CategoryBrandRelationEntity> relationEntities =
                categoryBrandRelationDao.selectList(new QueryWrapper<CategoryBrandRelationEntity>().eq("catelog_id", catId));
        List<BrandEntity> brandEntityList = relationEntities.stream().map(relationEntitiy -> {
            Long brandId = relationEntitiy.getBrandId();
            BrandEntity brandEntity = brandService.getById(brandId);// 相当于在brand表中查brandEntity
            return brandEntity;
        }).collect(Collectors.toList());
        return brandEntityList;
    }

    /**
     * 把前端传过来的两个id，查找出对应名字(因为关联分类表有冗余字段)，保存。
     * @param categoryBrandRelation
     */
    @Override
    public void saveDetail(CategoryBrandRelationEntity categoryBrandRelation) {
        Long brandId = categoryBrandRelation.getBrandId();
        Long catelogId = categoryBrandRelation.getCatelogId();
        // 1. 查询 品牌名字和分类名字
        CategoryEntity categoryEntity = categoryDao.selectById(catelogId);
        BrandEntity brandEntity = brandDao.selectById(brandId);
        // 2. 设置 新的品牌名字和分类名字（保存到关联分类表里）
        categoryBrandRelation.setCatelogName(categoryEntity.getName());
        categoryBrandRelation.setBrandName(brandEntity.getName());

        // 保存新信息
        this.save(categoryBrandRelation);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryBrandRelationEntity> page = this.page(
                new Query<CategoryBrandRelationEntity>().getPage(params),
                new QueryWrapper<CategoryBrandRelationEntity>()
        );

        return new PageUtils(page);
    }

}