package dev.anvilcraft.resource.ageratum.client.gui;

import java.util.function.DoublePredicate;
import org.jspecify.annotations.Nullable;

/** 连续滚轮输入始终交给首次接收它的区域，停顿后才重新命中测试。 */
public final class GuideScrollGesture {
    private static final long IDLE_NANOS = 500_000_000L;
    private @Nullable DoublePredicate target;
    private long lastScroll;

    public boolean continueScroll(double delta, long now) {
        if (delta == 0) return false;
        if (this.target == null || now - this.lastScroll >= IDLE_NANOS) {
            this.reset();
            return false;
        }
        this.lastScroll = now;
        this.target.test(delta);
        return true;
    }

    public boolean begin(double delta, long now, DoublePredicate target) {
        if (delta == 0 || !target.test(delta)) return false;
        this.target = target;
        this.lastScroll = now;
        return true;
    }

    public void reset() {
        this.target = null;
    }
}
