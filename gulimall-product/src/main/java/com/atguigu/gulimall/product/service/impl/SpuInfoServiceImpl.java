package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.to.SpuBoundTo;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.entity.SpuImagesEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import com.atguigu.gulimall.product.fegin.CouponFeignService;
import com.atguigu.gulimall.product.service.*;
import com.atguigu.gulimall.product.vo.BaseAttrs;
import com.atguigu.gulimall.product.vo.Bounds;
import com.atguigu.gulimall.product.vo.Skus;
import com.atguigu.gulimall.product.vo.SpuSaveVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.SpuInfoDao;
import com.atguigu.gulimall.product.entity.SpuInfoEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    private SpuInfoDescService spuInfoDescService;

    @Autowired
    private SpuImagesService spuImagesService;

    @Autowired
    private ProductAttrValueService productAttrValueService;

    @Autowired
    private SkuInfoService skuInfoService;

    @Autowired
    private CouponFeignService couponFeignService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * TODO 高级篇
     * @param vo
     */
    @Transactional
    @Override
    public void saveSpuInfo(SpuSaveVo vo) {
        // 1. 保存spu基本信息 `pms_spu_info`
        SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(vo, spuInfoEntity);
        spuInfoEntity.setCreateTime(new Date());
        spuInfoEntity.setUpdateTime(new Date());
        this.saveBaseSpuInfo(spuInfoEntity);

        // 2. 保存spu的描述图片 `pms_spu_info_desc`
        List<String> descript = vo.getDecript();
        SpuInfoDescEntity spuInfoDescEntity = new SpuInfoDescEntity();
        spuInfoDescEntity.setSpuId(spuInfoEntity.getId());
        spuInfoDescEntity.setDecript(String.join(",", descript));
        spuInfoDescService.saveSpuInfoDesc(spuInfoDescEntity);

        // 3. 保存spu的图片集 `pms_spu_images`
        List<String> images = vo.getImages();
        spuImagesService.saveImages(spuInfoEntity.getId(), images);

        // 4. 保存spu的规格参数 `pms_product_attr_value`
        // 我自己写的，模仿上面，把获取数据保存进去的过程写到service里
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
        productAttrValueService.saveBaseAttrs(spuInfoEntity.getId(), baseAttrs);

        // 5. 保存spu的积分信息 `gulimall_sms->sms_spu_bounds`
        Bounds bounds = vo.getBounds();
        SpuBoundTo spuBoundTo = new SpuBoundTo();
        BeanUtils.copyProperties(vo, spuBoundTo);
        spuBoundTo.setSpuId(spuInfoEntity.getId());
        R r = couponFeignService.saveSpuBounds(spuBoundTo);
        if(r.getCode() != 0) {
            log.error("远程保存spu积分信息失败");
        }

        // 6. 保存spu对应的所有sku信息
        List<Skus> skus = vo.getSkus();
        if (!CollectionUtils.isEmpty(skus)) {
            skuInfoService.saveSkus(skus, spuInfoEntity);
        }
    }

    /**
     * 按照条件返回spu页面
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SpuInfoEntity> queryWrapper = new QueryWrapper<>();

        String catelogId = (String) params.get("catelogId");
        if (!StringUtils.isEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)){
            queryWrapper.eq("catalog_id", catelogId);
        }

        String brandId = (String) params.get("brandId");
        if (!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(brandId)){
            queryWrapper.eq("brand_id", brandId);
        }

        String status = (String) params.get("status");
        if (!StringUtils.isEmpty(status)){
            queryWrapper.eq("publish_status", status);
        }

        String key = (String) params.get("key");
        if(StringUtils.isEmpty(key)) {
            queryWrapper.and(w -> {
                w.eq("id", key).or().like("name", key);
            });
        }

        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    private void saveBaseSpuInfo(SpuInfoEntity spuInfoEntity) {
        this.baseMapper.insert(spuInfoEntity);
    }
}