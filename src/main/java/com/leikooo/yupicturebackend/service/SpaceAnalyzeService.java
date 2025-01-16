package com.leikooo.yupicturebackend.service;

import com.leikooo.yupicturebackend.model.dto.space.analyze.*;
import com.leikooo.yupicturebackend.model.entity.Space;
import com.leikooo.yupicturebackend.model.entity.User;
import com.leikooo.yupicturebackend.model.vo.space.analyze.*;

import java.util.List;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2025/1/10
 * @description
 */
public interface SpaceAnalyzeService {

    /**
     * 检验空间权限
     *
     * @param spaceAnalyzeRequest spaceAnalyzeRequest
     * @param loginUser           登录的用户
     */
    void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser);

    /**
     * 分析空间 category
     *
     * @param spaceCategoryAnalyzeRequest SpaceCategoryAnalyzeRequests
     * @param loginUser                   当前登录用户
     * @return List<SpaceCategoryAnalyzeResponse>
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser);

    /**
     * 获取空间使用分析数据
     *
     * @param spaceUsageAnalyzeRequest SpaceUsageAnalyzeRequest 请求参数
     * @param loginUser                当前登录用户
     * @return SpaceUsageAnalyzeResponse 分析结果
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser);

    /**
     * 获取空间标签分析
     *
     * @param spaceTagAnalyzeRequest SpaceTagAnalyzeRequests
     * @param loginUser              登录用户
     * @return List<SpaceTagAnalyzeResponse>
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);

    /**
     * 获取空间大小分析
     *
     * @param spaceSizeAnalyzeRequest SpaceSizeAnalyzeRequests
     * @param loginUser               当前登录用户
     * @return List<SpaceSizeAnalyzeResponse>
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);

    /**
     * 用户上传时间分析
     *
     * @param spaceUserAnalyzeRequest  spaceUserAnalyzeRequest
     * @param loginUser 当前登录的用户
     * @return List<SpaceUserAnalyzeResponse>
     */
    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);

    /**
     * 空间使用排行 仅管理员可用
     *
     * @param spaceRankAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser);
}
