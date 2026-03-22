package com.zqw.qwpicturebackend.controller;

import com.zqw.qwpicturebackend.annotation.AuthCheck;
import com.zqw.qwpicturebackend.common.BaseResult;
import com.zqw.qwpicturebackend.common.ResultUtils;
import com.zqw.qwpicturebackend.constant.UserConstant;
import com.zqw.qwpicturebackend.exception.BusinessException;
import com.zqw.qwpicturebackend.exception.ErrorCode;
import com.zqw.qwpicturebackend.manager.CosManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Resource
    private CosManager cosManager;

    /**
     * 测试对象存储是否可用
     *
     * @param multipartFile 上传文件
     * @return 返会上传后的图片路径
     */
    @PostMapping("/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResult<?> uploadFile(@RequestPart MultipartFile multipartFile) {
        // 需要key 和 filename
        String filename = multipartFile.getOriginalFilename();
        String filepath = String.format("/test/%s", filename);

        File file = null;
        try {
            // 为了将 multipartFile 转换为 File 类型文件，我们在内存中创建file文件
            file = File.createTempFile(filepath, null);
            multipartFile.transferTo(file);
            cosManager.putObject(filepath, file);
            return ResultUtils.success(filepath);
        } catch (IOException e) {
            log.error("文件上传到COS失败：" + e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败~");
        } finally {
            if (file != null) {
                boolean delete = file.delete();
                if (!delete) {
                    log.error("内存文件删除失败：filepath = {}", filepath);
                }
            }
        }
    }
}
