#!/bin/bash
# POS Test App - Build and Run Script (macOS / Linux)

echo "========================================"
echo "  POS Test App - Build Script"
echo "========================================"
echo

# Check Java
if ! command -v java &> /dev/null; then
    echo "[ERROR] Java not found! Please install Java 11 or higher."
    echo "macOS: brew install openjdk@17"
    echo "Ubuntu: sudo apt install openjdk-17-jdk"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
echo "[OK] Java found (version $JAVA_VERSION)"

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo "[ERROR] Maven not found!"
    echo "macOS: brew install maven"
    echo "Ubuntu: sudo apt install maven"
    exit 1
fi

echo "[OK] Maven found."
echo
echo "[BUILD] Compiling and packaging..."
echo

mvn clean package -q

if [ $? -ne 0 ]; then
    echo "[ERROR] Build failed!"
    exit 1
fi

echo "[SUCCESS] Build complete!"
echo
echo "Running POS Test Tool..."
java -jar -Dfile.encoding=UTF-8 target/POSTestApp-1.0.0.jar
