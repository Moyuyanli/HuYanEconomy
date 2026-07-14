import cn.chahuyun.economy.image.EconomyImageRenderer;
import cn.chahuyun.economy.image.FarmDetailImageRenderer;
import cn.chahuyun.economy.image.FishingInfoImageRenderer;
import cn.chahuyun.economy.image.model.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        BufferedImage privateBank = EconomyImageRenderer.renderPrivateBankInfo(previewPrivateBank(), font);
        BufferedImage farmDetail = FarmDetailImageRenderer.INSTANCE.render(previewFarmDetail());
        BufferedImage fishingInfo = FishingInfoImageRenderer.INSTANCE.render(previewFishingInfo());

        write(personal, new File(outputDir, "personal-info.png"));
        write(help, new File(outputDir, "help.png"));
        write(gameHelp, new File(outputDir, "game-help.png"));
        write(privateBank, new File(outputDir, "private-bank-info.png"));
        write(farmDetail, new File(outputDir, "farm-detail.png"));
        write(fishingInfo, new File(outputDir, "fishing-info.png"));

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

    private static PrivateBankInfoCard previewPrivateBank() {
        return new PrivateBankInfoCard(
                "狐言中央银行",
                "huyan",
                "稳健经营，轻松周转。",
                "572490972",
                4,
                "3.5%",
                "4.8",
                "88.6w",
                "96%",
                "无",
                Arrays.asList(
                        new BankInfoFundLine("准备资金池", "120M", "主银行账户"),
                        new BankInfoFundLine("流动资金池", "35M", "可周转资金"),
                        new BankInfoFundLine("放贷库存", "20M", "冻结额度"),
                        new BankInfoFundLine("风险保证金", "512.1M", "行长主银行"),
                        new BankInfoFundLine("国卷持仓", "8M", "未赎回"),
                        new BankInfoFundLine("狐卷持仓", "6.8M", "未到期")
                ),
                Arrays.asList(
                        new BankInfoLoanLine("可借额度", "20M", "2 个可借项目"),
                        new BankInfoLoanLine("发布总额", "32M", "当前启用额度"),
                        new BankInfoLoanLine("代收本息", "2.5M", "3 笔未结清"),
                        new BankInfoLoanLine("放贷利率", "1.5%/3%", "最小 / 最大")
                )
        );
    }

    private static FarmDetailCard previewFarmDetail() {
        List<FarmPlotDetailLine> plots = new ArrayList<>();
        for (int index = 1; index <= 18; index++) {
            if (index <= 3) {
                plots.add(new FarmPlotDetailLine(
                        index,
                        "胡萝卜",
                        "第1/2季",
                        index == 1 ? "可收获" : "成长中",
                        index == 1 ? "已成熟" : (index * 8) + "分钟",
                        index == 1 ? FarmPlotDetailStatus.READY : FarmPlotDetailStatus.GROWING
                ));
            } else if (index <= 8) {
                plots.add(new FarmPlotDetailLine(
                        index,
                        "空闲土地",
                        "可播种",
                        "空闲",
                        "等待种植",
                        FarmPlotDetailStatus.EMPTY
                ));
            } else {
                plots.add(new FarmPlotDetailLine(
                        index,
                        "未开垦",
                        "升级农场",
                        "锁定",
                        "待开垦",
                        FarmPlotDetailStatus.LOCKED
                ));
            }
        }

        return new FarmDetailCard(
                "预览用户(572490972)",
                4,
                9,
                18,
                3,
                1,
                5,
                "未激活",
                "13级开放",
                "浇水与偷菜共享每日次数。",
                plots
        );
    }

    private static FishingInfoCard previewFishingInfo() {
        return new FishingInfoCard(
                "预览用户(572490972)",
                "Lv.100",
                "小狐狸的星怒鱼塘(Lv.9)",
                "鲑鱼 114cm",
                "鱼等级 Lv.3 / 价值 912.0 / 鱼塘 小狐狸的星怒鱼塘",
                "266",
                "266",
                "小狐狸的星怒鱼塘(Lv.9)",
                "最低鱼竿等级 10 / 累计上鱼 16267 次 / 鱼种 24 个"
        );
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
