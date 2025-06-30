package com.pandaterry.concurrent_entity_change_logger.shared.error;

import lombok.Getter;

@Getter
public abstract class BaseException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Object[] messageArgs;

    protected BaseException(ErrorCode errorCode) {
        this(errorCode, (Object[]) null);
    }

    protected BaseException(ErrorCode errorCode, Object... messageArgs) {
        super(formatMessage(errorCode, messageArgs));
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
    }

    protected BaseException(ErrorCode errorCode, Throwable cause) {
        super(formatMessage(errorCode, null), cause);
        this.errorCode = errorCode;
        this.messageArgs = null;
    }

    private static String formatMessage(ErrorCode errorCode, Object[] args) {
        if (args == null || args.length == 0) {
            return errorCode.getFullMessage();
        }
        return String.format(errorCode.getFullMessage(), args);
    }

    public int getHttpStatus() {
        return errorCode.getStatus();
    }
}
