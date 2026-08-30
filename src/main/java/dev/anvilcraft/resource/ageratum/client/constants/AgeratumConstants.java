package dev.anvilcraft.resource.ageratum.client.constants;

import dev.anvilcraft.resource.ageratum.Ageratum;
import net.minecraft.resources.Identifier;

import java.util.regex.Pattern;

public interface AgeratumConstants {
    interface Guide {
        /**
         * 文档根目录（assets/<namespace>/ 下）。
         */
        String ROOT_FOLDER = Ageratum.MOD_ID;

        /**
         * 文档文件扩展名。
         */
        String MARKDOWN_EXTENSION = ".md";

        /**
         * 默认首页文件名（不含扩展名）。
         */
        String INDEX_FILE = "index";
    }

    interface I18n {
        /**
         * 默认语言代码。
         */
        String DEFAULT_LANGUAGE_CODE = "en_us";
    }

    interface Preview {
        /**
         * 预览命名空间。
         */
        String NAMESPACE = "ageratum_review";
    }

    interface ItemBinding {
        /**
         * 长按持续时间（毫秒）。
         */
        long HOLD_DURATION_MS = 1_500L;

        /**
         * 悬停状态过期时间（毫秒）。
         */
        long HOVER_STALE_MS = 200L;
    }

    interface Patterns {
        /**
         * 颜色标签。
         */
        Pattern COLOR_TAG_PATTERN = Pattern.compile("<color=(#?[0-9a-zA-Z_]+)>");

        /**
         * Markdown 标题模式（ATX形式）。
         */
        Pattern HEADER_PATTERN = Pattern.compile("^\\s{0,3}(#{1,6})\\s+(.+?)\\s*#*\\s*$");

        /**
         * 有序列表模式。
         */
        Pattern ORDERED_LIST_PATTERN = Pattern.compile("^(\\s*)(\\d+)\\.\\s+(.+)$");

        /**
         * 任务列表模式。
         */
        Pattern TASK_LIST_PATTERN = Pattern.compile("^(\\s*)[-+*]\\s+\\[([ xX])]\\s+(.+)$");

        /**
         * 无序列表模式。
         */
        Pattern UNORDERED_LIST_PATTERN = Pattern.compile("^(\\s*)[-+*]\\s+(.+)$");

        /**
         * 块引用模式。
         */
        Pattern BLOCKQUOTE_PATTERN = Pattern.compile("^\\s*((?:>\\s*)+)(.*)$");

        /**
         * 水平分隔线模式。
         */
        Pattern HORIZONTAL_RULE_PATTERN = Pattern.compile("^\\s*([-*_])(?:\\s*\\1){2,}\\s*$");

        /**
         * Setext标题1模式。
         */
        Pattern SETEXT_H1_PATTERN = Pattern.compile("^=+\\s*$");

        /**
         * Setext标题2模式。
         */
        Pattern SETEXT_H2_PATTERN = Pattern.compile("^-+\\s*$");

        /**
         * 代码块围栏模式。
         */
        Pattern CODE_FENCE_PATTERN = Pattern.compile("^(`{3,}|~{3,})(.*)$");

        /**
         * 缩进代码模式。
         */
        Pattern INDENTED_CODE_PATTERN = Pattern.compile("^(?: {4}|\\t)(.*)$");

        /**
         * 表格行模式。
         */
        Pattern TABLE_ROW_PATTERN = Pattern.compile("^\\|.*\\|\\s*$");

        /**
         * 链接参考定义模式。
         */
        Pattern LINK_REF_DEF_PATTERN = Pattern.compile(
            "^\\s{0,3}\\[([^]]+)]:\\s*(\\S+)(?:\\s+(?:\"[^\"]*\"|'[^']*'|\\([^)]*\\)))?\\s*$"
        );

        /**
         * 完整链接参考模式。
         */
        Pattern LINK_REF_FULL_PATTERN = Pattern.compile("\\[([^]]+)]\\[([^]]*)]");

        /**
         * 简短链接参考模式。
         */
        Pattern LINK_REF_SHORT_PATTERN = Pattern.compile("\\[([^]\\[]+)](?![\\[(])");

        /**
         * 冒号形式扩展标签打开模式。
         */
        Pattern EXTENSION_COLON_OPEN_PATTERN = Pattern.compile(
            "^:::\\s+((?:[a-z0-9_.-]+:)?[a-z0-9_./-]+)(?:\\s+(.*))?$",
            Pattern.CASE_INSENSITIVE
        );

        /**
         * 尖括号形式扩展标签打开模式。
         */
        Pattern EXTENSION_TAG_OPEN_PATTERN = Pattern.compile(
            "^<\\s*((?:[a-z0-9_.-]+:)?[a-z0-9_./-]+)(?:\\s+([^>]*?))?\\s*(/?)>\\s*$",
            Pattern.CASE_INSENSITIVE
        );

        /**
         * 图片模式。
         */
        Pattern IMAGE_PATTERN = Pattern.compile("^\\s*!\\[[^]]*]\\(([^):]+):([^)]+)\\)\\s*$");

        /**
         * 图片后备模式（仅路径）。
         */
        Pattern FALLBACK_IMAGE_PATTERN = Pattern.compile("^\\s*!\\[[^]]*]\\(([^)]+)\\)\\s*$");

        /**
         * 内联样式解析器：模糊标签模式。
         */
        Pattern OBFUSCATED_TAG_PATTERN = Pattern.compile("<o>");

        /**
         * 内联样式解析器：悬停标签模式。
         */
        Pattern HOVER_TAG_PATTERN = Pattern.compile("<hover\\b([^>]*)>", Pattern.CASE_INSENSITIVE);

        /**
         * 内联样式解析器：点击标签模式。
         */
        Pattern CLICK_TAG_PATTERN = Pattern.compile("<click\\b([^>]*)>", Pattern.CASE_INSENSITIVE);

        /**
         * 内联样式解析器：渐变标签模式。
         */
        Pattern GRADIENT_TAG_PATTERN = Pattern.compile("<gradient\\b([^>]*)>", Pattern.CASE_INSENSITIVE);

        /**
         * 标签属性模式。
         */
        Pattern TAG_ATTRIBUTE_PATTERN = Pattern.compile("([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*\"([^\"]*)\"");

        /**
         * 扩展参数对模式。
         */
        Pattern PARAM_PAIR_PATTERN = Pattern.compile("([a-zA-Z0-9_.-]+)=(\"[^\"]*\"|'[^']*'|\\S+)");

        /**
         * LaTeX 旧版本格式模式。
         */
        Pattern LEGACY_LATEX_LINE_PATTERN = Pattern.compile("^\\s*\\[(?:tex|latex|formula)([:;,!+])([^]]+)]\\s*$");
    }

    interface GuideScreenUI {
        /**
         * 背景纹理资源位置。
         */
        interface Textures {
            Identifier GUIDE = Ageratum.location("textures/gui/guide/guide.png");
            Identifier LABEL_PRIMARY = Ageratum.location("textures/gui/guide/label_primary.png");
            Identifier LABEL_SECONDARY = Ageratum.location("textures/gui/guide/label_secondary.png");
            Identifier BUTTON_DOWN = Ageratum.location("textures/gui/guide/button_down.png");
            Identifier BUTTON_UP = Ageratum.location("textures/gui/guide/button_up.png");
            Identifier BUTTON_CLOSE = Ageratum.location("textures/gui/guide/button_close.png");
            Identifier BUTTON_SHARE = Ageratum.location("textures/gui/guide/button_share.png");
            Identifier BUTTON_RETURN = Ageratum.location("textures/gui/guide/button_back.png");
            Identifier BUTTON_ADD = Ageratum.location("textures/gui/guide/button_add.png");
            Identifier LABEL_BOOKMARK = Ageratum.location("textures/gui/guide/label_bookmark.png");
        }

        /**
         * 纹理尺寸（原始像素）。
         */
        interface TextureSizes {
            int GUIDE_IMAGE_SIZE = 512;
            int GUIDE_IMAGE_WIDTH = 360;
            int GUIDE_IMAGE_HEIGHT = 232;
            int LABEL_IMAGE_SIZE = 64;
            int LABEL_IMAGE_WIDTH = 60;
            int LABEL_IMAGE_HEIGHT = 16;
            int BUTTON_IMAGE_SIZE = 32;
            int BUTTON_IMAGE_WIDTH = 32;
            int BUTTON_IMAGE_HEIGHT = 16;
        }

        /**
         * 布局与间距（屏幕像素）。
         */
        interface Layout {
            int CLOSE_BUTTON_X_OFFSET = -5;
            int BOOKMARK_HOVER_SHIFT = 5;
            int MIN_HORIZONTAL_MARGIN = 32;
            int MIN_VERTICAL_MARGIN = 10;
            int MIN_LABEL_ROW_MARGIN = 2;
            int LABEL_LEVEL2_INDENT = 10;
            int LABEL_HOVER_SHIFT = 5;
            int CONTENT_ROWS_MARGIN = 5;
        }

        /**
         * 滚动与交互参数。
         */
        interface Interaction {
            float SCROLL_STEP = 16.0f;
            long PREVIEW_REFRESH_INTERVAL_MS = 500L;
        }

        /**
         * 颜色相关常量。
         */
        interface Colors {
            /**
             * 链接颜色（蓝色）
             */
            int LINK_COLOR = 0x66CCFF;
            /**
             * 断链颜色（红色），用于标记指向不存在文档的引用/链接。
             */
            int BROKEN_LINK_COLOR = 0xFF5555;
            /**
             * 标签活跃状态文本颜色（棕色）
             */
            int LABEL_TEXT_ACTIVE = 0xFF8B5A2B;
            /**
             * 标签可点击状态文本颜色（深棕色）
             */
            int LABEL_TEXT_CLICKABLE = 0xFF5D4630;
            /**
             * 标签不可点击状态文本颜色（灰色）
             */
            int LABEL_TEXT_DISABLED = 0xFF3f3f3f;
            /**
             * 书签文本颜色（深棕色）
             */
            int BOOKMARK_TEXT = 0xFF5D4630;
            /**
             * 背景梯度色1
             */
            int BACKGROUND_GRADIENT_1 = 0xC0101010;
            /**
             * 背景梯度色2
             */
            int BACKGROUND_GRADIENT_2 = 0xD0101010;
            /**
             * 层指示器背景色（半透明黑色）
             */
            int LAYER_INDICATOR_BG = 0x88000000;
            /**
             * 层指示器文本色（白色）
             */
            int LAYER_INDICATOR_TEXT = 0xFFFFFFFF;
        }

        /**
         * 位置与尺寸相关常量。
         */
        interface Positions {
            /**
             * 标签基础 X 坐标偏移
             */
            int LABEL_BASE_X = -30;
            /**
             * 内容区域 X 坐标起始偏移（相对于背景左边界）
             */
            int CONTENT_START_X_OFFSET = 15;
            /**
             * 内容区域 Y 坐标起始偏移（相对于背景上边界）
             */
            int CONTENT_START_Y_OFFSET = 18;
            /**
             * 标签文本左内边距
             */
            int LABEL_TEXT_PADDING_LEFT = 10;
            /**
             * 标签文本左内边距（二级）
             */
            int LABEL_TEXT_PADDING_LEFT_LEVEL2 = 5;
            /**
             * 标签文本上下内边距
             */
            int LABEL_TEXT_PADDING_VERTICAL = 4;
            /**
             * 标签宽度限制的内边距
             */
            int LABEL_TEXT_MAX_WIDTH_PADDING = 6;
            /**
             * 书签文本绘制基础偏移 X
             */
            int BOOKMARK_TEXT_BASE_X = 50;
            /**
             * 背景图像下方额外绘制范围
             */
            int BACKGROUND_EXTRA_PADDING = 10;
            /**
             * 按钮间距
             */
            int BUTTON_SPACING = 10;
            /**
             * 结构预览按钮右边距
             */
            int STRUCTURE_BUTTON_RIGHT_MARGIN = 21;
            /**
             * 结构预览按钮上边距
             */
            int STRUCTURE_BUTTON_TOP_MARGIN = 5;
            /**
             * 结构预览按钮宽度
             */
            int STRUCTURE_BUTTON_WIDTH = 16;
            /**
             * 结构预览按钮高度
             */
            int STRUCTURE_BUTTON_HEIGHT = 16;
            /**
             * 层指示器内边距
             */
            int LAYER_INDICATOR_PADDING = 3;
            /**
             * 屏幕尺寸阈值 - 宽度
             */
            int SCREEN_THRESHOLD_WIDTH = 1920;
            /**
             * 屏幕尺寸阈值 - 高度
             */
            int SCREEN_THRESHOLD_HEIGHT = 1080;
            /**
             * 无效鼠标按钮代码
             */
            int INVALID_BUTTON = -1;
        }
    }

    interface LaTeX {
        /**
         * LaTeX API 地址。
         */
        String LATEX_API_URL = "https://latex.codecogs.com/png.latex?";

        /**
         * 最小 LaTeX 高度（像素）。
         */
        int MIN_LATEX_HEIGHT = 11;

        /**
         * LaTeX 缩放因子。
         */
        interface Scale {
            /**
             * 默认缩放
             */
            float DEFAULT = 0.1f;
            /**
             * 最小缩放
             */
            float MIN = 0.5f;
            /**
             * 最大缩放
             */
            float MAX = 2.0f;
        }

        /**
         * LaTeX DPI 设置。
         */
        interface Dpi {
            /**
             * 默认 DPI
             */
            int DEFAULT = 150;
            /**
             * 最小 DPI
             */
            int MIN = 72;
            /**
             * 最大 DPI
             */
            int MAX = 600;
        }

        /**
         * 网络连接参数。
         */
        interface Network {
            /**
             * 连接超时（毫秒）
             */
            int CONNECT_TIMEOUT_MS = 5000;
            /**
             * 读取超时（毫秒）
             */
            int READ_TIMEOUT_MS = 10000;
        }
    }

    interface Structure {
        /**
         * 结构预览资源位置。
         */
        interface Textures {
            Identifier BUTTON_PROJECTION = Ageratum.location("textures/gui/guide/button_projection.png");
        }

        /**
         * 相机缩放参数。
         */
        interface Camera {
            /**
             * 最小缩放
             */
            float MIN_ZOOM = 0.2f;
            /**
             * 最大缩放
             */
            float MAX_ZOOM = 8.0f;
        }

        /**
         * 鼠标敏感度参数。
         */
        interface Sensitivity {
            /**
             * 旋转偏航敏感度（Yaw）
             */
            float ROTATE_YAW = 0.8f;
            /**
             * 旋转俯仰敏感度（Pitch）
             */
            float ROTATE_PITCH = 0.6f;
            /**
             * 平移敏感度（Pan）
             */
            float PAN = 1.0f;
        }

        /**
         * 结构渲染参数。
         */
        interface Render {
            /**
             * 屏幕宽度缩放因子
             */
            float SCREEN_WIDTH_SCALE = 330.0f;
            /**
             * 内容高度计算系数
             */
            double CONTENT_HEIGHT_FACTOR = 20.0d;
            /**
             * 底部高度计算系数
             */
            double BOTTOM_HEIGHT_FACTOR = 18.0d;
        }
    }

    interface Network {
        /**
         * 网络协议版本。
         */
        String PROTOCOL_VERSION = "2";
    }

    interface Image {
        /**
         * 默认占位图像尺寸（像素）。
         */
        int DEFAULT_PLACEHOLDER_WIDTH = 16;
        /**
         * 默认占位图像尺寸（像素）。
         */
        int DEFAULT_PLACEHOLDER_HEIGHT = 16;
    }
}
