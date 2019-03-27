# Add appropriate copyright banner here
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
 
# We only want this apk build for tests.
LOCAL_MODULE_TAGS := tests
  
# Include all test java files.
LOCAL_SRC_FILES := $(call all-java-files-under, src)
   
# Notice that we don't have to include the src files of ApiDemos because, by
# running the tests using an instrumentation targeting ApiDemos, we
# automatically get all of its classes loaded into our environment.
    
LOCAL_PACKAGE_NAME := Media3DTests

LOCAL_JAVA_LIBRARIES := android.test.runner

LOCAL_STATIC_JAVA_LIBRARIES := libjunitreport-for-media3d-tests hamcrest-core-for-media3d-tests hamcrest-library-for-media3d-tests robotium-solo-for-media3d-tests libperfhelper

LOCAL_INSTRUMENTATION_FOR := Media3D
      
include $(BUILD_PACKAGE)

##################################################
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := eng
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := libjunitreport-for-media3d-tests:libs/android-junit-report-dev.jar hamcrest-core-for-media3d-tests:libs/hamcrest-core-SNAPSHOT.jar hamcrest-library-for-media3d-tests:libs/hamcrest-library-SNAPSHOT.jar robotium-solo-for-media3d-tests:libs/robotium-solo-3.1.jar libperfhelper:libs/perfhelper.jar
include $(BUILD_MULTI_PREBUILT)

