package com.atguigu.gulimall.product;

import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.service.BrandService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class GulimallProductApplicationTests {

    @Autowired
    BrandService brandService;

    @Test
    public void contextLoads() {
        BrandEntity brandEntity = new BrandEntity();
        /* 添加用户
        brandEntity.setName("小米");
        brandService.save(brandEntity);
        System.out.println("保存成功....");
        */
        /* 更新
        brandEntity.setBrandId(1L);
        brandEntity.setDescript("小米品牌的创始人是雷军");
        brandService.updateById(brandEntity);
        */
        // 查询
        QueryWrapper<BrandEntity> queryWrapper = new QueryWrapper<>();
        List<BrandEntity> list = brandService.list(queryWrapper.eq("brand_id", 1L));
        list.forEach((item) -> {
            System.out.println(item);
        });
    }

}
