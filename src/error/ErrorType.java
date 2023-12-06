package error;

public enum ErrorType {
    /**
     * 格式字符串中出现非法字符
     */
    a,
    /**
     * 函数名或者变量名在当前作用域下重复定义
     */
    b,
    /**
     * 使用了未定义的标识符
     */
    c,
    /**
     * 函数调用语句中，参数个数与函数定义中的参数个数不匹配
     */
    d,
    /**
     * 函数调用语句中，参数类型与函数定义中对应位置的参数类型不匹配
     */
    e,
    /**
     * 无返回值的函数存在不匹配的return语句
     */
    f,
    /**
     * 有返回值的函数缺少return语句
     */
    g,
    /**
     * 试图修改常量的值
     */
    h,
    /**
     * 缺少分号
     */
    i,
    /**
     * 缺少右小括号’)’
     */
    j,
    /**
     * 缺少右中括号’]’
     */
    k,
    /**
     * printf中格式字符与表达式个数不匹配
     */
    l,
    /**
     * 在非循环块中使用break和continue语句
     */
    m
}
