package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.error;

import com.pandaterry.concurrent_entity_change_logger.shared.error.ErrorCode;
import lombok.Getter;

@Getter
public enum LoggingErrorCode implements ErrorCode {

    FAILED_INIT_LOGGING_INFRA(500, "ERR_001", "로깅 인프라 실행에 실패했습니다.")
    ;

    private final int status;
    private final String code;
    private final String message;

    LoggingErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
