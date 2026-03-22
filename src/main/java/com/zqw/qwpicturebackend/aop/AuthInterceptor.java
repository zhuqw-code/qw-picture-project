package com.zqw.qwpicturebackend.aop;

import com.zqw.qwpicturebackend.annotation.AuthCheck;
import com.zqw.qwpicturebackend.exception.BusinessException;
import com.zqw.qwpicturebackend.exception.ErrorCode;
import com.zqw.qwpicturebackend.model.entity.User;
import com.zqw.qwpicturebackend.model.enums.UserRoleEnum;
import com.zqw.qwpicturebackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect  // 将当前类定义为切面类
@Component // 交给 Spring 管理
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 通知 = 切点 + 增强
     *
     * @param joinPoint 可以获取到标记注解的方法
     * @param authCheck 可以获取到注解上绑定的参数
     * @return 返回
     */
    @Around("@annotation(authCheck)")            // 在那个方法上添加的注解
    public Object doInterception(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 获取到注解上的信息
        String mustRole = authCheck.mustRole();
        // 我们需要将注解上配置的 mustRole（已获得） 与 当前用户信息（可以获得）进行判断
        // 通过全局上下文可以获取到当前请求的所有内容
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // HttpServletRequest request = (HttpServletRequest) requestAttributes;
        // 获取用户信息
        User user = userService.getLoginUser(request);
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);

        // 权限校验  【不需要权限的放行，没有权限的过滤，再单独判断用户权限访问管理员权限】
        // 不需要权限
        if (mustRoleEnum == null) {
            return joinPoint.proceed();    // 返回出去？？？
        }
        // 需要权限校验
        // todo 这里可以在枚举类中给每个角色添加各自的权重 weight 在进行权限校验时直接判断是否大于当前权限权重
        // 根据用户角色获取用户权限枚举值
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(user.getUserRole());
        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "你没有权限~");
        }
        // 最棘手的就是用户访问管理员权限 【访问管理员页面但是我是用户】
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "你没有权限~");
        }
        return joinPoint.proceed();
    }
}
