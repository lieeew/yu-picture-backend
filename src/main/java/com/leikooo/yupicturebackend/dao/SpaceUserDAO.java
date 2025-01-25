package com.leikooo.yupicturebackend.dao;

import com.baomidou.mybatisplus.extension.conditions.query.ChainQuery;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leikooo.yupicturebackend.mapper.SpaceUserMapper;
import com.leikooo.yupicturebackend.model.entity.SpaceUser;
import org.springframework.stereotype.Service;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2025/1/16
 * @description
 */
@Service
public class SpaceUserDAO extends ServiceImpl<SpaceUserMapper, SpaceUser> {

    public SpaceUser getBySpaceUserId(Long spaceId, Long userId) {
        return this.lambdaQuery()
                .eq(SpaceUser::getSpaceId, spaceId)
                .eq(SpaceUser::getUserId, userId)
                .one();
    }

}
