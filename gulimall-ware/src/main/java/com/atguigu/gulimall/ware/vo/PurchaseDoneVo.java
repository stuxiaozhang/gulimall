package com.atguigu.gulimall.ware.vo;

import lombok.Data;

import java.util.List;

@Data
public class PurchaseDoneVo {
    // {itemId:1, status:4, reason: "null"}
    private Long id;  // 采购单id
    private List<PurchaseItemDoneVo> items;  // 采购项。。？
}
