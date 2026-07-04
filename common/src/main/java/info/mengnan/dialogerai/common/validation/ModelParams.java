package info.mengnan.dialogerai.common.validation;

import info.mengnan.dialogerai.common.param.ModelType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明参数 POJO 绑定的 Provider 与模型类型
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModelParams {

    String provider();

    ModelType[] types();
}
