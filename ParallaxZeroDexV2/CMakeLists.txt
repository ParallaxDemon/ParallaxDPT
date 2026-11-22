cmake_minimum_required(VERSION 3.22.1)

project(ParallaxN LANGUAGES C CXX)

add_library(ParallaxN SHARED
        native_main.cpp)

set_target_properties(ParallaxN PROPERTIES
        CXX_STANDARD 20
        CXX_STANDARD_REQUIRED ON
        CXX_EXTENSIONS OFF)

target_compile_options(ParallaxN PRIVATE
        -Oz
        -fvisibility=hidden
        -ffunction-sections
        -fdata-sections
        -fstack-protector-strong
        -D_FORTIFY_SOURCE=2
        -Wall
        -Wextra
        -Werror)

target_link_options(ParallaxN PRIVATE
        -Wl,--gc-sections
        -Wl,-z,relro
        -Wl,-z,now
        -Wl,-z,noexecstack
        -Wl,-z,max-page-size=16384)

find_library(android-lib android)
find_library(log-lib log)

target_link_libraries(ParallaxN
        ${android-lib}
        ${log-lib})
