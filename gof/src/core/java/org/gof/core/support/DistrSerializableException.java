package org.gof.core.support;

/**
 * 当输出流发现不能串行化的对象后会抛出此异常
 */
public class DistrSerializableException extends RuntimeException {
    private static final long serialVersionUID = 1;

    public DistrSerializableException(String str, Object... params) {
        super(Utils.createStr(str, params));
    }
}