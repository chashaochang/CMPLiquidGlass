package com.kyant.backdrop.catalog

import androidx.compose.ui.napi.JsEnv
import androidx.compose.ui.window.ComposeArkUIViewController
import cnames.structs.napi_env__
import cnames.structs.napi_value__
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.initMainHandler

@OptIn(ExperimentalForeignApi::class)
fun MainArkUIViewController(env: CPointer<napi_env__>): CPointer<napi_value__> {
    initMainHandler(env)
    JsEnv.init(env)
    return ComposeArkUIViewController(env) {
        MainContent()
    }
}
