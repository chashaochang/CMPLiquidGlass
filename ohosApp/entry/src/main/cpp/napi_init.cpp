#include "libkn_api.h"
#include "napi/native_api.h"

static napi_value MainArkUIViewControllerWrapper(napi_env env, napi_callback_info info) {
    return reinterpret_cast<napi_value>(
        libkn_symbols()->kotlin.root.com.kyant.backdrop.catalog.MainArkUIViewController(env)
    );
}

static napi_value SetOhosGravityWrapper(napi_env env, napi_callback_info info) {
    size_t argc = 2;
    napi_value args[2] = {nullptr, nullptr};
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);
    if (argc >= 2 && args[0] != nullptr && args[1] != nullptr) {
        double x = 0.0;
        double y = 0.0;
        napi_get_value_double(env, args[0], &x);
        napi_get_value_double(env, args[1], &y);
        libkn_symbols()->kotlin.root.com.kyant.backdrop.catalog.utils.SetOhosGravity(
            static_cast<float>(x),
            static_cast<float>(y)
        );
    }
    return nullptr;
}

EXTERN_C_START
static napi_value Init(napi_env env, napi_value exports) {
    androidx_compose_ui_arkui_init(env, exports);
    napi_property_descriptor desc[] = {
        {"MainArkUIViewController", nullptr, MainArkUIViewControllerWrapper, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"SetOhosGravity", nullptr, SetOhosGravityWrapper, nullptr, nullptr, nullptr, napi_default, nullptr},
    };
    napi_define_properties(env, exports, sizeof(desc) / sizeof(desc[0]), desc);
    return exports;
}
EXTERN_C_END

static napi_module entryModule = {
    .nm_version = 1,
    .nm_flags = 0,
    .nm_filename = nullptr,
    .nm_register_func = Init,
    .nm_modname = "entry",
    .nm_priv = ((void*)0),
    .reserved = {0},
};

extern "C" __attribute__((constructor)) void RegisterEntryModule(void) {
    napi_module_register(&entryModule);
}
