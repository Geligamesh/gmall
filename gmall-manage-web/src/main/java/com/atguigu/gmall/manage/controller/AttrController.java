package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsBaseAttrInfo;
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

        return "success";
    }

    @GetMapping("attrInfoList")
    public List<PmsBaseAttrInfo> attrInfoList(String catalog3Id){
        return attrService.attrInfoList(catalog3Id);
    }
}
