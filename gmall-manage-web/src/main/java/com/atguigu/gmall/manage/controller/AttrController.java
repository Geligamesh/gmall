package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsBaseAttrInfo;
import com.atguigu.gmall.bean.PmsBaseAttrValue;
import com.atguigu.gmall.bean.PmsBaseSaleAttr;
import com.atguigu.gmall.bean.PmsProductInfo;
import com.atguigu.gmall.service.AttrService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
public class AttrController  {

    @Reference
    AttrService attrService;

    @PostMapping("saveAttrInfo")
    public String saveAttrInfo(@RequestBody PmsBaseAttrInfo pmsBaseAttrInfo){
        if (pmsBaseAttrInfo == null) {
            return null;
        }
        //保存平台属性和平台属性值
        attrService.saveAttr(pmsBaseAttrInfo);
        return "success";
    }

    @GetMapping("attrInfoList")
    public List<PmsBaseAttrInfo> attrInfoList(String catalog3Id){
        return attrService.attrInfoList(catalog3Id);
    }

    //根据平台属性id查找属性值
    @PostMapping("getAttrValueList")
    public List<PmsBaseAttrValue> getAttrValueList(String attrId) {
        return attrService.getAttrValueList(attrId);
    }

    //查询所有商品销售属性
    @PostMapping("baseSaleAttrList")
    public List<PmsBaseSaleAttr> baseSaleAttrList() {
        return attrService.baseSaleAttrList();
    }

    @PostMapping("saveSpuInfo")
    public String saveSpuInfo(@RequestBody PmsProductInfo pmsProductInfo) {

        return "success";
    }

}
