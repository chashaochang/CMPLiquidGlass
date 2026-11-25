@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.kyant.backdrop.probe

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Surface
import platform.egl.*
import platform.gles3.GL_RENDERER
import platform.gles3.GL_VENDOR
import platform.gles3.GL_VERSION
import platform.gles3.glGetString
import platform.posix.pthread_self
import kotlin.time.measureTime

internal class MaterialGpu private constructor(
    private val display: COpaquePointer,
    private val eglContext: COpaquePointer,
    private val pbuffer: COpaquePointer,
) {
    private val thread = pthread_self()
    private var directContext: DirectContext? = null
    private var surface: Surface? = null
    private var closed = false
    private var restoredCalls = 0

    fun render(width: Int, height: Int, result: Bitmap, mode: Int, trace: Boolean = true, draw: (Canvas) -> Unit) = withCurrent {
        val context = directContext ?: DirectContext.makeGL().also {
            directContext = it
            println(
                "GlassMaterialExecutor GPU_READY backend=skia-ganesh " +
                    "vendor=${glString(GL_VENDOR)} renderer=${glString(GL_RENDERER)} version=${glString(GL_VERSION)}"
            )
        }
        var target = surface
        if (target == null || target.width != width || target.height != height) {
            target?.close()
            target = Surface.makeRenderTarget(
                context, true,
                ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.PREMUL, ColorSpace.sRGB),
            )
            surface = target
        }
        val drawTime = measureTime { draw(target.canvas) }
        val submitTime = measureTime { context.flushAndSubmit(target, syncCpu = true) }
        val readTime = measureTime { check(target.readPixels(result, 0, 0)) { "GPU readback failed" } }
        if (trace) println(
            "GlassMaterialExecutor GPU_STAGE mode=$mode size=${width}x$height " +
                "drawUs=${drawTime.inWholeMicroseconds} submitWaitUs=${submitTime.inWholeMicroseconds} " +
                "readbackUs=${readTime.inWholeMicroseconds} timing=cpu-wall-clock"
        )
    }

    fun <T> withGpuContext(block: () -> T): T = withCurrent(block)

    fun close() {
        if (closed) return
        withCurrent {
            surface?.close()
            surface = null
            directContext?.close()
            directContext = null
        }
        checkEgl(eglDestroySurface(display, pbuffer).toInt(), "destroy pbuffer")
        checkEgl(eglDestroyContext(display, eglContext).toInt(), "destroy context")
        // EGLDisplay can be shared with the host; eglTerminate would disturb its renderer.
        closed = true
        println("GlassMaterialExecutor GPU_CLOSED restoredCalls=$restoredCalls eglStateVerified=true")
    }

    private fun <T> withCurrent(block: () -> T): T {
        check(!closed && pthread_self() == thread) { "GPU executor used after close or on another thread" }
        val oldApi = eglQueryAPI()
        val oldDisplay = eglGetCurrentDisplay()
        val oldContext = eglGetCurrentContext()
        val oldDraw = eglGetCurrentSurface(EGL_DRAW)
        val oldRead = eglGetCurrentSurface(EGL_READ)
        try {
            checkEgl(eglBindAPI(EGL_OPENGL_ES_API.toUInt()).toInt(), "bind GLES")
            checkEgl(eglMakeCurrent(display, pbuffer, pbuffer, eglContext).toInt(), "make current")
            return block()
        } finally {
            checkEgl(eglMakeCurrent(display, null, null, null).toInt(), "detach context")
            checkEgl(eglBindAPI(oldApi).toInt(), "restore API")
            if (oldContext != null) {
                checkEgl(eglMakeCurrent(oldDisplay, oldDraw, oldRead, oldContext).toInt(), "restore host context")
            }
            check(
                eglQueryAPI() == oldApi && eglGetCurrentDisplay() == oldDisplay &&
                    eglGetCurrentContext() == oldContext && eglGetCurrentSurface(EGL_DRAW) == oldDraw &&
                    eglGetCurrentSurface(EGL_READ) == oldRead
            ) { "Host EGL API/context/surfaces changed after material execution" }
            restoredCalls++
        }
    }

    companion object {
        fun create(): MaterialGpu {
            val display = checkNotNull(eglGetDisplay(null)) { "EGL display unavailable" }
            checkEgl(eglInitialize(display, null, null).toInt(), "initialize display")
            val oldApi = eglQueryAPI()
            var context: COpaquePointer? = null
            try {
                checkEgl(eglBindAPI(EGL_OPENGL_ES_API.toUInt()).toInt(), "bind GLES")
                val config = memScoped {
                    val config = alloc<COpaquePointerVar>()
                    val count = alloc<IntVar>()
                    intArrayOf(
                        EGL_SURFACE_TYPE, EGL_PBUFFER_BIT, EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
                        EGL_RED_SIZE, 8, EGL_GREEN_SIZE, 8, EGL_BLUE_SIZE, 8, EGL_ALPHA_SIZE, 8, EGL_NONE,
                    ).usePinned {
                        checkEgl(eglChooseConfig(display, it.addressOf(0), config.ptr, 1, count.ptr).toInt(), "choose config")
                    }
                    check(count.value == 1) { "No GLES3 pbuffer config" }
                    checkNotNull(config.value)
                }
                context = intArrayOf(EGL_CONTEXT_CLIENT_VERSION, 3, EGL_NONE).usePinned {
                    checkNotNull(eglCreateContext(display, config, null, it.addressOf(0))) { "EGL context creation failed" }
                }
                val pbuffer = intArrayOf(EGL_WIDTH, 1, EGL_HEIGHT, 1, EGL_NONE).usePinned {
                    checkNotNull(eglCreatePbufferSurface(display, config, it.addressOf(0))) { "EGL pbuffer creation failed" }
                }
                return MaterialGpu(display, context, pbuffer)
            } catch (error: Throwable) {
                context?.let { eglDestroyContext(display, it) }
                throw error
            } finally {
                checkEgl(eglBindAPI(oldApi).toInt(), "restore API after creation")
                check(eglQueryAPI() == oldApi) { "Host EGL API changed during executor creation" }
            }
        }

        private fun checkEgl(result: Int, operation: String) {
            check(result == 1) { "EGL $operation failed: 0x${eglGetError().toString(16)}" }
        }

        private fun glString(name: Int): String =
            glGetString(name.toUInt())?.reinterpret<ByteVar>()?.toKString() ?: "unknown"
    }
}
