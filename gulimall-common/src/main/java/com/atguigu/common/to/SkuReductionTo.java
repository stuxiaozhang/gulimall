package com.atguigu.common.to;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SkuReductionTo {
    private Long skuId;
    private int fullCount;  // 满几件
    private BigDecimal discount;  // 打几折
    private int countStatus;
    private BigDecimal fullPrice;  // 满多少
    private BigDecimal reducePrice;  // 减多少
    private int priceStatus;
    private List<MemberPrice> memberPrice;  // 商品会员价格 实体
}

