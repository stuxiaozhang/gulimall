package com.atguigu.gulimall.ware.dao;

import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 商品库存
 * 
 * @author zhangxinyan
 * @email zhangxinyan1999@126.com
 * @date 2023-04-24 11:02:45
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {

    void addstock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("skuNum") Integer skuNum);
}
