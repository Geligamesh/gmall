package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.PmsBaseAttrInfo;
import com.atguigu.gmall.bean.PmsBaseAttrValue;
import com.atguigu.gmall.bean.PmsBaseSaleAttr;
import com.atguigu.gmall.manage.mapper.PmsBaseAttrInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsBaseAttrValueMapper;
import com.atguigu.gmall.manage.mapper.PmsBaseSaleAttrMapper;
import com.atguigu.gmall.service.AttrService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AttrServiceImpl implements AttrService {

    @Autowired
    private PmsBaseAttrInfoMapper pmsBaseAttrInfoMapper;
    @Autowired
    private PmsBaseAttrValueMapper pmsBaseAttrValueMapper;
    @Autowired
    private PmsBaseSaleAttrMapper pmsBaseSaleAttrMapper;

    @Override
    //根据三级分类获取平台属性
    public List<PmsBaseAttrInfo> attrInfoList(String catalog3Id) {
        PmsBaseAttrInfo pmsBaseAttrInfo = new PmsBaseAttrInfo();
        pmsBaseAttrInfo.setCatalog3Id(catalog3Id);
        List<PmsBaseAttrInfo> pmsBaseAttrInfos = pmsBaseAttrInfoMapper.select(pmsBaseAttrInfo);
        for (PmsBaseAttrInfo baseAttrInfo : pmsBaseAttrInfos) {
            String id = baseAttrInfo.getId();
            PmsBaseAttrValue pmsBaseAttrValue = new PmsBaseAttrValue();
            pmsBaseAttrValue.setAttrId(id);
            List<PmsBaseAttrValue> pmsBaseAttrValues = pmsBaseAttrValueMapper.select(pmsBaseAttrValue);
            baseAttrInfo.setAttrValueList(pmsBaseAttrValues);
        }
        return pmsBaseAttrInfos;
    }

    //保存更新删除属性和属性值
    @Override
    public void saveAttr(PmsBaseAttrInfo pmsBaseAttrInfo) {
        //如果数据库中已经保存了平台属性了，则不处理
        if (StringUtils.isBlank(pmsBaseAttrInfo.getId())) {
            pmsBaseAttrInfoMapper.insertSelective(pmsBaseAttrInfo);
        }
        //从数据库中查找需要平台属性名，如果跟传进来的不一样则更新
        PmsBaseAttrInfo result = pmsBaseAttrInfoMapper.selectByPrimaryKey(pmsBaseAttrInfo.getId());
        if (!result.getAttrName().equals(pmsBaseAttrInfo.getAttrName())) {
            pmsBaseAttrInfoMapper.updateByPrimaryKey(pmsBaseAttrInfo);
        }
        List<PmsBaseAttrValue> attrValueList = pmsBaseAttrInfo.getAttrValueList();
        List<PmsBaseAttrValue> valueList = getAttrValueList(pmsBaseAttrInfo.getId());
        if (attrValueList != null && attrValueList.size() > 0) {
            attrValueList.forEach(pmsBaseAttrValue -> {
                //如果传进来的平台属性值id为null，表示要保存到平台属性值数据库，如果不为null，则不处理
                if (pmsBaseAttrValue.getId() == null) {
                    pmsBaseAttrValue.setAttrId(pmsBaseAttrInfo.getId());
                    pmsBaseAttrValueMapper.insertSelective(pmsBaseAttrValue);
                }
            });
        }
        List<String> attrIdList = valueList.stream().map(PmsBaseAttrValue::getId).collect(Collectors.toList());
        valueList.forEach(value -> attrValueList.forEach(attrValue -> {
            if (value.getId().equals(attrValue.getId())) {
                attrIdList.remove(attrValue.getId());
            }
        }));
        //删除平台属性
        attrIdList.forEach(attrId -> {
            pmsBaseAttrValueMapper.deleteByPrimaryKey(attrId);
        });
    }

    //根据平台属性id查找属性值
    @Override
    public List<PmsBaseAttrValue> getAttrValueList(String attrId) {
        Example example = new Example(PmsBaseAttrValue.class);
        example.createCriteria().andEqualTo("attrId", attrId);
        return pmsBaseAttrValueMapper.selectByExample(example);
    }

    @Override
    public List<PmsBaseSaleAttr> baseSaleAttrList() {
        return pmsBaseSaleAttrMapper.selectAll();
    }
}
