package de.themoep.utils;/*

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

public class BitUtils {
    
    public static boolean[] getBits(int number) {
        String binary = Integer.toBinaryString(number);
        boolean[] bits = new boolean[16];
        for (int i = 0; i < binary.length(); i++) {
            bits[15 - i] = (number & (1 << i)) != 0;
        }
        return bits;        
    }
    
    public static int getRange(int number, int start, int end) {
        boolean[] bits = getBits(number);

        if(end > bits.length || start > end) return 0;
        
        String range = "";
        for (int i = end; i >= start; i--) {
            range += (bits[15 - i]) ? 1 : 0;
        }
        try {
            return Integer.parseInt(range, 2);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
