package com.zqw.qwpicturebackend.exception;

import com.zqw.qwpicturebackend.common.BaseResult;
import com.zqw.qwpicturebackend.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.Serializable;

/**
 * 使用 Spring-apo 中的环绕通知进行异常的捕获
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandle {  // 不需要序列化？

    /**
     * 处理系统异常
     *
     * @param exception 异常信息
     * @return 返回统一结果
     */
    @ExceptionHandler(RuntimeException.class)
    public BaseResult<?> runtimeExceptionHandle(RuntimeException exception) {
        log.error("RuntimeException：" + exception);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR);
    }

    /**
     * todo: 观察这里是不是有顺序要求，是否都被最大的拦截了？
     * 处理自定义异常
     *
     * @param businessException 异常信息
     * @return 返沪通过一结果
     */
    @ExceptionHandler(BusinessException.class)
    public BaseResult<?> businessExceptionHandle(BusinessException businessException) {
        log.error("BusinessException：" + businessException);
        return ResultUtils.error(businessException.getCode(), businessException.getMessage());
    }

}
