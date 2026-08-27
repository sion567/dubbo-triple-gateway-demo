#!/usr/bin/env bash
# 下载并解压 SkyWalking Java Agent 到项目根目录 skywalking-agent/（一次性操作）
set -e
VERSION=9.6.0
cd "$(dirname "$0")/.."
if [ -d skywalking-agent ]; then
  echo "skywalking-agent/ 已存在，跳过"
  exit 0
fi
curl -L -o sw-agent.tar.gz \
  "https://archive.apache.org/dist/skywalking/javaagent/${VERSION}/apache-skywalking-java-agent-${VERSION}.tgz"
tar -xzf sw-agent.tar.gz
mv skywalking-agent skywalking-agent
rm sw-agent.tar.gz
echo "完成: $(pwd)/skywalking-agent/skywalking-agent.jar"
