#include "libkn_api.h"
#include "napi/native_api.h"

static napi_value CreateProbe(napi_env env, napi_callback_info) {
    return reinterpret_cast<napi_value>(
        libkn_symbols()->kotlin.root.com.kyant.backdrop.probe.MainArkUIViewController(env)
    );
}

static napi_value Init(napi_env env, napi_value exports) {
    androidx_compose_ui_arkui_init(env, exports);
    napi_property_descriptor desc[] = {
        {"MainArkUIViewController", nullptr, CreateProbe, nullptr, nullptr, nullptr, napi_default, nullptr},
    };
    napi_define_properties(env, exports, sizeof(desc) / sizeof(desc[0]), desc);
    return exports;
}

static napi_module module = {
    .nm_version = 1,
    .nm_flags = 0,
    .nm_filename = nullptr,
    .nm_register_func = Init,
    .nm_modname = "entry",
    .nm_priv = nullptr,
    .reserved = {0},
};

extern "C" __attribute__((constructor)) void RegisterEntryModule() {
    napi_module_register(&module);
}
