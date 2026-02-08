import re

# 定义要查找的21个消息
messages = [
    "SCSellFrag", "SCFragInfo", "SCOneFragInfo", "SCGeneralToAttIng", 
    "SCGeneralList", "SCGeneralInfo", "CSGeneralExpAdd", "CSGeneralEquipUp", 
    "DInitDataStage", "CSOneFragInfo", "CSSellFrag", "CSEnterGeneralTask", 
    "CSGeneralInfoAttIng", "SCGeneralInfoAttIng", "SCGeneralRecruitResult", 
    "SCGeneralStarUp", "SCGeneralQualityUp", "SCGeneralEquipUp"
]

# 读取文件
with open('d:\work\Java\game-server-mmoarpg-lz\lzWorldSrv\gen\org\gof\demo\worldsrv\msg\Msg.java.bak', 'r', encoding='utf-8') as f:
    content = f.read()

# 查找每个消息的位置
for msg in messages:
    pattern = rf'public static final class {msg} extends[\s\S]*?(?=public static final class|public interface|$)'
    match = re.search(pattern, content)
    if match:
        class_content = match.group(0)
        # 提取字段定义
        field_pattern = r'//\s*(required|optional|repeated)\s+([^\s]+)\s+(\w+)\s*=\s*([0-9]+);'
        fields = re.findall(field_pattern, class_content)
        
        print(f"=== {msg} ===")
        for field in fields:
            modifier, type, name, number = field
            print(f"字段{number}: {type} {name} = {number} [{modifier}]")
        print()
