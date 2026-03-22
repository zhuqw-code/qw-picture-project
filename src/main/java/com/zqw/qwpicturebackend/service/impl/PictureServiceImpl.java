package com.zqw.qwpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zqw.qwpicturebackend.Utils.ColorSimilarUtils;
import com.zqw.qwpicturebackend.api.aliyunai.api.AliYunAiApi;
import com.zqw.qwpicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.zqw.qwpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.zqw.qwpicturebackend.exception.BusinessException;
import com.zqw.qwpicturebackend.exception.ErrorCode;
import com.zqw.qwpicturebackend.exception.ThrowUtils;
import com.zqw.qwpicturebackend.manager.CosManager;
import com.zqw.qwpicturebackend.manager.upload.FilePictureUpload;
import com.zqw.qwpicturebackend.manager.upload.PictureUploadTemplate;
import com.zqw.qwpicturebackend.manager.upload.UrlPictureUpload;
import com.zqw.qwpicturebackend.mapper.PictureMapper;
import com.zqw.qwpicturebackend.model.dto.file.UploadPictureResult;
import com.zqw.qwpicturebackend.model.dto.picture.*;
import com.zqw.qwpicturebackend.model.entity.Picture;
import com.zqw.qwpicturebackend.model.entity.Space;
import com.zqw.qwpicturebackend.model.entity.User;
import com.zqw.qwpicturebackend.model.enums.PictureReviewStatusEnum;
import com.zqw.qwpicturebackend.model.vo.PictureVO;
import com.zqw.qwpicturebackend.model.vo.UserVO;
import com.zqw.qwpicturebackend.service.PictureService;
import com.zqw.qwpicturebackend.service.SpaceService;
import com.zqw.qwpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zhuqw
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-10-25 22:45:47
 * add1:   给修改添加权限校验，只有 用户本身/ admin才能进行修改
 */
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private CosManager cosManager;

    @Resource
    private TransactionTemplate transactionTemplate;

    private static final String PUBLIC_SPACE = "public";
    @Autowired
    private AliYunAiApi aliYunAiApi;


    /**
     * 用户上传图片
     *
     * @param inputResource        图片信息来源（本地/url？）
     * @param pictureUploadRequest 图片id信息
     * @param loginUser            用户信息
     * @return 返回上传后的图片信息
     */
    @Override
    public PictureVO uploadPicture(Object inputResource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 1.校验图片
        ThrowUtils.throwif(inputResource == null, ErrorCode.PARAMS_ERROR, "上传图片信息有误~");
        ThrowUtils.throwif(loginUser == null, ErrorCode.PARAMS_ERROR, "无法获取上传用户信息~");

        // plus 添加空间校验，只有空间存在才能操作，并且进行空间权限校验
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            // 如果space为null可能是公共空间啊！不能报错
            ThrowUtils.throwif(space == null, ErrorCode.NOT_FOUND_ERROR, "没有待上传的空间信息");
            // 存在空间就判断当前用户是否是该空间的创建者
            Long userId = loginUser.getId();
            if (!userId.equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "你没有操作该空间的权限");
            }
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数不足");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小不足");
            }
        }

        // todo 用户修改图片，没有携带图片空间id，我们需要根据旧图片id获取可能不为null的图片空间id吗
        Long picId = pictureUploadRequest.getId();
        Picture picture = null;
        if (picId != null) {
            // 修改操作，是否又spaceId
            picture = this.getById(picId);
            ThrowUtils.throwif(picture == null, ErrorCode.NOT_FOUND_ERROR, "没有要修改的图片");
            Long spaceId1 = picture.getSpaceId();
            if (spaceId1 != null) {
                spaceId = spaceId1;
            }
        }

        // 2.设置图片上传信息
        // 合法的上传/更新图片（二话不说直接放到COS中，之后将返回信息存储到数据库中）
        // ？更新是直接将原始的覆盖还是直接？
        // ! 所谓更新就是在COS中新添加一张图片存储，之后将新的信息返回存储到数据库中
        // ！更新时我们可以根据图片 id获取到图片的COS路径信息！
        // 按照用户id划分公共图库区域

        String uploadPathPrefix = String.format("/%s/%s", PUBLIC_SPACE, loginUser.getId());
        // 这里没有考虑空间id
        if (spaceId != null) {
            uploadPathPrefix = String.format("/%s/%s", "space", spaceId);
        }
        // 3.设置上传方式
        // todo 这里需要判断到底使用那种方式（本地/url）
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputResource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        // 4.上传
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadLoadPicture(inputResource, uploadPathPrefix);

        // 5.根据 上传/更新 进行数据库操作
        // 5.1上传图片（本次请求是上传还是更新？？？）

        if (picId == null) {  // 新图片构造
            picture = buildPicture(uploadPictureResult, null, loginUser.getId(), pictureUploadRequest);
        }
        // 5.2如果是更新判断图片是否存在
        if (picId != null) {
            // 旧图片构造
            // boolean exists = this.lambdaQuery()
            //         .eq(Picture::getId, picId)
            //         .exists();
            // ThrowUtils.throwif(!exists, ErrorCode.OPERATION_ERROR, "图片不存在~");
            // todo 修改操作需要判断用户身份
            Picture oldPicture = this.getById(picId);
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "你没有修改该图片的权限~");
            }
            // plus 判断图片空间是否和之前一致？？（房子偷梁换柱）
            Long oldSpaceId = oldPicture.getSpaceId();
            if (spaceId == null) {
                spaceId = oldSpaceId;
            } else {
                // 如果传入的 spaceId不为空
                if (!ObjUtil.equals(spaceId, oldSpaceId)) {   // todo 如果oldSpaceId 为null呢？需要判断spaceId是否等于当前用户创建的空间id
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无法将图片添加到当前空间中");
                }
            }
            // 构建存储 picture存储后获取到信息再返回
            picture = this.buildPicture(uploadPictureResult, oldPicture, loginUser.getId(), pictureUploadRequest);
        }

        // 6.填充公共数据
        // 无论上传/修改 都要设置图片状态
        this.fillReviewParams(picture, loginUser);
        // 给图片设置空间 id
        picture.setSpaceId(spaceId);
        picture.setEditTime(new Date());
        // 7.加入到数据库中
        // 会根据是否有id进行存储
        // 只要是修改数据库操作就要 开启事务
        Picture finalPicture = picture;
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            // 插入数据
            boolean isOk = this.saveOrUpdate(finalPicture);
            ThrowUtils.throwif(!isOk, ErrorCode.OPERATION_ERROR, "插入数据库失败~");
            // todo 如果是公共空间可以直接上传
            if (finalSpaceId == null) {
                return finalPicture;
            }
            // 更新空间使用额度
            boolean update = spaceService.lambdaUpdate()
                    .eq(Space::getId, finalSpaceId)
                    .setSql("totalSize = totalSize + " + finalPicture.getPicSize())
                    .setSql("totalCount = totalCount + 1")
                    .update();
            ThrowUtils.throwif(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            return finalPicture;
        });

        // 8.返回脱敏后的图片信息
        return PictureVO.objToVo(picture);
    }

    /**
     * 获取图片查询器
     *
     * @param pictureQueryRequest 查询器
     * @return 使用查询条件拼接成的查询器
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        // 查询条件可以为空，为空就是查询所有参数
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }

        // 拼接查询条件
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        // 根据两个参数判断查询的空间类型
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        // 根据图片状态信息进行查询
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        //  新增根据时间段进行搜索
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();

        // 1.包含searchText字段，可以使用name，introduction进行模糊查询
        // and (数据库name like %searchText% or 数据库introduction like %searchText%)
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw
                    .like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        // 2.拼接普通参数
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        // 私有空间和公共空间的区分
        queryWrapper.eq(ObjUtil.isNotNull(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");     // 当 参数1为true，就将 spaceId == null 的条件添加到查询条件中
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        // 设置时间段 ge >= greatAndEquals
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        queryWrapper.le(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);
        // 添加图片状态查询
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        // 3.将tags通过for拼接上
        // 数据库tags信息 like "我们遍历的的tag"
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 4.拼接排序规则
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), "ascend".equals(sortOrder), sortField);
        return queryWrapper;
    }

    /**
     * 图片转VO，不涉及业务
     *
     * @param picture 图片信息
     * @param request 用于获取用户信息
     * @return 返回脱敏后的图片信息
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        ThrowUtils.throwif(picture == null, ErrorCode.PARAMS_ERROR, "找不到图片信息~");
        // 转换为VO对象
        PictureVO pictureVO = PictureVO.objToVo(picture);
        Long userId = pictureVO.getUserId();
        // 判断用户能否访问图片
        // todo 不需要校验图片是否能够访问吗？
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUserVO(userVO);
        }
        return pictureVO;
    }


    /**
     * 将分页图片信息转化为脱敏后的图片信息
     *
     * @param picturePage 未脱敏的图片信息
     * @param request     用于获取用户信息
     * @return 返回脱敏后的图片信息
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        // 判空，长度为0也要判断哦
        if (pictureList == null || pictureList.size() == 0) {
            return pictureVOPage;
        }
        // 获取到所有PictureVO
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 获取到涉及到的所有用户id
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        // 获取到用到的所有用户VO类
        Map<Long, List<UserVO>> userIdUserListMap = userService.listByIds(userIdSet).stream().map(userService::getUserVO).collect(Collectors.groupingBy(UserVO::getId));
        // 封装每个PictureVO中的UserVO
        pictureVOList.forEach(pictureVO ->
        {
            Long userId = pictureVO.getUserId();
            if (userIdUserListMap.containsKey(userId)) {
                pictureVO.setUserVO(userIdUserListMap.get(userId).get(0));
            }
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    /**
     * 对图片信息进行校验
     *
     * @param picture 需要校验的 picture 对象
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwif(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwif(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwif(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwif(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    /**
     * 管理员对图片进行审核
     *
     * @param pictureReviewRequest 待校验的图片
     * @param loginUser            当前是那个管理员，因为需要记录审核人id，我们需要将id存储
     */
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        // 判断请求信息，以及审核人信息
        ThrowUtils.throwif(pictureReviewRequest == null || loginUser == null, ErrorCode.PARAMS_ERROR, "请求信息不能为空哦~");

        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        // 获取数据库中oldPicture
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwif(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "待修改图片信息不存在~");
        // 判断状态是否能够进行审核，如果是审核成功的图片就不必进行重复审核了
        ThrowUtils.throwif(oldPicture.getReviewStatus().equals(reviewStatus), ErrorCode.OPERATION_ERROR, "图片已是预期状态~");
        // 将请求信息转化为对数据库操作的对象 pictureReviewRequest  ->  picture
        Picture updatePicture = buildUpdateReviewPicture(loginUser, oldPicture, reviewStatus, reviewMessage);
        // 封装新的picture对象【原因只对审核信息进行操作，并且数据库会对非空字段进行修改，不需要将数据库中查询到的数据全部传入】
        ThrowUtils.throwif(!this.updateById(updatePicture), ErrorCode.SYSTEM_ERROR, "审核失败~");
    }

    /**
     * 根据用户身份设置图片审核状态
     *
     * @param picture   待审核图片信息
     * @param loginUser 当前进行操作的用户/管理员
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewMessage("管理员自动过审~");
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
        } else {
            // 普通用户设置待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    /**
     * 封装对数据库修改的图片信息
     *
     * @param uploadPictureResult
     * @param oldPicture
     * @param userId
     * @param pictureUploadRequest
     * @return
     */
    public Picture buildPicture(UploadPictureResult uploadPictureResult, Picture oldPicture, Long userId,
                                PictureUploadRequest pictureUploadRequest) {
        Picture picture = new Picture();
        if (ObjUtil.isNotNull(oldPicture)) {
            BeanUtils.copyProperties(oldPicture, picture);
        }
        picture.setUrl(uploadPictureResult.getUrl());
        // todo 设置缩略图地址
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        // 如果图片名称有值（picName）就设置
        String picName = uploadPictureResult.getPicName();
        String sendName = pictureUploadRequest.getPicName();
        if (StrUtil.isNotBlank(sendName)) {
            picName = sendName;
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getWidth());
        picture.setPicHeight(uploadPictureResult.getHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        // 将主色调颜色设置到数据库中
        picture.setPicColor(uploadPictureResult.getPicColor());
        picture.setUserId(userId);
        // 上传新的图片
        return picture;
    }

    /**
     * @param loginUser
     * @param oldPicture
     * @param reviewStatus
     * @param reviewMessage
     * @return
     */
    private Picture buildUpdateReviewPicture(User loginUser, Picture oldPicture,
                                             Integer reviewStatus, String reviewMessage) {
        Picture updatePicture = new Picture();
        // todo 这里是不是有问题，直接将状态信息修改不就行了吗？mybatis-plus不是自动将null跳过了，不会影响原未修改的字段
        BeanUtils.copyProperties(oldPicture, updatePicture);
        updatePicture.setReviewStatus(reviewStatus);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewMessage(reviewMessage);
        updatePicture.setReviewTime(new Date());
        return updatePicture;
    }


    /**
     * 批量上传文件
     *
     * @param pictureUploadByBatchRequest 请求参数包括（搜索词，图片统一名称，每次爬取数据条数）
     * @param loginUser                   当前用户信息，在插入数据库时需要指定创建人信息
     * @return 成功插入记录数
     */
    public int uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        // 请求参数非空校验
        ThrowUtils.throwif(!ObjUtil.isNotEmpty(pictureUploadByBatchRequest), ErrorCode.PARAMS_ERROR, "批量插入数据不能为空");
        // 请求参数合法性校验
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        // 如果没有为图片设置默认名称就使用搜索参数
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        ThrowUtils.throwif(count > 30, ErrorCode.PARAMS_ERROR, "单次上传次数超额~");
        // 要抓取的地址
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (Exception e) {
            log.info("本次批量请求失败，具体原因：{}", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "本次抓取失败~");
        }
        // 因为浏览器中所有图片都是在 dgControl中，我们获取这个 div
        Element dgDiv = document.getElementsByClass("dgControl").first();
        ThrowUtils.throwif(ObjUtil.isNull(dgDiv), ErrorCode.OPERATION_ERROR, "获取请求参数失败~");
        Elements imgElementList = dgDiv.select("img.mimg");
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (!StrUtil.isNotBlank(fileUrl)) {
                log.info("当前链接为空，已跳过，详情：{}", fileUrl);
                continue;
            }
            // 处理图片的地址，去掉冗余参数
            int idx = fileUrl.indexOf("?");
            if (idx > -1) {
                fileUrl = fileUrl.substring(0, idx);
            }
            // todo 这里可以再对图片url进行爬取获取到更高清图片
            // 上传图片(因为我们需要为图片设置名称，需要在pictureUploadRequest中传入图片名称信息，这样就能在插入数据库时设置上)
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            pictureUploadRequest.setPicName(namePrefix + uploadCount);
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片成功上传，id={}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.info("图片上传失败：具体请查看图片信息：url={}", fileUrl);
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

    /**
     * 抽离出来的删除逻辑
     *
     * @param picId
     * @param loginUser
     */
    @Override
    public void deletePicture(Long picId, User loginUser) {
        // 判断图片是否存在
        Picture picture = this.getById(picId);
        ThrowUtils.throwif(picture == null, ErrorCode.OPERATION_ERROR, "图片信息不存在~");
        // 只有本用户和管理员才能删除图片 todo【需要判断空间类型】
        this.checkPictureAuth(picture, loginUser);
        // if (!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
        //     throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "你没有删除该图片的权力~");
        // }

        // 只要是修改数据库操作就要 开启事务
        transactionTemplate.execute(status -> {
            // 无论如何都要删除
            boolean isDelete = this.removeById(picId);
            ThrowUtils.throwif(!isDelete, ErrorCode.OPERATION_ERROR, "无法删除该图片~");
            // 更新空间使用额度
            // 如果没有关联空间就没有必要更新额度了
            Long spaceId = picture.getSpaceId();
            // 确保 picSize 是数字类型（Long/Integer），避免注入
            if (ObjectUtil.isNotEmpty(spaceId)) {
                Long picSize = picture.getPicSize();
                // 对于spaceId为空的情况，在公共图库，管理员是又权限删除的
                // ThrowUtils.throwif(spaceId == null, ErrorCode.OPERATION_ERROR, "当前图片没有归属的空间");
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, picture.getSpaceId())
                        .setSql("totalSize = totalSize - " + picSize)
                        .setSql("totalCount = totalCount - " + 1)
                        // 注意：这里的逗号很重要，且必须确保 picSize 是数值类型防止注入
                        // .apply("totalSize = totalSize - " + picture.getPicSize() + ", totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwif(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return true;
        });
        // 清理 Cos 文件
        this.clearPictureFile(picture);
    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        // 判断图片信息是否存在
        Long picId = pictureEditRequest.getId();
        Picture oldPicture = this.getById(picId);
        ThrowUtils.throwif(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "无法找到原图片信息~");

        // 图片类型转换 dto  ->  entity
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 因为 tags的类型不同我们需要将 List<String> -->  String
        List<String> tags = pictureEditRequest.getTags();
        if (tags != null) {
            String tagsStr = JSONUtil.toJsonStr(tags);
            picture.setTags(tagsStr);
        }
        // 需要重新编辑修改日期
        picture.setUpdateTime(new Date());

        // 数据校验
        this.validPicture(picture);
        // 设置图片的审核状态
        this.fillReviewParams(picture, loginUser);

        // 判断是图片归属者还是管理员 todo【添加上空间判断】
        // picture 属性拷贝后没有用户id，需要用含有id的对象来判断
        this.checkPictureAuth(picture, loginUser);
        // if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
        //     throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "你不能删除别人的图片或者你不是管理员~");
        // }
        // 修改数据库
        boolean isUpdate = this.updateById(picture);
        ThrowUtils.throwif(!isUpdate, ErrorCode.OPERATION_ERROR, "修改图片信息失败~");
    }

    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        Long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        // 除了自己的信息，如果又其他的图片关联就拒绝删除
        if (count > 1) {
            return;
        }
        // 删除图片
        cosManager.deleteObject(pictureUrl);
        // 删除缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StringUtils.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }

    /**
     * 校验当前用户是否能够 删，改，查 传入图片
     *
     * @param picture   图片信息
     * @param loginUser 用户信息
     */
    @Override
    public void checkPictureAuth(Picture picture, User loginUser) {
        // 先判断图片的空间位置
        Long spaceId = picture.getSpaceId();
        // 如果是公共空间只用图片拥有者和管理员能操作
        Long userId = loginUser.getId();
        if (spaceId == null) {
            if (!userId.equals(picture.getUserId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您没有操作别人空间的权限");
            }
        } else {
            // 如果是私人空间只用用户本身能操作
            if (!userId.equals(picture.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您没有操作别人空间的权限");
            }
        }
    }


    /**
     * 根据上传的图片主色调查询相关联的图片
     *
     * @param spaceId   查询的依据数据就是空间id
     * @param picColor  带查询的颜色
     * @param loginUser 需要进行用户身份校验
     * @return 返回查询到的图片信息
     */
    @Override
    public List searchPictureByColor(Long spaceId, String picColor, User loginUser) {
        ThrowUtils.throwif(spaceId == null || picColor == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空~");
        ThrowUtils.throwif(ObjUtil.isEmpty(loginUser), ErrorCode.NO_AUTH_ERROR, "没有权限操作，登录后才能享受~");
        // 0.校验空间是否存在，用户是否有权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwif(ObjUtil.isEmpty(space), ErrorCode.OPERATION_ERROR, "没有数据源进行查询~");
        if (!space.getUserId().equals(loginUser.getId())){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您没有权限~");
        }
        // 1.将空间中所有图片都查询到
        List<Picture> list = lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();
        if (ObjUtil.isEmpty(list)){
            return Collections.EMPTY_LIST;
        }
        // 2.根据主色调调用相似度算法进行相似度匹配
        // 3.封装并返回脱敏后的图片信息
        Color targetColor = Color.decode(picColor);
        return list.stream()
                .sorted(Comparator.comparingDouble(picture -> {
                    String hexColor = picture.getPicColor();
                    // 判空
                    if (StrUtil.isBlank(hexColor)) {
                        return Double.MAX_VALUE;
                    }
                    Color pictureColor = Color.decode(hexColor);
                    // 越小排得越靠前
                    return -ColorSimilarUtils.calculateSimilarity(targetColor, pictureColor);
                }))
                .limit(12)      // 最多取最相似的12张
                .map(PictureVO::objToVo).collect(Collectors.toList());
    }


    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
        // 获取图片信息
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        Picture picture = Optional.ofNullable(this.getById(pictureId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR));
        // 权限校验
        checkPictureAuth(picture, loginUser);
        // 构造请求参数
        CreateOutPaintingTaskRequest taskRequest = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        taskRequest.setInput(input);
        BeanUtil.copyProperties(createPictureOutPaintingTaskRequest, taskRequest);
        // 创建任务
        return aliYunAiApi.createOutPaintingTask(taskRequest);
    }
}




