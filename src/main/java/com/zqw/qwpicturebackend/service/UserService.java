package com.zqw.qwpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zqw.qwpicturebackend.common.BaseResult;
import com.zqw.qwpicturebackend.model.dto.user.UserQueryRequest;
import com.zqw.qwpicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zqw.qwpicturebackend.model.vo.LoginUserVO;
import com.zqw.qwpicturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author zhuqw
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2025-10-24 11:58:01
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册接口
     *
     * @param userAccount   账号
     * @param userPassword  密码
     * @param checkPassword 确认密码
     * @return 插入成功后这条记录在数据库中的记录
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);


    /**
     * 对于用户输入的密码进行加密，管理员也无权知道
     *
     * @param userPassword 用户明文密码
     * @return 用户加密后的密码
     * 为什么要封装成一个方法？因为登录也要使用
     */
    String getEncryptPassword(String userPassword);

    /**
     * 用户登录接口
     *
     * @param userAccount  账号
     * @param userPassword 密码
     * @return 登录成功后脱敏的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);


    /**
     * 获取脱敏后的用户信心
     *
     * @param user 完整用户信息
     * @return 脱敏后的用户信息 VO对象
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取用户信息【对内暴露，不会对外共享】
     *
     * @param request HttpServletRequest对象
     * @return 用户信息
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户退出
     *
     * @param request HttpServletRequest
     * @return 返回是否正确退出
     */
    Boolean userLogout(HttpServletRequest request);

    /**
     * 对用户信息进行脱敏
     *
     * @param user 原始数据
     * @return 脱敏后的用户数据
     */
    UserVO getUserVO(User user);

    /**
     * 获取多个脱敏后的用户信息
     *
     * @param userList 原始用户列表
     * @return 脱敏后的用户列表
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 根据传递的用户信息拼接一个查询器
     *
     * @param userQueryRequest 查询条件
     * @return 返回一个查询器
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 判断当前用户角色方便后续操作
     *
     * @param user 登录者信息
     * @return 返回是否为管理员
     */
    Boolean isAdmin(User user);
}
