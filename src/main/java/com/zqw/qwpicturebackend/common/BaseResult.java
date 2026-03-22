package com.zqw.qwpicturebackend.common;

import com.zqw.qwpicturebackend.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 封装全局响应结果
 *
 * @param <T> 设置响应结果中data 的类型
 */
@Data        // 不添加前端会报 406错误，没有get/set 方法，导致无法解析响应信息
public class BaseResult<T> implements Serializable {

    /**
     * 状态码
     */
    private int code;

    /**
     * 响应信息
     */
    private T data;

    /**
     * 附加的信息
     */
    private String message;

    /**
     * 封装正常带有数据的响应结果
     *
     * @param code    状态码
     * @param data    响应数据
     * @param message 请求信息
     */
    public BaseResult(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    /**
     * 封装正常的对于没有响应信息的请求
     *
     * @param code    状态码
     * @param message 请求信息
     */
    public BaseResult(int code, String message) {
        this(code, null, message);
    }

    /**
     * 封装错误信息
     *
     * @param errorCode 错误码枚举信息
     */
    public BaseResult(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }

    /**
     * 封装错误信息
     *
     * @param errorCode 错误码枚举信息
     * @param message   自定义错误信息
     */
    public BaseResult(ErrorCode errorCode, String message) {
        this(errorCode.getCode(), null, message);
    }
}

