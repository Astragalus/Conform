LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := conform
LOCAL_SRC_FILES := conform.cpp

LOCAL_LDLIBS    += -ljnigraphics

include $(BUILD_SHARED_LIBRARY)
