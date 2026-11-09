package dev.anvilcraft.resource.ageratum.client.util.level;

import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureBlockEntityDetailsTest {
    private static Language original;

    @BeforeAll
    static void loadTranslations() throws Exception {
        original = Language.getInstance();
        var json = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/ageratum/lang/zh_cn.json")));
        Map<String, String> values = json.getAsJsonObject().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getAsString()));
        Language.inject(new Language() {
            @Override public String getOrDefault(String key, String fallback) { return values.getOrDefault(key, fallback); }
            @Override public boolean has(String key) { return values.containsKey(key); }
            @Override public boolean isDefaultRightToLeft() { return false; }
            @Override public FormattedCharSequence getVisualOrder(FormattedText text) {
                return FormattedCharSequence.forward(text.getString(), Style.EMPTY);
            }
        });
    }

    @AfterAll
    static void restoreTranslations() {
        Language.inject(original);
    }

    @Test
    void pulseModesTimingAndOutputUseSavedValues() {
        String[] modes = {"上升沿触发", "下降沿触发", "循环触发"};
        for (int mode = 0; mode < modes.length; mode++) {
            CompoundTag config = new CompoundTag();
            config.putByte("StartMode", (byte) mode);
            config.putInt("WaitingTime", 17);
            config.putInt("SignalDuration", 41);
            config.putBoolean("OutputMode", true);
            CompoundTag data = new CompoundTag();
            data.put("ExtraData", config);
            var lines = describe("anvilcraft:pulse_generator", data);
            assertTrue(lines.contains("触发模式: " + modes[mode]));
            assertTrue(lines.contains("延时: 17 游戏刻"));
            assertTrue(lines.contains("脉冲长度: 41 游戏刻"));
            assertTrue(lines.contains("输出反转: 开启"));
        }
    }

    @Test
    void modeNamesDoNotLeakBetweenDifferentBlocks() {
        CompoundTag data = new CompoundTag();
        data.putInt("OutputMode", 2);
        data.putString("FilterMode", "ALL");
        assertTrue(describe("anvilcraft:item_detector", data).contains("输出模式: 15 减去信号强度"));
        assertTrue(describe("anvilcraft:item_detector", data).contains("匹配模式: 全部匹配"));
        assertTrue(describe("other:machine", data).contains("输出模式: 2"));
        CompoundTag extra = new CompoundTag();
        extra.putByte("CompareMode", (byte) 1);
        extra.putInt("HighLimit", 12);
        extra.putInt("LowLimit", 4);
        data = new CompoundTag();
        data.put("ExtraData", extra);
        assertTrue(describe("anvilcraft:advanced_comparator", data).contains("比较模式: 窗口比较"));
        assertTrue(describe("anvilcraft:advanced_comparator", data).contains("上限: 12"));
        assertTrue(describe("anvilcraft:advanced_comparator", data).contains("下限: 4"));
    }

    @Test
    void collectorIndicesAreShownAsActualSettings() {
        CompoundTag data = new CompoundTag();
        data.putInt("Cooldown", 3);
        data.putInt("RangeRadius", 2);
        assertTrue(describe("anvilcraft:item_collector", data).contains("收集间隔: 60 游戏刻"));
        assertTrue(describe("anvilcraft:item_collector", data).contains("范围半径: 4 格"));
    }

    @Test
    void snapshotUsesStructureCoordinatesAndSurvivesSourceMutation() {
        CompoundTag saved = new CompoundTag();
        saved.putInt("WaitingTime", 37);
        CompoundTag block = new CompoundTag();
        ListTag position = new ListTag();
        for (int value : new int[]{2, 3, 4}) position.add(IntTag.valueOf(value));
        block.put("pos", position);
        block.put("nbt", saved);
        ListTag blocks = new ListTag();
        blocks.add(block);
        CompoundTag structure = new CompoundTag();
        structure.put("blocks", blocks);
        var snapshot = StructureBlockEntityDetails.capture(structure, new BlockPos(-5, 0, -7));
        CompoundTag captured = snapshot.get(new BlockPos(-3, 3, -3));
        assertEquals(saved, captured);
        assertNotSame(saved, captured);
        saved.putInt("WaitingTime", 0);
        assertEquals(List.of("WaitingTime: 37"), describe("other:machine", captured));
    }

    @Test
    void nestedItemsFluidsComponentsAndCoordinatesAreNotDiscarded() {
        CompoundTag item = new CompoundTag();
        item.putString("id", "minecraft:diamond");
        item.putInt("count", 23);
        item.putInt("Slot", 4);
        ListTag items = new ListTag();
        items.add(item);
        CompoundTag fluid = new CompoundTag();
        fluid.putString("id", "minecraft:water");
        fluid.putInt("Amount", 1000);
        CompoundTag target = new CompoundTag();
        target.putInt("x", 42);
        CompoundTag data = new CompoundTag();
        data.putString("id", "other:machine");
        data.putInt("x", 999);
        data.put("Items", items);
        data.put("Fluid", fluid);
        data.put("target", target);
        CompoundTag components = new CompoundTag();
        components.putString("other:setting", "custom value");
        data.put("components", components);
        var lines = describe("other:machine", data);
        assertTrue(lines.contains("物品[0].标识: minecraft:diamond"));
        assertTrue(lines.contains("物品[0].数量: 23"));
        assertTrue(lines.contains("物品[0].槽位: 4"));
        assertTrue(lines.contains("流体.数量: 1000"));
        assertTrue(lines.contains("目标.x: 42"));
        assertTrue(lines.contains("组件.other:setting: custom value"));
        assertFalse(lines.stream().anyMatch(line -> line.contains("999") || line.contains("other:machine")));
    }

    @Test
    void arraysUnknownModesAndEmptyValuesRemainInspectable() {
        CompoundTag data = new CompoundTag();
        data.putByteArray("bytes", new byte[]{-1, 2});
        data.putIntArray("ints", new int[]{3, 4});
        data.putLongArray("longs", new long[]{Long.MAX_VALUE});
        data.put("empty", new CompoundTag());
        data.put("list", new ListTag());
        var lines = describe("other:machine", data);
        assertTrue(lines.contains("bytes[0]: -1"));
        assertTrue(lines.contains("ints[1]: 4"));
        assertTrue(lines.contains("longs[0]: " + Long.MAX_VALUE));
        assertTrue(lines.contains("empty: {}"));
        assertTrue(lines.contains("list: []"));
        data = new CompoundTag();
        data.putInt("OutputMode", 999);
        assertEquals(List.of("输出模式: 999"), describe("anvilcraft:item_detector", data));
    }

    @Test
    void expansionLimitsAreExplicit() {
        CompoundTag data = new CompoundTag();
        data.putIntArray("large", new int[5000]);
        var lines = describe("other:machine", data);
        assertEquals(4097, lines.size());
        assertTrue(lines.getLast().contains("数据过大"));
        CompoundTag deep = new CompoundTag();
        CompoundTag current = deep;
        for (int i = 0; i < 40; i++) {
            CompoundTag child = new CompoundTag();
            current.put("child", child);
            current = child;
        }
        current.putString("leaf", "value");
        assertTrue(describe("other:machine", deep).getFirst().contains("数据过大"));
    }

    @Test
    void paginationReachesEveryLineAndClampsAtBothEnds() {
        List<Component> lines = IntStream.range(0, 53).mapToObj(i -> (Component) Component.literal("field " + i)).toList();
        List<String> seen = new ArrayList<>();
        for (int page = 0; page < 6; page++) {
            var visible = StructureDetailsPager.page(lines, page, 10);
            assertTrue(visible.size() <= 11);
            seen.addAll(visible.subList(0, visible.size() - 1).stream().map(Component::getString).toList());
        }
        assertEquals(lines.stream().map(Component::getString).toList(), seen);
        assertEquals("field 0", StructureDetailsPager.page(lines, -1, 10).getFirst().getString());
        assertEquals("field 50", StructureDetailsPager.page(lines, 999, 10).getFirst().getString());
        assertTrue(StructureDetailsPager.page(List.of(), 1, 10).isEmpty());
    }

    private static List<String> describe(String blockId, CompoundTag data) {
        return StructureBlockEntityDetails.describe(blockId, data).stream().map(Component::getString).toList();
    }
}
