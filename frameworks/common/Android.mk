# limitations under the License.
#

# This makefile shows how to build your own shared library that can be
# shipped on the system of a phone, and included additional examples of
# including JNI code with the library and writing client applications against it.

LOCAL_PATH := $(call my-dir)

# MediaTek common library.
# ============================================================
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE := mediatek-common

LOCAL_SRC_FILES := $(call all-java-files-under,src)

LOCAL_SRC_FILES += src/com/mediatek/common/agps/IMtkAgpsManager.aidl \
                   src/com/mediatek/common/audioprofile/IAudioProfileService.aidl \
                   src/com/mediatek/common/audioprofile/IAudioProfileListener.aidl \
                   src/com/mediatek/common/dm/DmAgent.aidl \
                   src/com/mediatek/common/voicecommand/IVoiceCommandListener.aidl \
                   src/com/mediatek/common/voicecommand/IVoiceCommandManagerService.aidl\
                   src/com/mediatek/common/telephony/ITelephonyEx.aidl\
                   src/com/mediatek/common/msgmonitorservice/IMessageLogger.aidl \
                   src/com/mediatek/common/msgmonitorservice/IMessageLoggerWrapper.aidl \
                   src/com/mediatek/common/search/ISearchEngineManagerService.aidl \
                   src/com/mediatek/common/perfservice/IPerfService.aidl \
                   src/com/mediatek/common/mom/IMobileManagerService.aidl \
                   src/com/mediatek/common/mom/IPermissionListener.aidl \
                   src/com/mediatek/common/mom/IRequestedPermissionCallback.aidl \
                   src/com/mediatek/common/mom/IPackageInstallCallback.aidl \
                   src/com/mediatek/common/mom/INotificationListener.aidl \
                   src/com/mediatek/common/mom/IMobileConnectionCallback.aidl \
                   src/com/mediatek/common/mom/ICallInterceptionListener.aidl \
                   src/com/mediatek/common/hdmi/IMtkHdmiManager.aidl \
                   src/com/mediatek/common/mom/IMessageInterceptListener.aidl \ 

#LOCAL_SRC_FILES += src/com/mediatek/common/IMyModuleCallback.aidl \
                   src/com/mediatek/common/dm/DmAgent.aidl \
                   src/com/mediatek/common/audioprofile/IAudioProfileService.aidl \
                   src/com/mediatek/common/audioprofile/IAudioProfileListener.aidl \
                   src/com/mediatek/common/agps/IMtkAgpsManager.aidl \

# Always use the latest prebuilt Android library.
LOCAL_SDK_VERSION := 16

include $(BUILD_JAVA_LIBRARY)
