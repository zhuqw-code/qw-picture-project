package com.zqw.qwpicturebackend.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zqw.qwpicturebackend.annotation.AuthCheck;
import com.zqw.qwpicturebackend.api.aliyunai.api.AliYunAiApi;
import com.zqw.qwpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.zqw.qwpicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.zqw.qwpicturebackend.api.imagsearch.ImageSearchApiFacade;
import com.zqw.qwpicturebackend.api.imagsearch.model.ImageSearchResult;
import com.zqw.qwpicturebackend.api.imagsearch.picture.SearchPictureByPictureRequest;
import com.zqw.qwpicturebackend.auth.SpaceUserAuthManager;
import com.zqw.qwpicturebackend.auth.StpKit;
import com.zqw.qwpicturebackend.auth.annotation.SaSpaceCheckPermission;
import com.zqw.qwpicturebackend.auth.constant.SpaceUserPermissionConstant;
import com.zqw.qwpicturebackend.common.BaseResult;
import com.zqw.qwpicturebackend.common.DeleteRequest;
import com.zqw.qwpicturebackend.common.ResultUtils;
import com.zqw.qwpicturebackend.constant.UserConstant;
import com.zqw.qwpicturebackend.exception.BusinessException;
import com.zqw.qwpicturebackend.exception.ErrorCode;
import com.zqw.qwpicturebackend.exception.ThrowUtils;
import com.zqw.qwpicturebackend.model.dto.picture.*;
import com.zqw.qwpicturebackend.model.dto.space.SpaceLevel;
import com.zqw.qwpicturebackend.model.entity.Picture;
import com.zqw.qwpicturebackend.model.entity.Space;
import com.zqw.qwpicturebackend.model.entity.SpaceLevelEnum;
import com.zqw.qwpicturebackend.model.entity.User;
import com.zqw.qwpicturebackend.model.enums.PictureReviewStatusEnum;
import com.zqw.qwpicturebackend.model.vo.PictureTagCategory;
import com.zqw.qwpicturebackend.model.vo.PictureVO;
import com.zqw.qwpicturebackend.service.PictureService;
import com.zqw.qwpicturebackend.service.SpaceService;
import com.zqw.qwpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/picture")
@Slf4j
public class PictureController {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private AliYunAiApi aliYunAiApi;
    @Autowired
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 上传图片
     *
     * @param multipartFile        图片信息
     * @param pictureUploadRequest 仅仅包含id，有id是修改，没有就是添加
     * @param request              用于进行权限校验
     * @return 返回上传后脱敏的图片信息
     */
    @PostMapping("/upload")
    @SaSpaceCheckPermission(SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResult<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 上传图片
     *
     * @param pictureUploadRequest 仅仅包含id，有id是修改，没有就是添加
     * @param request              用于进行权限校验
     * @return 返回上传后脱敏的图片信息
     */
    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResult<PictureVO> uploadPictureByUrl(
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片【管理员+图片归属者】
     *
     * @param deleteRequest 删除信息，包含删除id
     * @param request       用于获取当前用户，以校权限
     * @return 返回删除是否成功
     */
    @DeleteMapping("/delete")
    @SaSpaceCheckPermission(SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResult<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest,
                                             HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片信息不合法~");
        }
        User loginUser = userService.getLoginUser(request);
        pictureService.deletePicture(deleteRequest.getId(), loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 更新图片信息【管理员】（走后门）
     *
     * @param pictureUpdateRequest 需要修改的信息
     * @param request              通过切面编程实现了权限校验为什么还要获取到管理员信息呢？因为我们需要给图片设置审核人信息
     * @return 是否成功修改
     */
    @PutMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResult<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest,
                                             HttpServletRequest request) {
        // 校验参数
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "修改参数不能为空~");
        }
        // 判断图片信息是否存在
        Long picId = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(picId);
        ThrowUtils.throwif(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "无法找到原图片信息~");

        // 图片类型转换 dto  ->  entity
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        // 因为 tags的类型不同我们需要将 List<String> -->  String
        List<String> tags = pictureUpdateRequest.getTags();
        if (tags != null) {
            String tagsStr = JSONUtil.toJsonStr(tags);
            picture.setTags(tagsStr);
        }
        // 数据校验
        pictureService.validPicture(picture);
        // 图片状态设置
        User loginUser = userService.getLoginUser(request);
        pictureService.fillReviewParams(picture, loginUser);

        // 修改数据库
        boolean isUpdate = pictureService.updateById(picture);
        ThrowUtils.throwif(!isUpdate, ErrorCode.OPERATION_ERROR, "修改图片信息失败~");
        return ResultUtils.success(true);
    }

    /**
     * 编辑图片信息
     *
     * @param pictureEditRequest 需要修改的信息
     * @return 是否成功修改
     */
    @PutMapping("/edit")
    @SaSpaceCheckPermission(SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResult<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        // 校验参数
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "修改参数不能为空~");
        }

        User loginUser = userService.getLoginUser(request);
        pictureService.editPicture(pictureEditRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 获取图片信息【管理员】（走后门）
     *
     * @param id 查询图片的id
     * @return 查询后的图片完整信息
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    BaseResult<Picture> getPictureById(Long id) {
        // 图片校验
        ThrowUtils.throwif(id == null || id < 0, ErrorCode.PARAMS_ERROR, "查询图片Id不存在~");
        // 查看数据库是否存在
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwif(picture == null, ErrorCode.NOT_FOUND_ERROR, "查询信息不存在~");
        // todo 是否需要将String 类型转化为List<String> 类型
        // 不转化了，因为我们查询到的数据和返回的数据都是统一类型的
        return ResultUtils.success(picture);
    }

    /**
     * 获取图片信息
     *
     * @param id      查询图片的id
     * @param request 用于获取到用户数据进行权限校验
     * @return 查询后的图片完整信息
     * todo 后续开发用户自已空间后能够将状态不合法的图片只供用户查看，这里就进行状态校验了
     */
    @GetMapping("/get/vo")
    BaseResult<PictureVO> getPictureVOById(Long id, HttpServletRequest request) {
        // 图片校验
        ThrowUtils.throwif(id == null || id < 0, ErrorCode.PARAMS_ERROR, "查询图片Id不存在~");
        // 查看数据库是否存在
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwif(picture == null, ErrorCode.NOT_FOUND_ERROR, "查询信息不存在~");

        // 是否为管理员或本图片创始人 todo【需要根据空间进行权限校验】
        User loginUser = userService.getLoginUser(request);
        // 只对私人空间进行校验
        Long spaceId = picture.getSpaceId();

        // 已经设置为 sa-token 校验逻辑
        // if (spaceId != null) {
        //     pictureService.checkPictureAuth(picture, loginUser);
        // }
        // if (!picture.getId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)){
        //     throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "你没有查询该图片的权限~");
        // }
        if (spaceId != null) {
            boolean access = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有查询权限");
        }
        // 获取脱敏后的用户信息
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 添加冗余属性
        pictureVO.setUserVO(userService.getUserVO(loginUser));
        Space space = spaceService.getById(spaceId);
        pictureVO.setPermissionList(spaceUserAuthManager.getPermissionList(space, loginUser));
        return ResultUtils.success(pictureVO);
    }

    /**
     * 根据传入的查询信息获取到列表信息【管理员】（走后门）
     *
     * @param pictureQueryRequest 查询参数
     * @return 返回符合的page信息
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    BaseResult<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        // 分页查询不需要参数
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * 用户获取分页参数
     *
     * @param pictureQueryRequest 查询参数
     * @param request             用于校验用户查询到的图片是否为自己的
     * @return 脱敏后的本用户的所有图片page
     */
    @PostMapping("/list/page/vo")
    BaseResult<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                    HttpServletRequest request) {
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwif(pageSize > 20, ErrorCode.OPERATION_ERROR, "小子想爬老子网站~");

        // 设置用户只能查看过审图片
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        // 不同空间的权限校验
        Long spaceId = pictureQueryRequest.getSpaceId();
        if (spaceId == null) {
            // 公共空间
            // 设置需要 查询 空间id为 null 的数据，以及过审的图片
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        } else {
            // 私有空间
            User loginUser = userService.getLoginUser(request);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwif(space == null, ErrorCode.NOT_FOUND_ERROR, "没有查询的数据");
            boolean access = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            if (!access){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有查询权限");
            }
            // ThrowUtils.throwif(!loginUser.getId().equals(space.getUserId()), ErrorCode.NO_AUTH_ERROR, "没有操作当前空间的权限");
        }

        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
    }

    /**
     * 项目标签管理
     *
     * @return 项目所有标签列表
     */
    @GetMapping("/tag_category")
    public BaseResult<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "御姐", "可爱", "性感", "自愈", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "美女", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 管理员对图片进行审核
     *
     * @param pictureReviewRequest 待审核的图片信息
     * @param request              用来标明当前是那个管理员进行审核的
     * @return 返回审核是否成功
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResult<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest,
                                               HttpServletRequest request) {
        ThrowUtils.throwif(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR, "状态信息不能为空~");
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwif(loginUser == null, ErrorCode.OPERATION_ERROR, "没有找到对应管理员信息~");
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 批量导入图片
     *
     * @param pictureUploadByBatchRequest 导入图片信息
     * @param request                     获取用户信息
     * @return 返回导入成功数
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResult<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
                                                    HttpServletRequest request) {
        ThrowUtils.throwif(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR, "请求参数不合法~");
        Integer uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, userService.getLoginUser(request));
        return ResultUtils.success(uploadCount);
    }

    @GetMapping("/list/level")
    public BaseResult<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values())
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()
                )).collect(Collectors.toList());
        return ResultUtils.success(spaceLevelList);
    }

    /**
     * 以图搜图
     */
    @PostMapping("/search/picture")
    public BaseResult<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureRequest
                                                                               searchPictureByPictureRequest) {
        ThrowUtils.throwif(ObjectUtil.isEmpty(searchPictureByPictureRequest), ErrorCode.PARAMS_ERROR, "没有找到带查询图片信息");
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        ThrowUtils.throwif(pictureId == null, ErrorCode.PARAMS_ERROR, "没有找到带查询图片信息");
        Picture picture = pictureService.getById(pictureId);
        // 因为我们这里使用的是 真实没有压缩过的图片，可以直接获取
        String searchUrl = picture.getUrl();
        List<ImageSearchResult> imageSearchResults = ImageSearchApiFacade.searchImage(searchUrl);
        return ResultUtils.success(imageSearchResults);
    }

    /**
     * 根据传入的图片 色调进行图片查询
     * @param searchPictureByColorRequest 查询参数
     * @param request 用于判断用户权限
     * @return 返回查询到的最相近的图片集合
     */
    @PostMapping("/search/color")
    @SaSpaceCheckPermission(SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResult<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest, HttpServletRequest request) {
        ThrowUtils.throwif(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        String picColor = searchPictureByColorRequest.getPicColor();
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        User loginUser = userService.getLoginUser(request);
        List<PictureVO> result = pictureService.searchPictureByColor(spaceId, picColor, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 创建 AI 扩图任务
     */
    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResult<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(
            @RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
            HttpServletRequest request) {
        if (createPictureOutPaintingTaskRequest == null || createPictureOutPaintingTaskRequest.getPictureId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        CreateOutPaintingTaskResponse response = pictureService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 查询 AI 扩图任务
     */
    @GetMapping("/out_painting/get_task")
    public BaseResult<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId) {
        ThrowUtils.throwif(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        GetOutPaintingTaskResponse task = aliYunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.success(task);
    }
}
