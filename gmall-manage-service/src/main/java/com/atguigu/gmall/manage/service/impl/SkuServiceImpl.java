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
import java.util.UUID;

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
        if (pmsSkuInfo == null) {
            return null;
        }
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
            //设置分布式锁
            //拿到锁的线程有10秒的过期时间
            String token = UUID.randomUUID().toString();
            String OK = jedis.set("sku:" + skuId + ":lock", token, "nx", "px", 10*1000);
            if (StringUtils.isNotBlank(OK) && OK.equals("OK")) {
                //如果缓存中没有则查询MySQL
                pmsSkuInfo = this.getSkuByIdFromDB(skuId);
                //可在此处休眠5秒钟以等待其他线程自旋
                // Thread.sleep(5000);
                if (pmsSkuInfo != null) {
                    //mysql查询结果存入redis
                    String toJSONString = JSON.toJSONString(pmsSkuInfo);
                    jedis.set(skuKey, toJSONString);
                }else {
                    //数据库中不存在该sku
                    //为了防止缓存穿透将null值或者空字符串设置给redis
                    jedis.setex(skuKey, 60*3, JSON.toJSONString(""));
                }
                //在访问MySQL之后，将MySQL的分布式锁释放
                String lockToken = jedis.get("sku:" + skuId + ":lock");
                if (StringUtils.isNotBlank(lockToken) && token.equals(lockToken)) {
                    //jedis.eval("lua");可与lua脚本，在查询到key的同时删除该key，防止高并发下的意外的发生
                    //用token确认删除的是自己的sku的锁
                    jedis.del("sku:" + skuId + ":lock");
                }
            }else {
                //设置失败,自旋（线程在睡眠几秒后，重新尝试访问本方法）
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return getSkuById(skuId);
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
