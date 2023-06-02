package com.atguigu.gulimall.ware.service.impl;

import com.atguigu.common.constant.WareConstant;
import com.atguigu.gulimall.ware.entity.PurchaseDetailEntity;
import com.atguigu.gulimall.ware.service.PurchaseDetailService;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.atguigu.gulimall.ware.vo.MergeVo;
import com.atguigu.gulimall.ware.vo.PurchaseDoneVo;
import com.atguigu.gulimall.ware.vo.PurchaseItemDoneVo;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.ware.dao.PurchaseDao;
import com.atguigu.gulimall.ware.entity.PurchaseEntity;
import com.atguigu.gulimall.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    @Autowired
    private PurchaseDetailService purchaseDetailService;

    @Autowired
    private WareSkuService wareSkuService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageUnreceive(Map<String, Object> params) {
        QueryWrapper<PurchaseEntity> wrapper = new QueryWrapper<>();
        //
        wrapper.eq("status", WareConstant.PurchaseStatusEnum.CREATED.getCode())
                .or().eq("status", WareConstant.PurchaseStatusEnum.ASSIGNED.getCode());

        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void mergePurchase(MergeVo mergeVo) {
        Long purchaseId = mergeVo.getPurchaseId();
        // 如果采购id为null 说明没选采购单
        if (purchaseId == null){
            //新建采购单
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATED.getCode());
            this.save(purchaseEntity);
            purchaseId = purchaseEntity.getId();
        }
        List<Long> items = mergeVo.getItems();
        //合并采购需求
        Long finalPurchaseId = purchaseId;
        // 分配，就是修改【采购需求】里对应的【采购单id、采购需求状态】，即purchase_detail表
        List<PurchaseDetailEntity> list = purchaseDetailService.getBaseMapper().selectBatchIds(items).stream().filter(item -> {
            // 如果还没去采购，或者采购失败，就可以修改
            return item.getStatus() == WareConstant.PurchaseDetailStatusEnum.CREATED.getCode()
                    || item.getStatus() == WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode();
        }).map(entity -> {
            // 修改状态，以及采购单id
            entity.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());
            entity.setPurchaseId(finalPurchaseId);
            return entity;
        }).collect(Collectors.toList());

        purchaseDetailService.saveBatch(list);
    }

    @Transactional
    @Override
    public void received(List<Long> ids) {
        // 没有采购需求直接返回，否则会破坏采购单
        if (ids == null || ids.size() == 0) {
            return ;
        }

        List<PurchaseEntity> purchaseEntityList = this.getBaseMapper().selectBatchIds(ids).stream().filter(entity -> {
            // 1、 确保采购单的状态是新建或者已分配
            return entity.getStatus() == WareConstant.PurchaseStatusEnum.CREATED.getCode()
                    || entity.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode();
        }).map(entity -> {
            // 2、改变采购单的状态
            entity.setStatus(WareConstant.PurchaseStatusEnum.ASSIGNED.getCode());
            return entity;
        }).collect(Collectors.toList());
        this.saveBatch(purchaseEntityList);

        // 3、修改该采购单下的所有采购需求的状态为正在采购
        UpdateWrapper<PurchaseDetailEntity> updateWrapper = new UpdateWrapper<PurchaseDetailEntity>().in("purchase_id", ids);
        PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
        purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.BUYING.getCode());
        purchaseDetailService.update(purchaseDetailEntity, updateWrapper);
    }

    /**
     * 完成采购单里的采购需求，并更新库存
     * 采购单里应该有很多采购需求(采购项)，得都完成了采购单才完成
     * @param vo
     */
    @Transactional
    @Override
    public void finishPurchase(PurchaseDoneVo doneVo) {
        //1、根据前端发过来的信息，更新采购需求的状态
        List<PurchaseItemDoneVo> items = doneVo.getItems();
        List<PurchaseDetailEntity> updateList = new ArrayList<>();

        // todo: 数据库表里没有reason字段，需要可以自己加上
        boolean flag = true;
        for (PurchaseItemDoneVo item: items) {
            Long detailId = item.getItemId();
            PurchaseDetailEntity detailEntity = purchaseDetailService.getById(detailId);
            detailEntity.setStatus(item.getStatus());

            //采购需求失败
            if (item.getStatus() == WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode()) {
                flag = false;
            }
            else {
                // 3、根据采购需求的状态，更新库存
                // sku_id, sku_num, ware_id
                // sku_id, ware_id, stock sku_name(调用远程服务获取), stock_locked(先获取已经有的库存，再加上新购买的数量)

                // 更新库存
                wareSkuService.addStock(detailEntity.getSkuId(), detailEntity.getWareId(), detailEntity.getSkuNum());
            }
            updateList.add(detailEntity);
        }
        //保存采购需求
        purchaseDetailService.updateBatchById(updateList);

        // 2、根据采购需求的状态，更新采购单的状态
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(doneVo.getId());
        purchaseEntity.setStatus(flag ? WareConstant.PurchaseStatusEnum.FINISH.getCode() : WareConstant.PurchaseStatusEnum.HASERROR.getCode());
        this.updateById(purchaseEntity);
    }

}