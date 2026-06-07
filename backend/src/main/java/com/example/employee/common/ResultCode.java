package com.example.employee.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    BAD_REQUEST(400),
    CAPTCHA_ERROR(1001),
    USER_NOT_FOUND(1002),
    PASSWORD_ERROR(1003),
    USER_LOCKED(1004),
    USER_DISABLED(1005),
    TOKEN_EXPIRED(1006),
    TOKEN_INVALID(1007),
    FIRST_LOGIN(1008),
    SESSION_KICKED(1009);

    private final Integer code;
}
