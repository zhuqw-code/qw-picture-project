package com.zqw.qwpicturebackend.manager;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.zqw.qwpicturebackend.config.CosClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;

/**
 * 上传对象到COS，通过向 CosClient传送 PutObjectRequest对象
 */
@Component
@Slf4j
public class CosManager {

    /**
     * 上传的 COS配置信息
     */
    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 上传文件的客户端
     */
    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     *
     * @param key  存储位置
     * @param file 上传文件
     * @return 一些图片的加密信息，创建时间，元信息啥的没有用
     * @throws CosClientException
     * @throws CosServiceException
     */
    public PutObjectResult putObject(String key, File file) throws
            CosClientException, CosServiceException {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 通过创建请求参数 PutObjectRequest  -> AbstractPutObjectRequest  -> CosServiceRequest
     *
     * @param key  路径信息
     * @param file 图片信息
     * @return 返回文件信息
     * @throws CosClientException
     * @throws CosServiceException
     */
    public PutObjectResult putPictureObject(String key, File file) throws
            CosClientException, CosServiceException {
        // 创建请求信息
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        // 设置返回参数，并交给请求信息
        PicOperations picOperations = new PicOperations();
        picOperations.setIsPicInfo(1);  // 默认0，不返返回上传的图片信息
        /* 这里为了方便就不进行压缩和缩略了
        List<PicOperations.Rule> rules = new ArrayList<>();
        // 设置压缩参数
        String webpKey = FileUtil.mainName(key) + ".webp";  // 文件名称

        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setRule("imageMogr2/format/webp");
        compressRule.setFileId(webpKey);
        rules.add(compressRule);

        // 设置缩略图（过小就不设置）
        if (file.length() > 2 * 1024) {
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            String thumbnailKey = FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key);
            thumbnailRule.setFileId(thumbnailKey);
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 256, 256)); // 缩放具体规则
            rules.add(thumbnailRule);
        }
        // 将规则设置进去
        picOperations.setRules(rules);*/
        putObjectRequest.setPicOperations(picOperations);
        // 发送请求信息
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 删除对象存储图片信息
     *
     * @param pictureUrl 图片路径信息
     */
    public void deleteObject(String pictureUrl) {
        cosClient.deleteObject(cosClientConfig.getBucket(), pictureUrl);
        log.info("删除路径为{}的图片", pictureUrl);
    }
}
