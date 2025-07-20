#!/bin/bash

set -e

# constants
SRC_DIR="src"
BUILD_DIR="build"
DELIVER_DIR="deliverables/Haign"
PKG_PATH="com/haign/wallet"
PACKAGE="com.haign.wallet"
APPLET_CLASS="WalletApplet"

JC_API_JAR="/opt/javacard-sdk/lib/api_classic-3.0.5.jar"
CONVERTER="/opt/javacard-sdk/bin/converter.sh"
EXP_PATH="/build"

# JavaCard AID and version
PACKAGE_AID=0xF0:0x00:0x00:0x00:0x01:0x00
APPLET_AID=0xF0:0x00:0x00:0x00:0x01:0x01
VERSION=1.0

# create directory
mkdir -p "$BUILD_DIR"
mkdir -p "$DELIVER_DIR"

echo "[+] Compiling Java sources..."
javac \
  -source 1.3 -target 1.3 \
  -bootclasspath "$JC_API_JAR" \
  -d "$BUILD_DIR" \
  $(find "$SRC_DIR" -name "*.java")

echo "[+] Converting to CAP..."
"$CONVERTER" \
  -classdir "$BUILD_DIR" \
  -applet 0xF0:0x00:0x00:0x00:0x01:0x01 com.haign.wallet.WalletApplet \
  -out CAP JCA EXP \
  -d "$DELIVER_DIR" \
  -exportpath "$BUILD_DIR" \
  -target 3.0.5 \
  com.haign.wallet 0xF0:0x00:0x00:0x00:0x01:0x00 1.0


echo "Build and conversion complete."
