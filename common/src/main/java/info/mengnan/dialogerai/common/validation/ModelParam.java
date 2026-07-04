package info.mengnan.dialogerai.common.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 模型参数字段元数据：前端 Schema、默认值与校验规则均由此注解声明
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModelParam {

    String name() default "";

    String description() default "";

    boolean required() default false;

    String defaultValue() default "";

    double min() default Double.NaN;

    double max() default Double.NaN;
}
