package com.weacsoft.jaravel.app.event;

import com.weacsoft.jaravel.vendor.event.Event;

/**
 * 用户注册事件，对齐 Laravel 的 {@code UserRegisteredEvent}。
 * <p>
 * 通过 {@code Dispatcher.dispatch(new UserRegisteredEvent(...))} 分发，
 * 监听器可通过 {@code @ListensTo(UserRegisteredEvent.class)} 注册。
 */
public class UserRegisteredEvent implements Event {

    private final Long userId;
    private final String userName;

    public UserRegisteredEvent(Long userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }
}
