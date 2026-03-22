package com.zqw.qwpicturebackend.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zqw.qwpicturebackend.exception.BusinessException;
import com.zqw.qwpicturebackend.exception.ErrorCode;
import com.zqw.qwpicturebackend.exception.ThrowUtils;
import com.zqw.qwpicturebackend.model.dto.user.UserQueryRequest;
import com.zqw.qwpicturebackend.model.entity.User;
import com.zqw.qwpicturebackend.model.enums.UserRoleEnum;
import com.zqw.qwpicturebackend.model.vo.LoginUserVO;
import com.zqw.qwpicturebackend.model.vo.UserVO;
import com.zqw.qwpicturebackend.service.UserService;
import com.zqw.qwpicturebackend.mapper.UserMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.zqw.qwpicturebackend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author zhuqw
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-10-24 11:58:01
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    /**
     * 用户注册接口
     *
     * @param userAccount   用户名
     * @param userPassword  密码
     * @param checkPassword 确认密码
     * @return 插入成功后这条记录在数据库中的记录
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 请求参数校验
        if (ObjUtil.hasNull(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码不能为空~");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不合法~");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码非法~");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "两次密码要相同哦~");
        }
        // 用户名是否可用
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount", userAccount);
        Long count = this.baseMapper.selectCount(userQueryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "账号已存在~");
        }
        // 密码加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 插入数据库
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("鞠婧祎yyds");

        boolean save = this.save(user);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "无法正确插入信息~");
        }
        return user.getId();
    }


    /**
     * 用户登录接口
     *
     * @param userAccount  账号
     * @param userPassword 密码
     * @param request      本次请求 【用于获取本次会话 session 存储用户状态】
     * @return 登录成功后脱敏的用户信息
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 对用户信息进行验空
        if (ObjUtil.hasNull(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码不能为空~");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不合法~");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码非法~");
        }
        // 密码进行加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 查询数据库
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount", userAccount);
        userQueryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(userQueryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户信息不存在或账号密码错误~");
        }
        // 用户信息存放到 Session 中
        HttpSession session = request.getSession();
        session.setAttribute(USER_LOGIN_STATE, user);
        // 返回脱敏后的用户信息
        return getLoginUserVO(user);
    }


    /**
     * 对于用户输入的密码进行加密，管理员也无权知道
     *
     * @param userPassword 用户明文密码
     * @return 用户加密后的密码
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        // 颜值（盐值）
        final String SALT = "zqw";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    /**
     * 获取脱敏后的用户信心
     *
     * @param user 完整用户信息
     * @return 脱敏后的用户信息 VO对象
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        LoginUserVO loginUserVO = new LoginUserVO();
        // 属性拷贝(值拷贝：将target的地址拷贝一份给到copyProperties，对target指向的地址内容进行改变)
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 获取用户信息【对内暴露，不会对外共享】
     *
     * @param request HttpServletRequest对象
     * @return 用户信息
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // request 一定不为空
        // ThrowUtils.throwif(request == null, ErrorCode.OPERATION_ERROR, "请求为空");
        // 判断用户状态是否存在
        Object attr = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) attr;
        ThrowUtils.throwif(user == null, ErrorCode.NOT_LOGIN_ERROR, "您还未登录，请先登录~");
        // 但是考虑到用户可能登录后就进行了一些操作，修改了一些信息，如果只是一味从缓存中读取信息，可能造成数据时效性
        User latestUser = this.baseMapper.selectById(user.getId());
        ThrowUtils.throwif(latestUser == null, ErrorCode.NOT_LOGIN_ERROR, "您可能还未登录，请先登录~");
        return latestUser;
    }

    /**
     * 用户退出
     *
     * @param request HttpServletRequest
     * @return 返回是否正确退出
     */
    @Override
    public Boolean userLogout(HttpServletRequest request) {
        // 首先判断用户是否登录，只有登录后的用户才能退出登录
        Object attribute = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) attribute;
        ThrowUtils.throwif(user == null, ErrorCode.NOT_LOGIN_ERROR, "登录后才能退出哦~");

        // 校验通过后直接删除用户状态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    /**
     * 对用户信息进行脱敏
     *
     * @param user 原始数据
     * @return 脱敏后的用户数据
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 获取多个脱敏后的用户信息
     *
     * @param userList 原始用户列表
     * @return 脱敏后的用户列表
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        // serviceImpl 内部也要进行校验，防止内部相互调用出错
        if (userList == null) {
            return new ArrayList<>();
        }
        List<UserVO> userVOList = userList.stream()
                // .map(user -> this.getUserVO(user))
                .map(this::getUserVO)
                .collect(Collectors.toList());
        return userVOList;
    }

    /**
     * 根据传递的用户信息拼接一个查询器
     *
     * @param userQueryRequest 查询条件【为空就是查询查询所哟信息】
     * @return 返回一个查询器
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        // 分页才能使用到
        // int current = userQueryRequest.getCurrent();
        // int pageSize = userQueryRequest.getPageSize();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();

        // 判断逻辑
        userQueryWrapper.eq(Objects.nonNull(id), "id", id);
        userQueryWrapper.eq(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        userQueryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        userQueryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        userQueryWrapper.like(StrUtil.isNotBlank(userRole), "userRole", userRole);
        userQueryWrapper.orderBy(StrUtil.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);
        return userQueryWrapper;
    }

    /**
     * 判断是否为管理员
     *
     * @param user 登录信息
     * @return 返回是否为管理员
     */
    @Override
    public Boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }
}

