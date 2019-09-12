/*
 * Copyright (C) 2019 Coa
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package rs.alexanderstojanovich.udfc.util;

import java.awt.image.WritableRaster;

/**
 *
 * @author Coa
 */
public class ColorSample { // used primarily for drawing outline in GUILogic

    private int red = 0;
    private int green = 0;
    private int blue = 0;
    private int alpha = 0;

    //--------------------------------------------------------------------------
    // A - CONSTRUCTORS 
    //--------------------------------------------------------------------------
    public ColorSample() {
        // .. EMPTY -> DEFAULT CONSTRUCTOR
    }

    // Constructor for Essential Static Method
    public ColorSample(int red, int green, int blue, int alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    //--------------------------------------------------------------------------
    // B - ESSENTIAL STATIC METHODS
    //--------------------------------------------------------------------------    
    // B1 - Get Sample from all of adjacent pixels on given the offset
    public static ColorSample getSample(WritableRaster wr, int px, int py, int offset) {
        // INPUT LOGIC          
        int len = 2 * offset + 1;
        int[] offX = new int[len];
        int[] offY = new int[len];
        int e = 0; // used for indexing
        for (int i = -offset; i <= offset; i++) {
            if (i < 0) {
                offX[e] = Math.max(px + i, 0);
                offY[e] = Math.max(py + i, 0);
            } else {
                offX[e] = Math.min(px + i, wr.getWidth() - 1);
                offY[e] = Math.min(py + i, wr.getHeight() - 1);
            }
            e++;
        }
        //----------------------------------------------------------------------
        // RED, GREEN, BLUE AND ALPHA SAMPLE        
        int sumR = 0;
        int sumG = 0;
        int sumB = 0;
        int sumA = 0;
        for (int i = 0; i < offX.length; i++) {
            for (int j = 0; j < offY.length; j++) {
                sumR += wr.getSample(offX[i], offY[j], 0);
                sumG += wr.getSample(offX[i], offY[j], 1);
                sumB += wr.getSample(offX[i], offY[j], 2);
                sumA += wr.getSample(offX[i], offY[j], 3);
            }
        }
        int avgR = sumR / (len * len);
        int avgG = sumG / (len * len);
        int avgB = sumB / (len * len);
        int avgA = sumA / (len * len);
        //----------------------------------------------------------------------        
        // OUTPUT LOGIC
        return new ColorSample(avgR, avgG, avgB, avgA);
    }

    // B2 - Get Sample from all of adjacent pixels with Gauss kernel coefficients for single pass
    public static ColorSample getGaussianBlurSample(WritableRaster wr, int px, int py) {
        // INPUT LOGIC
        final float a = 0.123317f; // up-left-right-down
        final float b = 0.077847f; // diagonal
        final float c = 0.195346f; // center  
        int[] offX = {Math.max(px - 1, 0), px, Math.min(px + 1, wr.getWidth() - 1)};
        int[] offY = {Math.max(py - 1, 0), py, Math.min(py + 1, wr.getHeight() - 1)};
        int red = 0;
        int green = 0;
        int blue = 0;
        int alpha = 0;
        // [0] - RED
        red += b * (wr.getSample(offX[0], offY[0], 0)
                + wr.getSample(offX[2], offY[0], 0)
                + wr.getSample(offX[0], offY[2], 0)
                + wr.getSample(offX[2], offY[2], 0));

        red += c * (wr.getSample(offX[1], offY[1], 0));

        red += a * (wr.getSample(offX[1], offY[0], 0) + wr.getSample(offX[0], offY[1], 0)
                + wr.getSample(offX[1], offY[2], 0) + wr.getSample(offX[2], offY[1], 0));
        // [1] - GREEN
        green += b * (wr.getSample(offX[0], offY[0], 1)
                + wr.getSample(offX[2], offY[0], 1)
                + wr.getSample(offX[0], offY[2], 1)
                + wr.getSample(offX[2], offY[2], 1));

        green += c * (wr.getSample(offX[1], offY[1], 1));

        green += a * (wr.getSample(offX[1], offY[0], 1) + wr.getSample(offX[0], offY[1], 1)
                + wr.getSample(offX[1], offY[2], 1) + wr.getSample(offX[2], offY[1], 1));
        // [2] - BLUE
        blue += b * (wr.getSample(offX[0], offY[0], 2)
                + wr.getSample(offX[2], offY[0], 2)
                + wr.getSample(offX[0], offY[2], 2)
                + wr.getSample(offX[2], offY[2], 2));

        blue += c * (wr.getSample(offX[1], offY[1], 2));

        blue += a * (wr.getSample(offX[1], offY[0], 2) + wr.getSample(offX[0], offY[1], 2)
                + wr.getSample(offX[1], offY[2], 2) + wr.getSample(offX[2], offY[1], 2));
        // [3] - ALPHA
        alpha += b * (wr.getSample(offX[0], offY[0], 3)
                + wr.getSample(offX[2], offY[0], 3)
                + wr.getSample(offX[0], offY[2], 3)
                + wr.getSample(offX[2], offY[2], 3));

        alpha += c * (wr.getSample(offX[1], offY[1], 3));

        alpha += a * (wr.getSample(offX[1], offY[0], 3) + wr.getSample(offX[0], offY[1], 3)
                + wr.getSample(offX[1], offY[2], 3) + wr.getSample(offX[2], offY[1], 3));
        // FINALLY, OUTPUT LOGIC
        return new ColorSample(red, green, blue, alpha);
    }

    //--------------------------------------------------------------------------
    // C - GETTERS
    //--------------------------------------------------------------------------
    public int getRed() {
        return red;
    }

    public int getGreen() {
        return green;
    }

    public int getBlue() {
        return blue;
    }

    public int getAlpha() {
        return alpha;
    }

}
