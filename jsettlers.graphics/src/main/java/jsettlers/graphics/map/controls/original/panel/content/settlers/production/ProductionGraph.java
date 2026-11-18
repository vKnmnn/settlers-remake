package jsettlers.graphics.map.controls.original.panel.content.settlers.production;

import jsettlers.common.Color;
import go.graphics.EPrimitiveType;
import go.graphics.GLDrawContext;
import go.graphics.UnifiedDrawHandle;
import go.graphics.text.EFontSize;
import go.graphics.text.TextDrawer;
import jsettlers.common.player.ProductionHistory;
import jsettlers.common.position.FloatRectangle;
import jsettlers.graphics.localization.Labels;
import jsettlers.graphics.ui.UIPanel;

/** Interval counts, with unknown history left blank rather than invented as zero production. */
public final class ProductionGraph extends UIPanel {
    private volatile Plot plot = Plot.create(null, 15);
    private UnifiedDrawHandle rectangle;
    private UnifiedDrawHandle line;

    public void setHistory(ProductionHistory history, int windowMinutes) {
        plot = Plot.create(history, windowMinutes);
    }

    static final class Plot {
        final float[] left, right, height;
        final int maximum, windowMinutes;
        final float knownFrom;
        final boolean hasHistory;

        private Plot(float[] left, float[] right, float[] height, int maximum,
                     int windowMinutes, float knownFrom, boolean hasHistory) {
            this.left = left; this.right = right; this.height = height;
            this.maximum = maximum; this.windowMinutes = windowMinutes;
            this.knownFrom = knownFrom; this.hasHistory = hasHistory;
        }

        static Plot create(ProductionHistory history, int minutes) {
            if (minutes <= 0) throw new IllegalArgumentException("Non-positive graph window");
            double[] values = history == null ? new double[0] : history.getSmoothedAmounts();
            float[] left = new float[values.length], right = new float[values.length], height = new float[values.length];
            long window = minutes * 60000L;
            long end = history == null ? 0 : history.getEndTimeMillis();
            long start = end - window;
            long available = history == null ? end : history.getAvailableSinceMillis();
            long bucket = history == null ? 1 : history.getBucketMillis();
            int maximum = 1;
            for (int i = 0; i < values.length; i++) {
                long bucketEnd = end - (values.length - 1L - i) * bucket;
                if (bucketEnd > start && bucketEnd > available) maximum = Math.max(maximum, (int) Math.ceil(values[i]));
            }
            for (int i = 0; i < values.length; i++) {
                long bucketEnd = end - (values.length - 1L - i) * bucket;
                long bucketStart = Math.max(bucketEnd - bucket, available);
                left[i] = clamp((bucketStart - start) / (float) window);
                right[i] = clamp((bucketEnd - start) / (float) window);
                height[i] = right[i] > left[i] ? (float) (Math.max(0, values[i]) / maximum) : 0;
            }
            return new Plot(left, right, height, maximum, minutes,
                    clamp((available - start) / (float) window), values.length > 0 && available < end);
        }
        private static float clamp(float value) { return Math.max(0, Math.min(1, value)); }
    }

    @Override public void drawAt(GLDrawContext gl) {
        Plot data = plot;
        if (rectangle == null || !rectangle.isValid()) {
            rectangle = gl.createUnifiedDrawCall(4, "production-history-bar", null, null,
                    new float[] {0, 0, 1, 0, 1, 1, 0, 1});
            line = gl.createUnifiedDrawCall(2, "production-history-line", null, null, new float[] {0, 0, 1, 1});
        }
        FloatRectangle pos = getPosition();
        TextDrawer text = gl.getTextDrawer(EFontSize.SMALL);
        float margin = Math.max(15, text.getWidth(Integer.toString(data.maximum)) + 3);
        float x = pos.getMinX() + margin, y = pos.getMinY() + 18;
        float width = Math.max(1, pos.getWidth() - margin - 3), height = Math.max(1, pos.getHeight() - 24);
        rect(x, y, width, height, new Color(.08f, .09f, .08f, 1));
        if (data.knownFrom > 0) rect(x, y, width * data.knownFrom, height, new Color(.24f, .24f, .24f, 1));
        int previous = -1;
        Color curve = new Color(.85f, .72f, .32f, 1);
        for (int i = 0; i < data.left.length; i++) {
            if (data.right[i] <= data.left[i]) continue;
            float px = x + (data.left[i] + data.right[i]) * .5f * width;
            float py = y + data.height[i] * height;
            if (previous >= 0) {
                float oldX = x + (data.left[previous] + data.right[previous]) * .5f * width;
                float oldY = y + data.height[previous] * height;
                line.drawSimple(EPrimitiveType.Line, oldX, oldY, 0, px - oldX, py - oldY, curve, 1);
            }
            rect(px - 1, py - 1, 2, 2, curve);
            previous = i;
        }
        rect(x, y, 1, height, Color.WHITE);
        rect(x, y, width, 1, Color.WHITE);
        text.drawString(pos.getMinX(), y + height - text.getHeight("0"), Color.WHITE, Integer.toString(data.maximum));
        text.drawString(x - text.getWidth("0") - 3, y, Color.WHITE, "0");
        String start = "-" + data.windowMinutes + " min";
        text.drawString(x, pos.getMinY(), Color.WHITE, start);
        text.drawString(x + width - text.getWidth("0"), pos.getMinY(), Color.WHITE, "0");
        if (!data.hasHistory) text.renderCentered(x + width / 2, y + height / 2, Labels.getString("production_history_empty"));
    }

    private void rect(float x, float y, float width, float height, Color color) {
        rectangle.drawSimple(EPrimitiveType.Quad, x, y, 0, width, height, color, 1);
    }
}
