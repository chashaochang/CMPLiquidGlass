cmake_minimum_required(VERSION 3.5.0)
project(harmonyApp)

set(NATIVERENDER_ROOT_PATH ${CMAKE_CURRENT_SOURCE_DIR})

if(DEFINED PACKAGE_FIND_FILE)
    include(${PACKAGE_FIND_FILE})
endif()

include_directories(${NATIVERENDER_ROOT_PATH}
                    ${NATIVERENDER_ROOT_PATH}/include
                    ${NATIVERENDER_ROOT_PATH}/include/arm64-v8a)

find_package(compose)

add_library(entry SHARED napi_init.cpp)

add_library(ComposeApp SHARED IMPORTED)
set_target_properties(ComposeApp PROPERTIES
    IMPORTED_LOCATION "${CMAKE_CURRENT_SOURCE_DIR}/../../../libs/arm64-v8a/libkn.so"
)

target_link_libraries(entry PUBLIC libace_napi.z.so)
target_link_libraries(entry PUBLIC libhilog_ndk.z.so)
target_link_libraries(entry PUBLIC ComposeApp)
target_link_libraries(entry PUBLIC compose::skikobridge)
target_link_libraries(entry PUBLIC ${hilog-lib} ${libnapi-lib} libc++_shared.so)
