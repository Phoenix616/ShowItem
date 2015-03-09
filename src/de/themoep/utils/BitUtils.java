package de.themoep.utils;


/**
 * Created by Phoenix616 on 09.03.2015.
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
