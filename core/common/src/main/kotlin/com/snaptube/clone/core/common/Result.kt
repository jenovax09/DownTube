package com.snaptube.clone.core.common

/** Minimal result wrapper shared by the extraction and download layers. */
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Error(val message: String, val cause: Throwable? = null) : AppResult<Nothing>
}

inline fun <T> runCatchingResult(block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (t: Throwable) {
    AppResult.Error(t.message ?: t.javaClass.simpleName, t)
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.data
