package com.leikooo.yupicturebackend.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leikooo.yupicturebackend.exception.ErrorCode;
import com.leikooo.yupicturebackend.exception.ThrowUtils;
import com.leikooo.yupicturebackend.mapper.PictureMapper;
import com.leikooo.yupicturebackend.model.entity.Picture;
import org.springframework.stereotype.Service;

import java.util.List;

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

    /**
     * sql
     *SELECT COUNT(*)
     * FROM picture
     * WHERE JSON_CONTAINS(urls, '"xxxx"', '$.url');
     *
     * @param pictureUrl
     * @return
     */
    public Long getOneUrlCount(String pictureUrl) {
        // 构造 JSON 路径
        String jsonPath = "$." + "url";
        String queryUrl = "\"" + pictureUrl + "\"";
        return this.lambdaQuery()
                .apply("JSON_CONTAINS(urls, {0}, {1})", queryUrl, jsonPath)
                .count();
    }


    public List<Picture> getListPageBySpaceId(Long spaceId, Long userId) {
        return this.lambdaQuery().eq(Picture::getSpaceId, spaceId)
                .eq(Picture::getUserId, userId)
                .eq(Picture::getIsDelete, 0).list();
    }

    public void deleteBatchIds(List<Long> list) {
        this.removeByIds(list);
    }

    public List<Picture> queryHasColor(Long spaceId) {
        return this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();
    }

    public List<Picture> getPictureByIds(List<Long> pictureIdList, Long spaceId) {
        return this.lambdaQuery()
                .select(Picture::getId, Picture::getSpaceId)
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, pictureIdList)
                .list();
    }
}
