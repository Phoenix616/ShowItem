package de.themoep.utils;

import org.bukkit.ChatColor;
import org.bukkit.Color;

/**
 * Utility class for color related operations.
 */
public class ColorUtils {

    static Color[] colors = {
            Color.fromRGB(0, 0, 0),
            Color.fromRGB(0, 0, 170),
            Color.fromRGB(0, 170, 0),
            Color.fromRGB(0, 170, 170),
            Color.fromRGB(170, 0, 0),
            Color.fromRGB(170, 0, 170),
            Color.fromRGB(255, 170, 0),
            Color.fromRGB(170, 170, 170),
            Color.fromRGB(85, 85, 85),
            Color.fromRGB(85, 85, 255),
            Color.fromRGB(85, 255, 85),
            Color.fromRGB(85, 255, 255),
            Color.fromRGB(255, 85, 85),
            Color.fromRGB(255, 85, 255),
            Color.fromRGB(255, 255, 85),
            Color.fromRGB(255, 255, 255)
    };

    /**
     * Get the distance between two colors
     * @param colorA
     * @param colorB
     * @return The distance between the color values.
     */
    private static double getDistance(Color colorA, Color colorB) {
        double mean = (colorA.getRed() + colorB.getRed()) / 2.0;

        double r = colorA.getRed() - colorB.getRed();
        double g = colorA.getGreen() - colorB.getGreen();
        int b = colorA.getBlue() - colorB.getBlue();

        double weightR = 2 + mean / 256.0;
        double weightG = 4.0;
        double weightB = 2 + (255 - mean) / 256.0;

        return weightR * r * r + weightG * g * g + weightB * b * b;
    }

    /**
     * Get the nearest Minecraft ChatColor to a normal Color
     * @param color The color value
     * @return The nearest Minecraft ChatColor
     */
    public static ChatColor getNearestChatColor(Color color) {
        int index = 0;
        int best = -1;
        for (int i = 0; i < colors.length; i++) {
            double distance = getDistance(color, colors[i]);
            if (distance < best || best == -1) {
                best = (int) distance;
                index = i;
            }
        }
        return ChatColor.values()[index];
    }
}
