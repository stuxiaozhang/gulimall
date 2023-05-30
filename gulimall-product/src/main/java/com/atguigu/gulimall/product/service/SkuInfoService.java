package com.atguigu.gulimall.product.service;

import com.atguigu.gulimall.product.entity.SpuInfoEntity;
import com.atguigu.gulimall.product.vo.Skus;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;

import java.util.List;
import java.util.Map;

/**
 * sku信息
 *
 * @author zhangxinyan
 * @email zhangxinyan1999@126.com
 * @date 2023-04-24 09:49:46
 */
public interface SkuInfoService extends IService<SkuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSkus(List<Skus> skus, SpuInfoEntity spuInfoEntity);

    PageUtils queryPageByCondition(Map<String, Object> params);

}

