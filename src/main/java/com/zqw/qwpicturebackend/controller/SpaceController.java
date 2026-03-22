package com.zqw.qwpicturebackend.controller;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zqw.qwpicturebackend.annotation.AuthCheck;
import com.zqw.qwpicturebackend.common.BaseResult;
import com.zqw.qwpicturebackend.common.DeleteRequest;
import com.zqw.qwpicturebackend.common.ResultUtils;
import com.zqw.qwpicturebackend.constant.UserConstant;
import com.zqw.qwpicturebackend.exception.BusinessException;
import com.zqw.qwpicturebackend.exception.ErrorCode;
import com.zqw.qwpicturebackend.exception.ThrowUtils;
import com.zqw.qwpicturebackend.model.dto.space.SpaceAddRequest;
import com.zqw.qwpicturebackend.model.dto.space.SpaceEditRequest;
import com.zqw.qwpicturebackend.model.dto.space.SpaceQueryRequest;
import com.zqw.qwpicturebackend.model.dto.space.SpaceUpdateRequest;
import com.zqw.qwpicturebackend.model.entity.Space;
import com.zqw.qwpicturebackend.model.entity.User;
import com.zqw.qwpicturebackend.model.vo.SpaceVO;
import com.zqw.qwpicturebackend.service.SpaceService;
import com.zqw.qwpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/space")
@Slf4j
public class SpaceController {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    /**
     * 删除空间【管理员+空间归属者】
     *
     * @param deleteRequest 删除信息，包含删除id
     * @param request       用于获取当前用户，以校权限
     * @return 返回删除是否成功
     */
    @DeleteMapping("/delete")
    public BaseResult<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest,
                                           HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间信息不合法~");
        }
        // 判断空间是否存在
        Long picId = deleteRequest.getId();
        Space space = spaceService.getById(picId);
        ThrowUtils.throwif(space == null, ErrorCode.OPERATION_ERROR, "空间信息不存在~");
        // 只有本用户和管理员才能删除空间
        User loginUser = userService.getLoginUser(request);
        if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "你没有删除该空间的权力~");
        }
        boolean isDelete = spaceService.removeById(picId);
        ThrowUtils.throwif(!isDelete, ErrorCode.OPERATION_ERROR, "无法删除该空间~");
        return ResultUtils.success(true);
    }

    /**
     * 更新空间信息【管理员】（走后门）
     *
     * @param spaceUpdateRequest 需要修改的信息
     * @param request            通过切面编程实现了权限校验为什么还要获取到管理员信息呢？因为我们需要给空间设置审核人信息
     * @return 是否成功修改
     */
    @PutMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResult<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest,
                                           HttpServletRequest request) {
        // 校验参数
        if (spaceUpdateRequest == null || spaceUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "修改参数不能为空~");
        }
        // 判断空间信息是否存在
        Long picId = spaceUpdateRequest.getId();
        Space oldSpace = spaceService.getById(picId);
        ThrowUtils.throwif(oldSpace == null, ErrorCode.NOT_FOUND_ERROR, "无法找到原空间信息~");

        // 空间类型转换 dto  ->  entity
        Space space = new Space();
        BeanUtils.copyProperties(spaceUpdateRequest, space);
        // 数据校验
        spaceService.validSpace(space, false);
        // 自动填充空间状态
        spaceService.fillSpaceBySpaceLevel(space);

        // 修改数据库
        boolean isUpdate = spaceService.updateById(space);
        ThrowUtils.throwif(!isUpdate, ErrorCode.OPERATION_ERROR, "修改空间信息失败~");
        return ResultUtils.success(true);
    }

    /**
     * 编辑空间信息
     *
     * @param spaceEditRequest 需要修改的信息
     * @return 是否成功修改
     */
    @PutMapping("/edit")
    public BaseResult<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest,
                                         HttpServletRequest request) {
        // 校验参数
        if (spaceEditRequest == null || spaceEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "修改参数不能为空~");
        }
        // 判断空间信息是否存在
        Long picId = spaceEditRequest.getId();
        Space oldSpace = spaceService.getById(picId);
        ThrowUtils.throwif(oldSpace == null, ErrorCode.NOT_FOUND_ERROR, "无法找到原空间信息~");

        // 空间类型转换 dto  ->  entity
        Space space = new Space();
        BeanUtils.copyProperties(spaceEditRequest, space);
        // 需要重新编辑修改日期
        space.setUpdateTime(new Date());


        // 数据校验
        spaceService.validSpace(space, false);
        // 设置空间的审核状态
        spaceService.fillSpaceBySpaceLevel(space);

        // 判断是空间归属者还是管理员
        // space 属性拷贝后没有用户id，需要用含有id的对象来判断
        User loginUser = userService.getLoginUser(request);
        if (!oldSpace.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "你不能删除别人的空间或者你不是管理员~");
        }
        // 修改数据库
        boolean isUpdate = spaceService.updateById(space);
        ThrowUtils.throwif(!isUpdate, ErrorCode.OPERATION_ERROR, "修改空间信息失败~");
        return ResultUtils.success(true);
    }

    /**
     * 获取空间信息【管理员】（走后门）
     *
     * @param id 查询空间的id
     * @return 查询后的空间完整信息
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    BaseResult<Space> getSpaceById(Long id) {
        // 空间校验
        ThrowUtils.throwif(id == null || id < 0, ErrorCode.PARAMS_ERROR, "查询空间Id不存在~");
        // 查看数据库是否存在
        Space space = spaceService.getById(id);
        ThrowUtils.throwif(space == null, ErrorCode.NOT_FOUND_ERROR, "查询信息不存在~");
        // todo 是否需要将String 类型转化为List<String> 类型
        // 不转化了，因为我们查询到的数据和返回的数据都是统一类型的
        return ResultUtils.success(space);
    }

    /**
     * 获取空间信息
     *
     * @param id      查询空间的id
     * @param request 用于获取到用户数据进行权限校验
     * @return 查询后的空间完整信息
     * todo 后续开发用户自已空间后能够将状态不合法的空间只供用户查看，这里就进行状态校验了
     */
    @GetMapping("/get/vo")
    BaseResult<SpaceVO> getSpaceVOById(Long id, HttpServletRequest request) {
        // 空间校验
        ThrowUtils.throwif(id == null || id < 0, ErrorCode.PARAMS_ERROR, "查询空间Id不存在~");
        // 查看数据库是否存在
        Space space = spaceService.getById(id);
        ThrowUtils.throwif(space == null, ErrorCode.NOT_FOUND_ERROR, "查询信息不存在~");

        // 是否为管理员或本空间创始人
        User loginUser = userService.getLoginUser(request);
        // if (!space.getId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)){
        //     throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "你没有查询该空间的权限~");
        // }

        // 获取脱敏后的用户信息
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 添加冗余属性
        spaceVO.setUser(userService.getUserVO(loginUser));
        return ResultUtils.success(spaceVO);
    }

    /**
     * 根据传入的查询信息获取到列表信息【管理员】（走后门）
     *
     * @param spaceQueryRequest 查询参数
     * @return 返回符合的page信息
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    BaseResult<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        // 分页查询不需要参数
        int current = spaceQueryRequest.getCurrent();
        int pageSize = spaceQueryRequest.getPageSize();
        // 查询数据库
        Page<Space> spacePage = spaceService.page(new Page<>(current, pageSize),
                spaceService.getQueryWrapper(spaceQueryRequest));
        return ResultUtils.success(spacePage);
    }

    /**
     * 用户获取分页参数
     *
     * @param spaceQueryRequest 查询参数
     * @param request           用于校验用户查询到的空间是否为自己的
     * @return 脱敏后的本用户的所有空间page
     */
    @PostMapping("/list/page/vo")
    BaseResult<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequest spaceQueryRequest,
                                                HttpServletRequest request) {
        int current = spaceQueryRequest.getCurrent();
        int pageSize = spaceQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwif(pageSize > 20, ErrorCode.OPERATION_ERROR, "小子想爬老子网站~");

        Page<Space> spacePage = spaceService.page(new Page<>(current, pageSize),
                spaceService.getQueryWrapper(spaceQueryRequest));
        return ResultUtils.success(spaceService.getSpaceVOPage(spacePage, request));
    }

    /**
     * 创建空间接口
     *
     * @param spaceAddRequest 空间参数包括 空间名称， 空间等级
     * @param request         用来获取用户判断用户权限
     * @return 返回创成功的空间id
     */
    @PostMapping("/add")
    BaseResult<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        ThrowUtils.throwif(ObjUtil.isEmpty(spaceAddRequest), ErrorCode.PARAMS_ERROR, "参数不能为空");
        User loginUser = userService.getLoginUser(request);
        long spaceId = spaceService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(spaceId);
    }
}
