package com.zy.blog.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;

import com.zy.blog.base.ServiceImplBase;
import com.zy.blog.entity.SystemConfig;
import com.zy.blog.example.SystemConfigExample;
import com.zy.blog.service.mapper.SystemConfigMapper;
import com.zy.blog.service.service.SystemConfigService;
import com.zy.blog.utils.JsonUtils;
import com.zy.blog.utils.StringUtils;
import com.zy.blog.utils.constant.*;
import com.zy.blog.utils.exception.type.QueryException;
import com.zy.blog.utils.util.RedisUtil;
import com.zy.blog.utils.util.ResultUtil;
import com.zy.blog.view.SystemConfigView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SystemConfigServiceImpl extends ServiceImplBase<SystemConfigMapper,SystemConfig> implements SystemConfigService {

    @Autowired
    private SystemConfigService systemConfigService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Resource
    private SystemConfigMapper systemConfigMapper;
    @Autowired
    private RedisUtil redisUtil;
    @Override
    public SystemConfig selectSystemConfig() {
        //from redis
        String systemConfigCache= redisTemplate.opsForValue().get(RedisConf.SYSTEM_CONFIG);
        if(StringUtils.isEmpty(systemConfigCache)){
            QueryWrapper<SystemConfig> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq(EntityConstant.STATUS,SqlConstant.STATUS_ONE)
                    .orderByDesc(EntityConstant.CREATE_TIME).last(SqlConstant.LIMIT_ONE);
            SystemConfig systemConfig = systemConfigMapper.selectOne(queryWrapper);
            if(systemConfig==null){
                throw new RuntimeException("????????????????????????");
            }else {
                redisTemplate.opsForValue().set(RedisConf.SYSTEM_CONFIG, JsonUtils.objectToJson(systemConfig),48, TimeUnit.HOURS);
            }
            return systemConfig;
        } else {
            SystemConfig systemConfig = JsonUtils.jsonToObj(systemConfigCache,SystemConfig.class);
            log.info(LogConstant.CACHE_LOG+systemConfig);
            return systemConfig;
        }
    }

    @Override
    public int editSystemConfig(SystemConfigView systemConfigView) {
        UpdateWrapper<SystemConfig> uw = new UpdateWrapper();
        SystemConfig systemConfig = new SystemConfig();
        BeanUtils.copyProperties(systemConfigView,systemConfig);
        SystemConfigExample example =new SystemConfigExample();
        SystemConfigExample.Criteria criteria = example.createCriteria();
        criteria.andUidEqualTo(systemConfigView.getUid());
        int res =  systemConfigMapper.editSystemConfig(systemConfig,example);
        if(res==1){//????????????
            redisTemplate.opsForValue().set(RedisKeyConstant.SYSTEM_CONFIG, JsonUtils.objectToJson(systemConfig),48, TimeUnit.HOURS);
        }
        return res;
    }

    @Override
    public SystemConfig getConfig() {
        // ???Redis?????????????????????
        String systemConfigJson = redisUtil.get(RedisConf.SYSTEM_CONFIG);
        if(StringUtils.isEmpty(systemConfigJson)) {
            QueryWrapper<SystemConfig> queryWrapper = new QueryWrapper<>();
            queryWrapper.orderByDesc(SQLConf.CREATE_TIME);
            queryWrapper.eq(SQLConf.STATUS, EStatus.ENABLE);
            queryWrapper.last(SysConf.LIMIT_ONE);
            SystemConfig systemConfig = systemConfigService.getOne(queryWrapper);
            if (systemConfig == null) {
                throw new QueryException(MessageConf.SYSTEM_CONFIG_IS_NOT_EXIST);
            } else {
                // ?????????????????????Redis????????????????????????24?????????
                redisUtil.setEx(RedisConf.SYSTEM_CONFIG, JsonUtils.objectToJson(systemConfig), 24, TimeUnit.HOURS);
            }
            return systemConfig;
        } else {
            SystemConfig systemConfig = com.zy.blog.utils.util.JsonUtils.jsonToPojo(systemConfigJson, SystemConfig.class);
            if(systemConfig == null) {
                throw new QueryException(ErrorCode.QUERY_DEFAULT_ERROR, "???????????????????????????????????????????????????????????????Redis????????????");
            }
            return systemConfig;
        }
    }

    @Override
    public String cleanRedisByKey(List<String> key) {
        if (key == null) {
            return ResultUtil.result(SysConf.ERROR, MessageConf.OPERATION_FAIL);
        }
        key.forEach(item -> {
            // ??????????????????key
            if (RedisConf.ALL.equals(item)) {
                Set<String> keys = redisUtil.keys(Constants.SYMBOL_STAR);
                redisUtil.delete(keys);
            } else {
                // ??????Redis???????????????
                Set<String> keys = redisUtil.keys(key + Constants.SYMBOL_STAR);
                redisUtil.delete(keys);
            }
        });
        return ResultUtil.successWithMessage(MessageConf.OPERATION_SUCCESS);
    }



    @Override
    public String getSearchModel() {
        SystemConfig systemConfig = this.getConfig();
        return systemConfig.getSearchModel();
    }



}
