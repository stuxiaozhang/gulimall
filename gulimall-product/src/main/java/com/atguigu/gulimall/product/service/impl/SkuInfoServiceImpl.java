package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.to.SkuReductionTo;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SkuSaleAttrValueEntity;
import com.atguigu.gulimall.product.entity.SpuInfoEntity;
import com.atguigu.gulimall.product.fegin.CouponFeignService;
import com.atguigu.gulimall.product.service.SkuImagesService;
import com.atguigu.gulimall.product.service.SkuSaleAttrValueService;
import com.atguigu.gulimall.product.vo.Attr;
import com.atguigu.gulimall.product.vo.Images;
import com.atguigu.gulimall.product.vo.Skus;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.SkuInfoDao;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.service.SkuInfoService;
import org.springframework.util.StringUtils;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {

    @Autowired
    private SkuImagesService skuImagesService;

    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    private CouponFeignService couponFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 保存spu对应的所有sku信息. 又是我自己抽出来写到这里的。
     *
     * @param skus
     * @param spuInfoEntity
     */
    @Override
    public void saveSkus(List<Skus> skus, SpuInfoEntity spuInfoEntity) {
        // 6、保存spu对应的所有sku信息
        skus.forEach(sku -> {
            String defaultImg = "";
            //查找出默认图片
            for (Images image: sku.getImages()) {
                if (image.getDefaultImg() == 1) {
                    defaultImg = image.getImgUrl();
                    break;  // 找到就停下（我自己加的）
                }
            }
            SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
            BeanUtils.copyProperties(sku, skuInfoEntity);

            skuInfoEntity.setSpuId(spuInfoEntity.getId());
            skuInfoEntity.setBrandId(spuInfoEntity.getBrandId());
            skuInfoEntity.setCatalogId(spuInfoEntity.getCatalogId());
            skuInfoEntity.setSaleCount(0L);
            skuInfoEntity.setSkuDefaultImg(defaultImg);

            // 6.1 保存 sku的基本信息 `pms_sku_info`
            this.save(skuInfoEntity);

            // 6.2 插入sku的图片信息 `pms_sku_images`
            Long skuId = skuInfoEntity.getSkuId();
            List<SkuImagesEntity> skuImagesEntities = sku.getImages().stream().filter(img -> {
                    return !StringUtils.isEmpty(img.getImgUrl());  // 先过滤掉url为空的，返回true是需要，false就是剔除
                }).map(image -> {
                SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                skuImagesEntity.setSkuId(skuId);
                skuImagesEntity.setImgUrl(image.getImgUrl());
                skuImagesEntity.setDefaultImg(image.getDefaultImg());
                return skuImagesEntity;
            }).collect(Collectors.toList());
            skuImagesService.saveBatch(skuImagesEntities);

            // 6.3 插入sku的销售属性信息 `pms_sku_sale_attr_value`
            List<Attr> attrs = sku.getAttr();
            List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = attrs.stream().map(attr -> {
                SkuSaleAttrValueEntity skuSaleAttrValueEntity = new SkuSaleAttrValueEntity();
                BeanUtils.copyProperties(attr, skuSaleAttrValueEntity);
                skuSaleAttrValueEntity.setSkuId(skuId);
                return skuSaleAttrValueEntity;
            }).collect(Collectors.toList());
            skuSaleAttrValueService.saveBatch(skuSaleAttrValueEntities);


            // 6.4 sku的优惠、满减等信息 `gulimall_sms`->`sms_sku_ladder`/`sms_sku_full_reduction`/`sms_member_price`
            SkuReductionTo skuReductionTo = new SkuReductionTo();
            BeanUtils.copyProperties(sku, skuReductionTo);
            skuReductionTo.setSkuId(skuId);
            // 满几件(打几折) >0 或 满多少(减多少) > 0
            if (skuReductionTo.getFullCount() > 0 || skuReductionTo.getFullPrice().compareTo(new BigDecimal("0")) == 1) {
                R r1 = couponFeignService.saveSkuReduction(skuReductionTo);
                if (r1.getCode() != 0){
                    log.error("远程保存优惠信息失败");
                }
            }
        });
    }

    /**
     * 按照条件返回sku页面
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SkuInfoEntity> wrapper = new QueryWrapper<>();
        /**
         * key:
         * catelogId: 0
         * brandId: 0
         * min: 0
         * max: 0
         */
        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)) {
            wrapper.and(w -> {
                w.eq("sku_id", key).or().like("name", key);
            });
        }

        String catelogId = (String) params.get("catelogId");
        if(!StringUtils.isEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)) {
            wrapper.eq("catelog_id", catelogId);
        }

        String brandId = (String) params.get("brandId");
        if (!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(brandId)) {
            wrapper.eq("brand_id", brandId);
        }

        String min = (String) params.get("min");
        if (!StringUtils.isEmpty(min)) {  // min 可以等于 0...
            wrapper.ge("price", min);
        }

        String max = (String) params.get("max");
        if (!StringUtils.isEmpty(max)) {
            try {
                BigDecimal max_decimal = new BigDecimal(max);
                if (max_decimal.compareTo(new BigDecimal("0")) == 1) {  // max_decimal > 0
                    wrapper.le("price", max);
                }
            }
            catch (Exception e) {

            }
        }

        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

}