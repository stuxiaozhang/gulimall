package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.constant.ProductConstant;
import com.atguigu.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.atguigu.gulimall.product.dao.AttrGroupDao;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.atguigu.gulimall.product.entity.AttrGroupEntity;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.AttrGroupRelationVo;
import com.atguigu.gulimall.product.vo.AttrRespVo;
import com.atguigu.gulimall.product.vo.AttrVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.AttrDao;
import com.atguigu.gulimall.product.entity.AttrEntity;
import com.atguigu.gulimall.product.service.AttrService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {

    @Autowired
    private AttrAttrgroupRelationDao attrAttrgroupRelationDao;

    @Autowired
    private CategoryDao categoryDao;

    @Autowired
    private AttrGroupDao attrGroupDao;

    @Autowired
    private AttrDao attrDao;

    @Autowired
    private CategoryService categoryService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId, String attrType) {
                                                                                                // 如果类型是base，就为1
        QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<AttrEntity>().eq("attrType", "base".equalsIgnoreCase(attrType)? ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode(): ProductConstant.AttrEnum.ATTR_TYPE_SALE.getCode());

        // 1. 如果=0查询全部，否则查询指定第三级分类
        if (catelogId != 0) {
            queryWrapper.eq("catelog_id", catelogId);
        }

        // 2. 查看是否有key查询
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            queryWrapper.and((wrapper) -> {
                //attr_id  attr_name
                wrapper.eq("attr_id", key).or().like("attr_name", key);
            });
        }

        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                queryWrapper
        );

        //封装分页数据
        PageUtils pageUtils = new PageUtils(page);

        /* 把前端新增的属性查出来响应回去 */
        // 查出新增的属性 catelogName groupName
        List<AttrEntity> records = page.getRecords();
        List<AttrRespVo> respVos = records.stream().map((attrEntity) -> {
            AttrRespVo attrRespVo = new AttrRespVo();
            BeanUtils.copyProperties(attrEntity, attrRespVo);
            // 查询分类并设置分类名
            // catelogName,用attr表的catelogId，去catelog表查name
            CategoryEntity categoryEntity = categoryDao.selectOne(new QueryWrapper<CategoryEntity>().eq("cat_id", attrEntity.getCatelogId()));
            if (categoryEntity != null) {
                attrRespVo.setCatelogName(categoryEntity.getName());
            }
            // 如果是查询规格参数才查询设置分组名
            // groupName,用attr表的attr_id,去关联relation表查attr_group_id，然后去attr_group表查groupName
            if ("base".equalsIgnoreCase(attrType)) {
                AttrAttrgroupRelationEntity relationEntity = attrAttrgroupRelationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrEntity.getAttrId()));
                if (relationEntity != null && relationEntity.getAttrGroupId() != null) {
                    AttrGroupEntity attrGroupEntity = attrGroupDao.selectOne(new QueryWrapper<AttrGroupEntity>().eq("attr_group_id", relationEntity.getAttrGroupId()));
                    attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }
            return attrRespVo;
        }).collect(Collectors.toList());

        // 把新的数据传送过去
        pageUtils.setList(respVos);
        return pageUtils;
    }

    @Override
    public void saveAttr(AttrVo attr) {
        /* 1.先保存AttrEntity的值 */
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity);
        this.save(attrEntity);
        /* 2.再保存前端过来多出的值到 relation表 */
        if (attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() && attr.getAttrGroupId() != null) { //如果是规格参数，再操作分组信息
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attr.getAttrGroupId());
            relationEntity.setAttrId(attrEntity.getAttrId());
            attrAttrgroupRelationDao.insert(relationEntity);
        }

    }

    @Override
    public AttrRespVo getAttrInfo(Long attrId) {
        AttrRespVo attrRespVo = new AttrRespVo();
        AttrEntity attrEntity = this.getById(attrId);
        BeanUtils.copyProperties(attrEntity, attrRespVo);
        // 如果是规格参数，再操作分组信息
        if (attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()) {
            // 1. 设置分组信息 （attr_group_id）
            AttrAttrgroupRelationEntity attrgroupRelationEntity = attrAttrgroupRelationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrId));
            if (attrgroupRelationEntity != null) {
                attrRespVo.setAttrGroupId(attrgroupRelationEntity.getAttrGroupId());
            }
        }
        // 2. 设置分类信息（catelogPath）
        Long[] catelogPath = categoryService.findCatelogPath(attrEntity.getCatelogId());
        attrRespVo.setCatelogPath(catelogPath);
        return attrRespVo;
    }

    /* 保存时，要修改两张表. 先更新基本信息。再判断是更新还是新增 */
    @Transactional
    @Override
    public void updateAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity);
        this.updateById(attrEntity);
        // 如果是规格参数，再操作分组信息
        if (attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()) {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrId(attr.getAttrId());
            relationEntity.setAttrGroupId(attr.getAttrGroupId());

            //判断是新增还是删除
            Integer count = attrAttrgroupRelationDao.selectCount(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attr.getAttrId()));
            if (count > 0) {
                attrAttrgroupRelationDao.update(relationEntity, new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attr.getAttrId()));
            } else {
                attrAttrgroupRelationDao.insert(relationEntity);
            }
        }
    }

    /**
     * 根据分组id查找关联的所有基本属性
     * @param attrgroupId
     * @return
     */
    @Override
    public List<AttrEntity> getRelationAttr(Long attrgroupId) {
        List<AttrAttrgroupRelationEntity> relationEntities = attrAttrgroupRelationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", attrgroupId));

        /* 啊我觉得应该是因为返回的是一个map，所以得用stream */
        List<Long> attrIds = relationEntities.stream().map((attr) -> {
            return attr.getAttrId();
        }).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(attrIds)) {
        // if (attrIds == null || attrIds.size() == 0)
            return null;
        }
        List<AttrEntity> attrEntities = this.baseMapper.selectBatchIds(attrIds);
        return attrEntities;
    }

    /**
     * 12.4 获取属性分组没有关联的其他属性
     * @param params
     * @param attrgroupId
     * @return
     */
    @Override
    public PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId) {
        // 1. 当前分组只能关联自己所属的分类里面的所有属性(先查出当前分类id)
        // 这里必须先拿分组再拿分组中的分类id，直接在属性中确实可以拿到分类id，但是不能保证是这个分组的属性
        AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupId);
        Long catelogId = attrGroupEntity.getCatelogId();

        /** 找到本分类下所有属性，再判断关联的分组id是否为空
         * 整体思路：先找出其他分组已经关联的属性，再排除掉这些属性即可
         * 实现思路：
         *      1. 根据catelogId获取分组对象集合
         *         获取分组对象的attrGroupId并封装为集合
         *      2. 根据attrGroupId获取中间表对象集合
         *         获取中间表对象的attrId并封装为集合,此时就有了其他分组已经关联的属性
         *      3. 移除这些属性
         */
        // 2、当前分组只能关联别的分组没有引用的属性
        // 2.1 当前分类下的所有分组
        List<AttrGroupEntity> attrGroupEntities = attrGroupDao.selectList(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));
        List<Long> attrGroupIds = attrGroupEntities.stream().map(attrGroupEntity1 -> {
            return attrGroupEntity1.getAttrGroupId();
        }).collect(Collectors.toList());

        // 2.2 这些分组关联的属性
        List<AttrAttrgroupRelationEntity> relationEntities =
                attrAttrgroupRelationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().in("attr_group_id", attrGroupIds));
        List<Long> attrIds = relationEntities.stream().map(attrAttrgroupRelationEntity -> {
            return attrAttrgroupRelationEntity.getAttrId();
        }).collect(Collectors.toList());

        // 2.3 从当前分类(catelogId)的所有属性中移除这些属性
        QueryWrapper<AttrEntity> queryWrapper =
                new QueryWrapper<AttrEntity>().eq("catelog_id", catelogId).eq("attr_type", ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode());
        if (attrIds != null && attrIds.size() > 0){
        // if (!CollectionUtils.isEmpty(attrIds)) {
            queryWrapper.notIn("attr_id", attrIds);
        }

        //模糊查询
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            queryWrapper.and((wrapper) -> {
                wrapper.eq("attr_id", key).or().like("attr_name", key);
            });
        }

        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), queryWrapper);
        return new PageUtils(page);
    }
}