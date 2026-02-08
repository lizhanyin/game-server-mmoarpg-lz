#!/bin/bash

# 合并拆分的 proto 文件

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

OUTPUT_FILE="$SCRIPT_DIR/msg.proto"
SPLIT_DIR="$SCRIPT_DIR/split"

echo "========================================"
echo "Merging proto files from $SPLIT_DIR"
echo "Output: $OUTPUT_FILE"
echo "========================================"
echo ""

# 创建输出文件
cat > "$OUTPUT_FILE" << 'HEADER'
// msg.proto
// 合并的 proto 文件，包含所有消息定义
syntax = "proto2";

package org.gof.demo.worldsrv.msg;

import "google/protobuf/descriptor.proto";
import "options.proto";

HEADER

# 处理每个文件
for f in "$SPLIT_DIR"/*.proto; do
    filename=$(basename "$f")
    echo "Processing: $filename"
    
    echo "" >> "$OUTPUT_FILE"
    echo "// ===== $filename =====" >> "$OUTPUT_FILE"
    
    # 读取文件内容，跳过头部声明
    in_header=0
    while IFS= read -r line || [ -n "$line" ]; do
        # 跳过文件开头的注释
        if [[ "$line" =~ ^//[0-9]+_[0-9]+_.*\.proto ]]; then
            continue
        fi
        # 跳过 syntax 声明
        if [[ "$line" =~ ^syntax ]]; then
            in_header=1
            continue
        fi
        # 跳过 package 声明
        if [[ "$line" =~ ^package ]]; then
            continue
        fi
        # 跳过 import 声明
        if [[ "$line" =~ ^import ]]; then
            continue
        fi
        # 跳过空行（如果前面也是空行）
        if [[ "$line" =~ ^[[:space:]]*$ ]]; then
            if [[ "$in_header" -eq 1 ]]; then
                continue
            fi
        fi

        in_header=0
        echo "$line" >> "$OUTPUT_FILE"
    done < "$f"
    
    echo "" >> "$OUTPUT_FILE"
done

echo ""
echo "========================================"
echo "Merge completed!"
echo "Output file: $OUTPUT_FILE"
echo "========================================"
