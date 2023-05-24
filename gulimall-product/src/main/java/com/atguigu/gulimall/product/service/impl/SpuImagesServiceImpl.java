package com.atguigu.gulimall.product.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.SpuImagesDao;
import com.atguigu.gulimall.product.entity.SpuImagesEntity;
import com.atguigu.gulimall.product.service.SpuImagesService;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;


@Service("spuImagesService")
public class SpuImagesServiceImpl extends ServiceImpl<SpuImagesDao, SpuImagesEntity> implements SpuImagesService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuImagesEntity> page = this.page(
                new Query<SpuImagesEntity>().getPage(params),
                new QueryWrapper<SpuImagesEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 保存spu的图片集
     * 注意：有的图片没有选中，这样的图片没有路径，无需保存，需要筛选出去
     * @param id
     * @param images
     */
    @Override
    public void saveImages(Long id, List<String> images) {
        if (!CollectionUtils.isEmpty(images)) {
            List<SpuImagesEntity> imagesEntities = images.stream().map(img -> {
                SpuImagesEntity spuImagesEntity = new SpuImagesEntity();
                spuImagesEntity.setSpuId(id);
                spuImagesEntity.setImgUrl(img);
                // 剩下的没有维护，需要的话可以添加
                return spuImagesEntity;
            }).filter(item -> {
                //返回true是需要，返回false是过滤掉
                return !StringUtils.isEmpty(item.getImgUrl());
            }).collect(Collectors.toList());

            this.saveBatch(imagesEntities);
        }
    }

}