package com.zqw.qwpicturebackend.controller;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zqw.qwpicturebackend.annotation.AuthCheck;
import com.zqw.qwpicturebackend.common.BaseResult;
import com.zqw.qwpicturebackend.common.DeleteRequest;
import com.zqw.qwpicturebackend.common.ResultUtils;
import com.zqw.qwpicturebackend.constant.UserConstant;
import com.zqw.qwpicturebackend.exception.BusinessException;
import com.zqw.qwpicturebackend.exception.ErrorCode;
import com.zqw.qwpicturebackend.exception.ThrowUtils;
import com.zqw.qwpicturebackend.model.dto.user.*;
import com.zqw.qwpicturebackend.model.entity.User;
import com.zqw.qwpicturebackend.model.vo.LoginUserVO;
import com.zqw.qwpicturebackend.model.vo.UserVO;
import com.zqw.qwpicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册页面
     *
     * @param userRegisterRequest 用户注册信息
     * @return 返回注册成功后的用户 id
     */
    @PostMapping("/register")
    public BaseResult<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (ObjUtil.hasNull(userRegisterRequest)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码不能为空");
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long id = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(id, "注册成功~");
    }

    /**
     * 用户登录接口
     *
     * @param userLoginRequest 用户登录信息
     * @param request          请求，用于获取当前会话
     * @return 返回用户脱敏后的信息
     */
    @PostMapping("/login")
    public BaseResult<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        // 请求信息校验
        ThrowUtils.throwif(userLoginRequest == null, ErrorCode.PARAMS_ERROR, "登录失败，请重新登录~");
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO, "登录成功~");
    }

    /**
     * 获取当前用户信息
     *
     * @param request HttpServletRequest 请求
     * @return 返回当前脱敏后用户
     */
    @GetMapping("/get/current")
    public BaseResult<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        LoginUserVO loginUserVO = userService.getLoginUserVO(loginUser);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 用户退出登录
     *
     * @param request HttpServletRequest
     * @return 返回是否退出成功
     */
    @PostMapping("/logout")
    public BaseResult<Boolean> userLogout(HttpServletRequest request) {
        Boolean isLogout = userService.userLogout(request);
        return ResultUtils.success(isLogout, "已安全退出~");
    }


    /**
     * 管理员添加用户（走后门）
     *
     * @param userAddRequest 添加用户信息
     * @return 新增用户id
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResult<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwif(userAddRequest == null, ErrorCode.PARAMS_ERROR, "请正确输入新增用户信息~");
        // 将用户信息转化为 User 再添加
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        // 将密码进行加密后再存储
        final String defaultPassword = "12345678";
        String encryptPassword = userService.getEncryptPassword(defaultPassword);
        user.setUserPassword(encryptPassword);
        // 保存到数据库
        boolean isSave = userService.save(user);
        ThrowUtils.throwif(!isSave, ErrorCode.OPERATION_ERROR, "新增用户失败，请联系程序员debug~");
        return ResultUtils.success(user.getId());
    }

    /**
     * 管理员删除用户信息（走后门）
     *
     * @param deleteRequest 只有删除的id
     * @return 返回是否删除成功
     */
    @DeleteMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResult<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwif(deleteRequest == null, ErrorCode.PARAMS_ERROR, "没有找到要删除的用户呢~");
        Long id = deleteRequest.getId();
        boolean isDelete = userService.removeById(id);
        ThrowUtils.throwif(!isDelete, ErrorCode.OPERATION_ERROR, "删除失败，请联系程序员debug~");
        return ResultUtils.success(isDelete);
    }

    /**
     * 管理员修改用户信息（走后门）
     *
     * @param userUpdateRequest 用户新信息
     * @return 返回注册成功后的用户 id
     */
    @PutMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResult<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (ObjUtil.hasNull(userUpdateRequest) || userUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码不能为空");
        }
        // 修改用户信息
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        // 修改
        boolean isUpdate = userService.updateById(user);
        ThrowUtils.throwif(!isUpdate, ErrorCode.OPERATION_ERROR, "无法进行修改，请联系程序员debug~");
        return ResultUtils.success(isUpdate);
    }


    /**
     * 管理员获取用户信息（走后门）
     *
     * @param id 查询id
     * @return 返回用户所有信息
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResult<User> getUserById(long id) {
        ThrowUtils.throwif(id <= 0, ErrorCode.PARAMS_ERROR, "没有获取到有效id~");
        User user = userService.getById(id);
        ThrowUtils.throwif(user == null, ErrorCode.NOT_FOUND_ERROR, "没有指定id的信息~");
        return ResultUtils.success(user);
    }

    /**
     * 管理员获取用户列表（走后门）
     *
     * @param userQueryRequest 查询参数
     * @return 返回脱敏后的用户分页
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResult<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        // todo 查询参数可以为 null 啊
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        // 开始通过查询器 + 页面直接获取到页面信息，但是没有脱敏，我们就拿出页面信息（record）脱敏后再放进去
        // 首先创建分页器
        Page<User> userPage = userService.page(new Page<>(current, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        // 脱敏
        // userPage的getRecord获取的就是List集合
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }


    /**
     * 用户获取信息
     *
     * @param id 查询id
     * @return 返回脱敏用户
     */
    @GetMapping("/get/vo")
    public BaseResult<UserVO> getUserVOById(long id) {
        ThrowUtils.throwif(id <= 0, ErrorCode.PARAMS_ERROR, "没有获取到有效id~");
        User user = userService.getById(id);
        ThrowUtils.throwif(user == null, ErrorCode.NOT_FOUND_ERROR, "没有指定id的信息~");
        return ResultUtils.success(userService.getUserVO(user));
    }
}
