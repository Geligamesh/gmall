package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PmsProductSaleAttr;
import com.atguigu.gmall.bean.PmsSkuInfo;

import java.util.List;

public interface SkuService {

    void saveSkuInfo(PmsSkuInfo pmsSkuInfo);

    PmsSkuInfo getSkuById(String skuId);

    List<PmsProductSaleAttr> spuSaleAttrListCheckBySku(String productId,String skuId);

    List<PmsSkuInfo> getSkuSaleAttrValueBySpu(String productId);
}
