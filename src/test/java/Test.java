import cn.chahuyun.economy.constant.ImageDrawXY;
import cn.chahuyun.economy.entity.title.CustomTitle;
import cn.chahuyun.economy.utils.ImageUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * @author Moyuyanli
 * @Date 2024/8/7 19:29
 */
public class Test {

    public static void main(String[] args) throws Exception {
        titleTest();
    }


    public static void titleTest() {
        CustomTitle titleTemplateSimple = new CustomTitle(
                "template", -1,
                "模板", 0.0,
                false, false, "[模板]",
                "#00000", "#ffffff");
        JSONObject entries = JSONUtil.parseObj(titleTemplateSimple);
        System.out.printf("json-> %s",entries.toStringPretty());
    }


    public static void imageTest() throws Exception {

//        int i = RandomUtil.randomInt(1, 9);
        BufferedImage background = ImageIO.read(new File("D:\\ideaProjects\\github\\HuYanEconomy\\HuYanEconomy\\src\\test\\java\\bottom" + 3 + ".png"));
        int height = background.getHeight();
        System.out.println("height->" + height);
        int width = background.getWidth();
        System.out.printf("width -> %s %n", width);


        BufferedImage bottom = ImageIO.read(new File("D:\\ideaProjects\\github\\HuYanEconomy\\HuYanEconomy\\src\\test\\java\\bottom.png"));

        height = bottom.getHeight();
        width = bottom.getWidth();
        System.out.printf("height -> %s %n", height);
        System.out.printf("width -> %s %n", width);

        File fontFile = new File("D:\\ideaProjects\\github\\HuYanEconomy\\HuYanEconomy\\src\\test\\java\\Maple UI.ttf");
        Font customFont = Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(24f);

        System.out.printf("font %s %n", customFont.getName());
        System.out.printf("font-name %s %n", customFont.getFontName());


        // 创建一个新的 BufferedImage
        BufferedImage canvas = new BufferedImage(1024, 576, BufferedImage.TYPE_INT_ARGB);


        // 获取 Graphics2D 对象用于绘制
        Graphics2D g2d = canvas.createGraphics();

        // 设置抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 计算缩放比例
        double scaleWidth = (double) 1024 / background.getWidth();
        double scaleHeight = (double) 576 / background.getHeight();
        double scale = Math.max(scaleWidth, scaleHeight);


        System.out.printf("缩放比->%s %n", scale);
        // 缩放后的宽度和高度
        int scaledWidth = (int) (background.getWidth() * scale);
        int scaledHeight = (int) (background.getHeight() * scale);


        // 居中的位置
        int x = (1024 - scaledWidth) / 2;
        int y = (576 - scaledHeight) / 2;


        // 绘制缩放后的背景图片到新的 BufferedImage 上
        g2d.drawImage(background, x, y, scaledWidth, scaledHeight, null);

        // 绘制底
        g2d.drawImage(bottom, 0, 0, null);


        height = canvas.getHeight();
        width = canvas.getWidth();
        System.out.printf("height -> %s %n", height);
        System.out.printf("width -> %s %n", width);

        g2d.setFont(customFont.deriveFont(20f));
        g2d.setColor(ImageUtil.hexColor("f44336"));
        g2d.drawString("572490972", ImageDrawXY.ID.getX(), ImageDrawXY.ID.getY());
        drawStringGradient("[是放空!]",ImageDrawXY.TITLE.getX(), ImageDrawXY.TITLE.getY(),
                ImageUtil.hexColor("f50057"),
                ImageUtil.hexColor("e1f5c4"),
                g2d
        );
        g2d.setFont(customFont.deriveFont(Font.BOLD,60f));
        drawStringGradient("放空",230,200,
                ImageUtil.hexColor("fce38a"),
                ImageUtil.hexColor("f38181 "),
                g2d);

        g2d.setFont(customFont);
        g2d.setColor(Color.BLACK);


        g2d.drawString("签到时间:", 70, 340);
        g2d.drawString("2024-8-7 20:35:12", 180, 340);
        g2d.drawString("连签次数:", 70, 385);
        g2d.drawString("1", 180, 385);


        String text = "如果你是在图片上操作而不是在GUI框体中，那么你可以使用类似的方法来处理自动换行的问题。这里是一";
        drawString(text, 556, 315, 435, g2d);

        g2d.drawString("--- xxxxx:xxxxxx", 556, 410);

        int infoY = 478;
        g2d.setFont(customFont.deriveFont(32f));
        g2d.drawString("我的金币", 118, infoY);
        g2d.drawString("签到获得", 358, infoY);
        g2d.drawString("我的银行", 608, infoY);
        g2d.drawString("今日收益", 858, infoY);


        infoY = 530;

        g2d.setFont(customFont.deriveFont(Font.BOLD,40f));
        g2d.drawString("512.1", 118, infoY);
        g2d.drawString("60.0", 358, infoY);
        g2d.drawString("15243.9", 580, infoY);
        g2d.drawString("298.9 ", 858, infoY);



        Font font = customFont.deriveFont(12f);
        g2d.setFont(font);
        g2d.drawString("by Mirai & HuYanEconomy(壶言经济) v1.0.16", 730, 573);


        // 释放资源
        g2d.dispose();
        BufferedImage image = makeRoundedCorner(canvas, 30);

        // 显示新创建的 BufferedImage
        view(image);
    }


    public static void view(BufferedImage image) {
        SwingUtilities.invokeLater(() -> {
            // 创建一个JFrame实例
            JFrame frame = new JFrame("Image Display");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(image.getWidth() + 50, image.getHeight() + 50); // 窗口大小略大于图片

            // 创建一个 JLabel 并设置它的图标为 BufferedImage
            JLabel label = new JLabel(new ImageIcon(image));

            // 把 JLabel 添加到 JFrame 中
            frame.add(label, BorderLayout.CENTER);

            // 设置 JFrame 可见
            frame.setVisible(true);
        });
    }


    /**
     * 圆角处理
     *
     * @param image        BufferedImage 需要处理的图片
     * @param cornerRadius 圆角度
     * @return 处理后的图片
     */
    private static BufferedImage makeRoundedCorner(BufferedImage image, int cornerRadius) {
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
}





