package lombok;

public @interface vals {
	val[] value() default {};
}
