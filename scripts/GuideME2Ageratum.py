import shutil
import os
import re

GuideME_path = r"D:\usualUsing\ProgramAndScript\PY\study\guidebook"
Ageratum_path = r"D:\usualUsing\ProgramAndScript\Java\AnvilCraft\AnvilCraft\run\ageratum_preview"

# 删除 Ageratum_path 下的所有内容
if os.path.exists(Ageratum_path):
    shutil.rmtree(Ageratum_path)

# 将 GuideME_path 的内容复制到 Ageratum_path
shutil.copytree(GuideME_path, Ageratum_path)

# 如果主文件夹下同时存在 name 文件夹和 name.md，将 name.md 移入 name 文件夹并重命名为 index.md
for item in os.listdir(Ageratum_path):
    if os.path.isdir(os.path.join(Ageratum_path, item)):
        md_file = os.path.join(Ageratum_path, item + ".md")
        if os.path.isfile(md_file):
            dest = os.path.join(Ageratum_path, item, "index.md")
            shutil.move(md_file, dest)

# 扫描所有 md 文件，构建 item_id → 文件相对路径的映射
item_id_to_file = {}  # "namespace:id" → md文件相对于 Ageratum_path 的路径
for root, dirs, files in os.walk(Ageratum_path):
    for file in files:
        if file.endswith(".md"):
            filepath = os.path.join(root, file)
            with open(filepath, "r", encoding="utf-8") as f:
                content = f.read()
            # 匹配 item_ids: 或 items: 下的列表项
            match = re.search(r'^item_ids:\s*\n((?:\s+-\s+.+\n?)*)', content, re.MULTILINE)
            if match:
                items_block = match.group(1)
                for item_match in re.finditer(r'^\s+-\s+(.+)$', items_block, re.MULTILINE):
                    item_id = item_match.group(1).strip()
                    rel_path = os.path.relpath(filepath, Ageratum_path).replace('\\', '/')
                    item_id_to_file[item_id] = rel_path

# 遍历所有 md 文件，将 <Recipe .../> 和 <Recipe ...></Recipe> 统一替换为 <recipe .../>
for root, dirs, files in os.walk(Ageratum_path):
    for file in files:
        if file.endswith(".md"):
            filepath = os.path.join(root, file)
            with open(filepath, "r", encoding="utf-8") as f:
                content = f.read()
            # 将 §a 替换为 §2
            new_content = re.sub(r'§a', '§2', content)
            new_content = re.sub(r'§d', '§5', new_content)
            # 将 item_ids: 替换为 items:
            new_content = re.sub(r'\bitem_ids:', 'items:', new_content)
            # 将 <Recipe id="***"></Recipe> 替换为 <recipe id="***"/>
            new_content = re.sub(r'<Recipe\s+id="([^"]*)"></Recipe>', r'<recipe id="\1"/>', new_content)
            new_content = re.sub(r'<Recipe\s+id="([^"]*)"> </Recipe>', r'<recipe id="\1"/>', new_content)
            new_content = re.sub(r'<Recipe\s+id="([^"]*)"/>', r'<recipe id="\1"/>', new_content)
            new_content = re.sub(r'<Recipe\s+id="([^"]*)" />', r'<recipe id="\1"/>', new_content)
            # 将 <ItemImage id="***" scale="?"></ItemImage> 替换为 <item id="***"/>
            new_content = re.sub(r'<ItemImage\s+id="([^"]*)"\s+scale="[^"]*"></ItemImage>', r'<item id="\1"/>', new_content)
            new_content = re.sub(r'<ItemImage\s+id="([^"]*)"\s+scale="[^"]*"> </ItemImage>', r'<item id="\1"/>', new_content)
            new_content = re.sub(r'<ItemImage\s+id="([^"]*)"\s+scale="[^"]*" />', r'<item id="\1"/>', new_content)
            # 将 <Row>*</Row> 替换为 <row>*</row>（支持跨行内容）
            new_content = re.sub(r'<Row>(.*?)</Row>', r'<row halign="center">\1</row>', new_content, flags=re.DOTALL)
            # 将 <ItemLink id="{A}:{B}" /> 替换为 (<translate key="item.{A}.{B}"/>)[对应文件相对路径]
            def replace_item_link(m, current_dir=os.path.relpath(root, Ageratum_path)):
                namespace = m.group(1)
                item_name = m.group(2)
                full_id = f"{namespace}:{item_name}"
                # 含 "block" 的物品使用 block 键前缀，否则使用 item 键前缀（因为游戏内方块和物品的翻译键不同）
                if 'block' in item_name:
                    translated = f'<translate key="block.{namespace}.{item_name}"/>'
                else:
                    translated = f'<translate key="item.{namespace}.{item_name}"/>'
                if full_id in item_id_to_file:
                    target_path = item_id_to_file[full_id]
                    # 计算从当前文件目录到目标文件的相对路径
                    if current_dir == '.':
                        rel = target_path
                    else:
                        rel = os.path.relpath(target_path, current_dir).replace('\\', '/')
                    return f'[{translated}]({rel})'
                return translated
            new_content = re.sub(r'<ItemLink\s+id="([^:]*):([^"]*)"\s*/>', replace_item_link, new_content)
            # 将 <Colour firstColor="{A}" lastColor="{B}">{C}</Colour> 替换为 <gradient start="#{A}" end="#{B}">{C}</gradient>
            new_content = re.sub(r'<Colour\s+firstColor="([^"]*)"\s+lastColor="([^"]*)">(.*?)</Colour>', r'<gradient start="#\1" end="#\2">\3</gradient>', new_content, flags=re.DOTALL)
            # 将 <ImportStructure src="***" /> 替换为 <structure id="***"/>
            new_content = re.sub(r'<ImportStructure\s+src="([^"]*)"\s*/>', r'<structure id="\1"/>', new_content)
            new_content = re.sub(r'<ImportStructure\s+src="([^"]*)"\s*> </ImportStructure>', r'<structure id="\1"/>', new_content)
            # 将 <NeoColor id="{A}">{B}</NeoColor> 替换为 <color=#{A}>{B}</color>
            new_content = re.sub(r'<NeoColor\s+id="([^"]*)">(.*?)</NeoColor>', r'<color=#\1>\2</color>', new_content, flags=re.DOTALL)
            # index.md 中将 title: "铁砧工艺-{A}" 替换为 title: "{A}"
            if file == "index.md":
                new_content = re.sub(r'(title:\s*")铁砧工艺-([^"]*")', r'\1\2', new_content)
            if new_content != content:
                with open(filepath, "w", encoding="utf-8") as f:
                    f.write(new_content)

# 根据 md 文件内容中的 position 字段重命名文件或文件夹，格式为 "position值_原名"，不足3位前补0
# 如果是 index.md，则重命名其所属文件夹（不包括根文件夹）
# 分三步：1.收集重命名映射 2.更新MD中的相对链接 3.执行重命名

# --- 第一步：收集所有重命名映射，删除 position 行，但暂不重命名 ---
rename_map = {}  # (parent_rel_path, old_name) → new_name，用于链接更新
folder_renames = []  # (folder_abs_path, pos)
file_renames = []    # (old_abs_path, new_abs_path)

for root, dirs, files in os.walk(Ageratum_path):
    for file in files:
        if file.endswith(".md"):
            filepath = os.path.join(root, file)
            with open(filepath, "r", encoding="utf-8") as f:
                content = f.read()
            match = re.search(r'^\s*position:\s*(\d+)', content, re.MULTILINE)
            if match:
                pos = match.group(1).zfill(3)
                # 删除内容中的 position 行
                content = re.sub(r'^\s*position:\s*\d+\n?', '', content, count=1, flags=re.MULTILINE)
                with open(filepath, "w", encoding="utf-8") as f:
                    f.write(content)
                if file == "index.md" and root != Ageratum_path:
                    # index.md → 重命名其所属文件夹
                    folder_name = os.path.basename(root)
                    parent_rel = os.path.relpath(os.path.dirname(root), Ageratum_path)
                    if parent_rel == '.':
                        parent_rel = ''
                    new_folder_name = f"{pos}_{folder_name}"
                    rename_map[(parent_rel, folder_name)] = new_folder_name
                    folder_renames.append((root, pos))
                else:
                    # 普通 md 文件
                    rel_root = os.path.relpath(root, Ageratum_path)
                    if rel_root == '.':
                        rel_root = ''
                    new_name = f"{pos}_{file}"
                    rename_map[(rel_root, file)] = new_name
                    file_renames.append((filepath, os.path.join(root, new_name)))

# --- 第二步：在重命名前，更新所有 md 文件中的 Markdown 相对链接 ---
for root, dirs, files in os.walk(Ageratum_path):
    for file in files:
        if file.endswith(".md"):
            filepath = os.path.join(root, file)
            with open(filepath, "r", encoding="utf-8") as f:
                content = f.read()

            rel_root = os.path.relpath(root, Ageratum_path)
            if rel_root == '.':
                rel_root = ''

            def make_replacer(current_rel_root):
                """闭包捕获当前文件所在目录的原始相对路径"""
                def replace_link(m):
                    link_text = m.group(1)
                    link_path = m.group(2)
                    # 跳过网络链接和空链接
                    if not link_path or link_path.startswith('http://') or link_path.startswith('https://'):
                        return m.group(0)
                    # 分离锚点
                    if '#' in link_path:
                        path_part, anchor = link_path.split('#', 1)
                        anchor = '#' + anchor
                    else:
                        path_part = link_path
                        anchor = ''
                    if not path_part:
                        return m.group(0)
                    # 拆分路径为各级组件
                    parts = path_part.replace('\\', '/').split('/')
                    current_dir = current_rel_root
                    new_parts = []
                    for i, part in enumerate(parts):
                        if part == '..':
                            # 回退到上一级目录
                            if current_dir:
                                current_dir = '/'.join(current_dir.split('/')[:-1]) if '/' in current_dir else ''
                            new_parts.append('..')
                        elif part == '.':
                            new_parts.append('.')
                        else:
                            # 查找该组件是否有重命名映射
                            key = (current_dir, part)
                            if key in rename_map:
                                new_parts.append(rename_map[key])
                            else:
                                new_parts.append(part)
                            # 如果不是最后一个组件，则它是目录，需要更新 current_dir
                            if i < len(parts) - 1:
                                current_dir = f"{current_dir}/{part}" if current_dir else part
                    new_link = '/'.join(new_parts) + anchor
                    return f'[{link_text}]({new_link})'
                return replace_link

            new_content = re.sub(r'\[([^\]]*)\]\(([^)]+)\)', make_replacer(rel_root), content)
            if new_content != content:
                with open(filepath, "w", encoding="utf-8") as f:
                    f.write(new_content)

# --- 第三步：执行文件重命名 ---
for old_path, new_path in file_renames:
    os.rename(old_path, new_path)

# 统一执行文件夹重命名（从深层往浅层，避免父目录先改名导致子目录路径失效）
folder_renames.sort(key=lambda x: x[0].count(os.sep), reverse=True)
for folder_path, pos in folder_renames:
    folder_name = os.path.basename(folder_path)
    parent_dir = os.path.dirname(folder_path)
    new_folder_name = f"{pos}_{folder_name}"
    new_folder_path = os.path.join(parent_dir, new_folder_name)
    os.rename(folder_path, new_folder_path)
