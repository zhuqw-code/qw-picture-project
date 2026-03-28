package com.zqw.qwpicturebackend.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.zqw.qwpicturebackend.auth.constant.SpaceUserPermissionConstant;
import com.zqw.qwpicturebackend.exception.BusinessException;
import com.zqw.qwpicturebackend.exception.ErrorCode;
import com.zqw.qwpicturebackend.exception.ThrowUtils;
import com.zqw.qwpicturebackend.model.entity.Picture;
import com.zqw.qwpicturebackend.model.entity.Space;
import com.zqw.qwpicturebackend.model.entity.SpaceUser;
import com.zqw.qwpicturebackend.model.entity.User;
import com.zqw.qwpicturebackend.model.enums.SpaceRoleEnum;
import com.zqw.qwpicturebackend.model.enums.SpaceTypeEnum;
import com.zqw.qwpicturebackend.model.enums.UserRoleEnum;
import com.zqw.qwpicturebackend.service.PictureService;
import com.zqw.qwpicturebackend.service.SpaceService;
import com.zqw.qwpicturebackend.service.SpaceUserService;
import com.zqw.qwpicturebackend.service.UserService;
import io.netty.util.internal.StringUtil;
import org.apache.http.protocol.HTTP;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.zqw.qwpicturebackend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 自定义权限加载接口实现类
 */
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    /**
     * 返回一个账号所拥有的权限码集合 
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 1. 如果当前类型不属于本校验逻辑，就直接返回空权限
        if (!StpKit.SPACE_TYPE.equals(loginType)) {
            return new ArrayList<>();
        }
        // 2. 如果当前是管理员将所有权限都返回
        List<String> adminPermission = spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 3. 获取上下文对象，SpaceUserAuthContext == null, 直接默认是公共图库，根据当前用户角色返回对应权限
        SpaceUserAuthContext spaceUserAuthContext = this.getSpaceUserAuthContext();
        // 如果都为 null，就认为查询公共空间
        if (isAllFieldsNull(spaceUserAuthContext)) {
            return adminPermission;
        }
        // 获取 userId 后续会经常使用
        User loginUser = (User) StpKit.SPACE.getSession().get(USER_LOGIN_STATE);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }
        Long userId = loginUser.getId();
        // 4. 如果能够从 SpaceUserAuthContext中获取到 spaceUser对象，判空成功 直接查表返回对应权限
        SpaceUser spaceUser = spaceUserAuthContext.getSpaceUser();
        if (ObjectUtils.isNotEmpty(spaceUser)) {
            // todo 是否需要判断是否存在
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
        // 5. 通过spaceUserId 尝试获取 spaceUser
        Long spaceUserId = spaceUserAuthContext.getSpaceUserId();
        if (spaceUserId != null) {
            spaceUser = spaceUserService.getById(spaceUserId);
            if (spaceUser == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "没有要查询的空间用户");
            }
            // 如果找到就返回权限，防止用户使用其他钥匙进入其他的团队空间
            SpaceUser loginSpaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceUser.getSpaceId())
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            // todo 但是这里有问题啊？ 管理员不可能是其他空间的人员，查出来一定是 null
            if (loginSpaceUser == null) {
                return new ArrayList<>();
            }
            // 防止用户拥有其他团队空间角色，用到别的空间上
            // 为什么这里对管理员访问私有空间有问题
            return spaceUserAuthManager.getPermissionsByRole(loginSpaceUser.getSpaceRole());
        }
        // 6. 如果 spaceUserId == null 了想办法获取 spaceUser -> spaceId -> userId 通过这两个字段获取，如果有就返回权限，否则继续向下
        Long spaceId = spaceUserAuthContext.getSpaceId();
        if (spaceId == null) {   // 公共空间
            // 没有spaceId就用 pictureId进行查找
            Long pictureId = spaceUserAuthContext.getPictureId();
            if (pictureId == null) {
                return adminPermission;
            }
            // 查一下
            // 7. 如果通过6还是没有获取到spaceUser对象我们还可以通过当前 pictureId -> spaceId + userId 就能继续获取到spaceUser
            Picture picture = pictureService.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .select(Picture::getId, Picture::getSpaceId, Picture::getUserId)
                    .one();
            if (picture == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "没有找到对应图片资源");
            }
            // 这里如果picture能够将spaceId携带回来我们还是能够判断当前用户是否有spaceId的权限【如果能够拿到直接跳出内层判断，交给外层去拿权限】
            spaceId = picture.getSpaceId();

            // 没有空间才当成公共空间操作
            if (spaceId == null) {
                // 公共空间，仅管理员和本人能够操作
                if (picture.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)) {
                    return adminPermission;
                } else {
                    // 公共空间图片，能够让非管理员和非本人用户查看
                    return spaceUserAuthManager.getPermissionsByRole(SpaceUserPermissionConstant.PICTURE_VIEW);
                }
            }
            // 如果spaceId非空我们在外面判断
        }
        // 8. 根据获取的space返回对应权限【私有空间 / 团队空间】
        Space space = spaceService.getById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "没有找到对应空间信息");
        }
        // 根据空间获取用户空间信息
        if (space.getSpaceType().equals(SpaceTypeEnum.PRIVATE.getValue())) {
            // 私有空间
            if (space.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)) {
                return adminPermission;
            } else {
                // 公共空间图片，能够让非管理员和非本人用户查看
                return spaceUserAuthManager.getPermissionsByRole(SpaceUserPermissionConstant.PICTURE_VIEW);
            }
        } else {
            // 团队空间
            spaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            if (spaceUser == null) {
                return new ArrayList<>();
            }
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 本 list 仅做模拟，实际项目中要根据具体业务逻辑来查询角色
        List<String> list = new ArrayList<String>();    
        list.add("admin");
        list.add("super-admin");
        return list;
    }

    @Value("${server.servlet.context-path}")
    private String contextPath;


    /**
     * 通过反射获取SpaceUserAuthContext，并判断是否所有字段都为null，都为null标识用户没有任何恶意，就放行
     * @param object
     * @return
     */
    private boolean isAllFieldsNull(Object object) {
        if (object == null) return true;
        return Arrays.stream(ReflectUtil.getFields(object.getClass()))
                .map(field -> ReflectUtil.getFieldValue(object, field)) // 获取字段值
                .allMatch(ObjectUtils::isEmpty);
    }


    /**
     * 从请求中获取上下文对象，之后会被用在 getPermissionList 和 getRoleList 方法中使用我们重 request中获取的id等参数
     */
    private SpaceUserAuthContext getSpaceUserAuthContext() {
        // 1. 获取到 HttpServletRequest 对象
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        // 2. 对不同的请求类型使用不同的解析逻辑 (get / post)
        String headerType = request.getHeader(Header.CONTENT_TYPE.getValue());
        // JSON("application/json")
        SpaceUserAuthContext userAuthContext;
        if (ContentType.JSON.getValue().equals(headerType)) {
            // 如果是json类型数据，就取requestBody
            String body = ServletUtil.getBody(request);
            userAuthContext = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        } else {
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            userAuthContext = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }
        // 3. 从 request 中获取的 URI， 对 URI 进行业务字段裁剪，设置到对应的 SpaceUserAuthContext 的字段中
        Long id = userAuthContext.getId();
        if (ObjectUtils.isNotNull(id)) {
            String requestURI = request.getRequestURI();
            String URI = requestURI.replaceFirst(contextPath + "/", "");
            String modelName = StringUtil.substringBefore(URI, '/');
            switch (modelName) {
                case "picture" -> {
                    userAuthContext.setPictureId(id);
                }
                case "spaceUser" -> {
                    userAuthContext.setSpaceUserId(id);
                }
                case "space" -> {
                    userAuthContext.setSpaceId(id);
                }
                default -> {
                }
            }
        }
        // 4. 返回 SpaceUserAuthContext 对象
        return userAuthContext;
    }

}
