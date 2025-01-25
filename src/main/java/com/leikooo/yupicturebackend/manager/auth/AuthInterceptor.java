package com.leikooo.yupicturebackend.manager.auth;

import cn.dev33.satoken.filter.SaFilterAuthStrategy;
import cn.dev33.satoken.router.SaRouter;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.leikooo.yupicturebackend.dao.PictureDAO;
import com.leikooo.yupicturebackend.dao.SpaceDAO;
import com.leikooo.yupicturebackend.dao.SpaceUserDAO;
import com.leikooo.yupicturebackend.exception.BusinessException;
import com.leikooo.yupicturebackend.exception.ErrorCode;
import com.leikooo.yupicturebackend.exception.ThrowUtils;
import com.leikooo.yupicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.leikooo.yupicturebackend.model.entity.Picture;
import com.leikooo.yupicturebackend.model.entity.Space;
import com.leikooo.yupicturebackend.model.entity.SpaceUser;
import com.leikooo.yupicturebackend.model.entity.User;
import com.leikooo.yupicturebackend.model.enums.SpaceRoleEnum;
import com.leikooo.yupicturebackend.model.enums.SpaceTypeEnum;
import com.leikooo.yupicturebackend.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

import static com.leikooo.yupicturebackend.model.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2025/1/19
 * @description
 */
@Slf4j
@AllArgsConstructor
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final UserService userService;

    private final SpaceUserAuthManager spaceUserAuthManager;

    private final PictureDAO pictureDAO;

    private final SpaceDAO spaceDAO;

    private final SpaceUserDAO spaceUserDAO;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!SaRouter.match("/api/user/login").isHit) {
            initAuthContextByRequest(request);
        }
        SaTokenFilterAuthStrategy saTokenFilterAuthStrategy = new SaTokenFilterAuthStrategy();
        SaFilterAuthStrategy saFilterAuthStrategy = saTokenFilterAuthStrategy.getSaFilterAuthStrategy();
        saFilterAuthStrategy.run(handler);
        return true;
    }


    /**
     * preHandle 不抛出异常或者返回 false 的时候这个可以生效，
     * 但是抛出异常的话这个就不生效了，所以 preHandle 需要套一层 try finally
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        SaTokenContextHolder.clear();
    }


    /**
     * 从请求中获取上下文对象
     */
    private void initAuthContextByRequest(HttpServletRequest request) {
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
        SpaceUserAuthContext authRequest;
        // 获取请求参数
        if (ContentType.JSON.getValue().equals(contentType)) {
            String body = ServletUtil.getBody(request);
            authRequest = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        } else {
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            authRequest = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }
        // 根据请求路径区分 id 字段的含义
        Long id = authRequest.getId();
        User loginUser = (User) StpKit.SPACE.getSession().get(USER_LOGIN_STATE);
        if (Objects.nonNull(id)) {
            // 获取到请求路径的业务前缀，/api/picture/aaa?a=1
            String requestURI = request.getRequestURI();
            // 先替换掉上下文，剩下的就是前缀
            String partURI = requestURI.replace("/api" + "/", "");
            // 获取前缀的第一个斜杠前的字符串
            String moduleName = StrUtil.subBefore(partURI, "/", false);
            switch (moduleName) {
                case "picture":
                    authRequest.setPictureId(id);
                    break;
                case "spaceUser":
                    authRequest.setSpaceUserId(id);
                    break;
                case "space":
                    authRequest.setSpaceId(id);
                    break;
                default:
            }
        }
        List<String> permissionList = this.getPermissionList(loginUser.getId(), authRequest);
        authRequest.setPermissionList(permissionList);
        SaTokenContextHolder.set(loginUser.getId().toString(), authRequest);
    }

    /**
     * 返回一个账号所拥有的权限码集合
     */
    public List<String> getPermissionList(Object loginId, SpaceUserAuthContext authContext) {
        // 管理员权限，表示权限校验通过
        List<String> ADMIN_PERMISSIONS = spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 如果所有字段都为空，表示查询公共图库，可以通过
        if (isAllFieldsNull(authContext)) {
            return ADMIN_PERMISSIONS;
        }
        // 获取 userId
        User loginUser = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(USER_LOGIN_STATE);
        ThrowUtils.throwIf(Objects.isNull(loginUser), ErrorCode.NOT_FOUND_ERROR, "未找到用户信息");
        Long userId = loginUser.getId();
        // 优先从上下文中获取 SpaceUser 对象
        SpaceUser spaceUser = authContext.getSpaceUser();
        if (spaceUser != null) {
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
        // 如果有 spaceUserId，必然是团队空间，通过数据库查询 SpaceUser 对象
        Long spaceUserId = authContext.getSpaceUserId();
        if (spaceUserId != null) {
            spaceUser = spaceUserDAO.getById(spaceUserId);
            ThrowUtils.throwIf(Objects.isNull(spaceUser), ErrorCode.NOT_FOUND_ERROR, "未找到空间用户信息");
            // 取出当前登录用户对应的 spaceUser
            SpaceUser loginSpaceUser = spaceUserDAO.getBySpaceUserId(spaceUser.getSpaceId(), userId);
            if (loginSpaceUser == null) {
                return new ArrayList<>();
            }
            // 这里会导致管理员在私有空间没有权限，可以再查一次库处理
            return spaceUserAuthManager.getPermissionsByRole(loginSpaceUser.getSpaceRole());
        }
        // 如果没有 spaceUserId，尝试通过 spaceId 或 pictureId 获取 Space 对象并处理
        Long spaceId = authContext.getSpaceId();
        if (spaceId == null) {
            // 如果没有 spaceId，通过 pictureId 获取 Picture 对象和 Space 对象
            Long pictureId = authContext.getPictureId();
            // 图片 id 也没有，则默认通过权限校验
            if (pictureId == null) {
                return ADMIN_PERMISSIONS;
            }
            Picture picture = pictureDAO.getByPictureId(pictureId);
            ThrowUtils.throwIf(Objects.isNull(picture), new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到图片信息"));
            spaceId = picture.getSpaceId();
            // 公共图库，仅本人或管理员可操作
            if (spaceId == null) {
                if (picture.getUserId().equals(userId) || userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                } else {
                    // 不是自己的图片，仅可查看
                    return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
                }
            }
        }
        // 获取 Space 对象
        Space space = spaceDAO.getById(spaceId);
        ThrowUtils.throwIf(Objects.isNull(space), ErrorCode.NOT_FOUND_ERROR, "未找到空间信息");
        // 根据 Space 类型判断权限
        if (space.getSpaceType() == SpaceTypeEnum.PRIVATE.getValue()) {
            // 私有空间，仅本人或管理员有权限
            if (space.getUserId().equals(userId) || userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            } else {
                return new ArrayList<>();
            }
        } else {
            // 团队空间，查询 SpaceUser 并获取角色和权限
            spaceUser = spaceUserDAO.getBySpaceUserId(spaceId, userId);
            if (spaceUser == null) {
                return new ArrayList<>();
            }
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
    }

    /**
     * 判断对象的所有字段是否为空
     */
    private boolean isAllFieldsNull(Object object) {
        if (object == null) {
            // 对象本身为空
            return true;
        }
        // 获取所有字段并判断是否所有字段都为空
        return Arrays.stream(ReflectUtil.getFields(object.getClass()))
                // 获取字段值
                .map(field -> ReflectUtil.getFieldValue(object, field))
                // 检查是否所有字段都为空
                .allMatch(ObjectUtil::isEmpty);
    }
}

