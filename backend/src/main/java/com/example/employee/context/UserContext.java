package com.example.employee.context;

import com.example.employee.dto.UserInfoDTO;

public class UserContext {

    private static final ThreadLocal<UserInfoDTO> USER_HOLDER = new ThreadLocal<>();

    public static void setCurrentUser(UserInfoDTO userInfo) {
        USER_HOLDER.set(userInfo);
    }

    public static UserInfoDTO getCurrentUser() {
        return USER_HOLDER.get();
    }

    public static void clear() {
        USER_HOLDER.remove();
    }
}
