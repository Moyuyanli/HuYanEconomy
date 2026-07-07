import cn.chahuyun.economy.image.EconomyImageRenderer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Manual image preview tool.
 *
 * This class is intentionally not a JUnit test. Run main() from the IDE or java command
 * to regenerate build/image-preview/*.png while tuning AWT coordinates.
 */
public class Test {

    public static void main(String[] args) throws Exception {
        imageTest();
    }

    public static void imageTest() throws Exception {
        // Keep generated previews out of src/ so design checks do not dirty resource files.
        File outputDir = new File("build/image-preview");
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IllegalStateException("Cannot create preview dir: " + outputDir.getAbsolutePath());
        }

        // Use the same renderer as production commands. The preview output should match bot output
        // except for mock data/avatar values in previewPersonalInfo().
        Font font = loadPreviewFont();
        BufferedImage personal = EconomyImageRenderer.previewPersonalInfo(font);
        BufferedImage help = EconomyImageRenderer.renderMainHelp(font);
        BufferedImage gameHelp = EconomyImageRenderer.renderGameHelp(font);

        write(personal, new File(outputDir, "personal-info.png"));
        write(help, new File(outputDir, "help.png"));
        write(gameHelp, new File(outputDir, "game-help.png"));

        System.out.println("image-preview -> " + outputDir.getAbsolutePath());
        if (!GraphicsEnvironment.isHeadless()) {
            view(personal);
        }
    }

    private static Font loadPreviewFont() {
        // Prefer the bundled test font so local previews look close to plugin custom-font output.
        File fontFile = new File("src/test/java/Maple UI.ttf");
        try {
            if (fontFile.exists()) {
                return Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(24f);
            }
        } catch (Exception ignored) {
        }
        return new Font("Microsoft YaHei UI", Font.PLAIN, 24);
    }

    private static void write(BufferedImage image, File file) throws Exception {
        ImageIO.write(image, "png", file);
        System.out.println("write -> " + file.getAbsolutePath());
    }

    public static void view(BufferedImage image) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("HuYanEconomy Image Preview");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(image.getWidth() + 50, image.getHeight() + 70);
            frame.add(new JLabel(new ImageIcon(image)), BorderLayout.CENTER);
            frame.setVisible(true);
        });
    }
}
