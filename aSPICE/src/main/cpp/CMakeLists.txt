
#
# Copyright (C) The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_VERBOSE_MAKEFILE on)

# configure import libs
aux_source_directory(${CMAKE_SOURCE_DIR}/android  spice_SRCS1)
aux_source_directory(${CMAKE_SOURCE_DIR}/gtk  spice_SRCS2)
aux_source_directory(${CMAKE_SOURCE_DIR}/spice-common/common  spice_SRCS3)
aux_source_directory(${CMAKE_SOURCE_DIR}/spice-common/spice-protocol/spice  spice_SRCS4)
aux_source_directory(${CMAKE_SOURCE_DIR}/virt-viewer  spice_SRCS5)


MESSAGE(STATUS "yxlei spice_SRCS: " ${spice_SRCS2})

set(native_lib_DIR ${CMAKE_SOURCE_DIR}/../../../../native)
set(native_src_DIR ${CMAKE_SOURCE_DIR})


add_library(lib_celt STATIC IMPORTED )
set_target_properties(lib_celt PROPERTIES IMPORTED_LOCATION
    ${native_lib_DIR}/libs/deps/lib/libcelt051.a)

add_library(lib_crypto STATIC IMPORTED )
set_target_properties(lib_crypto PROPERTIES IMPORTED_LOCATION
    ${native_lib_DIR}/libs/deps/lib/libcrypto.a)


add_library(lib_govirt STATIC IMPORTED )
set_target_properties(lib_govirt PROPERTIES IMPORTED_LOCATION
     ${native_lib_DIR}/libs/deps/lib/libgovirt.a)

add_library(lib_jpeg STATIC IMPORTED )
set_target_properties(lib_jpeg PROPERTIES IMPORTED_LOCATION
    ${native_lib_DIR}/libs/deps/lib/libjpeg.a)

add_library(lib_rest STATIC IMPORTED )
set_target_properties(lib_rest PROPERTIES IMPORTED_LOCATION
    ${native_lib_DIR}/libs/deps/lib/librest-0.7.a)


add_library(lib_ssl STATIC IMPORTED )
set_target_properties(lib_ssl PROPERTIES IMPORTED_LOCATION
    ${native_lib_DIR}/libs/deps/lib/libssl.a)

add_library(lib_usb STATIC IMPORTED )
set_target_properties(lib_usb PROPERTIES IMPORTED_LOCATION
    ${native_lib_DIR}/libs/deps/lib/libusb.a)

add_library(lib_usbredirhost STATIC IMPORTED )
set_target_properties(lib_usbredirhost PROPERTIES IMPORTED_LOCATION
    ${native_lib_DIR}/libs/deps/lib/libusbredirhost.a)

add_library(lib_usbredirparser STATIC IMPORTED )
set_target_properties(lib_usbredirparser PROPERTIES IMPORTED_LOCATION
    ${native_lib_DIR}/libs/deps/lib/libusbredirparser.a)

add_library(lib_flac STATIC IMPORTED )
set_target_properties(lib_flac PROPERTIES IMPORTED_LOCATION
    ${native_lib_DIR}/libs/deps/lib/libFLAC.a)

add_library(lib_soup STATIC IMPORTED )
set_target_properties(lib_soup PROPERTIES IMPORTED_LOCATION
    ${native_lib_DIR}/libs/deps/lib/libsoup-2.4.a)

add_library(lib_xml STATIC IMPORTED )
set_target_properties(lib_xml PROPERTIES IMPORTED_LOCATION
    ${native_lib_DIR}/libs/deps/lib/libxml2.a)

# shared lib will also be tucked into APK and sent to target
# refer to app/build.gradle, jniLibs section for that purpose.
# ${ANDROID_ABI} is handy for our purpose here. Probably this ${ANDROID_ABI} is
# the most valuable thing of this sample, the rest are pretty much normal cmake
add_library(lib_gstreamer SHARED IMPORTED )
set_target_properties(lib_gstreamer PROPERTIES IMPORTED_LOCATION
    ${native_lib_DIR}/libs/gstreamer/${ANDROID_ABI}/libgstreamer_android.so)

# build application's shared lib
include_directories(${native_lib_DIR}/libs/deps/include
                    ${native_lib_DIR}/libs/deps/include/celt051
                    ${native_lib_DIR}/libs/deps/include/govirt-1.0
                    ${native_lib_DIR}/libs/deps/include/jpeg-turbo
                    ${native_lib_DIR}/libs/deps/include/libusb
                    ${native_lib_DIR}/libs/deps/include/openssl
                    ${native_lib_DIR}/libs/deps/include/libxml2
                    ${native_lib_DIR}/libs/deps/include/pixman-1
                    ${native_lib_DIR}/libs/deps/include/libsoup-2.4
                    ${native_lib_DIR}/libs/deps/include/rest-0.7
                    ${native_lib_DIR}/libs/deps/include/usbredirhost
                    ${native_lib_DIR}/libs/deps/include/usbredirparser
                    ${native_lib_DIR}/libs/deps/include/glib-2.0
                    ${native_lib_DIR}/libs/deps/include/gio-unix-2.0
                    ${native_lib_DIR}/libs/deps/include/FLAC
                    ${native_src_DIR}
                    ${native_src_DIR}/android
                    ${native_src_DIR}/gtk
                    ${native_src_DIR}/spice-common
                    ${native_src_DIR}/spice-common/spice-protocol
                    ${native_src_DIR}/virt-viewer)


set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

add_library(libspice SHARED ${spice_SRCS1} ${spice_SRCS2} ${spice_SRCS3} ${spice_SRCS4} ${spice_SRCS5})
target_link_libraries(libspice log android lib_celt lib_crypto lib_govirt lib_jpeg
                      lib_rest lib_ssl lib_usb lib_usbredirhost lib_usbredirparser
                      lib_xml lib_soup lib_flac)
