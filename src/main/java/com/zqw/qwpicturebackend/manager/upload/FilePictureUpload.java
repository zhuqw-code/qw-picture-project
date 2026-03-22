package com.zqw.qwpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.zqw.qwpicturebackend.exception.BusinessException;
import com.zqw.qwpicturebackend.exception.ErrorCode;
import com.zqw.qwpicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * 模板方法的子类
 */
@Component
public class FilePictureUpload extends PictureUploadTemplate {

    /**
     * 对本地上传图片信息进行校验
     *
     * @param inputResource 校验信息来源
     */
    @Override
    public void validPicture(Object inputResource) {
        MultipartFile multipartFile = (MultipartFile) inputResource;
        // 判空
        ThrowUtils.throwif(multipartFile == null, ErrorCode.PARAMS_ERROR, "上传文件不能为空~");
        // 判大小（字节）
        long size = multipartFile.getSize();
        ThrowUtils.throwif(size > ONE_M * 6.6, ErrorCode.OPERATION_ERROR, "图片不能超过2M~");
        // 图片格式校验
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowUtils.throwif(!ALLOW_FORMAT_LIST.contains(suffix), ErrorCode.OPERATION_ERROR, "没有预期图片格式~");
    }


    /**
     * 根据自己的方法获取图片信息
     *
     * @param inputResource 图片来源
     * @return 返回图片的名称
     */
    @Override
    public String getOriginFilename(Object inputResource) {
        MultipartFile multipartFile = (MultipartFile) inputResource;
        return multipartFile.getOriginalFilename();
    }

    /**
     * 重写自己的逻辑
     *
     * @param inputResource 上传的图片来源
     * @param file          本地内春的暂存文件
     */
    @Override
    public void processFile(Object inputResource, File file) {
        MultipartFile multipartFile = (MultipartFile) inputResource;
        try {
            multipartFile.transferTo(file);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传图片失败~");
        }
    }
}
