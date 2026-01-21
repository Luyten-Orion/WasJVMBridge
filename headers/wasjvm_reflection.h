#ifndef WASJVM_REFLECTION_H
#define WASJVM_REFLECTION_H

#include <stdint.h>

// --- Imports ---
__attribute__((import_module("wasjvmbridge"), import_name("get_class")))
extern int64_t wasjvmbridge_get_class(int32_t name_ptr, int32_t name_len);

__attribute__((import_module("wasjvmbridge"), import_name("get_constructor_id")))
extern int64_t wasjvmbridge_get_constructor_id(int64_t class_handle, int32_t params_ptr, int32_t count);

__attribute__((import_module("wasjvmbridge"), import_name("new_instance")))
extern int64_t wasjvmbridge_new_instance(int64_t ctor_handle, int32_t args_ptr, int32_t count);

__attribute__((import_module("wasjvmbridge"), import_name("get_method_id")))
extern int64_t wasjvmbridge_get_method_id(int64_t class_handle, int32_t name_ptr, int32_t name_len, int32_t params_ptr, int32_t params_count);

__attribute__((import_module("wasjvmbridge"), import_name("call_method_void")))
extern void wasjvmbridge_call_method_void(int64_t obj_handle, int64_t method_id, int32_t args_ptr, int32_t args_count);

__attribute__((import_module("wasjvmbridge"), import_name("call_method_obj")))
extern int64_t wasjvmbridge_call_method_obj(int64_t obj_handle, int64_t method_id, int32_t args_ptr, int32_t args_count);

__attribute__((import_module("wasjvmbridge"), import_name("get_field_id")))
extern int64_t wasjvmbridge_get_field_id(int64_t class_handle, int32_t name_ptr, int32_t name_len);

__attribute__((import_module("wasjvmbridge"), import_name("get_field_obj")))
extern int64_t wasjvmbridge_get_field_obj(int64_t obj_handle, int64_t field_id);

__attribute__((import_module("wasjvmbridge"), import_name("set_field_obj")))
extern void wasjvmbridge_set_field_obj(int64_t obj_handle, int64_t field_id, int64_t value_handle);

#endif