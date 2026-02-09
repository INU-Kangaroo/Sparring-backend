package com.kangaroo.sparring.global.security.principal;

import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;

public final class PrincipalResolver {

    private PrincipalResolver() {}

    public static Long resolveUserId(UserIdPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return principal.getUserId();
    }
}
