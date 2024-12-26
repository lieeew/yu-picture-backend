package com.leikooo.yupicturebackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2024/12/25
 * @description
 */
@SpringBootTest
public class RedisTemplate {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void testRedis() {
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();

        String key = "testKey";
        String value = "testValue";

        // 1、测试新政和更新操作
        valueOps.set(key, value, 2, TimeUnit.MINUTES);
        String storeValue = valueOps.get(key);
        System.out.println(storeValue);
        assertEquals(storeValue, value, "存储的值和预期不一致");

        // 2、测试修改
        String updateValue = "updateValue";
        valueOps.set(key, updateValue);
        storeValue = valueOps.get(key);
        System.out.println(storeValue);
        assertEquals(storeValue, "updateValue", "存储的值和预期不一致");

        // 3、测试查询操作
        valueOps.get(key);
        storeValue = valueOps.get(key);
        System.out.println(storeValue);
        assertEquals(storeValue, updateValue, "存储的值和预期不一致");

        // 4、测试删除操作
        stringRedisTemplate.delete(key);
        storeValue = valueOps.get(key);
        System.out.println(storeValue);
        assertNull(storeValue, "删除后值不为 null ");
    }
}
