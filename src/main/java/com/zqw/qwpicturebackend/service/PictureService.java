package com.zqw.qwpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zqw.qwpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.zqw.qwpicturebackend.model.dto.file.UploadPictureResult;
import com.zqw.qwpicturebackend.model.dto.picture.*;
import com.zqw.qwpicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zqw.qwpicturebackend.model.entity.User;
import com.zqw.qwpicturebackend.model.vo.PictureVO;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author zhuqw
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-10-25 22:45:47
 */
public interface PictureService extends IService<Picture> {

    /**
     * 用户上传图片
     *
     * @param inputResource        图片信息来源
     * @param pictureUploadRequest 图片id信息
     * @param loginUser            用户信息
     * @return 返回上传后的图片信息
     */
    PictureVO uploadPicture(Object inputResource, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 获取图片查询器
     *
     * @param pictureQueryRequest 查询器
     * @return 使用查询条件拼接成的查询器
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);


    /**
     * 图片转化为脱敏后图片信息
     *
     * @param picture 图片信息
     * @param request 用于获取用户信息
     * @return 返回脱敏后的图片信息
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);


    /**
     * 将分页图片信息转化为脱敏后的图片信息
     *
     * @param picturePage 未脱敏的图片信息
     * @param request     用于获取用户信息
     * @return 返回脱敏后的图片信息
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 校验参数
     *
     * @param picture 需要校验的 picture 对象
     */
    void validPicture(Picture picture);


    /**
     * 管理员进行图片信息校验
     *
     * @param pictureReviewRequest 待校验的图片
     * @param loginUser            当前是那个管理员，因为需要记录审核人id，我们需要将id存储
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 根据用户身份设置图片审核状态
     *
     * @param picture   待审核图片信息
     * @param loginUser 当前进行操作的用户/管理员
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 批量上传文件
     *
     * @param pictureUploadByBatchRequest 请求参数包括（搜索词，图片统一名称，每次爬取数据条数）
     * @param loginUser                   当前用户信息，在插入数据库时需要指定创建人信息
     * @return 成功插入记录数
     */
    int uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    /**
     * 将删除图片代码抽离到serviceimpl中
     *
     * @param id
     * @param loginUser
     */
    void deletePicture(Long id, User loginUser);

    /**
     * 用户编辑空间
     *
     * @param pictureEditRequest 用户编辑图片
     * @param loginUser          用户信息
     */
    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    /**
     * 删除 Cos中的图片信息
     *
     * @param oldPicture 需要删除的图片信息
     */
    void clearPictureFile(Picture oldPicture);


    /**
     * 校验用户是否能够操作图片（查询，修改，删除）
     *
     * @param picture   图片信息
     * @param loginUser 用户信息
     */
    void checkPictureAuth(Picture picture, User loginUser);

    /**
     * 根据上传的图片主色调查询相关联的图片
     *
     * @param spaceId 查询的依据数据就是空间id
     * @param picColor 带查询的颜色
     * @param loginUser 需要进行用户身份校验
     * @return 返回查询到的图片信息
     */
    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);

    /**
     * 扩图接口
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     * @return
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);
}
