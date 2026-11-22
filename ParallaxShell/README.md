# parallax-shell

[![](https://img.shields.io/github/license/parallax/parallax-shell)](https://github.com/parallax/parallax-shell/blob/main/LICENSE) [![](https://img.shields.io/github/downloads/parallax/parallax-shell/total?color=blue)](https://github.com/parallax/parallax-shell/releases/latest) [![](https://img.shields.io/github/issues-raw/parallax/parallax-shell?color=red)](https://github.com/parallax/parallax-shell/issues) ![](https://img.shields.io/badge/Android-5.0%2B-brightgreen)

English | [简体中文](./README.zh-CN.md)

parallax-shell is an Android Dex protection shell that hollows out Dex method implementations and reconstructs them at runtime.

## Usage

### Quick uses

Go to [Releases](https://github.com/parallax/parallax-shell/releases/latest) download `executable.zip` and unzip it, run the follow command lines in terminal:

```shell
java -jar parallax.jar -f /path/to/android-package-file
```

### Optional VM conversion for APKs

Pass a locally supplied, nmmp-compatible VM converter to convert supported
method bodies before applying the Parallax shell to the remaining DEX:

```shell
java -jar parallax.jar -f app.apk --vmp-jar /path/to/vm-protect.jar
java -jar parallax.jar -f app.apk --vmp-jar /path/to/vm-protect.jar --vmp-rules /path/to/convertRules.txt
```

The VM converter, Android SDK and NDK must be configured as required by that
converter. Parallax invokes it with `apk <input> [rules]`, checks that it
produced an APK, and fails instead of packaging a stale or missing result.
Conversion runs in a fresh temporary directory; Parallax then signs the final
APK using its usual signing path. This option applies only to APK inputs.

This is a two-stage integration, not a Parallax VM implementation. The VM
backend decides which methods it can convert. Constructors, class initializers,
excluded or unsupported methods, and remaining Java/Kotlin classes may still
require ART bytecode. The shell still restores remaining method instructions in
DEX memory. Test startup and app functionality on actual Android devices before
distributing a protected APK. Root/Frida checks do not make a client-side app
immune to runtime dumping.

### Manual builds

```shell
git clone --recursive https://github.com/parallax/parallax-shell
cd parallax-shell
./gradlew assemble
cd executable
java -jar parallax.jar -f /path/to/android-package-file
```

### Command line options

```text
usage: java -jar parallax.jar [option] -f <package_file>
 -c,--protect-config <arg>   Protect config file.

    --debug                  Make package debuggable.
    --disable-acf            Disable app component factory(just use for
                             debug).
    --disable-anti-debug     Disable runtime anti-debug.
    --disable-crc-detect     Disable runtime libc .text CRC detection.

    --disable-frida-detect   Disable runtime Frida detection.

    --dump-code              Dump the code item of DEX and save it to
                             .json files.
 -e,--exclude-abi <arg>      Exclude specific ABIs (comma separated, e.g.
                             x86,x86_64).
                             Supported ABIs:
                             - arm       (armeabi-v7a)
                             - arm64     (arm64-v8a)
                             - x86
                             - x86_64
 -f,--package-file <arg>     Need to protect android package(*.apk, *.aab)
                             file.
 -K,--keep-classes           Keeping some classes in the package can
                             improve the app's startup speed to a certain
                             extent, but it is not supported by some
                             application packages.
    --noisy-log              Open noisy log.
 -o,--output <arg>           Output directory for protected package.
 -r,--rules-file <arg>       Rules file for class names that will not be
                             protected.
 -S,--smaller                Trade some of the app's performance for a
                             smaller app size.
 -v,--version                Show program's version number.
 -vs,--verify-sign           Enable runtime app signature verification.
                             The certificate SHA-256 is computed
                             automatically from the signing keystore.
 -x,--no-sign                Do not sign package.
```

## Notice

This project has not too many tests, be careful use in prod environment. Otherwise, all consequences are at your own risk.

## Dependency or use follows project code

- [dx](https://android.googlesource.com/platform/dalvik/+/refs/heads/master/dx/)
- [Dobby](https://github.com/jmpews/Dobby)
- ~~[libzip-android](https://github.com/julienr/libzip-android)~~
- [ManifestEditor](https://github.com/WindySha/ManifestEditor)
- ~~[Xpatch](https://github.com/WindySha/Xpatch)~~
- [bhook](https://github.com/bytedance/bhook)
- [zipalign-java](https://github.com/Iyxan23/zipalign-java)
- [minizip-ng](https://github.com/zlib-ng/minizip-ng)
- [JSON-java](https://github.com/stleary/JSON-java)
- [zip4j](https://github.com/srikanth-lingala/zip4j)
- [commons-cli](https://github.com/apache/commons-cli)
- [dexmaker](https://android.googlesource.com/platform/external/dexmaker)
- [Obfuscate](https://github.com/adamyaxley/Obfuscate)
