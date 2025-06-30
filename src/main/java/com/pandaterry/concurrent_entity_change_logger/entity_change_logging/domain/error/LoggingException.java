package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.error;

import com.pandaterry.concurrent_entity_change_logger.shared.error.BaseException;
import com.pandaterry.concurrent_entity_change_logger.shared.error.ErrorCode;

public class LoggingException extends BaseException {
    protected LoggingException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected LoggingException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode, messageArgs);
    }

    protected LoggingException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public static LoggingException of(ErrorCode errorCode, Throwable e){
        return new LoggingException(errorCode, e);
    }
}
