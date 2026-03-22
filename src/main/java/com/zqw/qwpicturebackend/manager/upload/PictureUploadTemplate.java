package com.zqw.qwpicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import com.zqw.qwpicturebackend.config.CosClientConfig;
import com.zqw.qwpicturebackend.exception.BusinessException;
import com.zqw.qwpicturebackend.exception.ErrorCode;
import com.zqw.qwpicturebackend.exception.ThrowUtils;
import com.zqw.qwpicturebackend.manager.CosManager;
import com.zqw.qwpicturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 抽象父类，将通用方法提取为非抽象方法，其余抽取为抽象方法
 */
@Slf4j
public abstract class PictureUploadTemplate {

    /**
     * COS 配置信息
     */
    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 上传文件的客户端
     */
    @Resource
    private CosManager cosManager;

    /**
     * 图片大小限制
     */
    public static final long ONE_M = 1024 * 1024;

    /**
     * 合法图片格式
     */
    public static final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpg", "png", "jpeg", "webp");


    /**
     * 供子类实现的校验方法
     *
     * @param inputResource 校验信息来源
     */
    public abstract void validPicture(Object inputResource);

    /**
     * 根据上传图片来源设置图片上传目录
     *
     * @param inputResource 图片来源
     * @return 上传到COS的路径
     */
    public abstract String getOriginFilename(Object inputResource);

    /**
     * 供子类将上传的图片来源存到本地内存中，便于传递到 COS中
     *
     * @param inputResource 上传的图片来源
     * @param file          本地内春的暂存文件
     */
    public abstract void processFile(Object inputResource, File file);

    /**
     * 文件上传
     *
     * @param inputResource    上传的数据源（本地文件/网络图片url）
     * @param uploadPathPrefix /public 还是用户私有路径
     * @return 返回封装后的图片信息
     */
    public UploadPictureResult uploadLoadPicture(Object inputResource, String uploadPathPrefix) {
        // todo 校验文件是否合法
        validPicture(inputResource);
        String uuid = UUID.randomUUID().toString();
        // todo 设置文件上传路径
        String originalFilename = getOriginFilename(inputResource);
        // todo 对originFilename进行设置，因为我们本地无法生成带有特殊符号的路径
        //  因为图片路径中的&符号，导致无法再本地内存中创建文件，我们应该对文件路径进行设置
        //   并且不必考虑图片是否能够拿到，因为我们是直接根据 inputSource拿图片的
        int idx = originalFilename.indexOf("?");
        if (idx > -1) {
            originalFilename = originalFilename.substring(0, idx);
        }
        String filename = String.format("%s_%s_%s", LocalDate.now(), uuid, originalFilename);
        String uploadPath = String.format("%s/%s", uploadPathPrefix, filename);
        // 文件上传（先写在内存中，在调用 CosManager 的putPictureObject 方法上传）
        File file = null;
        try {
            // todo 文件获取,这里需要取出？后的参数吗
            file = File.createTempFile(uploadPath, null);
            processFile(inputResource, file);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            return analyzeCosReturn(originalFilename, uploadPath, putObjectResult);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名、目录名或卷标语法不正确~");
        } finally {
            deleteTemplate(file);
        }
    }

    private UploadPictureResult analyzeCosReturn(String originalFilename, String uploadPath, PutObjectResult putObjectResult) {
        ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

        // todo 使用压缩图片的url
        ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
        if (processResults == null) {
            return buildResult(originalFilename, uploadPath, imageInfo);
        }
        List<CIObject> objectList = processResults.getObjectList();
        // 如果不为空就将设置第一个规则（压缩）的信息取出设置为返回对象
        if (CollUtil.isNotEmpty(objectList)) {
            CIObject ciObject = objectList.get(0);
            // 获取缩略信息
            // 如果没有缩略图就使用压缩图
            CIObject thumbnailCiObject = ciObject;
            if (objectList.size() > 1) {
                thumbnailCiObject = objectList.get(1);
            }
            // webp 直接使用压缩后的图片路径
            return buildResult(originalFilename, ciObject, thumbnailCiObject, imageInfo);
        }
        // 原始涂片路径
        return buildResult(originalFilename, uploadPath, imageInfo);
    }

    private UploadPictureResult buildResult(String originalFilename, CIObject ciObject, CIObject thumbnailCiObject,
                                            ImageInfo imageInfo) {
        // 封装放回结果
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int width = ciObject.getWidth();
        int height = ciObject.getHeight();
        Double picScale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();
        uploadPictureResult.setUrl(String.format("%s/%s", cosClientConfig.getHost(), ciObject.getKey()));
        // todo 将COS 中缩略图路径带上返回
        uploadPictureResult.setThumbnailUrl(String.format("%s/%s", cosClientConfig.getHost(), thumbnailCiObject.getKey()));
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize(ciObject.getQuality().longValue());
        uploadPictureResult.setWidth(width);
        uploadPictureResult.setHeight(height);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(ciObject.getFormat());
        uploadPictureResult.setPicColor(imageInfo.getAve());
        return uploadPictureResult;
    }

    private UploadPictureResult buildResult(String originalFilename, String uploadPath, ImageInfo imageInfo) {
        // 封装放回结果
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int width = imageInfo.getWidth();
        int height = imageInfo.getHeight();
        Double picScale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        // 我们要记录真实存储的文件大小（压缩图/缩略图）
        uploadPictureResult.setPicSize((long) imageInfo.getQuality());
        uploadPictureResult.setWidth(width);
        uploadPictureResult.setHeight(height);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        // 添加图片主色调
        uploadPictureResult.setPicColor(imageInfo.getAve());
        return uploadPictureResult;
    }


    /**
     * 释放本地内存文件
     *
     * @param file 本地的内存文件
     */
    private static void deleteTemplate(File file) {
        // 释放本地资源
        if (file != null) {
            boolean isDelete = file.delete();
            if (!isDelete) {
                log.info("上传图片信息失败：filepath={}", file.getAbsoluteFile());
            }
        }
    }
}
