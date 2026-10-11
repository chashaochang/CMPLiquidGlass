#ifndef KONAN_LIBKN_H
#define KONAN_LIBKN_H
#ifdef __cplusplus
extern "C" {
#endif
#ifdef __cplusplus
typedef bool            libkn_KBoolean;
#else
typedef _Bool           libkn_KBoolean;
#endif
typedef unsigned short     libkn_KChar;
typedef signed char        libkn_KByte;
typedef short              libkn_KShort;
typedef int                libkn_KInt;
typedef long long          libkn_KLong;
typedef unsigned char      libkn_KUByte;
typedef unsigned short     libkn_KUShort;
typedef unsigned int       libkn_KUInt;
typedef unsigned long long libkn_KULong;
typedef float              libkn_KFloat;
typedef double             libkn_KDouble;
typedef float __attribute__ ((__vector_size__ (16))) libkn_KVector128;
typedef void*              libkn_KNativePtr;
struct libkn_KType;
typedef struct libkn_KType libkn_KType;

typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_Byte;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_Short;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_Int;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_Long;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_Float;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_Double;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_Char;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_Boolean;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_Unit;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_UByte;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_UShort;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_UInt;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_ULong;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_androidx_compose_foundation_IndicationNodeFactory;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_androidx_compose_ui_graphics_ColorProducer;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_androidx_compose_ui_graphics_vector_ImageVector;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_androidx_compose_ui_Modifier;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_com_kyant_backdrop_Backdrop;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_com_kyant_backdrop_backdrops_LayerBackdrop;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_androidx_compose_ui_graphics_ImageBitmap;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_com_kyant_backdrop_catalog_utils_SdfShader;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_com_kyant_backdrop_catalog_utils_DampedDragAnimation;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlinx_coroutines_CoroutineScope;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_ranges_ClosedRange;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_Function2;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_Function1;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_Function3;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_com_kyant_backdrop_catalog_utils_InteractiveHighlight;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_com_kyant_backdrop_catalog_utils_ProgressConverter;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_com_kyant_backdrop_catalog_utils_ProgressConverter_Companion;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_com_kyant_backdrop_BackdropEffectScope;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_com_kyant_backdrop_catalog_utils_UISensor;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_com_kyant_backdrop_catalog_CatalogDestination;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_com_kyant_backdrop_catalog_CatalogDestination_Home;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_com_kyant_backdrop_catalog_CatalogDestination_Buttons;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_com_kyant_backdrop_catalog_CatalogDestination_ActionBar;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_com_kyant_backdrop_catalog_CatalogDestination_Toggle;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_com_kyant_backdrop_catalog_CatalogDestination_Slider;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_com_kyant_backdrop_catalog_CatalogDestination_BottomTabs;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_com_kyant_backdrop_catalog_CatalogDestination_FloatingTabs;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_com_kyant_backdrop_catalog_CatalogDestination_MiniBar;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_com_kyant_backdrop_catalog_CatalogDestination_Dialog;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_com_kyant_backdrop_catalog_CatalogDestination_LockScreen;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_com_kyant_backdrop_catalog_CatalogDestination_ControlCenter;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_com_kyant_backdrop_catalog_CatalogDestination_Magnifier;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_com_kyant_backdrop_catalog_CatalogDestination_GlassPlayground;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_com_kyant_backdrop_catalog_CatalogDestination_AdaptiveLuminanceGlass;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_com_kyant_backdrop_catalog_CatalogDestination_ProgressiveBlur;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_com_kyant_backdrop_catalog_CatalogDestination_ScrollContainer;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_com_kyant_backdrop_catalog_CatalogDestination_LazyScrollContainer;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_com_kyant_backdrop_catalog_RippleDefaults;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_androidx_compose_material_ripple_RippleAlpha;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_com_kyant_backdrop_catalog_RippleConfiguration;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_Any;

extern void InitJsRenderNodeContext(void* env, void* nodeConstructor, void* statusModifyConstructor, libkn_KDouble ratio, libkn_KBoolean fixed);
extern void androidx_compose_ui_arkui_ArkUIViewController_aboutToAppear(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_aboutToDisappear(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_cancelSyncRefresh(void* controllerRef, libkn_KInt refreshId);
extern libkn_KBoolean androidx_compose_ui_arkui_ArkUIViewController_dispatchComposeKeyEvent(void* controllerRef, libkn_KLong keyCode, libkn_KInt nativeAction, libkn_KLong modifierKeyStates);
extern libkn_KBoolean androidx_compose_ui_arkui_ArkUIViewController_dispatchComposeMouseEvent(void* controllerRef, void* nativeMouseEvent);
extern libkn_KBoolean androidx_compose_ui_arkui_ArkUIViewController_dispatchComposePinchEvent(void* controllerRef, void* nativePinchEvent);
extern void androidx_compose_ui_arkui_ArkUIViewController_dispatchHoverEvent(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_dispatchMouseEvent(void* controllerRef);
extern libkn_KBoolean androidx_compose_ui_arkui_ArkUIViewController_dispatchTouchEvent(void* controllerRef, void* nativeTouchEvent, libkn_KBoolean ignoreInteropView);
extern void AndroidxComposeUiArkuiArkUiViewControllerDraw(void* controllerRef, void* canvas);
extern libkn_KInt androidx_compose_ui_arkui_ArkUIViewController_findNodeIdAt(void* controllerRef, libkn_KFloat x, libkn_KFloat y);
extern const char* androidx_compose_ui_arkui_ArkUIViewController_getId(void* controllerRef);
extern void* AndroidxComposeUiArkuiArkUiViewControllerGetJsNode(void* controllerRef);
extern libkn_KInt androidx_compose_ui_arkui_ArkUIViewController_getRendererTypeId();
extern void* androidx_compose_ui_arkui_ArkUIViewController_getXComponentRender(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_initFusionRendererNode(void* controllerRef, libkn_KBoolean enableCApi, void* rootContent, void* frameMgr);
extern void androidx_compose_ui_arkui_ArkUIViewController_initFusionRendererNodePreComposed(void* controllerRef, libkn_KBoolean enableCApi, void* rootContent, void* frameMgr, libkn_KBoolean isPreCompose);
extern void androidx_compose_ui_arkui_ArkUIViewController_keyboardWillHide(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_keyboardWillShow(void* controllerRef, libkn_KFloat keyboardHeight);
extern libkn_KBoolean androidx_compose_ui_arkui_ArkUIViewController_onBackPress(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_onConfigurationUpdate(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_onFinalize(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_onFocusEvent(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_onFrame(void* controllerRef, libkn_KLong timestamp, libkn_KLong targetTimestamp);
extern void AndroidXComposeUIArkUIArkUIViewControllerOnIdle(void* controllerRef, libkn_KLong timeLeft);
extern void androidx_compose_ui_arkui_ArkUIViewController_onKeyEvent(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_onPageHide(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_onPageShow(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_onSurfaceChanged(void* controllerRef, libkn_KInt width, libkn_KInt height);
extern void androidx_compose_ui_arkui_ArkUIViewController_onSurfaceCreated(void* controllerRef, void* xcomponentPtr, libkn_KInt width, libkn_KInt height);
extern void androidx_compose_ui_arkui_ArkUIViewController_onSurfaceDestroyed(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_onSurfaceHide(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_onSurfaceShow(void* controllerRef);
extern libkn_KInt androidx_compose_ui_arkui_ArkUIViewController_requestSyncRefresh(void* controllerRef);
extern const char* androidx_compose_ui_arkui_ArkUIViewController_sendMessage(void* controllerRef, const char* type, const char* message);
extern void androidx_compose_ui_arkui_ArkUIViewController_setContext(void* controllerRef, void* context);
extern void androidx_compose_ui_arkui_ArkUIViewController_setEnv(void* controllerRef, void* env);
extern void androidx_compose_ui_arkui_ArkUIViewController_setFusionRendererRootNode(void* controllerRef, void* rootFrameNode, void* uiContext);
extern void androidx_compose_ui_arkui_ArkUIViewController_setId(void* controllerRef, const char* id);
extern void androidx_compose_ui_arkui_ArkUIViewController_setLocaleAndStringProvider(void* controllerRef, void* provider);
extern void androidx_compose_ui_arkui_ArkUIViewController_setMessenger(void* controllerRef, void* messenger);
extern void androidx_compose_ui_arkui_ArkUIViewController_setRendererBackendId(void* controllerRef, libkn_KInt backendId);
extern void androidx_compose_ui_arkui_ArkUIViewController_setRootView(void* controllerRef, void* backRootView, void* foreRootView, void* touchableRootView);
extern void androidx_compose_ui_arkui_ArkUIViewController_setUIContext(void* controllerRef, void* uiContext);
extern void androidx_compose_ui_arkui_ArkUIViewController_setXComponentRender(void* controllerRef, void* render);
extern void androidx_compose_ui_platform_LowMemoryMonitor_notifyLowMemoryFromHost(libkn_KInt rawLevel);
extern libkn_KFloat androidx_compose_ui_arkui_ArkUIViewController_consumeOuterScroll(void* controllerRef, libkn_KFloat offsetVp, libkn_KInt sourceInt);
extern void androidx_compose_ui_arkui_ArkUIViewController_setParentScrollBridge(void* controllerRef, void* scroller);
extern void androidx_compose_ui_arkui_init(void* env, void* exports);

typedef struct {
  /* Service functions. */
  void (*DisposeStablePointer)(libkn_KNativePtr ptr);
  void (*DisposeString)(const char* string);
  libkn_KBoolean (*IsInstance)(libkn_KNativePtr ref, const libkn_KType* type);
  libkn_kref_kotlin_Byte (*createNullableByte)(libkn_KByte);
  libkn_KByte (*getNonNullValueOfByte)(libkn_kref_kotlin_Byte);
  libkn_kref_kotlin_Short (*createNullableShort)(libkn_KShort);
  libkn_KShort (*getNonNullValueOfShort)(libkn_kref_kotlin_Short);
  libkn_kref_kotlin_Int (*createNullableInt)(libkn_KInt);
  libkn_KInt (*getNonNullValueOfInt)(libkn_kref_kotlin_Int);
  libkn_kref_kotlin_Long (*createNullableLong)(libkn_KLong);
  libkn_KLong (*getNonNullValueOfLong)(libkn_kref_kotlin_Long);
  libkn_kref_kotlin_Float (*createNullableFloat)(libkn_KFloat);
  libkn_KFloat (*getNonNullValueOfFloat)(libkn_kref_kotlin_Float);
  libkn_kref_kotlin_Double (*createNullableDouble)(libkn_KDouble);
  libkn_KDouble (*getNonNullValueOfDouble)(libkn_kref_kotlin_Double);
  libkn_kref_kotlin_Char (*createNullableChar)(libkn_KChar);
  libkn_KChar (*getNonNullValueOfChar)(libkn_kref_kotlin_Char);
  libkn_kref_kotlin_Boolean (*createNullableBoolean)(libkn_KBoolean);
  libkn_KBoolean (*getNonNullValueOfBoolean)(libkn_kref_kotlin_Boolean);
  libkn_kref_kotlin_Unit (*createNullableUnit)(void);
  libkn_kref_kotlin_UByte (*createNullableUByte)(libkn_KUByte);
  libkn_KUByte (*getNonNullValueOfUByte)(libkn_kref_kotlin_UByte);
  libkn_kref_kotlin_UShort (*createNullableUShort)(libkn_KUShort);
  libkn_KUShort (*getNonNullValueOfUShort)(libkn_kref_kotlin_UShort);
  libkn_kref_kotlin_UInt (*createNullableUInt)(libkn_KUInt);
  libkn_KUInt (*getNonNullValueOfUInt)(libkn_kref_kotlin_UInt);
  libkn_kref_kotlin_ULong (*createNullableULong)(libkn_KULong);
  libkn_KULong (*getNonNullValueOfULong)(libkn_kref_kotlin_ULong);

  /* User functions. */
  struct {
    struct {
      struct {
        struct {
          struct {
            struct {
              struct {
                libkn_kref_androidx_compose_ui_graphics_vector_ImageVector (*get_LiquidActionBarBackIcon)();
                libkn_kref_androidx_compose_ui_graphics_vector_ImageVector (*get_LiquidActionBarMoreIcon)();
                libkn_kref_androidx_compose_ui_Modifier (*progressiveBlur)(libkn_kref_androidx_compose_ui_Modifier thiz, libkn_kref_com_kyant_backdrop_Backdrop backdrop, libkn_KULong tint, libkn_KFloat blurRadius, libkn_KFloat tintIntensity, libkn_KFloat gradientStartFraction, libkn_kref_com_kyant_backdrop_backdrops_LayerBackdrop exportedBackdrop);
              } components;
              struct {
                struct {
                  libkn_KType* (*_type)(void);
                  libkn_kref_com_kyant_backdrop_catalog_utils_DampedDragAnimation (*DampedDragAnimation)(libkn_kref_kotlinx_coroutines_CoroutineScope animationScope, libkn_KFloat initialValue, libkn_kref_kotlin_ranges_ClosedRange valueRange, libkn_KFloat visibilityThreshold, libkn_KFloat initialScale, libkn_KFloat pressedScale, libkn_kref_kotlin_Function2 onDragStarted, libkn_kref_kotlin_Function1 onDragStopped, libkn_kref_kotlin_Function3 onDrag);
                  libkn_KFloat (*get_initialScale)(libkn_kref_com_kyant_backdrop_catalog_utils_DampedDragAnimation thiz);
                  libkn_KFloat (*get_initialValue)(libkn_kref_com_kyant_backdrop_catalog_utils_DampedDragAnimation thiz);
                  libkn_kref_androidx_compose_ui_Modifier (*get_modifier)(libkn_kref_com_kyant_backdrop_catalog_utils_DampedDragAnimation thiz);
                  libkn_kref_kotlin_Function3 (*get_onDrag)(libkn_kref_com_kyant_backdrop_catalog_utils_DampedDragAnimation thiz);
                  libkn_kref_kotlin_Function2 (*get_onDragStarted)(libkn_kref_com_kyant_backdrop_catalog_utils_DampedDragAnimation thiz);
                  libkn_kref_kotlin_Function1 (*get_onDragStopped)(libkn_kref_com_kyant_backdrop_catalog_utils_DampedDragAnimation thiz);
                  libkn_KFloat (*get_pressProgress)(libkn_kref_com_kyant_backdrop_catalog_utils_DampedDragAnimation thiz);
                  libkn_KFloat (*get_pressedScale)(libkn_kref_com_kyant_backdrop_catalog_utils_DampedDragAnimation thiz);
                  libkn_KFloat (*get_progress)(libkn_kref_com_kyant_backdrop_catalog_utils_DampedDragAnimation thiz);
                  libkn_KFloat (*get_scaleX)(libkn_kref_com_kyant_backdrop_catalog_utils_DampedDragAnimation thiz);
                  libkn_KFloat (*get_scaleY)(libkn_kref_com_kyant_backdrop_catalog_utils_DampedDragAnimation thiz);
                  libkn_KFloat (*get_targetValue)(libkn_kref_com_kyant_backdrop_catalog_utils_DampedDragAnimation thiz);
                  libkn_KFloat (*get_value)(libkn_kref_com_kyant_backdrop_catalog_utils_DampedDragAnimation thiz);
                  libkn_kref_kotlin_ranges_ClosedRange (*get_valueRange)(libkn_kref_com_kyant_backdrop_catalog_utils_DampedDragAnimation thiz);
                  libkn_KFloat (*get_velocity)(libkn_kref_com_kyant_backdrop_catalog_utils_DampedDragAnimation thiz);
                  libkn_KFloat (*get_visibilityThreshold)(libkn_kref_com_kyant_backdrop_catalog_utils_DampedDragAnimation thiz);
                  void (*animateToValue)(libkn_kref_com_kyant_backdrop_catalog_utils_DampedDragAnimation thiz, libkn_KFloat value);
                  void (*press)(libkn_kref_com_kyant_backdrop_catalog_utils_DampedDragAnimation thiz);
                  void (*release)(libkn_kref_com_kyant_backdrop_catalog_utils_DampedDragAnimation thiz);
                  void (*updateValue)(libkn_kref_com_kyant_backdrop_catalog_utils_DampedDragAnimation thiz, libkn_KFloat value);
                } DampedDragAnimation;
                struct {
                  libkn_KType* (*_type)(void);
                  libkn_kref_com_kyant_backdrop_catalog_utils_InteractiveHighlight (*InteractiveHighlight)(libkn_kref_kotlinx_coroutines_CoroutineScope animationScope, libkn_kref_kotlin_Function2 position);
                  libkn_kref_kotlinx_coroutines_CoroutineScope (*get_animationScope)(libkn_kref_com_kyant_backdrop_catalog_utils_InteractiveHighlight thiz);
                  libkn_kref_androidx_compose_ui_Modifier (*get_gestureModifier)(libkn_kref_com_kyant_backdrop_catalog_utils_InteractiveHighlight thiz);
                  libkn_kref_androidx_compose_ui_Modifier (*get_modifier)(libkn_kref_com_kyant_backdrop_catalog_utils_InteractiveHighlight thiz);
                  libkn_KLong (*get_offset)(libkn_kref_com_kyant_backdrop_catalog_utils_InteractiveHighlight thiz);
                  libkn_kref_kotlin_Function2 (*get_position)(libkn_kref_com_kyant_backdrop_catalog_utils_InteractiveHighlight thiz);
                  libkn_KFloat (*get_pressProgress)(libkn_kref_com_kyant_backdrop_catalog_utils_InteractiveHighlight thiz);
                } InteractiveHighlight;
                struct {
                  struct {
                    libkn_KType* (*_type)(void);
                    libkn_kref_com_kyant_backdrop_catalog_utils_ProgressConverter_Companion (*_instance)();
                    libkn_kref_com_kyant_backdrop_catalog_utils_ProgressConverter (*get_Default)(libkn_kref_com_kyant_backdrop_catalog_utils_ProgressConverter_Companion thiz);
                  } Companion;
                  libkn_KType* (*_type)(void);
                  libkn_KFloat (*convert)(libkn_kref_com_kyant_backdrop_catalog_utils_ProgressConverter thiz, libkn_KFloat progress);
                } ProgressConverter;
                struct {
                  libkn_KType* (*_type)(void);
                  libkn_KInt (*get_height)(libkn_kref_com_kyant_backdrop_catalog_utils_SdfShader thiz);
                  libkn_KInt (*get_width)(libkn_kref_com_kyant_backdrop_catalog_utils_SdfShader thiz);
                  void (*apply)(libkn_kref_com_kyant_backdrop_catalog_utils_SdfShader thiz, libkn_kref_com_kyant_backdrop_BackdropEffectScope thiz1, libkn_KFloat refractionHeight, libkn_KFloat lightAngle);
                } SdfShader;
                struct {
                  libkn_KType* (*_type)(void);
                  libkn_KLong (*get_gravity)(libkn_kref_com_kyant_backdrop_catalog_utils_UISensor thiz);
                  libkn_KFloat (*get_gravityAngle)(libkn_kref_com_kyant_backdrop_catalog_utils_UISensor thiz);
                  void (*start)(libkn_kref_com_kyant_backdrop_catalog_utils_UISensor thiz);
                  void (*stop)(libkn_kref_com_kyant_backdrop_catalog_utils_UISensor thiz);
                } UISensor;
                libkn_kref_androidx_compose_ui_graphics_ImageBitmap (*scale)(libkn_kref_androidx_compose_ui_graphics_ImageBitmap thiz, libkn_KInt width, libkn_KInt height);
                libkn_kref_com_kyant_backdrop_catalog_utils_SdfShader (*SdfShader_)(libkn_kref_androidx_compose_ui_graphics_ImageBitmap imageBitmap);
                void (*SetOhosGravity)(libkn_KFloat x, libkn_KFloat y);
              } utils;
              struct {
                struct {
                  libkn_kref_com_kyant_backdrop_catalog_CatalogDestination (*get)(); /* enum entry for Home. */
                } Home;
                struct {
                  libkn_kref_com_kyant_backdrop_catalog_CatalogDestination (*get)(); /* enum entry for Buttons. */
                } Buttons;
                struct {
                  libkn_kref_com_kyant_backdrop_catalog_CatalogDestination (*get)(); /* enum entry for ActionBar. */
                } ActionBar;
                struct {
                  libkn_kref_com_kyant_backdrop_catalog_CatalogDestination (*get)(); /* enum entry for Toggle. */
                } Toggle;
                struct {
                  libkn_kref_com_kyant_backdrop_catalog_CatalogDestination (*get)(); /* enum entry for Slider. */
                } Slider;
                struct {
                  libkn_kref_com_kyant_backdrop_catalog_CatalogDestination (*get)(); /* enum entry for BottomTabs. */
                } BottomTabs;
                struct {
                  libkn_kref_com_kyant_backdrop_catalog_CatalogDestination (*get)(); /* enum entry for FloatingTabs. */
                } FloatingTabs;
                struct {
                  libkn_kref_com_kyant_backdrop_catalog_CatalogDestination (*get)(); /* enum entry for MiniBar. */
                } MiniBar;
                struct {
                  libkn_kref_com_kyant_backdrop_catalog_CatalogDestination (*get)(); /* enum entry for Dialog. */
                } Dialog;
                struct {
                  libkn_kref_com_kyant_backdrop_catalog_CatalogDestination (*get)(); /* enum entry for LockScreen. */
                } LockScreen;
                struct {
                  libkn_kref_com_kyant_backdrop_catalog_CatalogDestination (*get)(); /* enum entry for ControlCenter. */
                } ControlCenter;
                struct {
                  libkn_kref_com_kyant_backdrop_catalog_CatalogDestination (*get)(); /* enum entry for Magnifier. */
                } Magnifier;
                struct {
                  libkn_kref_com_kyant_backdrop_catalog_CatalogDestination (*get)(); /* enum entry for GlassPlayground. */
                } GlassPlayground;
                struct {
                  libkn_kref_com_kyant_backdrop_catalog_CatalogDestination (*get)(); /* enum entry for AdaptiveLuminanceGlass. */
                } AdaptiveLuminanceGlass;
                struct {
                  libkn_kref_com_kyant_backdrop_catalog_CatalogDestination (*get)(); /* enum entry for ProgressiveBlur. */
                } ProgressiveBlur;
                struct {
                  libkn_kref_com_kyant_backdrop_catalog_CatalogDestination (*get)(); /* enum entry for ScrollContainer. */
                } ScrollContainer;
                struct {
                  libkn_kref_com_kyant_backdrop_catalog_CatalogDestination (*get)(); /* enum entry for LazyScrollContainer. */
                } LazyScrollContainer;
                libkn_KType* (*_type)(void);
              } CatalogDestination;
              struct {
                libkn_KType* (*_type)(void);
                libkn_kref_com_kyant_backdrop_catalog_RippleDefaults (*_instance)();
                libkn_kref_androidx_compose_material_ripple_RippleAlpha (*get_RippleAlpha)(libkn_kref_com_kyant_backdrop_catalog_RippleDefaults thiz);
              } RippleDefaults;
              struct {
                libkn_KType* (*_type)(void);
                libkn_kref_com_kyant_backdrop_catalog_RippleConfiguration (*RippleConfiguration)(libkn_KULong color, libkn_kref_androidx_compose_material_ripple_RippleAlpha rippleAlpha);
                libkn_KULong (*get_color)(libkn_kref_com_kyant_backdrop_catalog_RippleConfiguration thiz);
                libkn_kref_androidx_compose_material_ripple_RippleAlpha (*get_rippleAlpha)(libkn_kref_com_kyant_backdrop_catalog_RippleConfiguration thiz);
                libkn_KBoolean (*equals)(libkn_kref_com_kyant_backdrop_catalog_RippleConfiguration thiz, libkn_kref_kotlin_Any other);
                libkn_KInt (*hashCode)(libkn_kref_com_kyant_backdrop_catalog_RippleConfiguration thiz);
                const char* (*toString)(libkn_kref_com_kyant_backdrop_catalog_RippleConfiguration thiz);
              } RippleConfiguration;
              libkn_kref_androidx_compose_foundation_IndicationNodeFactory (*ripple)(libkn_KBoolean bounded, libkn_KFloat radius, libkn_KULong color);
              libkn_kref_androidx_compose_foundation_IndicationNodeFactory (*ripple_)(libkn_kref_androidx_compose_ui_graphics_ColorProducer color, libkn_KBoolean bounded, libkn_KFloat radius);
              void* (*MainArkUIViewController)(void* env);
            } catalog;
          } backdrop;
        } kyant;
      } com;
      struct {
        struct {
          struct {
            struct {
              struct {
                void (*InitJsRenderNodeContext_)(void* env, void* nodeConstructor, void* statusModifyConstructor, libkn_KDouble ratio, libkn_KBoolean fixed);
                void (*_Export_ArkUIViewController_aboutToAppear)(void* controllerRef);
                void (*_Export_ArkUIViewController_aboutToDisappear)(void* controllerRef);
                void (*_Export_ArkUIViewController_cancelSyncRefresh)(void* controllerRef, libkn_KInt refreshId);
                libkn_KBoolean (*_Export_ArkUIViewController_dispatchComposeKeyEvent)(void* controllerRef, libkn_KLong keyCode, libkn_KInt nativeAction, libkn_KLong modifierKeyStates);
                libkn_KBoolean (*_Export_ArkUIViewController_dispatchComposeMouseEvent)(void* controllerRef, void* nativeMouseEvent);
                libkn_KBoolean (*_Export_ArkUIViewController_dispatchComposePinchEvent)(void* controllerRef, void* nativePinchEvent);
                void (*_Export_ArkUIViewController_dispatchHoverEvent)(void* controllerRef);
                void (*_Export_ArkUIViewController_dispatchMouseEvent)(void* controllerRef);
                libkn_KBoolean (*_Export_ArkUIViewController_dispatchTouchEvent)(void* controllerRef, void* nativeTouchEvent, libkn_KBoolean ignoreInteropView);
                void (*_Export_ArkUIViewController_draw)(void* controllerRef, void* canvas);
                libkn_KInt (*_Export_ArkUIViewController_findNodeIdAt)(void* controllerRef, libkn_KFloat x, libkn_KFloat y);
                const char* (*_Export_ArkUIViewController_getId)(void* controllerRef);
                void* (*_Export_ArkUIViewController_getJsNode)(void* controllerRef);
                libkn_KInt (*_Export_ArkUIViewController_getRendererTypeId)();
                void* (*_Export_ArkUIViewController_getXComponentRender)(void* controllerRef);
                void (*_Export_ArkUIViewController_initFusionRendererNode)(void* controllerRef, libkn_KBoolean enableCApi, void* rootContent, void* frameMgr);
                void (*_Export_ArkUIViewController_initFusionRendererNodePreComposed)(void* controllerRef, libkn_KBoolean enableCApi, void* rootContent, void* frameMgr, libkn_KBoolean isPreCompose);
                void (*_Export_ArkUIViewController_keyboardWillHide)(void* controllerRef);
                void (*_Export_ArkUIViewController_keyboardWillShow)(void* controllerRef, libkn_KFloat keyboardHeight);
                libkn_KBoolean (*_Export_ArkUIViewController_onBackPress)(void* controllerRef);
                void (*_Export_ArkUIViewController_onConfigurationUpdate)(void* controllerRef);
                void (*_Export_ArkUIViewController_onFinalize)(void* controllerRef);
                void (*_Export_ArkUIViewController_onFocusEvent)(void* controllerRef);
                void (*_Export_ArkUIViewController_onFrame)(void* controllerRef, libkn_KLong timestamp, libkn_KLong targetTimestamp);
                void (*_Export_ArkUIViewController_onIdle)(void* controllerRef, libkn_KLong timeLeft);
                void (*_Export_ArkUIViewController_onKeyEvent)(void* controllerRef);
                void (*_Export_ArkUIViewController_onPageHide)(void* controllerRef);
                void (*_Export_ArkUIViewController_onPageShow)(void* controllerRef);
                void (*_Export_ArkUIViewController_onSurfaceChanged)(void* controllerRef, libkn_KInt width, libkn_KInt height);
                void (*_Export_ArkUIViewController_onSurfaceCreated)(void* controllerRef, void* xcomponentPtr, libkn_KInt width, libkn_KInt height);
                void (*_Export_ArkUIViewController_onSurfaceDestroyed)(void* controllerRef);
                void (*_Export_ArkUIViewController_onSurfaceHide)(void* controllerRef);
                void (*_Export_ArkUIViewController_onSurfaceShow)(void* controllerRef);
                libkn_KInt (*_Export_ArkUIViewController_requestSyncRefresh)(void* controllerRef);
                const char* (*_Export_ArkUIViewController_sendMessage)(void* controllerRef, const char* type, const char* message);
                void (*_Export_ArkUIViewController_setContext)(void* controllerRef, void* context);
                void (*_Export_ArkUIViewController_setEnv)(void* controllerRef, void* env);
                void (*_Export_ArkUIViewController_setFusionRendererRootNode)(void* controllerRef, void* rootFrameNode, void* uiContext);
                void (*_Export_ArkUIViewController_setId)(void* controllerRef, const char* id);
                void (*_Export_ArkUIViewController_setLocaleAndStringProvider)(void* controllerRef, void* provider);
                void (*_Export_ArkUIViewController_setMessenger)(void* controllerRef, void* messenger);
                void (*_Export_ArkUIViewController_setRendererBackendId)(void* controllerRef, libkn_KInt backendId);
                void (*_Export_ArkUIViewController_setRootView)(void* controllerRef, void* backRootView, void* foreRootView, void* touchableRootView);
                void (*_Export_ArkUIViewController_setUIContext)(void* controllerRef, void* uiContext);
                void (*_Export_ArkUIViewController_setXComponentRender)(void* controllerRef, void* render);
                void (*_Export_LowMemoryMonitor_notifyLowMemoryFromHost)(libkn_KInt rawLevel);
                libkn_KLong (*getCurrentTimeNanos)();
                libkn_KFloat (*_Export_ArkUIViewController_consumeOuterScroll)(void* controllerRef, libkn_KFloat offsetVp, libkn_KInt sourceInt);
                void (*_Export_ArkUIViewController_setParentScrollBridge)(void* controllerRef, void* scroller);
                void (*_Export_ArkUIViewInitializer_init)(void* env, void* exports);
              } arkui;
            } ui;
          } export_;
        } compose;
      } androidx;
    } root;
  } kotlin;
} libkn_ExportedSymbols;
extern libkn_ExportedSymbols* libkn_symbols(void);
#ifdef __cplusplus
}  /* extern "C" */
#endif
#endif  /* KONAN_LIBKN_H */
