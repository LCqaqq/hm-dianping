package com.hmdp.controller;


import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.SystemConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/shop")
public class ShopController {

    @Resource
    public IShopService shopService;
    @Resource
    public StringRedisTemplate stringRedisTemplate;

    String CACHE_SHOP_KEY ="cache:shop";

    /**
     * 根据id查询商铺信息
     * @param id 商铺id
     * @return 商铺详情数据
     */
    @GetMapping("/{id}")
    public Result queryShopById(@PathVariable("id") Long id) {
        String key = CACHE_SHOP_KEY  +id;
        //1.从redis查询商户
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //2.判断是否存在
        if(StrUtil.isNotBlank(shopJson)){
            //3.存在直接返回
            Shop shop = JSONUtil.toBean(shopJson,Shop.class);
            return Result.ok(shop);
        }
        //4.不存在，根据id查询数据库
        Shop shop = shopService.getById(id);
        //5.不存在，返回
        if(shop == null){
            return Result.fail("店铺不存在");
        }
        //6.存在，存入redis
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),30L, TimeUnit.MINUTES);
        //7.返回
        return Result.ok(shop);
    }

    /**
     * 新增商铺信息
     * @param shop 商铺数据
     * @return 商铺id
     */
    @PostMapping
    public Result saveShop(@RequestBody Shop shop) {
        // 写入数据库
        shopService.save(shop);
        // 返回店铺id
        return Result.ok(shop.getId());
    }

    /**
     * 更新商铺信息
     * @param shop 商铺数据
     * @return 无
     */
    @PutMapping
    @Transactional
    public Result updateShop(@RequestBody Shop shop) {
        Long id = shop.getId();
        if(id == null){
            return Result.fail("id不能为空");
        }
        // 写入数据库
        shopService.updateById(shop);
        // 删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY+id);
        return Result.ok();
    }

    /**
     * 根据商铺类型分页查询商铺信息
     * @param typeId 商铺类型
     * @param current 页码
     * @return 商铺列表
     */
    @GetMapping("/of/type")
    public Result queryShopByType(
            @RequestParam("typeId") Integer typeId,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        // 根据类型分页查询
        Page<Shop> page = shopService.query()
                .eq("type_id", typeId)
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
        // 返回数据
        return Result.ok(page.getRecords());
    }

    /**
     * 根据商铺名称关键字分页查询商铺信息
     * @param name 商铺名称关键字
     * @param current 页码
     * @return 商铺列表
     */
    @GetMapping("/of/name")
    public Result queryShopByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        // 根据类型分页查询
        Page<Shop> page = shopService.query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 返回数据
        return Result.ok(page.getRecords());
    }
}
