package com.zqw.qwpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zqw.qwpicturebackend.model.dto.space.SpaceAddRequest;
import com.zqw.qwpicturebackend.model.dto.space.SpaceQueryRequest;
import com.zqw.qwpicturebackend.model.entity.Space;
import com.zqw.qwpicturebackend.model.entity.User;
import com.zqw.qwpicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
 * @author zhuqw
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2025-11-06 20:29:25
 */
public interface SpaceService extends IService<Space> {
    /**
     * 校验参数
     *
     * @param space 需要校验的 space 对象
     * @param isAdd 用来标识当前是新增还是修改（因为新增对名称和等级都要判空，但是编辑反而不需要）
     */
    void validSpace(Space space, boolean isAdd);

    /**
     * 空间转化为脱敏后空间信息
     *
     * @param space   空间信息
     * @param request 用于获取用户信息
     * @return 返回脱敏后的空间信息
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);


    /**
     * 将分页空间信息转化为脱敏后的空间信息
     *
     * @param spacePage 未脱敏的空间信息
     * @param request   用于获取用户信息
     * @return 返回脱敏后的空间信息
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 获取空间查询器
     *
     * @param spaceQueryRequest 查询器
     * @return 使用查询条件拼接成的查询器
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);


    /**
     * 封装根据 spaceLevel 填充 Space最大内存，最大条数
     *
     * @param space 需要进行参数设置的空间
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 用户创建空间
     *
     * @param spaceAddRequest 空间信息
     * @param loginUser       需要根据当前用户判断是否哦有权限进行添加空间操作
     * @return 返回上传成功的id ， 如果返回-1代表失败
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 根据id删除空间
     *
     * @param space     需要删除的空间
     * @param loginUser 当前登录用户
     */
    void checkSpaceAuth(Space space, User loginUser);
}
