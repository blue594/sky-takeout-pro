package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target(ElementType.METHOD)  //表示 AutoFill 这个自定义注解只能应用在方法上
@Retention(RetentionPolicy.RUNTIME) //表示 AutoFill 注解在运行时保留,通俗点就是让方法前的@AutoFill生效
public @interface AutoFill {
    OperationType value();
}
