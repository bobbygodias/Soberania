include versions.mk

ANDROID_API ?= 26
NDK_VERSION ?= 28.2.13676358
NDK_ROOT ?= $(ANDROID_HOME)/ndk/$(NDK_VERSION)
NDK_HOST ?= linux-x86_64

TOOLCHAIN := $(NDK_ROOT)/toolchains/llvm/prebuilt/$(NDK_HOST)
CC := $(TOOLCHAIN)/bin/aarch64-linux-android$(ANDROID_API)-clang

BUILD_DIR := $(CURDIR)/build/arm64-v8a
GO_LIBRARY := $(BUILD_DIR)/libsoberania-wireguard-go.so
JNI_LIBRARY := $(BUILD_DIR)/libsoberania-wg.so

.PHONY: verify build-arm64 build-go-arm64 build-jni-arm64 clean

verify:
	@test "$$(go env GOVERSION)" = "go$(SOBERANIA_GO_VERSION)" || 		( echo "Expected Go $(SOBERANIA_GO_VERSION), got $$(go env GOVERSION)" && exit 1 )
	@test -x "$(CC)" || 		( echo "Android NDK compiler not found: $(CC)" && exit 1 )
	go mod verify

build-arm64: verify build-go-arm64 build-jni-arm64

build-go-arm64:
	mkdir -p "$(BUILD_DIR)"
	GOOS=android 	GOARCH=arm64 	CGO_ENABLED=1 	CC="$(CC)" 	CGO_LDFLAGS="-Wl,-soname,libsoberania-wireguard-go.so" 	go build 		-tags linux 		-trimpath 		-buildvcs=false 		-ldflags="-buildid=" 		-buildmode=c-shared 		-o "$(GO_LIBRARY)" 		./adapter

build-jni-arm64:
	"$(CC)" 		-shared 		-fPIC 		-Wl,--no-undefined 		-Wl,-soname,libsoberania-wg.so 		"$(CURDIR)/jni/soberania_wireguard_jni.c" 		-L"$(BUILD_DIR)" 		-Wl,-rpath-link,"$(BUILD_DIR)" 		-lsoberania-wireguard-go 		-o "$(JNI_LIBRARY)"

clean:
	rm -rf "$(CURDIR)/build"
