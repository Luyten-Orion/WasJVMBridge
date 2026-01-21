#ifndef WASJVM_COLLECTIONS_H
#define WASJVM_COLLECTIONS_H

#include <stdint.h>
#include <stdbool.h>

/**
 * Type tags used in the JvmList header.
 */
typedef enum {
    TYPE_HANDLE  = 0, // int64_t
    TYPE_CHAR    = 1, // struct { i32 ptr, i32 len } (UTF-8)
    TYPE_I8      = 2, // int8_t
    TYPE_I16     = 3, // int16_t
    TYPE_I32     = 4, // int32_t
    TYPE_I64     = 5, // int64_t
    TYPE_F32     = 6, // float
    TYPE_F64     = 7, // double
    TYPE_BOOLEAN = 8  // uint8_t (bool)
} wasjvm_type_t;

/**
 * Represents the structure written by pull_list_elements.
 */
typedef struct {
    int32_t type_tag;  // wasjvm_type_t
    int32_t count;     // Number of elements
    uint8_t data[];    // Flexible array member - cast to appropriate primitive type
} wasjvm_list_header_t;

/**
 * Calculates the total bytes required for a JVM collection when serialized.
 * Includes 8 bytes for the header plus the packed primitive data.
 */
__attribute__((import_module("wasjvmbridge"))) __attribute__((import_name("get_encoded_list_size")))
extern int32_t wasjvmbridge_get_encoded_list_size(int64_t list_handle);

/**
 * Serializes a JVM collection/array into WASM memory.
 * Ensure the buffer at ptr is large enough (use get_encoded_list_size).
 * @return Number of elements written.
 */
__attribute__((import_module("wasjvmbridge"))) __attribute__((import_name("pull_list_elements")))
extern int32_t wasjvmbridge_pull_list_elements(int64_t list_handle, int32_t ptr);

#endif