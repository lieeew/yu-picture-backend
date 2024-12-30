package com.leikooo.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leikooo.yupicturebackend.model.dto.space.SpaceAddRequest;
import com.leikooo.yupicturebackend.model.dto.space.SpaceDeleteRequest;
import com.leikooo.yupicturebackend.model.dto.space.SpaceQueryRequest;
import com.leikooo.yupicturebackend.model.entity.Space;
import com.leikooo.yupicturebackend.model.entity.User;
import com.leikooo.yupicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author liang
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2024-12-26 23:54:29
*/
public interface SpaceService {
    /**
     * 创建空间
     *
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 校验空间
     *
     * @param space space
     * @param add   是否为创建时检验
     */
    void validSpace(Space space, boolean add);

    /**
     * 删除空间
     *
     * @param spaceDeleteRequest spaceId
     * @param loginUser 登录用户
     * @return boolean true 删除成功 、false 删除失败
     */
    boolean deleteSpace(SpaceDeleteRequest spaceDeleteRequest, User loginUser);

    /**
     * 获取空间包装类（单条）
     *
     * @param space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 获取空间包装类（分页）
     *
     * @param spacePage
     * @param request
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 获取查询对象
     *
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 根据空间级别填充空间对象
     */
    void fillSpaceBySpaceLevel(Space space);
}
