package com.dev.lib.util.statemachine;

import lombok.Data;

/**
 * 状态机上下文基类
 * 业务可继承此类添加自定义字段
 *
 * @param <S> 状态类型
 */
@Data
public abstract class StateMachineContext<S> {

    /**
     * 当前状态
     */
    private S currentState;

    /**
     * 状态变更次数
     */
    private int transitionCount = 0;

    /**
     * 最后一次状态变更时间戳
     */
    private long lastTransitionTime;

    /**
     * 更新状态
     */
    public void updateState(S newState) {

        this.currentState = newState;
        this.transitionCount++;
        this.lastTransitionTime = System.currentTimeMillis();
    }

}