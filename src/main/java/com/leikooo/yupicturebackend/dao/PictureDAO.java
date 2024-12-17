package com.leikooo.yupicturebackend.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leikooo.yupicturebackend.mapper.PictureMapper;
import com.leikooo.yupicturebackend.model.entity.Picture;
import org.springframework.stereotype.Service;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2024/12/16
 * @description
 */
@Service
public class PictureDAO extends ServiceImpl<PictureMapper, Picture> {
    public Picture getById(Long id) {
        return this.query()
                .eq("id", id)
                .eq("isDeleted", 0).getEntity();
    }
}
