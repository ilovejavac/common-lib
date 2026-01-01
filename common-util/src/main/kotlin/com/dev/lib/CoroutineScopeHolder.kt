package com.dev.lib

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 协程作用域静态工具类，统一管理全局 CoroutineScope
 */
object CoroutineScopeHolder {
    @Volatile
    private var GLOBAL_SCOPE: CoroutineScope? = null

    /**
     * 获取全局协程作用域（非空断言：确保初始化后调用）
     */
    @JvmStatic
    fun getGlobalScope(): CoroutineScope =
        GLOBAL_SCOPE ?: throw IllegalStateException("全局 CoroutineScope 未初始化！")

    fun launch(block: suspend CoroutineScope.() -> Unit) = getGlobalScope().launch(block = block)

    fun <T> async(block: suspend CoroutineScope.() -> T) = getGlobalScope().async(block = block)

    /**
     * 初始化全局协程作用域（仅由 Spring 配置类调用）
     */
    internal fun initGlobalScope(scope: CoroutineScope) {
        if (GLOBAL_SCOPE != null) {
            throw IllegalStateException("全局 CoroutineScope 已初始化，禁止重复赋值！")
        }
        GLOBAL_SCOPE = scope
    }

    /**
     * 销毁全局协程作用域（应用关闭时调用）
     */
    internal fun destroyGlobalScope() {
        GLOBAL_SCOPE?.cancel("应用关闭，取消全局协程作用域")
        GLOBAL_SCOPE = null
    }
}