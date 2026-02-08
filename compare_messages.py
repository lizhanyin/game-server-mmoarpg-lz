#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
比较Proto定义和Java接口的消息字段
"""

import re
import os

# 需要检查的消息列表
MESSAGES_TO_CHECK = [
    "SCSellFrag",
    "SCFragInfo",
    "SCOneFragInfo",
    "SCGeneralToAttIng",
    "DGeneralInfo",
    "DGeneralEquipment",
    "SCGeneralList",
    "SCGeneralInfo",
    "CSGeneralExpAdd",
    "CSGeneralEquipUp",
    "DInitDataStage",
    "CSOneFragInfo",
    "CSSellFrag",
    "CSEnterGeneralTask",
    "CSGeneralToAttIng",
    "CSGeneralInfoAttIng",
    "SCGeneralInfoAttIng",
    "SCGeneralRecruitResult",
    "SCGeneralExpAdd",
    "SCGeneralStarUp",
    "SCGeneralQualityUp",
    "SCGeneralEquipUp",
]

def extract_proto_fields(proto_file_path, message_name):
    """从proto文件中提取消息的字段定义"""
    with open(proto_file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # 查找消息定义
    pattern = rf'message {message_name}\s*\{{([^}}]+?)\}}'
    matches = re.findall(pattern, content, re.DOTALL)

    if not matches:
        return None

    # 使用最后一个匹配(可能存在多个同名消息)
    message_content = matches[-1]

    # 提取字段
    fields = []
    field_pattern = r'(optional|required|repeated)\s+(\w+)\s+(\w+)\s*=\s*(\d+);'
    for match in re.finditer(field_pattern, message_content):
        field_type = match.group(1)  # optional, required, repeated
        data_type = match.group(2)   # int32, string, etc.
        field_name = match.group(3)  # 字段名
        field_number = match.group(4)  # 字段编号

        fields.append({
            'name': field_name,
            'type': data_type,
            'modifier': field_type,
            'number': field_number
        })

    return fields

def extract_java_fields(java_file_path, message_name):
    """从Java文件中提取消息的字段定义"""
    with open(java_file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # 查找接口定义
    interface_pattern = rf'public interface {message_name}OrBuilder\s*extends[^{{]+\{{([^}}]+?)\}}'
    interface_match = re.search(interface_pattern, content, re.DOTALL)

    if not interface_match:
        return None

    interface_content = interface_match.group(1)

    # 提取字段注释和getter方法
    fields = []
    # 匹配字段定义注释,如: // required int32 result = 1;
    field_comment_pattern = r'//\s+(optional|required|repeated)\s+(\w+(?:\.\w+)*)\s+(\w+)\s*=\s*(\d+);'
    for match in re.finditer(field_comment_pattern, interface_content):
        field_modifier = match.group(1)
        field_type = match.group(2)
        field_name = match.group(3)
        field_number = match.group(4)

        # 处理嵌套类型,如 org.gof.demo.worldsrv.msg.DFragInfo -> DFragInfo
        if '.' in field_type:
            field_type = field_type.split('.')[-1]

        fields.append({
            'name': field_name,
            'type': field_type,
            'modifier': field_modifier,
            'number': field_number
        })

    return fields

def compare_fields(proto_fields, java_fields):
    """比较proto和Java字段"""
    if proto_fields is None and java_fields is None:
        return "未找到定义", [], []

    if proto_fields is None:
        return "Proto定义未找到", java_fields, []

    if java_fields is None:
        return "Java定义未找到", [], proto_fields

    # 转换为字典方便比较
    proto_dict = {f['name']: f for f in proto_fields}
    java_dict = {f['name']: f for f in java_fields}

    all_field_names = set(proto_dict.keys()) | set(java_dict.keys())

    mismatches = []
    for field_name in all_field_names:
        if field_name not in proto_dict:
            mismatches.append(f"字段 {field_name}: 在Proto中不存在")
        elif field_name not in java_dict:
            mismatches.append(f"字段 {field_name}: 在Java中不存在")
        else:
            proto_field = proto_dict[field_name]
            java_field = java_dict[field_name]

            # 比较类型
            if proto_field['type'] != java_field['type']:
                mismatches.append(f"字段 {field_name}: 类型不匹配 (Proto: {proto_field['type']}, Java: {java_field['type']})")

            # 比较修饰符
            if proto_field['modifier'] != java_field['modifier']:
                mismatches.append(f"字段 {field_name}: 修饰符不匹配 (Proto: {proto_field['modifier']}, Java: {java_field['modifier']})")

            # 比较编号
            if proto_field['number'] != java_field['number']:
                mismatches.append(f"字段 {field_name}: 编号不匹配 (Proto: {proto_field['number']}, Java: {java_field['number']})")

    if mismatches:
        return "不匹配", proto_fields, java_fields, mismatches
    else:
        return "匹配", proto_fields, java_fields, []

def main():
    proto_file = r"d:\work\Java\game-server-mmoarpg-lz\tools\proto\split\01401_01800_general.proto"
    def_file = r"d:\work\Java\game-server-mmoarpg-lz\tools\proto\split\00000_00000_def.proto"
    java_file = r"d:\work\Java\game-server-mmoarpg-lz\lzWorldSrv\gen\org\gof\demo\worldsrv\msg\Msg.java.bak"

    results = []

    print("=" * 120)
    print(f"{'消息名':<30} {'是否匹配':<10} {'Java接口字段':<30} {'Proto定义字段':<30} {'需要修复的内容'}")
    print("=" * 120)

    for message_name in MESSAGES_TO_CHECK:
        # 确定在哪个proto文件中查找
        if message_name.startswith('D'):
            # 数据结构定义在def.proto中
            proto_fields = extract_proto_fields(def_file, message_name)
        else:
            # 消息定义在general.proto中
            proto_fields = extract_proto_fields(proto_file, message_name)

        java_fields = extract_java_fields(java_file, message_name)

        if proto_fields is None and java_fields is None:
            status = "未找到定义"
            java_field_str = ""
            proto_field_str = ""
            fix_content = "消息定义不存在"
        elif proto_fields is None:
            status = "不匹配"
            java_field_str = ", ".join([f['name'] for f in java_fields])
            proto_field_str = ""
            fix_content = "Proto定义未找到"
        elif java_fields is None:
            status = "不匹配"
            java_field_str = ""
            proto_field_str = ", ".join([f['name'] for f in proto_fields])
            fix_content = "Java定义未找到"
        else:
            result, pf, jf, mismatches = compare_fields(proto_fields, java_fields)
            status = result
            proto_field_str = ", ".join([f['name'] for f in proto_fields])
            java_field_str = ", ".join([f['name'] for f in java_fields])
            fix_content = "; ".join(mismatches) if mismatches else ""

        print(f"{message_name:<30} {status:<10} {java_field_str:<30} {proto_field_str:<30} {fix_content}")

        results.append({
            'name': message_name,
            'status': status,
            'java_fields': java_fields,
            'proto_fields': proto_fields,
            'fix_content': fix_content
        })

    print("=" * 120)

    # 输出详细报告
    print("\n\n详细报告:")
    print("=" * 120)

    for result in results:
        if result['status'] != "匹配":
            print(f"\n消息: {result['name']}")
            print(f"状态: {result['status']}")
            if result['java_fields']:
                print("Java字段:")
                for f in result['java_fields']:
                    print(f"  - {f['modifier']} {f['type']} {f['name']} = {f['number']}")
            if result['proto_fields']:
                print("Proto字段:")
                for f in result['proto_fields']:
                    print(f"  - {f['modifier']} {f['type']} {f['name']} = {f['number']}")
            print(f"修复内容: {result['fix_content']}")

if __name__ == "__main__":
    main()
