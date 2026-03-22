package com.zqw.qwpicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.zqw.qwpicturebackend.config.CosClientConfig;
import com.zqw.qwpicturebackend.exception.BusinessException;
import com.zqw.qwpicturebackend.exception.ErrorCode;
import com.zqw.qwpicturebackend.exception.ThrowUtils;
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
 * 在对 CosManager 进行封装，因为返回的信息与我们数据库中信息不符，编写一个增强类
 */
@Slf4j
@Deprecated
public class FileManager {

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
    private static final long ONE_M = 1024 * 1024;

    /**
     * 合法图片格式
     */
    private static final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpg", "png", "jpeg", "webp");


    /**
     * 文件上传
     *
     * @param multipartFile    上传的文件
     * @param uploadPathPrefix /public 还是用户私有路径
     * @return 返回封装后的图片信息
     */
    public UploadPictureResult uploadLoadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        // 校验文件是否合法
        validPicture(multipartFile);
        // 设置文件上传路径
        String uuid = UUID.randomUUID().toString();
        String originalFilename = multipartFile.getOriginalFilename();
        String filename = String.format("%s_%s.%s", LocalDate.now(), uuid, originalFilename);
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, filename);
        // 文件上传（先写在内存中，在调用 CosManager 的putPictureObject 方法上传）
        File file = null;
        try {
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            // 封装放回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            int width = imageInfo.getWidth();
            int height = imageInfo.getHeight();
            Double picScale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setWidth(width);
            uploadPictureResult.setHeight(height);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            return uploadPictureResult;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            deleteTemplate(file);
        }
    }

    /**
     * 释放本地内存文件
     *
     * @param file
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

    /**
     * 图片信息校验
     *
     * @param multipartFile 需校验的文件
     */
    private void validPicture(MultipartFile multipartFile) {
        // 判空
        ThrowUtils.throwif(multipartFile == null, ErrorCode.PARAMS_ERROR, "上传文件不能为空~");
        // 判大小（字节）
        long size = multipartFile.getSize();
        ThrowUtils.throwif(size > ONE_M * 2, ErrorCode.OPERATION_ERROR, "图片不能超过2M~");
        // 图片格式校验
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowUtils.throwif(!ALLOW_FORMAT_LIST.contains(suffix), ErrorCode.OPERATION_ERROR, "没有预期图片格式~");
    }


    /**
     * 文件上传
     *
     * @param url              文件路径
     * @param uploadPathPrefix /public 还是用户私有路径
     * @return 返回封装后的图片信息
     */
    public UploadPictureResult uploadLoadPictureByUrl(String url, String uploadPathPrefix) {
        // todo 校验文件是否合法
        validPicture(url);
        // 设置文件上传路径
        String uuid = UUID.randomUUID().toString();
        // todo 根据url获取文件名称，创建文件夹
        String originalFilename = FileUtil.mainName(url);
        String filename = String.format("%s_%s.%s", LocalDate.now(), uuid, originalFilename);
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, filename);
        // 文件上传（先写在内存中，在调用 CosManager 的putPictureObject 方法上传）
        File file = null;
        try {
            file = File.createTempFile(uploadPath, null);
            // todo 获取图片信息，用于上传到 COS中
            HttpUtil.downloadFile(url, file);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            // 封装放回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            int width = imageInfo.getWidth();
            int height = imageInfo.getHeight();
            Double picScale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setWidth(width);
            uploadPictureResult.setHeight(height);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            return uploadPictureResult;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            deleteTemplate(file);
        }
    }

    /**
     * 校验该url对应图片是否合法
     *
     * @param url 待上传图片url
     */
    private void validPicture(String url) {
        // 校验是否为空
        ThrowUtils.throwif(StrUtil.isBlank(url), ErrorCode.PARAMS_ERROR, "Url不合法~");
        // 校验文件url（如果不报错就证明文件路径正常）
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        // 校验是否遵循http/https
        ThrowUtils.throwif(!url.startsWith("http://") && !url.startsWith("https://"),
                ErrorCode.PARAMS_ERROR, "请求协议不合法~");
        // 发送head请求（告别传统get/post方法访问图片路径直接下载，我们使用head请求）
        try (HttpResponse response = HttpUtil.createRequest(Method.HEAD, url).execute()) {
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return;    // 可能不支持，也可能有误
            }
            // 校验文件大小
            String contentType = response.header("Content-type");
            if (StrUtil.isBlank(contentType)) {
                // 图片类型百名单
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/webp", "image/png");
                if (!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片格式不支持~");
                }
            }
            // 校验文件格式
            String contentLengthStr = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    // 如果太大就会抛出异常
                    long contentLength = Long.parseLong(contentLengthStr);
                    ThrowUtils.throwif(contentLength > ONE_M * 2, ErrorCode.PARAMS_ERROR, "文件大小不能超过2M~");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小错误~");
                }
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "无法获取Head请求结果~");
        }
    }
}
