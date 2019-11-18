package com.atguigu.gmall.manage.redisson;

import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;

import java.util.concurrent.locks.Lock;

@RestController
public class RedissonController {

    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private RedissonClient redissonClient;

    @GetMapping("testRedisson")
    public String testRedisson() {
        Jedis jedis = redisUtil.getJedis();
        Lock lock = redissonClient.getLock("redis-lock");
        try {
            lock.lock();
            String value = jedis.get("key");
            if (StringUtils.isBlank(value)) {
                value = "1";
            }
            jedis.set("key", Integer.parseInt(value) + 1 + "");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return "success";
    }
}
