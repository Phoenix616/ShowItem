package de.themoep.utils;

/*
* Copyright (C) 2015 Max Lee (https://github.com/Phoenix616)
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the Mozilla Public License as published by
* the Mozilla Foundation, version 2.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* Mozilla Public License v2.0 for more details.
*
* You should have received a copy of the Mozilla Public License v2.0
* along with this program. If not, see <http://mozilla.org/MPL/2.0/>.
*/

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
