package com.zqw.qwpicturebackend.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zqw.qwpicturebackend.constant.UserConstant;
import com.zqw.qwpicturebackend.exception.BusinessException;
import com.zqw.qwpicturebackend.exception.ErrorCode;
import com.zqw.qwpicturebackend.exception.ThrowUtils;
import com.zqw.qwpicturebackend.manager.sharding.DynamicShardingManager;
import com.zqw.qwpicturebackend.model.dto.space.SpaceAddRequest;
import com.zqw.qwpicturebackend.model.dto.space.SpaceQueryRequest;
import com.zqw.qwpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.zqw.qwpicturebackend.model.entity.Space;
import com.zqw.qwpicturebackend.model.entity.SpaceLevelEnum;
import com.zqw.qwpicturebackend.model.entity.SpaceUser;
import com.zqw.qwpicturebackend.model.entity.User;
import com.zqw.qwpicturebackend.model.enums.SpaceRoleEnum;
import com.zqw.qwpicturebackend.model.enums.SpaceTypeEnum;
import com.zqw.qwpicturebackend.model.vo.SpaceVO;
import com.zqw.qwpicturebackend.model.vo.UserVO;
import com.zqw.qwpicturebackend.service.SpaceService;
import com.zqw.qwpicturebackend.mapper.SpaceMapper;
import com.zqw.qwpicturebackend.service.SpaceUserService;
import com.zqw.qwpicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author zhuqw
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-11-06 20:29:25
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {
    private final ConcurrentHashMap<Long, Object> lockMap = new ConcurrentHashMap<>();

    @Resource
    private UserService userService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private SpaceUserService spaceUserService;

    // 注释掉，不使用分库分表
    // @Resource
    // @Lazy
    // private DynamicShardingManager dynamicShardingManager;

    /**
     * 校验空间是否合法（包括上传/修改空间信息，都需要进行校验）
     *
     * @param space 需要校验的 space 对象
     */
    @Override
    public void validSpace(Space space, boolean isAdd) {
        ThrowUtils.throwif(space == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        Integer spaceType = space.getSpaceType();
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);

        // 判 null（但是如果是更新操作这两个都可能为 null，这也是可能的情况啊！！！！！！！！）
        // 最好的办法就是分情况讨论，如果是添加
        if (isAdd) {
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "图库空间名称错误~");
            }
            if (ObjUtil.isNull(spaceLevel)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "图库空间类型错误错误~");
            }
            if (spaceTypeEnum == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "图库空间类型错误~");
            }
        }

        // 判等级是否存在，修改的名称是否满足长度要求
        ThrowUtils.throwif(spaceName != null && spaceName.length() > 24, ErrorCode.PARAMS_ERROR, "图库名称不能超过24~");
        ThrowUtils.throwif(ObjUtil.isNull(spaceLevelEnum), ErrorCode.PARAMS_ERROR, "没有所选空间类型~");
    }

    /**
     * 获取空间vo对象
     *
     * @param space   空间信息
     * @param request 用于获取用户信息
     * @return
     */
    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        ThrowUtils.throwif(space == null, ErrorCode.PARAMS_ERROR, "找不到图片信息~");
        // 转换为VO对象
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        Long userId = spaceVO.getUserId();
        // 判断用户能否访问图片
        // todo 不需要校验图片是否能够访问吗？
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    /**
     * 批量获取用户空间信息（管理员后门）
     *
     * @param spacePage 未脱敏的空间信息
     * @param request   用于获取用户信息
     * @return
     */
    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        // 判空，长度为0也要判断哦
        if (spaceList == null || spaceList.size() == 0) {
            return spaceVOPage;
        }
        // 获取到所有SpaceVO
        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());
        // 获取到涉及到的所有用户id
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        // 获取到用到的所有用户VO类
        Map<Long, List<UserVO>> userIdUserListMap = userService.listByIds(userIdSet).stream().map(userService::getUserVO).collect(Collectors.groupingBy(UserVO::getId));
        // 封装每个SpaceVO中的UserVO
        spaceVOList.forEach(spaceVO ->
        {
            Long userId = spaceVO.getUserId();
            if (userIdUserListMap.containsKey(userId)) {
                spaceVO.setUser(userIdUserListMap.get(userId).get(0));
            }
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    /**
     * 创建分页查询器
     *
     * @param spaceQueryRequest 查询器
     * @return 返回分页查询器
     */
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        // 查询条件可以为空，为空就是查询所有参数
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }

        // 拼接查询条件
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        Integer spaceType = spaceQueryRequest.getSpaceType();

        // 拼接参数
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceType), "spaceType", spaceType);
        // 拼接排序规则
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), "ascend".equals(sortOrder), sortField);
        return queryWrapper;
    }

    /**
     * 管理员设置的空间信息优先级默认高于枚举中的（可能用户购买了扩容包）
     *
     * @param space 需要进行参数设置的空间
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 根据空间级别设置
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        // 如果为空就证明没有对应的枚举类型
        ThrowUtils.throwif(ObjUtil.isNull(spaceLevelEnum), ErrorCode.PARAMS_ERROR, "没有对应的空间类型~");
        // 如果没有设置限制就用枚举中设置的
        if (space.getMaxSize() == null) {
            space.setMaxSize(spaceLevelEnum.getMaxSize());
        }
        if (space.getMaxCount() == null) {
            space.setMaxCount(spaceLevelEnum.getMaxCount());
        }
    }

    /**
     * 新增空间
     *
     * @param spaceAddRequest 空间信息
     * @param loginUser       需要根据当前用户判断是否哦有权限进行添加空间操作
     * @return 返回新增空间的 id
     */
    @Override
    // @Transactional     使用注解式事务，会导致事务还没有提交锁就提交了，导致可能会有其他线程操作，导致并发问题
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // // 1.获取参数
        // Space space = new Space();
        // BeanUtils.copyProperties(spaceAddRequest, space);
        // // 2.填充空间信息
        // fillSpaceBySpaceLevel(space);
        // space.setUserId(userId);
        // // 3.空间校验
        // validSpace(space, true);
        // // 4.权限校验【想开通非普通空间，还不是管理员】
        // if (!space.getSpaceLevel().equals(SpaceLevelEnum.COMMON.getValue()) && !userService.isAdmin(loginUser)) {
        //     throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "你没有权限创建该空间");
        // }

        // 没有设置空间类型就默认使用普通空间
        if (StrUtil.isBlank(spaceAddRequest.getSpaceName())) {
            spaceAddRequest.setSpaceName("默认空间");
        }
        // 没有设置空间类型就默认使用普通空间
        if (spaceAddRequest.getSpaceLevel() == null) {
            spaceAddRequest.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        // 没有设置空间类型就默认使用私人空间（私人  vs  团队）
        if (spaceAddRequest.getSpaceType() == null) {
            spaceAddRequest.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        // 在此处将实体类和 DTO 进行转换
        Space space = new Space();
        Long userId = loginUser.getId();
        space.setUserId(userId);
        Integer spaceType = spaceAddRequest.getSpaceType();
        BeanUtils.copyProperties(spaceAddRequest, space);
        // 填充数据
        this.fillSpaceBySpaceLevel(space);

        // 5.添加到数据库【事务 + 锁】
        // 5.1查询是否已经创建过了
        // String lock = userId.toString().intern();
        Object lock = lockMap.computeIfAbsent(userId, k -> new Object()); // 确保每个用户只能拿到唯一的一个锁
        // todo 使用锁我能理解，但是为什么要使用事务
        synchronized (lock) {
            // 判断是否已经创建过了
            // 编程式事务，确保先提交事务再释放锁
            Long ret = transactionTemplate.execute(status -> {
                boolean exists = this.lambdaQuery()
                        .eq(Space::getUserId, userId)
                        .eq(Space::getSpaceType, spaceType)
                        .exists();
                ThrowUtils.throwif(exists, ErrorCode.OPERATION_ERROR, "您没有重复创建空间的权限");
                boolean isSave = this.save(space);
                ThrowUtils.throwif(!isSave, ErrorCode.OPERATION_ERROR, "创建" + SpaceTypeEnum.getEnumByValue(spaceType) + "空间失败！！！");
                // 创建成功，将创建者信息添加到空间表中
                if (spaceType.equals(SpaceTypeEnum.TEAM.getValue())) {
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setUserId(userId);
                    spaceUser.setSpaceId(space.getId());
                    spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                    boolean save = spaceUserService.save(spaceUser);
                    ThrowUtils.throwif(!save, ErrorCode.OPERATION_ERROR, "无法将空间创建者添加到团队空间表中");
                }
                // 创建分表，默认私有/团队空间能分
                // dynamicShardingManager.createSpacePictureTable(space);
                return space.getId();
            });
            return Optional.ofNullable(ret).orElse(-1L);
        }
    }

    /**
     * 查询空间信息
     *
     * @param space     空间
     * @param loginUser 当前登录用户
     */
    @Override
    public void checkSpaceAuth(Space space, User loginUser) {
        ThrowUtils.throwif(space == null || loginUser == null, ErrorCode.OPERATION_ERROR, "参数错误");
        if (!loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE) && !space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "您没有权限操作该空间");
        }
    }
}