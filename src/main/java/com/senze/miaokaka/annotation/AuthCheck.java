package com.senze.miaokaka.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解（由 JwtInterceptor 执行）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {

    /**
     * 必须具备的角色（如 admin），为空则只要求登录
     */
    String mustRole() default "";
}
