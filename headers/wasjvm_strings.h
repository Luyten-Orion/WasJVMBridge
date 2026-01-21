#ifndef WASJVM_STRINGS_H
#define WASJVM_STRINGS_H

#include <stdint.h>

/**
 * Creates a java.lang.String from UTF-8 bytes in WASM memory.
 */
__attribute__((import_module("wasjvmbridge")))
__attribute__((import_name("create_string")))
extern int64_t wasjvmbridge_create_string(int32_t ptr, int32_t len);

__attribute__((import_module("wasjvmbridge")))
__attribute__((import_name("get_string_content")))
extern int32_t wasjvmbridge_get_string_content(int64_t handle, int32_t len_out_ptr);

/**
 * Copies a JVM String into an existing buffer.
 */
__attribute__((import_module("wasjvmbridge")))
__attribute__((import_name("get_string_into")))
extern int32_t wasjvmbridge_get_string_into(int64_t handle, int32_t ptr, int32_t max_len);

#endif