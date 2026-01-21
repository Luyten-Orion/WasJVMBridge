(module
  ;; --- Imports from wasjvmbridge ---
  (import "wasjvmbridge" "get_class" (func $get_class (param i32 i32) (result i64)))
    (import "wasjvmbridge" "get_constructor_id" (func $get_constructor_id (param i64 i32 i32) (result i64)))
  (import "wasjvmbridge" "new_instance" (func $new_instance (param i64 i32 i32) (result i64)))
  (import "wasjvmbridge" "get_method_id" (func $get_method_id (param i64 i32 i32 i32 i32) (result i64)))
  (import "wasjvmbridge" "call_method_void" (func $call_method_void (param i64 i64 i32 i32)))
  (import "wasjvmbridge" "call_method_obj" (func $call_method_obj (param i64 i64 i32 i32) (result i64)))
  (import "wasjvmbridge" "get_field_id" (func $get_field_id (param i64 i32 i32) (result i64)))
  (import "wasjvmbridge" "get_field_obj" (func $get_field_obj (param i64 i64) (result i64)))
  (import "wasjvmbridge" "set_field_obj" (func $set_field_obj (param i64 i64 i64)))
  (import "wasjvmbridge" "release_instance" (func $release (param i64)))
  (import "wasjvmbridge" "flush_error" (func $flush))
  (import "wasjvmbridge" "get_last_error_handle" (func $get_err_h (result i64)))
  (import "wasjvmbridge" "create_string" (func $create_string (param i32 i32) (result i64)))
  (import "wasjvmbridge" "get_string_into" (func $get_string_into (param i64 i32 i32) (result i32)))
  (import "wasjvmbridge" "get_encoded_list_size" (func $get_list_size (param i64) (result i32)))
  (import "wasjvmbridge" "pull_list_elements" (func $pull_list (param i64 i32) (result i32)))

  ;; --- Memory Configuration ---
  (memory (export "memory") 1)
  (global $heap_ptr (mut i32) (i32.const 2048))

  ;; --- Data Section ---
  (data (i32.const 0) "quest.yu_vitaqua_fer_chronos.wasjvmbridge.TestTarget")
  (data (i32.const 60) "voidMethod")
  (data (i32.const 80) "message")
  (data (i32.const 100) "crash")
  (data (i32.const 120) "multiParam")
  (data (i32.const 140) "Result Prefix:")
  (data (i32.const 160) "java.lang.String")

  ;; --- Simple Bump Allocator for JVM -> WASM string copies ---
  (func $malloc (export "malloc") (param $size i32) (result i32)
      (local $old_ptr i32)
      global.get $heap_ptr
      local.set $old_ptr
      global.get $heap_ptr
      local.get $size
      i32.add
      global.set $heap_ptr
      local.get $old_ptr
  )

  ;; --- Test 1: Resolve Class ---
  (func (export "test_get_class") (result i64)
      (call $get_class (i32.const 0) (i32.const 52)))

  ;; --- Test 2: Instantiate ---
  (func (export "test_instantiate") (param $class_h i64) (result i64)
      (call $get_constructor_id (local.get $class_h) (i32.const 0) (i32.const 0))
      (call $new_instance (i32.const 0) (i32.const 0))
    )

  ;; --- Test 3: Set Field (sets to null) ---
  (func (export "test_set_field") (param i64 i64 i64)
    (call $set_field_obj (local.get 0) (local.get 1) (local.get 2)))

  ;; --- Test 4: Call Void Method ---
  (func (export "test_call_void") (param i64 i64)
    (call $call_method_void (local.get 0) (local.get 1) (i32.const 0) (i32.const 0)))

  ;; --- Test 5: Trigger and Get Error ---
  (func (export "test_error_flow") (param i64 i64) (result i64)
    (call $flush)
    (call $call_method_void (local.get 0) (local.get 1) (i32.const 0) (i32.const 0))
    (call $get_err_h))

  ;; --- Test 6: Release ---
  (func (export "test_release") (param i64)
    (call $release (local.get 0)))

  ;; --- Test 7: Multi-Parameter Call (String, int, boolean) ---
  ;; Uses the Tagged Union ABI (16-byte alignment)
  (func (export "test_multi_call") (param $inst i64) (param $mid i64)
    (local $str_h i64)

    ;; Create a Java String handle for the first argument
    (call $create_string (i32.const 140) (i32.const 14))
    local.set $str_h

    ;; Arg 0: Tag 0 (HANDLE) at 1024, Value at 1032
    (i32.store (i32.const 1024) (i32.const 0))
    (i64.store (i32.const 1032) (local.get $str_h))

    ;; Arg 1: Tag 4 (I32) at 1040, Value at 1048
    (i32.store (i32.const 1040) (i32.const 4))
    (i64.store (i32.const 1048) (i64.const 777))

    ;; Arg 2: Tag 8 (BOOLEAN) at 1056, Value at 1064
    (i32.store (i32.const 1056) (i32.const 8))
    (i64.store (i32.const 1064) (i64.const 1)) ;; true

    ;; Call multiParam(String, int, boolean)
    (call $call_method_void
        (local.get $inst)
        (local.get $mid)
        (i32.const 1024) ;; Pointer to start of arguments
        (i32.const 3)    ;; Count
    )
  )

  ;; --- Test 8: Get String Into (Direct buffer write) ---
  (func (export "test_get_string_into") (param $str_h i64) (param $buf_ptr i32) (param $max_len i32) (result i32)
    (call $get_string_into (local.get 0) (local.get 1) (local.get 2))
  )

  ;; --- Test 9: Collection Pull ---
  ;; Demonstrates the workflow: get size -> malloc -> pull
  (func (export "test_pull_collection") (param $list_h i64) (result i32)
    (local $buf_ptr i32)
    (local $total_bytes i32)

    ;; 1. Determine how much space we need
    (call $get_list_size (local.get $list_h))
    local.set $total_bytes

    ;; 2. Allocate the buffer in WASM memory
    (call $malloc (local.get $total_bytes))
    local.set $buf_ptr

    ;; 3. Pull the elements. Returns element count.
    (call $pull_list (local.get $list_h) (local.get $buf_ptr))
  )
)