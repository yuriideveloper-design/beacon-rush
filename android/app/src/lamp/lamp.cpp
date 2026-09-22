#include <jni.h>
#include <string>

namespace lampveil {

std::string unveil(const unsigned char *blob, size_t n) {
    static const unsigned char key[] = {
        0x2a, 0x91, 0x47, 0xde, 0x0c, 0x73, 0xb8, 0x15, 0xe4, 0x5f,
        0x06, 0xc1, 0x88, 0x3d, 0xa9, 0x52, 0x70, 0x1b, 0xf6
    };
    std::string out(n, 0);
    for (size_t i = 0; i < n; ++i) {
        unsigned char x = static_cast<unsigned char>(blob[i] ^ key[i % 19]);
        x = static_cast<unsigned char>((x >> 3) | (x << 5));
        x = static_cast<unsigned char>(x + static_cast<unsigned char>((i * 13 + 4) & 0xFF));
        out[i] = static_cast<char>(x);
    }
    return out;
}

void park(JNIEnv *env, jobject bag, jmethodID put,
        const unsigned char *slot, size_t slotN,
        const unsigned char *payload, size_t payloadN) {
    const std::string key = unveil(slot, slotN);
    const std::string value = unveil(payload, payloadN);
    env->CallObjectMethod(bag, put, env->NewStringUTF(key.c_str()), env->NewStringUTF(value.c_str()));
}

}

extern "C" JNIEXPORT jobject JNICALL
Java_com_pixelhaven_beaconrush_quay_LampCask_nativeRoster(JNIEnv *env, jobject) {
    jclass mapClass = env->FindClass("java/util/HashMap");
    if (mapClass == nullptr) return nullptr;
    jmethodID init = env->GetMethodID(mapClass, "<init>", "(I)V");
    jmethodID put = env->GetMethodID(mapClass, "put",
        "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    jobject bag = env->NewObject(mapClass, init, 16);

    static const unsigned char k0k[] = { 0x11, 0x69 };
    static const unsigned char k0v[] = {
        0x09, 0x8a, 0xf5, 0xf4, 0xd5, 0xdc, 0x56, 0x93, 0x53, 0x38,
        0xd8, 0x47, 0xf6, 0x33, 0x6c, 0x27, 0x8c, 0x27, 0xf4, 0xe9,
        0x92, 0x95, 0xac
    };
    lampveil::park(env, bag, put, k0k, sizeof(k0k), k0v, sizeof(k0v));

    static const unsigned char k1k[] = { 0x11, 0x90 };
    static const unsigned char k1v[] = {
        0x73, 0xda, 0xcf, 0xb6, 0xeb, 0xec, 0x28, 0x25, 0x2b
    };
    lampveil::park(env, bag, put, k1k, sizeof(k1k), k1v, sizeof(k1v));

    static const unsigned char k2k[] = { 0x11, 0x98 };
    static const unsigned char k2v[] = {
        0x73, 0xb2, 0xcf, 0x96, 0xd3, 0x83, 0x38, 0x0d, 0x5b, 0xea,
        0xf0, 0x25, 0x56, 0xd8, 0xfc, 0xe7
    };
    lampveil::park(env, bag, put, k2k, sizeof(k2k), k2v, sizeof(k2v));

    static const unsigned char k3k[] = { 0x11, 0x80 };
    static const unsigned char k3v[] = {
        0x73, 0xb2, 0xcf, 0x96, 0xd3, 0x83, 0x38, 0x0d, 0x5b, 0xea,
        0x49, 0x25, 0x56, 0xd8, 0xfc, 0xe7
    };
    lampveil::park(env, bag, put, k3k, sizeof(k3k), k3v, sizeof(k3v));

    static const unsigned char k4k[] = { 0x11, 0x88 };
    static const unsigned char k4v[] = {
        0x88, 0x71, 0x05, 0xbe, 0x65, 0xe4, 0x66, 0x5a, 0xc2
    };
    lampveil::park(env, bag, put, k4k, sizeof(k4k), k4v, sizeof(k4v));

    static const unsigned char k5k[] = { 0x11, 0xb0 };
    static const unsigned char k5v[] = {
        0x88, 0x71, 0x7d, 0xbe, 0xeb, 0xec, 0x66, 0x5a, 0xca
    };
    lampveil::park(env, bag, put, k5k, sizeof(k5k), k5v, sizeof(k5v));

    static const unsigned char k6k[] = { 0x11, 0xb8 };
    static const unsigned char k6v[] = { 0x48, 0x60, 0xee, 0x97 };
    lampveil::park(env, bag, put, k6k, sizeof(k6k), k6v, sizeof(k6v));

    static const unsigned char k7k[] = { 0x11, 0xa0 };
    static const unsigned char k7v[] = {
        0x21, 0x80, 0x9f, 0x86, 0x5d, 0x2c, 0x20, 0xc3, 0x8a, 0x00,
        0x63, 0xf4, 0x3c, 0x71, 0x42, 0x09, 0xfc, 0x3f, 0x45, 0x79,
        0xe0, 0x4e, 0x5e, 0x3c, 0x9c, 0x87, 0x75, 0xec, 0xc0, 0x9b,
        0x17, 0x74
    };
    lampveil::park(env, bag, put, k7k, sizeof(k7k), k7v, sizeof(k7v));

    return bag;
}
