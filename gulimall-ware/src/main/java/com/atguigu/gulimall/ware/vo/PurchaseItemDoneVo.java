package com.atguigu.gulimall.ware.vo;

import lombok.Data;

/**
 * 采购单里的采购项，应该就是采购需求的一部分吧？
 */
@Data
public class PurchaseItemDoneVo {
    private Long itemId;
    private Integer status;
    private String reason;
}
