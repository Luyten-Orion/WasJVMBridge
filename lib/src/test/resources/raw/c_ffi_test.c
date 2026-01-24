#include <stdint.h>
#include <stdbool.h>

typedef __SIZE_TYPE__ size_t;

// Include your generated header
#include "wasjvm_autogen.h"

// --- Minimal Bump Allocator ---
static uint8_t __heap[1024 * 128];
static uint32_t __heap_ptr = 2048; // Start after data section

__attribute__((export_name("malloc")))
void* malloc(size_t size) {
    uint32_t old = __heap_ptr;
    __heap_ptr += (uint32_t)size;
    return (void*)&__heap[old];
}

// --- Test 1: Full Reflection Flow ---
__attribute__((export_name("test_reflection_full_flow")))
jvm_handle_t test_reflection_full_flow() {
    const char* name = "quest.yu_vitaqua_fer_chronos.wasjvmbridge.TestTarget";
    jvm_handle_t class_h = wasjvmb_reflection_get_class((int32_t)name, 52);
    if (class_h <= 0) return -1;

    // 1. Create instance
    jvm_handle_t ctor_h = wasjvmb_reflection_get_constructor_id(class_h, 0, 0);
    jvm_handle_t inst_h = wasjvmb_reflection_new_instance(ctor_h, 0, 0);
    if (inst_h <= 0) return -2;

    // 2. Get Field ID for "message"
    const char* f_name = "message";
    jvm_handle_t field_h = wasjvmb_reflection_get_field_id(class_h, (int32_t)f_name, 7);

    // 3. Set the field to NULL (0)
    wasjvmb_reflection_set_field_obj(inst_h, field_h, 0);

    return inst_h;
}

// --- Test 2: String Logic ---
__attribute__((export_name("test_string_logic")))
jvm_handle_t test_string_logic(jvm_handle_t handle) {
    int32_t len = 0;
    // Returns i32 ptr, writes length to &len
    int32_t ptr = wasjvmb_strings_get_string_content(handle, (int32_t)&len);

    const char* result_str = "C-Bridge";
    return wasjvmb_strings_create_string((int32_t)result_str, 8);
}

// --- Test 3: Collection Sum ---
__attribute__((export_name("test_collection_sum")))
int32_t test_collection_sum(jvm_handle_t list_h) {
    int32_t size = wasjvmb_collections_get_encoded_list_size(list_h);
    int32_t* buffer = (int32_t*)malloc(size);

    wasjvmb_collections_pull_list_elements(list_h, (int32_t)buffer);

    // Header: [Tag i32][Count i32]
    int32_t count = buffer[1];
    int32_t sum = 0;
    for (int i = 0; i < count; i++) {
        sum += buffer[2 + i];
    }
    return sum;
}

// --- Test 4: Core Error Trap ---
__attribute__((export_name("test_core_error_trap")))
int32_t test_core_error_trap() {
    wasjvmb_core_flush_error();

    // TRIGGER AN ERROR: Try to get a non-existent class
    const char* fake_class = "this.does.not.Exist";
    wasjvmb_reflection_get_class((int32_t)fake_class, 19);

    char buf[128];
    // Now this should return > 0 because reflection_get_class failed
    return wasjvmb_core_get_last_error_message((int32_t)buf, 128);
}

// --- Test 5: HostStruct Class Definition using Correct Accessor Names ---
__attribute__((export_name("test_class_definition")))
jvm_handle_t test_class_definition() {
    const char* name = "quest.yu_vitaqua_fer_chronos.wasjvmbridge.TestTarget";
    jvm_handle_t super_h = wasjvmb_reflection_get_class((int32_t)name, 52);

    // 1. Allocate space for our opaque struct on the WASM heap (24 bytes)
    wasjvm_wasm_class_spec_t* spec = (wasjvm_wasm_class_spec_t*)malloc(24);

    // 2. Use generated accessors (including the _t prefix from the struct name)
    // These now match the wasjvm_autogen.h exactly.
    wasjvm_wasm_class_spec_t_set_super_handle(spec, super_h);
    wasjvm_wasm_class_spec_t_set_interface_count(spec, 0);
    wasjvm_wasm_class_spec_t_set_interfaces_ptr(spec, 0);
    wasjvm_wasm_class_spec_t_set_mapping_count(spec, 0);
    wasjvm_wasm_class_spec_t_set_mappings_ptr(spec, 0);

    // 3. Pass the pointer to the builder
    return wasjvmb_builder_define_class((int32_t)spec);
}