#ifndef WASJVM_CORE_H
#define WASJVM_CORE_H

#include <stdint.h>

/**
 * Resets the thread-local error state on the JVM side.
 * Should be called before invoking methods if you intend to check for fresh errors.
 */
__attribute__((import_module("wasjvmbridge")))
__attribute__((import_name("flush_error")))
extern void wasjvmbridge_flush_error(void);

/**
 * Returns a handle to the Class of the last exception thrown during a bridge operation.
 * @return Handle (i64) or INVALID_HANDLE (-1) if no error is present.
 */
__attribute__((import_module("wasjvmbridge")))
__attribute__((import_name("get_last_error_handle")))
extern int64_t wasjvmbridge_get_last_error_handle(void);

/**
 * Copies the last error message string into a provided WASM buffer.
 * @param ptr Pointer to the destination buffer.
 * @param max_len Size of the destination buffer.
 * @return Number of bytes actually written.
 */
__attribute__((import_module("wasjvmbridge")))
__attribute__((import_name("get_last_error_message")))
extern int32_t wasjvmbridge_get_last_error_message(int32_t ptr, int32_t max_len);

/**
 * Explicitly removes an object from the JVM global registry to prevent memory leaks.
 * @param obj_handle The handle of the instance to release.
 */
__attribute__((import_module("wasjvmbridge")))
__attribute__((import_name("release_instance")))
extern void wasjvmbridge_release_instance(int64_t obj_handle);

#endif