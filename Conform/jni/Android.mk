LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := conform
LOCAL_SRC_FILES := fixed_func.cpp bitmapper.cpp conform.cpp

#LOCAL_CFLAGS := -std=c++11 -O3 -ffast-math -funroll-loops
LOCAL_CFLAGS := -std=c++11 -Ofast -ffast-math -funroll-loops
#LOCAL_CFLAGS := -std=c++11 -ggdb3
LOCAL_LDLIBS := -ljnigraphics -llog

include $(BUILD_SHARED_LIBRARY)
