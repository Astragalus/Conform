LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := conform
LOCAL_SRC_FILES := fixed_func.cpp bitmapper.cpp conform.cpp

LOCAL_CFLAGS := -std=c++11 -pthread -O3 -Ofast -ffast-math -funroll-loops -faggressive-loop-optimizations -falign-functions \
				-falign-loops -fassociative-math -fexpensive-optimizations -ffinite-math-only \
				-floop-parallelize-all -floop-block -floop-interchange -floop-strip-mine -floop-nest-optimize  
				
#LOCAL_CFLAGS := -std=c++11 -ggdb3
LOCAL_LDLIBS := -ljnigraphics -llog

include $(BUILD_SHARED_LIBRARY)
