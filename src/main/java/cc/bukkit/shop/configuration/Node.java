package cc.bukkit.shop.configuration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Node {
  String value();

  boolean rewrite() default false;
  
  String ref() default "";
  
  NodeType type() default NodeType.CONFIG;
}
