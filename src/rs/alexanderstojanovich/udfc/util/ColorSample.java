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
    // B - ESSENTIAL STATIC METHOD
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
