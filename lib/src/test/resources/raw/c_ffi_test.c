#include <stdint.h>
#include <stdbool.h>

typedef __SIZE_TYPE__ size_t;

// --- Module Headers ---
#include "wasjvm_core.h"
#include "wasjvm_reflection.h"
#include "wasjvm_strings.h"
#include "wasjvm_collections.h"

// --- Minimal Freestanding Helpers ---

static uint8_t __heap[1024 * 128];
static uint32_t __heap_ptr = 0;

__attribute__((export_name("malloc"), no_builtin))
void* malloc(size_t size) {
    __heap_ptr = (__heap_ptr + 7) & ~7; // Align to 8 bytes
    uint32_t old = __heap_ptr;
    __heap_ptr += (uint32_t)size;
    return (void*)&__heap[old];
}

static int32_t internal_strlen(const char* s) {
    int32_t len = 0;
    while (s[len]) len++;
    return len;
}

// --- Reflection Tests ---

/**
 * Verifies Class resolution, Constructor ID, and Scalar Instantiation.
 * Note: get_instance_id and wasjvm_result_t are gone.
 */
__attribute__((export_name("test_reflection_full_flow")))
int64_t test_reflection_full_flow() {
    const char* name = "quest.yu_vitaqua_fer_chronos.wasjvmbridge.TestTarget";
    int64_t class_h = wasjvmbridge_get_class((int32_t)name, 52);
    if (class_h == -1) return -1;

    int64_t ctor_h = wasjvmbridge_get_constructor_id(class_h, 0, 0);

    // Call returns ONLY the i64 Instance Handle
    int64_t inst_h = wasjvmbridge_new_instance(ctor_h, 0, 0);

    if (inst_h <= 0) return -2;

    const char* f_name = "message";
    int64_t field_h = wasjvmbridge_get_field_id(class_h, (int32_t)f_name, 7);

    // Set field to null (handle 0)
    wasjvmbridge_set_field_obj(inst_h, field_h, 0);

    return inst_h;
}

// --- String Tests ---

/**
 * Verifies getting a string from Java via out-parameter for length.
 */
__attribute__((export_name("test_string_logic")))
int32_t test_string_logic(int64_t handle) {
    int32_t len = 0;
    // Returns ptr (i32), writes length to &len
    int32_t ptr = wasjvmbridge_get_string_content(handle, (int32_t)&len);

    if (ptr == 0 || len <= 0) return -1;

    char* str_ptr = (char*)ptr;
    if (str_ptr[0] != 'P') return -2;

    const char* hello = "C-Bridge";
    // Create new string and return its handle cast to i32
    return (int32_t)wasjvmbridge_create_string((int32_t)hello, 8);
}

// --- Collection Tests ---

/**
 * Verifies C-packed contiguous memory sum for I32.
 */
__attribute__((export_name("test_collection_sum")))
int32_t test_collection_sum(int64_t list_h) {
    int32_t size = wasjvmbridge_get_encoded_list_size(list_h);
    wasjvm_list_header_t* list = (wasjvm_list_header_t*)malloc(size);

    wasjvmbridge_pull_list_elements(list_h, (int32_t)list);

    if (list->type_tag != TYPE_I32) return -99;

    int32_t sum = 0;
    int32_t* data = (int32_t*)list->data;
    for (int i = 0; i < list->count; i++) {
        sum += data[i];
    }
    return sum;
}

/**
 * Verifies 1-byte packed boolean interop.
 */
__attribute__((export_name("test_collection_bool_check")))
int32_t test_collection_bool_check(int64_t list_h) {
    int32_t size = wasjvmbridge_get_encoded_list_size(list_h);
    wasjvm_list_header_t* list = (wasjvm_list_header_t*)malloc(size);

    wasjvmbridge_pull_list_elements(list_h, (int32_t)list);

    if (list->type_tag != TYPE_BOOLEAN) return -1;

    uint8_t* bools = (uint8_t*)list->data;
    for (int i = 0; i < list->count; i++) {
        if (bools[i] == 0) return 0;
    }
    return 1;
}

// --- Core/Error Tests ---

/**
 * Verifies exception trapping and message extraction.
 */
__attribute__((export_name("test_core_error_trap")))
int32_t test_core_error_trap() {
    // 1. Setup handles manually in C to ensure alignment
    const char* name = "quest.yu_vitaqua_fer_chronos.wasjvmbridge.TestTarget";
    int64_t class_h = wasjvmbridge_get_class((int32_t)name, 52);

    int64_t ctor_h = wasjvmbridge_get_constructor_id(class_h, 0, 0);
    int64_t inst_h = wasjvmbridge_new_instance(ctor_h, 0, 0);

    const char* m_name = "crash";
    int64_t method_id = wasjvmbridge_get_method_id(class_h, (int32_t)m_name, 5, 0, 0);

    // 2. Perform the actual error test
    wasjvmbridge_flush_error();
    wasjvmbridge_call_method_void(inst_h, method_id, 0, 0);

    int64_t err_h = wasjvmbridge_get_last_error_handle();
    if (err_h == -1) return -99; // Error: No exception was trapped

    char msg[128];
    // Return the length of the error message to Java
    return wasjvmbridge_get_last_error_message((int32_t)msg, 128);
}