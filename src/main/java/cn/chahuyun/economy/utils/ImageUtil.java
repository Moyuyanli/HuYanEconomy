package cn.chahuyun.economy.utils;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * @author Moyuyanli
 * @Date 2024/8/7 22:14
 */
public class ImageUtil {

    private ImageUtil() {
    }


    /**
     * 圆角处理
     *
     * @param image        BufferedImage 需要处理的图片
     * @param cornerRadius 圆角度
     * @return 处理后的图片
     */
    public static BufferedImage makeRoundedCorner(BufferedImage image, int cornerRadius) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = output.createGraphics();

        output = g2.getDeviceConfiguration().createCompatibleImage(w, h, Transparency.TRANSLUCENT);
        g2.dispose();
        g2 = output.createGraphics();
        /*
        这里绘画圆角矩形
        原图切圆边角
         */
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.fillRoundRect(0, 0, w, h, cornerRadius, cornerRadius);
        g2.setComposite(AlphaComposite.SrcIn);
        /*结束*/


        /*这里绘画原型图
        原图切成圆形
         */
//        Ellipse2D.Double shape = new Ellipse2D.Double(0, 0, w, h);
//        g2.setClip(shape);
        /*结束*/

        g2.drawImage(image, 0, 0, w, h, null);
        g2.dispose();

        return output;
    }

    /**
     * 按区域写入文字，自动换行
     *
     * @param text     需要写入的文本
     * @param x        写入坐标x
     * @param y        写入坐标y
     * @param maxWidth 最大宽度
     * @param g2d      绘图
     */
    public static void drawString(String text, int x, int y, int maxWidth, Graphics2D g2d) {
        float areaX = x;
        float areaY = y;

        int max = x + maxWidth;

        FontRenderContext frc = g2d.getFontRenderContext();
        Font font = g2d.getFont();

        // 将文本按行分割
        for (String line : text.split("\n")) {

            // 创建TextLayout
            TextLayout layout = new TextLayout(line, font, frc);

            // 获取TextLayout的边界
            Rectangle2D bounds = layout.getBounds();

            // 如果当前行的宽度超过了maxWidth，则需要换行
            if (bounds.getWidth() > maxWidth) {
                // 计算每个单词的宽度
                for (char word : line.toCharArray()) {
                    TextLayout wordLayout = new TextLayout(String.valueOf(word), font, frc);
                    Rectangle2D wordBounds = wordLayout.getBounds();

                    // 如果加上这个单词后超过了maxWidth，则换行
                    if (areaX + wordBounds.getWidth() > max) {
                        areaX = x; // 重新回到左边
                        areaY += (float) bounds.getHeight(); // 下一行
                    }

                    // 绘制单词
                    wordLayout.draw(g2d, areaX, areaY);
                    areaX += ((float) wordBounds.getWidth()) + 4f;
                }
            } else {
                // 如果当前行没有超过maxWidth，则直接绘制
                layout.draw(g2d, areaX, areaY);
                areaY += (float) bounds.getHeight();
            }

            // 只有在处理完一行之后才重置 areaX
            areaX = x;
        }
    }

    /**
     * 设置渐变文字
     *
     * @param text   文本
     * @param x      x
     * @param y      y
     * @param sColor 起始颜色
     * @param eColor 结束颜色
     * @param g2d    g2d,请先设置字体!
     */
    public static void drawStringGradient(String text, int x, int y, Color sColor, Color eColor, Graphics2D g2d) {
        // 获取字体的渲染上下文
        FontRenderContext frc = g2d.getFontRenderContext();

        // 计算文本宽度
        float textWidth = (float) g2d.getFont().getStringBounds(text, frc).getWidth();

        // 创建渐变
        GradientPaint gradient = new GradientPaint(x, y, sColor, x + textWidth, y, eColor);

        // 应用渐变
        g2d.setPaint(gradient);

        // 绘制文本
        g2d.drawString(text, x, y);
    }


    /**
     * 规范g2d
     *
     * @param bufferedImage 图片
     * @return g2d
     */
    public static Graphics2D getG2d(BufferedImage bufferedImage) {
        Graphics2D g2d = bufferedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return g2d;
    }

    /**
     * 十六进制颜色
     *
     * @param color 颜色
     * @return Color
     */
    public static Color hexColor(String color) {
        if (color != null && !color.isEmpty()) {
            String string = color.length() == 7 ? color.substring(1) : color;
            Integer r = Integer.valueOf(string.substring(0, 2), 16);
            Integer g = Integer.valueOf(string.substring(2, 4), 16);
            Integer b = Integer.valueOf(string.substring(4, 6), 16);
            return new Color(r, g, b);
        }
        throw new IllegalArgumentException("构建颜色错误!");
    }


    /**
     * 将color换成16进制的颜色，不带#
     *
     * @param color 颜色
     * @return 16进制的颜色
     */
    public static String colorHex(Color color) {
        // 将颜色的红、绿、蓝分量转换为16进制字符串
        String red = Integer.toHexString(color.getRed());
        String green = Integer.toHexString(color.getGreen());
        String blue = Integer.toHexString(color.getBlue());


        // 补齐两位数
        if (red.length() == 1) red = "0" + red;
        if (green.length() == 1) green = "0" + green;
        if (blue.length() == 1) blue = "0" + blue;

        // 返回格式化的16进制颜色字符串
        return red + green + blue;
    }

}
