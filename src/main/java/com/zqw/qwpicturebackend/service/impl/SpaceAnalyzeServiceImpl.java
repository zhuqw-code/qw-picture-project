package com.zqw.qwpicturebackend.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.zqw.qwpicturebackend.constant.UserConstant;
import com.zqw.qwpicturebackend.exception.BusinessException;
import com.zqw.qwpicturebackend.exception.ErrorCode;
import com.zqw.qwpicturebackend.exception.ThrowUtils;
import com.zqw.qwpicturebackend.model.dto.space.analyze.*;
import com.zqw.qwpicturebackend.model.entity.Picture;
import com.zqw.qwpicturebackend.model.entity.Space;
import com.zqw.qwpicturebackend.model.entity.User;
import com.zqw.qwpicturebackend.service.PictureService;
import com.zqw.qwpicturebackend.service.SpaceAnalyzeService;
import com.zqw.qwpicturebackend.service.SpaceService;
import com.zqw.qwpicturebackend.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SpaceAnalyzeServiceImpl implements SpaceAnalyzeService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;


    @Resource
    private PictureService pictureService;


    /**
     * 根据不同的空间查询不同的使用情况
     *
     * @param spaceUsageAnalyzeRequest 请求参数【所有图库/公共图库/私有图库】
     * @param loginUser                当前登录用户
     * @return 返回图库查询的结果（包括图片条数，图片空间使用情况，使用比）
     */
    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest,
                                                          User loginUser) {
        // 判空
        ThrowUtils.throwif(spaceUsageAnalyzeRequest == null, ErrorCode.NOT_FOUND_ERROR, "请求参数不能为空");
        if (spaceUsageAnalyzeRequest.isQueryAll() || spaceUsageAnalyzeRequest.isQueryPublic()) {
            // 1. 校验权限
            Boolean isAdmin = userService.isAdmin(loginUser);
            ThrowUtils.throwif(!isAdmin, ErrorCode.NOT_FOUND_ERROR, "没有权限");
            // 2. 构造查询条件
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("picSize");
            // 默认查找全部图片，要是设置了查找public图片
            if (spaceUsageAnalyzeRequest.isQueryPublic()) {
                // 这是public图库图片的特点
                queryWrapper.isNull("spaceId");
            }
            // 3. 查询数据库
            List<Object> objList = pictureService.getBaseMapper().selectObjs(queryWrapper);
            // 统计占用内存
            long useSize = objList.stream().mapToLong(obj -> obj instanceof Long ? (Long) obj : 0L).sum();
            long useCount = objList.size();
            // 4. 处理数据
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(useSize);
            spaceUsageAnalyzeResponse.setMaxSize(null);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setUsedCount(useCount);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            spaceUsageAnalyzeResponse.setCountUsageRatio(null);
            // 5. 返回结果
            return spaceUsageAnalyzeResponse;
        } else {
            // 1. 图库校验
            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
            ThrowUtils.throwif(spaceId == null, ErrorCode.NOT_FOUND_ERROR, "请求参数不能为空");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwif(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 2. 权限校验
            // 只有本人或管理员才能操作
            checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
            // 3. 查询数据
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("spaceId = " + spaceId);
            List<Object> objList = pictureService.getBaseMapper().selectObjs(queryWrapper);
            long useSize = objList.stream().mapToLong(obj -> obj instanceof Long ? (Long) obj : 0).sum();
            long useCount = objList.size();
            // 4. 封装返回值结果
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(useSize);
            spaceUsageAnalyzeResponse.setUsedCount(useCount);

            spaceUsageAnalyzeResponse.setMaxSize(space.getMaxSize());
            double sizeUsageRatio = NumberUtil.round(useSize * 100.0 / space.getMaxSize(), 2).doubleValue();
            spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);
            spaceUsageAnalyzeResponse.setMaxCount(space.getMaxCount());
            double countUsageRatio = NumberUtil.round(useCount * 100.0 / space.getMaxCount(), 2).doubleValue();
            spaceUsageAnalyzeResponse.setCountUsageRatio(countUsageRatio);
            return spaceUsageAnalyzeResponse;
        }
    }


    /**
     * 根据图片分类对指定空间图片进行分析查询
     *
     * @param spaceCategoryAnalyzeRequest 请求参数
     * @param loginUser                   登录用户
     * @return 分类分析结果
     */
    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest,
                                                                      User loginUser) {
        // 1. 非空判断
        ThrowUtils.throwif(spaceCategoryAnalyzeRequest == null || loginUser == null, ErrorCode.PARAMS_ERROR, "查询请求不能为空");
        // 2. 权限校验
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);
        // 3. 构造查询条件 【尽然还可以传入一个map类型的查询参数】
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        // 设置查询空间
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);
        queryWrapper.select(
                "category as category",
                "SUM(picSize) as totalSize",
                "count(*) as count"
        ).groupBy("category");
        // 4. 执行查询，封装返回结果
        return pictureService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(result -> {
                    String category = result.get("category") != null ? result.get("category").toString() : "未知类型";
                    Long totalSize = result.get("totalSize") != null ? Long.parseLong(result.get("totalSize").toString()) : 0L;
                    Long count = result.get("count") != null ? Long.parseLong(result.get("count").toString()) : 0L;
                    return new SpaceCategoryAnalyzeResponse(category, totalSize, count);
                }).collect(Collectors.toList());
    }


    /**
     * 获取空间标签分析
     *
     * @param spaceTagAnalyzeRequest 请求参数
     * @param loginUser              当前登录用户
     * @return 返回根据标签进行分类的结果
     */
    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest,
                                                            User loginUser) {
        // 1. 参数非空判断
        ThrowUtils.throwif(spaceTagAnalyzeRequest == null || loginUser == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        // 2. 参数权限校验
        checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);
        // 3. 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, queryWrapper);
        queryWrapper.select("tags");
        // 4. 执行查询【因为一张图片可能又多个标签，所以不能直接通过数据库函数查询得到分组】
        // 先获取到所有标签
        List<String> tagJsonList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .filter(ObjUtil::isNotNull)
                .map(ObjUtil::toString)
                .collect(Collectors.toList());
        // 对标签进行统计
        Map<String, Long> tagCountMap = tagJsonList.stream().flatMap(tagJson -> JSONUtil.toList(tagJson, String.class).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
        // 5. 封装并返回查询结果
        return tagCountMap.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e1.getValue(), e2.getValue()))
                .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }


    /**
     * 根据图片大小进行分类
     *
     * @param spaceSizeAnalyzeRequest 请求参数
     * @param loginUser               登录用户
     * @return 空间大小分析结果
     */
    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        ThrowUtils.throwif(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

        // 检查权限
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);

        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);

        // 查询所有符合条件的图片大小
        queryWrapper.select("picSize");
        List<Long> picSizes = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .map(size -> ((Number) size).longValue())
                .collect(Collectors.toList());

        // 定义分段范围，注意使用有序 Map
        Map<String, Long> sizeRanges = new LinkedHashMap<>();
        sizeRanges.put("<100KB", picSizes.stream().filter(size -> size < 100 * 1024).count());
        sizeRanges.put("100KB-500KB", picSizes.stream().filter(size -> size >= 100 * 1024 && size < 500 * 1024).count());
        sizeRanges.put("500KB-1MB", picSizes.stream().filter(size -> size >= 500 * 1024 && size < 1 * 1024 * 1024).count());
        sizeRanges.put(">1MB", picSizes.stream().filter(size -> size >= 1 * 1024 * 1024).count());

        // 转换为响应对象
        return sizeRanges.entrySet().stream()
                .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }


    /**
     * 根据空间使用情况进行统计
     *
     * @param spaceUserAnalyzeRequest 请求参数
     * @param loginUser               登录用户
     * @return 空间使用情况统计结果
     */
    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        ThrowUtils.throwif(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 检查权限
        checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);

        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        Long userId = spaceUserAnalyzeRequest.getUserId();
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);

        // 分析维度：每日、每周、每月
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') AS period", "COUNT(*) AS count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(createTime) AS period", "COUNT(*) AS count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') AS period", "COUNT(*) AS count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间维度");
        }

        // 分组和排序
        queryWrapper.groupBy("period").orderByAsc("period");

        // 查询结果并转换
        List<Map<String, Object>> queryResult = pictureService.getBaseMapper().selectMaps(queryWrapper);
        return queryResult.stream()
                .map(result -> {
                    String period = result.get("period").toString();
                    Long count = ((Number) result.get("count")).longValue();
                    return new SpaceUserAnalyzeResponse(period, count);
                })
                .collect(Collectors.toList());
    }


    /**
     * 空间使用排行
     *
     * @param spaceRankAnalyzeRequest 请求参数
     * @param loginUser               当前登录用户
     * @return 空间使用排行
     */
    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        ThrowUtils.throwif(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

        // 仅管理员可查看空间排行
        ThrowUtils.throwif(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "无权查看空间排行");

        // 构造查询条件
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "spaceName", "userId", "totalSize")
                .orderByDesc("totalSize")
                .last("LIMIT " + spaceRankAnalyzeRequest.getTopN()); // 取前 N 名

// 查询结果
        return spaceService.list(queryWrapper);
    }


    /**
     * 判断当前空间是否能够被当前用户查询
     *
     * @param spaceAnalyzeRequest 带查询的空间信息
     * @param loginUser           当前用户
     */
    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        // 检查权限
        if (spaceAnalyzeRequest.isQueryAll() || spaceAnalyzeRequest.isQueryPublic()) {
            // 全空间分析或者公共图库权限校验：仅管理员可访问
            ThrowUtils.throwif(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "无权访问公共图库");
        } else {
            // 私有空间权限校验
            Long spaceId = spaceAnalyzeRequest.getSpaceId();
            ThrowUtils.throwif(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwif(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 只有管理员和本人能够查询
            if (!loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE)
                    && !space.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有查看当前空间权限");
            }
        }
    }


    /**
     * 填充查询条件
     *
     * @param spaceAnalyzeRequest 需要填充的信息
     * @param wrapper             填充到querywrapper中
     */
    private static void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest,
                                                QueryWrapper<Picture> wrapper) {
        // 所有空间
        if (spaceAnalyzeRequest.isQueryAll()) {
            return;
        }
        // 公共空间
        if (spaceAnalyzeRequest.isQueryPublic()) {
            wrapper.isNull("spaceId");      // 公共空间就是图片空间为null的
            return;
        }
        // 私有空间
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != null) {
            wrapper.eq(ObjectUtils.isNotEmpty(spaceId), "spaceId", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "没有该数据");
    }
}
