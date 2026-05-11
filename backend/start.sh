#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# 解析真实路径，避免通过软链调用时 cwd/数据目录与 jar 不在同一处
if command -v realpath >/dev/null 2>&1; then
  SCRIPT_DIR="$(realpath "$SCRIPT_DIR")"
fi

cd "$SCRIPT_DIR"

# 上传目录必须用绝对路径：仅依赖「启动时的 cwd」在 nohup/systemd 等场景下不可靠，
# 且需与 app.upload-dir / app.album-image-dir（见 application.yml）一致。
export APP_UPLOAD_DIR="$SCRIPT_DIR/uploads"
export APP_ALBUM_IMAGE_DIR="$SCRIPT_DIR/uploads/albums"
mkdir -p "$APP_ALBUM_IMAGE_DIR"

PID_FILE="$SCRIPT_DIR/.server.pid"
JAR_NAME="chat-app.jar"
JAR_PATH="$SCRIPT_DIR/$JAR_NAME"

if [ -f "$PID_FILE" ]; then
  OLD_PID=$(cat "$PID_FILE")
  if kill -0 "$OLD_PID" 2>/dev/null; then
    echo "服务已在运行 (PID=$OLD_PID)，如需重启请先执行 ./stop.sh"
    exit 1
  fi
  rm -f "$PID_FILE"
fi

if [ ! -f "$JAR_PATH" ]; then
  echo "未找到 $JAR_PATH，请先执行 mvn package 生成 jar 后再启动"
  exit 1
fi

mkdir -p "$SCRIPT_DIR/logs"
nohup java -jar "$JAR_PATH" >> "$SCRIPT_DIR/logs/server.log" 2>&1 &
echo $! > "$PID_FILE"
echo "服务已启动 (PID=$(cat "$PID_FILE"))，日志: logs/server.log"
echo "上传目录: $APP_UPLOAD_DIR（请确认历史文件也在此目录下，否则请迁移或调整环境变量）"