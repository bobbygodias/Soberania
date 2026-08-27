/*
 * Soberania WireGuard JNI shim.
 *
 * Original glue for the Soberania project.
 * Does not implement WireGuard cryptography.
 */

#include <jni.h>
#include <stdlib.h>

/*
 * Exported by the Go c-shared adapter.
 */
extern int soberaniaWgTurnOn(char *interface_name, int tun_fd, char *settings);
extern void soberaniaWgTurnOff(int handle);
extern int soberaniaWgSocketV4(int handle);
extern int soberaniaWgSocketV6(int handle);
extern char *soberaniaWgVersion(void);

JNIEXPORT jint JNICALL
Java_org_soberania_app_transport_wireguard_JniWireGuardNativeEngine_nativeTurnOn(
        JNIEnv *env,
        jobject self,
        jstring interface_name,
        jint tun_fd,
        jstring settings) {
    (void) self;

    if (interface_name == NULL || settings == NULL) {
        return -1;
    }

    const char *name_chars = (*env)->GetStringUTFChars(env, interface_name, NULL);
    if (name_chars == NULL) {
        return -1;
    }

    const char *settings_chars = (*env)->GetStringUTFChars(env, settings, NULL);
    if (settings_chars == NULL) {
        (*env)->ReleaseStringUTFChars(env, interface_name, name_chars);
        return -1;
    }

    int result = soberaniaWgTurnOn(
        (char *) name_chars,
        (int) tun_fd,
        (char *) settings_chars
    );

    (*env)->ReleaseStringUTFChars(env, settings, settings_chars);
    (*env)->ReleaseStringUTFChars(env, interface_name, name_chars);

    return (jint) result;
}

JNIEXPORT void JNICALL
Java_org_soberania_app_transport_wireguard_JniWireGuardNativeEngine_nativeTurnOff(
        JNIEnv *env,
        jobject self,
        jint handle) {
    (void) env;
    (void) self;
    soberaniaWgTurnOff((int) handle);
}

JNIEXPORT jint JNICALL
Java_org_soberania_app_transport_wireguard_JniWireGuardNativeEngine_nativeSocketV4(
        JNIEnv *env,
        jobject self,
        jint handle) {
    (void) env;
    (void) self;
    return (jint) soberaniaWgSocketV4((int) handle);
}

JNIEXPORT jint JNICALL
Java_org_soberania_app_transport_wireguard_JniWireGuardNativeEngine_nativeSocketV6(
        JNIEnv *env,
        jobject self,
        jint handle) {
    (void) env;
    (void) self;
    return (jint) soberaniaWgSocketV6((int) handle);
}

JNIEXPORT jstring JNICALL
Java_org_soberania_app_transport_wireguard_JniWireGuardNativeEngine_nativeVersion(
        JNIEnv *env,
        jobject self) {
    (void) self;

    char *version = soberaniaWgVersion();
    if (version == NULL) {
        return NULL;
    }

    jstring result = (*env)->NewStringUTF(env, version);
    free(version);
    return result;
}
