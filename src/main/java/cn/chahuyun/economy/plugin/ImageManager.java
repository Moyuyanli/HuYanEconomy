package cn.chahuyun.economy.plugin;

import cn.chahuyun.economy.HuYanEconomy;
import cn.chahuyun.economy.utils.ImageUtil;
import lombok.Getter;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 底图管理
 *
 * @author Moyuyanli
 * @Date 2024/8/7 21:49
 */
public class ImageManager {

    private static int next = 1;
    private static final List<BufferedImage> bufferedImages = new ArrayList<>(8);

    @Getter
    private static Font customFont;

    protected static void init(HuYanEconomy instance) throws IOException, FontFormatException {
        Path path = instance.getDataFolderPath();
        File font = path.resolve("font").toFile();
        if (font.exists()) {
            File[] files = font.listFiles();
            if (files != null && files.length != 0) {
                customFont = Font.createFont(Font.TRUETYPE_FONT, files[0]).deriveFont(24f);
            }
        } else {
            customFont = new Font("宋体", Font.PLAIN, 24);
        }


        File bottom = path.resolve("bottom").toFile();
        if (!bottom.exists()) {
            return;
        }

        BufferedImage bottomPng = ImageIO.read(Objects.requireNonNull(instance.getResourceAsStream("bottom.png")));

        File[] files = bottom.listFiles();
        if (files != null) {
            for (File file : files) {
                drawBottom(ImageIO.read(file), bottomPng);
            }
        }
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


    private static void drawBottom(BufferedImage background, BufferedImage bottomPng) {
        // 创建一个新的 BufferedImage
        BufferedImage canvas = new BufferedImage(1024, 576, BufferedImage.TYPE_INT_ARGB);


        // 获取 Graphics2D 对象用于绘制
        Graphics2D g2d = ImageUtil.getG2d(canvas);

        // 计算缩放比例
        double scaleWidth = (double) 1024 / background.getWidth();
        double scaleHeight = (double) 576 / background.getHeight();
        double scale = Math.min(scaleWidth, scaleHeight);


        // 缩放后的宽度和高度
        int scaledWidth = (int) (background.getWidth() * scale);
        int scaledHeight = (int) (background.getHeight() * scale);


        // 居中的位置
        int x = (1024 - scaledWidth) / 2;
        int y = (576 - scaledHeight) / 2;


        // 绘制缩放后的背景图片到新的 BufferedImage 上
        g2d.drawImage(background, x, y, scaledWidth, scaledHeight, null);

        // 绘制底
        g2d.drawImage(bottomPng, 0, 0, null);


        g2d.setFont(customFont);
        g2d.setColor(Color.BLACK);

        g2d.drawString("签到时间:", 70, 340);
        g2d.drawString("连签次数:", 70, 385);

        String text = "如果你是在图片上操作而不是在GUI框体中，那么你可以使用类似的方法来处理自动换行的问题。这里是一";
        ImageUtil.drawString(text, 556, 315, 435, g2d);

        g2d.drawString("--- xxxxx:xxxxxx", 556, 410);

        int infoY = 478;
        g2d.setFont(customFont.deriveFont(32f));
        g2d.drawString("我的金币", 118, infoY);
        g2d.drawString("签到获得", 358, infoY);
        g2d.drawString("我的银行", 608, infoY);
        g2d.drawString("今日收益", 858, infoY);


        g2d.setFont(customFont.deriveFont(12f));
        g2d.drawString("by Mirai & HuYanEconomy(壶言经济) v" + HuYanEconomy.VERSION, 730, 573);

        // 释放资源
        g2d.dispose();

        bufferedImages.add(ImageUtil.makeRoundedCorner(canvas, 30));
    }

    /**
     * 获取下一个底图
     *
     * @return 新的一个底图
     */
    public static BufferedImage getNextBottom() {
        if (bufferedImages.isEmpty()) {
            return null;
        }

        BufferedImage bufferedImage = bufferedImages.get(bufferedImages.size() % next++);
        view(bufferedImage);
        return bufferedImage.getSubimage(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
    }


}