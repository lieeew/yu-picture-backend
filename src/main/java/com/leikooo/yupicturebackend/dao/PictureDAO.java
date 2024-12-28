package com.leikooo.yupicturebackend.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leikooo.yupicturebackend.exception.ErrorCode;
import com.leikooo.yupicturebackend.exception.ThrowUtils;
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
    public Picture getByPictureId(Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        return this.query()
                .eq("id", id)
                .eq("isDelete", 0).one();
    }

    public Long getOneUrlCount(String pictureUrl) {
        return this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
    }
}
