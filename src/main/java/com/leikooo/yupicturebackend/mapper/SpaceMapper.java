package com.leikooo.yupicturebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leikooo.yupicturebackend.model.entity.Space;

import java.util.List;

/**
* @author liang
* @description 针对表【space(空间)】的数据库操作Mapper
* @createDate 2024-12-26 23:54:29
* @Entity com.leikooo.yupicturebackend.model.entity.Space
*/
public interface SpaceMapper extends BaseMapper<Space> {

    List<Space> getTopNSpaceUsage(Integer topN);

    void deleteByUserId(Long userId);

}




