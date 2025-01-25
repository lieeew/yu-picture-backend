package com.leikooo.yupicturebackend.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leikooo.yupicturebackend.exception.ErrorCode;
import com.leikooo.yupicturebackend.exception.ThrowUtils;
import com.leikooo.yupicturebackend.mapper.PictureMapper;
import com.leikooo.yupicturebackend.model.entity.Picture;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2024/12/16
 * @description
 */
@Service
public class PictureDAO extends ServiceImpl<PictureMapper, Picture> {
    /**
     * SELECT *
     * FROM picture
     * WHERE (space_id = 0 AND id = ?) OR (id = ?)
     *   AND is_delete = 0;
     * 如果 spaceId 为 0 或者为任意值都差不出来结果的话那么对应的 SpaceId 就是不存在的
     * @param id pictureId
     * @return Picture
     */
    public Picture getByPictureId(Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        // 查询逻辑
        return this.lambdaQuery()
                .and(wrapper -> wrapper
                        .or(r -> r
                                .eq(Picture::getSpaceId, 0L)
                                .eq(Picture::getId, id))
                        .or(r -> r.eq(Picture::getId, id)))
                .one();
    }

    /**
     * sql
     * SELECT COUNT(*)
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
