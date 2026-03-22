package com.zqw.qwpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.zqw.qwpicturebackend.exception.BusinessException;
import com.zqw.qwpicturebackend.exception.ErrorCode;
import com.zqw.qwpicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * 模板方法的子类
 */
@Component
public class UrlPictureUpload extends PictureUploadTemplate {

    /**
     * 对图片信息来源进行校验
     *
     * @param inputResource 校验信息来源
     */
    @Override
    public void validPicture(Object inputResource) {
        String url = (String) inputResource;
        // 校验是否为空
        ThrowUtils.throwif(StrUtil.isBlank(url), ErrorCode.PARAMS_ERROR, "Url不合法~");
        // 校验文件url（如果不报错就证明文件路径正常）
        try {
            // todo
            //  https://img-baofun.zhhainiao.com/pcwallpaper_ugc_mobile/preview/476ee03957c864acc1055876d5eafe13_preview_mid.jpg
            new URL(url);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件格式错误~");
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


    /**
     * 通过图片url获取到图片的名称，方便在COS中设置目录
     *
     * @param inputResource 图片来源
     * @return 返回获取到的信息
     */
    @Override
    public String getOriginFilename(Object inputResource) {
        String url = (String) inputResource;
        return FileUtil.mainName(url);
    }

    /**
     * 使用 Hutool 工具类通过url获取图片信息并存储到本地内春中，为后续上传到COS中做准备
     *
     * @param inputResource 上传的图片来源
     * @param file          本地内春的暂存文件
     */
    @Override
    public void processFile(Object inputResource, File file) {
        HttpUtil.downloadFile((String) inputResource, file);
    }
}
