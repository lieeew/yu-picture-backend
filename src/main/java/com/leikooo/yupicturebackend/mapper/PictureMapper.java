package com.leikooo.yupicturebackend.mapper;

import com.leikooo.yupicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * @author liang
 * @description 针对表【picture(图片)】的数据库操作Mapper
 * @createDate 2024-12-16 11:56:07
 * @Entity com.leikooo.yupicturebackend.model.entity.Picture
 */
public interface PictureMapper extends BaseMapper<Picture> {

    /**
     * 查询分类统计信息
     *
     * @return 分类统计列表
     */
    @MapKey("category")
    List<Map<String, Object>> getCategoryStatistics(@Param("params") Map<String, Object> params);


    /**
     * 查询分类统计信息
     *
     * @return 分类统计列表
     */
    @MapKey("period")
    List<Map<String, Object>> analyzeByTimeDimension(@Param("params") Map<String, Object> params);
}




