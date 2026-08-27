include versions.mk

ANDROID_API ?= 26
NDK_VERSION ?= 28.2.13676358
NDK_ROOT ?= $(ANDROID_HOME)/ndk/$(NDK_VERSION)
NDK_HOST ?= linux-x86_64

TOOLCHAIN := $(NDK_ROOT)/toolchains/llvm/prebuilt/$(NDK_HOST)

ARM64_CC := $(TOOLCHAIN)/bin/aarch64-linux-android$(ANDROID_API)-clang
X86_64_CC := $(TOOLCHAIN)/bin/x86_64-linux-android$(ANDROID_API)-clang

ARM64_BUILD_DIR := $(CURDIR)/build/arm64-v8a
X86_64_BUILD_DIR := $(CURDIR)/build/x86_64

ARM64_GO_LIBRARY := $(ARM64_BUILD_DIR)/libsoberania-wireguard-go.so
ARM64_JNI_LIBRARY := $(ARM64_BUILD_DIR)/libsoberania-wg.so

X86_64_GO_LIBRARY := $(X86_64_BUILD_DIR)/libsoberania-wireguard-go.so
X86_64_JNI_LIBRARY := $(X86_64_BUILD_DIR)/libsoberania-wg.so

.PHONY: \
	verify-go verify-arm64 verify-x86_64 \
	build-arm64 build-go-arm64 build-jni-arm64 \
	build-x86_64 build-go-x86_64 build-jni-x86_64 \
	build-all clean

verify-go:
	@test "$$(go env GOVERSION)" = "go$(SOBERANIA_GO_VERSION)" || \
		( echo "Expected Go $(SOBERANIA_GO_VERSION), got $$(go env GOVERSION)" && exit 1 )
	go mod verify

verify-arm64: verify-go
	@test -x "$(ARM64_CC)" || \
		( echo "Android NDK arm64 compiler not found: $(ARM64_CC)" && exit 1 )

verify-x86_64: verify-go
	@test -x "$(X86_64_CC)" || \
		( echo "Android NDK x86_64 compiler not found: $(X86_64_CC)" && exit 1 )

build-arm64: verify-arm64 build-go-arm64 build-jni-arm64

build-go-arm64:
	mkdir -p "$(ARM64_BUILD_DIR)"
	GOOS=android \
	GOARCH=arm64 \
	CGO_ENABLED=1 \
	CC="$(ARM64_CC)" \
	CGO_LDFLAGS="-Wl,-soname,libsoberania-wireguard-go.so" \
	go build \
		-tags linux \
		-trimpath \
		-buildvcs=false \
		-ldflags="-buildid=" \
		-buildmode=c-shared \
		-o "$(ARM64_GO_LIBRARY)" \
		./adapter

build-jni-arm64:
	"$(ARM64_CC)" \
		-shared \
		-fPIC \
		-Wl,--no-undefined \
		-Wl,-soname,libsoberania-wg.so \
		"$(CURDIR)/jni/soberania_wireguard_jni.c" \
		-L"$(ARM64_BUILD_DIR)" \
		-Wl,-rpath-link,"$(ARM64_BUILD_DIR)" \
		-lsoberania-wireguard-go \
		-o "$(ARM64_JNI_LIBRARY)"

build-x86_64: verify-x86_64 build-go-x86_64 build-jni-x86_64

build-go-x86_64:
	mkdir -p "$(X86_64_BUILD_DIR)"
	GOOS=android \
	GOARCH=amd64 \
	CGO_ENABLED=1 \
	CC="$(X86_64_CC)" \
	CGO_LDFLAGS="-Wl,-soname,libsoberania-wireguard-go.so" \
	go build \
		-tags linux \
		-trimpath \
		-buildvcs=false \
		-ldflags="-buildid=" \
		-buildmode=c-shared \
		-o "$(X86_64_GO_LIBRARY)" \
		./adapter

build-jni-x86_64:
	"$(X86_64_CC)" \
		-shared \
		-fPIC \
		-Wl,--no-undefined \
		-Wl,-soname,libsoberania-wg.so \
		"$(CURDIR)/jni/soberania_wireguard_jni.c" \
		-L"$(X86_64_BUILD_DIR)" \
		-Wl,-rpath-link,"$(X86_64_BUILD_DIR)" \
		-lsoberania-wireguard-go \
		-o "$(X86_64_JNI_LIBRARY)"

build-all: build-arm64 build-x86_64

clean:
	rm -rf "$(CURDIR)/build"
