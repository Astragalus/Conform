LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := conform
LOCAL_SRC_FILES := conform.cpp bitmapper.cpp

LOCAL_CFLAGS := -O3
LOCAL_LDLIBS := -ljnigraphics -llog

include $(BUILD_SHARED_LIBRARY)
