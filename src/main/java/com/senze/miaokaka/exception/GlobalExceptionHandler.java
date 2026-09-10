package com.senze.miaokaka.exception;


import com.senze.miaokaka.common.BaseResponse;
import com.senze.miaokaka.common.ErrorCode;
import com.senze.miaokaka.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>

 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验失败：返回第一条校验信息
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<?> validationExceptionHandler(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(org.springframework.validation.FieldError::getDefaultMessage)
                .orElse(ErrorCode.PARAMS_ERROR.getMessage());
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, message);
    }

    /**
     * 数据库唯一键冲突（如并发重复打卡）
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public BaseResponse<?> duplicateKeyExceptionHandler(DuplicateKeyException e) {
        log.warn("DuplicateKeyException: {}", e.getMessage());
        return ResultUtils.error(ErrorCode.OPERATION_ERROR, "重复操作，请勿重复提交");
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }
}
