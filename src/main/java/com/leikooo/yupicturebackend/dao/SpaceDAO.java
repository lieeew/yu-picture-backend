package com.leikooo.yupicturebackend.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leikooo.yupicturebackend.mapper.SpaceMapper;
import com.leikooo.yupicturebackend.model.entity.Space;
import org.springframework.stereotype.Service;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2024/12/26
 * @description
 */
@Service
public class SpaceDAO extends ServiceImpl<SpaceMapper, Space> {
    public boolean isExistSpaceByUserId(Long id) {
        return this.lambdaQuery()
                .eq(Space::getUserId, id).exists();
    }
}
