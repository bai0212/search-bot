# GUI 材质目录说明（2026-10-31）

**唯一真源是 `风格库/`**，本目录下的三张 PNG 是科技风的历史文件，
已经原样归档进 `风格库/科技风/`，**不要从这里往 `src` 拷**（会盖掉当前生效的原版风）。

- 看当前生效的是哪套：`python 风格库/apply_gui_style.py list`
- 换风格：`python 风格库/apply_gui_style.py apply <原版风|科技风> --with-theme`
- 出图 / 看效果：`风格库/<风格>/gen_gui_textures*.py` + `风格库/原版风/mock_gui_vanilla.py`
- 需要改 Java 的 GUI 需求：`design/gui-code-requirements.md`

`mock_gui.py` 和 `gen_gui_textures.py` 是科技风时代的脚本，留在这里只当参考，
和 `风格库/科技风/` 里的副本内容一致。
