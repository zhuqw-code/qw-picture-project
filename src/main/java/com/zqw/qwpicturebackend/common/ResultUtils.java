package com.zqw.qwpicturebackend.common;

import com.zqw.qwpicturebackend.exception.BusinessException;
import com.zqw.qwpicturebackend.exception.ErrorCode;

/**
 * 通过编写工具类对响应数据进行 BaseResult的封装
 */
public class ResultUtils {  // 如果金泰方法需要使用泛型，该方法的类上不需要添加泛型

    /**
     * 封装成功请求后的响应信息
     *
     * @param <T> 传递的参数的类型
     * @return 返回封装后的信息
     */
    public static <T> BaseResult<T> success() {
        return new BaseResult<T>(0, "请求成功~");
    }

    /**
     * 封装成功请求后的响应信息
     *
     * @param data 响应信息
     * @param <T>  传递的参数的类型
     * @return 返回封装后的信息
     */
    public static <T> BaseResult<T> success(T data) {    // 只要有形参使用泛型，static后面都要添加对应泛型
        return new BaseResult<T>(0, data, "请求成功~");
    }

    /**
     * 封装成功请求后的响应信息
     *
     * @param data    响应信息
     * @param message 成功的信息
     * @param <T>     传递的参数的类型
     * @return 返回封装后的信息
     */
    public static <T> BaseResult<T> success(T data, String message) {    // 只要有形参使用泛型，static后面都要添加对应泛型
        return new BaseResult<T>(0, data, message);
    }

    /**
     * 根据自定义错误码封装响应信息
     *
     * @param code    错误码
     * @param message 错误信息
     * @return 返回统一包装类
     */
    public static BaseResult<?> error(int code, String message) {
        return new BaseResult<>(code, message);
    }

    /**
     * 根据自定义错误码封装响应信息
     *
     * @param errorCode 自定义错误码
     * @return 返回统一包装类
     */
    public static BaseResult<?> error(ErrorCode errorCode) {
        return new BaseResult<>(errorCode);
    }

    /**
     * 根据自定义错误码封装响应信息
     *
     * @param errorCode 自定义错误码
     * @param message   自定义更具体的错误信息
     * @return 返回统一包装类
     */
    public static BaseResult<?> error(ErrorCode errorCode, String message) {
        return new BaseResult<>(errorCode, message);
    }

}
