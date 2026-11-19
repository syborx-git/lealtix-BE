package com.lealtixservice.service.impl;

import com.lealtixservice.entity.CustomerLoyaltyCard;
import com.lealtixservice.entity.TenantLoyaltyCardConfig;
import com.lealtixservice.repository.CustomerLoyaltyCardRepository;
import com.lealtixservice.repository.TenantLoyaltyCardConfigRepository;
import com.lealtixservice.service.LoyaltyCardBannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoyaltyCardBannerServiceImpl implements LoyaltyCardBannerService {

    private final CustomerLoyaltyCardRepository cardRepository;
    private final TenantLoyaltyCardConfigRepository configRepository;

    private static final int WIDTH = 1032;
    private static final int HEIGHT = 336;

    @Override
    @Transactional(readOnly = true)
    public byte[] generateBannerPng(Long tenantId, Long customerId, Integer overrideStamps) {
        TenantLoyaltyCardConfig config = configRepository.findByTenantId(tenantId)
                .orElse(null);

        CustomerLoyaltyCard card = cardRepository.findByTenantIdAndCustomerId(tenantId, customerId)
                .orElse(null);

        return renderBanner(card, config, overrideStamps);
    }

    @Override
    public byte[] renderBanner(CustomerLoyaltyCard card, TenantLoyaltyCardConfig config, Integer overrideStamps) {
        int totalStamps = 8;
        int currentStamps = 0;
        String title = "Tarjeta de Fidelización";
        String subtitle = "Programa de Sellos y Recompensas";
        String rewardTitle = "Recompensa Exclusiva";
        String iconType = "STAR";
        String bgHex = "#1E1B4B"; // Default Indigo Dark

        if (config != null) {
            if (config.getTotalStamps() != null && config.getTotalStamps() > 0) {
                totalStamps = Math.min(config.getTotalStamps(), 12);
            }
            if (config.getCardTitle() != null && !config.getCardTitle().isBlank()) {
                title = config.getCardTitle();
            }
            if (config.getCardSubtitle() != null && !config.getCardSubtitle().isBlank()) {
                subtitle = config.getCardSubtitle();
            }
            if (config.getRewardTitle() != null && !config.getRewardTitle().isBlank()) {
                rewardTitle = config.getRewardTitle();
            }
            if (config.getStampIcon() != null && !config.getStampIcon().isBlank()) {
                iconType = config.getStampIcon().toUpperCase();
            }
            if (config.getBgColorHex() != null && !config.getBgColorHex().isBlank()) {
                bgHex = config.getBgColorHex();
            }
        }

        if (overrideStamps != null) {
            currentStamps = overrideStamps;
        } else if (card != null && card.getCurrentStamps() != null) {
            currentStamps = card.getCurrentStamps();
        }

        currentStamps = Math.max(0, Math.min(currentStamps, totalStamps));

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        try {
            // High-quality rendering hints
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            Color baseBg = parseColor(bgHex, new Color(30, 27, 75));
            Color darkBg = new Color(15, 23, 42); // slate-900

            // 1. Draw Background Gradient
            GradientPaint bgGradient = new GradientPaint(0, 0, baseBg, WIDTH, HEIGHT, darkBg);
            g.setPaint(bgGradient);
            g.fillRoundRect(0, 0, WIDTH, HEIGHT, 28, 28);

            // Subtle border glow
            g.setColor(new Color(255, 255, 255, 30));
            g.setStroke(new BasicStroke(2.0f));
            g.drawRoundRect(1, 1, WIDTH - 2, HEIGHT - 2, 28, 28);

            // 2. Header: Title, VIP Badge, and Progress Count
            // Title
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 26));
            FontMetrics fmTitle = g.getFontMetrics();
            int titleY = 48;
            g.drawString(title, 48, titleY);

            int titleWidth = fmTitle.stringWidth(title);

            // VIP Badge next to title
            int badgeX = 48 + titleWidth + 16;
            int badgeY = titleY - 20;
            int badgeW = 60;
            int badgeH = 26;
            g.setColor(new Color(255, 255, 255, 30));
            g.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 12, 12);
            g.setColor(new Color(245, 158, 11)); // Amber gold
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.drawString("VIP", badgeX + 18, badgeY + 18);

            // Subtitle
            g.setColor(new Color(203, 213, 225)); // Slate 300
            g.setFont(new Font("SansSerif", Font.PLAIN, 15));
            g.drawString(subtitle, 48, titleY + 24);

            // Right: Progress Counter
            String progressText = String.format("%d / %d SELLOS", currentStamps, totalStamps);
            g.setFont(new Font("SansSerif", Font.BOLD, 22));
            FontMetrics fmProg = g.getFontMetrics();
            int progW = fmProg.stringWidth(progressText);
            int progX = WIDTH - 48 - progW;
            g.setColor(currentStamps >= totalStamps ? new Color(245, 158, 11) : new Color(56, 189, 248));
            g.drawString(progressText, progX, titleY);

            // 3. Middle Section: Stamp Boxes
            drawStampsGrid(g, totalStamps, currentStamps, iconType);

            // 4. Bottom Section: Reward Ribbon / Pill
            drawRewardRibbon(g, rewardTitle, currentStamps, totalStamps);

        } finally {
            g.dispose();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Failed to encode loyalty card banner to PNG: {}", e.getMessage(), e);
            return new byte[0];
        }
    }

    private void drawStampsGrid(Graphics2D g, int totalStamps, int currentStamps, String iconType) {
        int boxSize = totalStamps <= 8 ? 82 : (totalStamps <= 10 ? 74 : 64);
        int gap = totalStamps <= 8 ? 16 : 12;
        int totalWidth = totalStamps * boxSize + (totalStamps - 1) * gap;
        int startX = (WIDTH - totalWidth) / 2;
        int boxY = 110;

        for (int i = 0; i < totalStamps; i++) {
            int bx = startX + i * (boxSize + gap);
            boolean isStamped = i < currentStamps;

            if (isStamped) {
                // Stamped: Filled card with soft white/light background
                g.setColor(new Color(255, 255, 255, 250));
                g.fillRoundRect(bx, boxY, boxSize, boxSize, 18, 18);

                // Accent border
                g.setColor(new Color(99, 102, 241)); // Indigo accent
                g.setStroke(new BasicStroke(2.0f));
                g.drawRoundRect(bx, boxY, boxSize, boxSize, 18, 18);

                // Draw Icon inside
                drawStampIcon(g, bx, boxY, boxSize, iconType);

            } else {
                // Pending / Unstamped: Dark translucent box with dashed border
                g.setColor(new Color(255, 255, 255, 14));
                g.fillRoundRect(bx, boxY, boxSize, boxSize, 18, 18);

                // Dashed stroke
                Stroke dashed = new BasicStroke(
                        2.0f,
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND,
                        1.0f,
                        new float[]{5.0f, 5.0f},
                        0.0f
                );
                g.setStroke(dashed);
                g.setColor(new Color(255, 255, 255, 60));
                g.drawRoundRect(bx, boxY, boxSize, boxSize, 18, 18);

                // Slot number
                g.setColor(new Color(203, 213, 225, 140));
                g.setFont(new Font("SansSerif", Font.BOLD, boxSize >= 80 ? 26 : 22));
                String numStr = String.valueOf(i + 1);
                FontMetrics fm = g.getFontMetrics();
                int numX = bx + (boxSize - fm.stringWidth(numStr)) / 2;
                int numY = boxY + (boxSize + fm.getAscent() - fm.getDescent()) / 2;
                g.drawString(numStr, numX, numY);
            }
        }
    }

    private void drawStampIcon(Graphics2D g, int bx, int by, int boxSize, String iconType) {
        int cx = bx + boxSize / 2;
        int cy = by + boxSize / 2;

        switch (iconType) {
            case "BURGER":
                // Hamburger
                // Top bun
                g.setColor(new Color(217, 119, 6)); // Amber bun
                g.fillArc(cx - 20, cy - 18, 40, 24, 0, 180);
                // Lettuce
                g.setColor(new Color(34, 197, 94)); // Green
                g.fillRect(cx - 21, cy - 6, 42, 5);
                // Patty
                g.setColor(new Color(120, 53, 15)); // Brown
                g.fillRoundRect(cx - 20, cy, 40, 8, 4, 4);
                // Bottom bun
                g.setColor(new Color(217, 119, 6));
                g.fillRoundRect(cx - 18, cy + 9, 36, 6, 4, 4);
                break;

            case "COFFEE":
                // Coffee Cup
                g.setColor(new Color(234, 88, 12)); // Orange-red
                g.fillRoundRect(cx - 16, cy - 10, 32, 24, 6, 6);
                // Handle
                g.setStroke(new BasicStroke(3.0f));
                g.drawArc(cx + 8, cy - 6, 14, 16, -90, 180);
                // Plate
                g.fillRoundRect(cx - 20, cy + 14, 40, 4, 2, 2);
                break;

            case "PIZZA":
                // Pizza slice
                Path2D pizza = new Path2D.Double();
                pizza.moveTo(cx, cy + 18);
                pizza.lineTo(cx - 18, cy - 14);
                pizza.lineTo(cx + 18, cy - 14);
                pizza.closePath();
                g.setColor(new Color(245, 158, 11)); // Cheese
                g.fill(pizza);
                // Pepperoni dots
                g.setColor(new Color(185, 28, 28));
                g.fillOval(cx - 6, cy - 6, 8, 8);
                g.fillOval(cx + 2, cy + 2, 6, 6);
                break;

            case "STAR":
            default:
                // Golden Star
                double outerR = boxSize * 0.28;
                double innerR = outerR * 0.44;
                Path2D star = createStar(cx, cy + 1, outerR, innerR, 5);
                GradientPaint starGrad = new GradientPaint(
                        cx, cy - (float) outerR, new Color(251, 191, 36),
                        cx, cy + (float) outerR, new Color(217, 119, 6)
                );
                g.setPaint(starGrad);
                g.fill(star);
                // Star subtle stroke
                g.setColor(new Color(180, 83, 9));
                g.setStroke(new BasicStroke(1.2f));
                g.draw(star);
                break;
        }
    }

    private Path2D createStar(double cx, double cy, double outerRadius, double innerRadius, int points) {
        Path2D path = new Path2D.Double();
        double angle = -Math.PI / 2.0;
        double delta = Math.PI / points;
        for (int i = 0; i < points * 2; i++) {
            double r = (i % 2 == 0) ? outerRadius : innerRadius;
            double x = cx + r * Math.cos(angle);
            double y = cy + r * Math.sin(angle);
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
            angle += delta;
        }
        path.closePath();
        return path;
    }

    private void drawRewardRibbon(Graphics2D g, String rewardTitle, int currentStamps, int totalStamps) {
        int ribbonW = 760;
        int ribbonH = 46;
        int ribbonX = (WIDTH - ribbonW) / 2;
        int ribbonY = 250;

        boolean isCompleted = currentStamps >= totalStamps;

        if (isCompleted) {
            // Gold completed banner
            GradientPaint goldGrad = new GradientPaint(
                    ribbonX, ribbonY, new Color(217, 119, 6),
                    ribbonX + ribbonW, ribbonY, new Color(245, 158, 11)
            );
            g.setPaint(goldGrad);
            g.fillRoundRect(ribbonX, ribbonY, ribbonW, ribbonH, 24, 24);

            g.setColor(new Color(255, 255, 255, 180));
            g.setStroke(new BasicStroke(2.0f));
            g.drawRoundRect(ribbonX, ribbonY, ribbonW, ribbonH, 24, 24);

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            String msg = "★ ¡RECOMPENSA GANADA: " + rewardTitle + "! (Presenta en caja) ★";
            FontMetrics fm = g.getFontMetrics();
            int tx = ribbonX + (ribbonW - fm.stringWidth(msg)) / 2;
            g.drawString(msg, tx, ribbonY + 29);

        } else {
            // In-progress dark translucent pill
            g.setColor(new Color(30, 41, 59, 210)); // Slate 800
            g.fillRoundRect(ribbonX, ribbonY, ribbonW, ribbonH, 24, 24);

            g.setColor(new Color(255, 255, 255, 25));
            g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(ribbonX, ribbonY, ribbonW, ribbonH, 24, 24);

            g.setColor(new Color(241, 245, 249)); // Slate 100
            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            String text = "Meta: " + rewardTitle + " (" + (totalStamps - currentStamps) + " sellos restantes)";
            FontMetrics fm = g.getFontMetrics();
            int tx = ribbonX + (ribbonW - fm.stringWidth(text)) / 2;
            g.drawString(text, tx, ribbonY + 28);
        }
    }

    private Color parseColor(String hex, Color defaultColor) {
        if (hex == null || hex.isBlank()) return defaultColor;
        try {
            String clean = hex.trim();
            if (clean.startsWith("#")) clean = clean.substring(1);
            if (clean.length() == 3) {
                clean = "" + clean.charAt(0) + clean.charAt(0) + clean.charAt(1) + clean.charAt(1) + clean.charAt(2) + clean.charAt(2);
            }
            if (clean.length() == 6) {
                return new Color(Integer.parseInt(clean, 16));
            }
        } catch (Exception ignored) {}
        return defaultColor;
    }
}
