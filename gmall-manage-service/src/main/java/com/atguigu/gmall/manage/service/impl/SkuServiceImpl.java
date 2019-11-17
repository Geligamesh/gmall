package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class SkuServiceImpl implements SkuService {

    @Autowired
    private PmsSkuInfoMapper pmsSkuInfoMapper;
    @Autowired
    private PmsSkuImageMapper pmsSkuImageMapper;
    @Autowired
    private PmsSkuAttrValueMapper pmsSkuAttrValueMapper;
    @Autowired
    private PmsSkuSaleAttrValueMapper pmsSkuSaleAttrValueMapper;
    @Autowired
    private PmsProductSaleAttrMapper pmsProductSaleAttrMapper;
    @Autowired
    private PmsProductSaleAttrValueMapper pmsProductSaleAttrValueMapper;
    @Autowired
    private RedisUtil redisUtil;

    @Override
    public void saveSkuInfo(PmsSkuInfo pmsSkuInfo) {
        // 插入skuInfo
        pmsSkuInfoMapper.insertSelective(pmsSkuInfo);
        String skuId = pmsSkuInfo.getId();

        // 插入平台属性关联
        List<PmsSkuAttrValue> skuAttrValueList = pmsSkuInfo.getSkuAttrValueList();
        for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
            pmsSkuAttrValue.setSkuId(skuId);
            pmsSkuAttrValueMapper.insertSelective(pmsSkuAttrValue);
        }

        // 插入销售属性关联
        List<PmsSkuSaleAttrValue> skuSaleAttrValueList = pmsSkuInfo.getSkuSaleAttrValueList();
        for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
            pmsSkuSaleAttrValue.setSkuId(skuId);
            pmsSkuSaleAttrValueMapper.insertSelective(pmsSkuSaleAttrValue);
        }

        // 插入图片信息
        List<PmsSkuImage> skuImageList = pmsSkuInfo.getSkuImageList();
        for (PmsSkuImage pmsSkuImage : skuImageList) {
            pmsSkuImage.setSkuId(skuId);
            pmsSkuImageMapper.insertSelective(pmsSkuImage);
        }
    }

    //从数据库中获取SKU相关信息
    public PmsSkuInfo getSkuByIdFromDB(String skuId) {
        PmsSkuInfo pmsSkuInfo = pmsSkuInfoMapper.selectByPrimaryKey(skuId);
        Example exampleImage = new Example(PmsSkuImage.class);
        exampleImage.createCriteria().andEqualTo("skuId", skuId);
        List<PmsSkuImage> pmsSkuImages = pmsSkuImageMapper.selectByExample(exampleImage);
        pmsSkuInfo.setSkuImageList(pmsSkuImages);

        Example exampleAttrValue = new Example(PmsSkuAttrValue.class);
        exampleAttrValue.createCriteria().andEqualTo("skuId", skuId);
        List<PmsSkuAttrValue> pmsSkuAttrValues = pmsSkuAttrValueMapper.selectByExample(exampleAttrValue);
        pmsSkuInfo.setSkuAttrValueList(pmsSkuAttrValues);

        Example exampleSaleAttrValue = new Example(PmsSkuSaleAttrValue.class);
        exampleSaleAttrValue.createCriteria().andEqualTo("skuId", skuId);
        List<PmsSkuSaleAttrValue> pmsSkuSaleAttrValues = pmsSkuSaleAttrValueMapper.selectByExample(exampleSaleAttrValue);
        pmsSkuInfo.setSkuSaleAttrValueList(pmsSkuSaleAttrValues);
        return pmsSkuInfo;
    }

    //从缓存中获取SKU相关信息
    @Override
    public PmsSkuInfo getSkuById(String skuId) {
        //连接缓存
        Jedis jedis = redisUtil.getJedis();
        String skuKey = "sku:" + skuId + ":info";
        String skuJson = jedis.get(skuKey);
        PmsSkuInfo pmsSkuInfo;
        if (StringUtils.isNotBlank(skuJson)) {
            //查询缓存
            pmsSkuInfo = JSON.parseObject(skuJson, PmsSkuInfo.class);
        }else {
            //如果缓存中没有则查询MySQL
            pmsSkuInfo = this.getSkuByIdFromDB(skuId);
            if (pmsSkuInfo != null) {
                //mysql查询结果存入redis
                String toJSONString = JSON.toJSONString(pmsSkuInfo);
                jedis.set(skuKey, toJSONString);
            }else {
                //数据库中不存在该sku
                //为了防止缓存穿透将null值或者空字符串设置给redis
                jedis.setex(skuKey, 60*3, JSON.toJSONString(""));

            }
        }
        jedis.close();
        return pmsSkuInfo;
    }

    @Override
    public List<PmsProductSaleAttr> spuSaleAttrListCheckBySku(String productId,String skuId) {
  /*      Example exampleSaleAttr = new Example(PmsProductSaleAttr.class);
        exampleSaleAttr.createCriteria().andEqualTo("productId", productId);
        List<PmsProductSaleAttr> pmsProductSaleAttrs = pmsProductSaleAttrMapper.selectByExample(exampleSaleAttr);

        for (PmsProductSaleAttr pmsProductSaleAttr : pmsProductSaleAttrs) {
            String saleAttrId = pmsProductSaleAttr.getSaleAttrId();
            PmsProductSaleAttrValue pmsProductSaleAttrValue = new PmsProductSaleAttrValue();
            pmsProductSaleAttrValue.setProductId(productId);
            pmsProductSaleAttrValue.se tSaleAttrId(saleAttrId);
            List<PmsProductSaleAttrValue> pmsProductSaleAttrValues = pmsProductSaleAttrValueMapper.select(pmsProductSaleAttrValue);
            pmsProductSaleAttr.setSpuSaleAttrValueList(pmsProductSaleAttrValues);
        }
        return pmsProductSaleAttrs;*/
         return pmsProductSaleAttrMapper.selectSpuSaleAttrListCheckBySku(productId,skuId);
    }

    @Override
    public List<PmsSkuInfo> getSkuSaleAttrValueBySpu(String productId) {
        return pmsSkuInfoMapper.selectSkuSaleAttrValueBySpu(productId);
    }
}
