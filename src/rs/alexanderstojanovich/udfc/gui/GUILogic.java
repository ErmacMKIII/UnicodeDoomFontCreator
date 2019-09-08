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
package rs.alexanderstojanovich.udfc.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.BevelBorder;
import rs.alexanderstojanovich.udfc.util.ColorSample;
import rs.alexanderstojanovich.udfc.util.Palette;

/**
 *
 * @author Coa
 */
public class GUILogic {

    // tells us did we initialize the GUI_Logic
    private boolean initialized = false;

    // goal is to make this archive
    private File fontPK3;

    // font from the OS from which we derive the GZDoom font
    private Font myFont = new Font("Courier New", Font.PLAIN, 12);
    private String fontFormat = "Console Font";

    // begin char of character range
    private int beginChar = 32;
    // end char of the character range
    private int endChar = 127;

    // multiplier of cell size
    private float multiplier = 1.0f;

    // primary color (foreground)
    private Color fgColor = Color.YELLOW;
    // secondary color (background)
    private Color bgColor = Color.CYAN;
    // outline color (if outline has been selected by the user)
    private Color outlineColor = Color.BLUE;
    // outline thickness
    private int outlineWidth = 0;

    // One of the five choices for the palette, choosing other than None
    // will convert image to indexed one
    private String palette = "None";

    // via several labels coloured differently
    private JLabel[] colorVector = new JLabel[256];
    // Color panel which holds all the color labels
    private JPanel colorPanel; // <-- this is Palette panel from the GUI

    // job progress
    private JProgressBar progressBar;

    // <-- this is component list 
    // which gets disabled when iterations are on    
    private List<JComponent> disCompList;

    // use two-color gradient for the font (or use single foreground color)
    private boolean useGradient = false;
    // use antialias for the font (better not)
    private boolean useAntialias = false;

    // object for thread synchronization
    private final Object syncObj = new Object();
    // thread which makes the pk3 file
    private Thread jobWorker;
    // stop request from the user, job worker will stop promptly
    private boolean reqSTOP = false;

    // way to test the font without making the pk3 file :)
    private final GUIFontPreview gfp = new GUIFontPreview();

    //--------------------------------------------------------------------------
    // A - CONSTRUCTORS 
    //--------------------------------------------------------------------------
    public GUILogic(JPanel colorPanel, JProgressBar progressBar, List<JComponent> disCompList) {
        this.colorPanel = colorPanel;
        this.progressBar = progressBar;
        this.progressBar.setForeground(Color.WHITE);
        this.disCompList = disCompList;
        initColorVectors();
        
        this.jobWorker = new Thread("Job Worker") {
            @Override
            public void run() {
                while (true) {
                    synchronized (syncObj) {
                        try {
                            syncObj.wait();
                        } catch (InterruptedException ex) {
                            Logger.getLogger(GUILogic.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    setDisabledComps();
                    go();
                    setEnabledComps();
                    progressBar.setValue(0);
                }
            }
        };
        this.initialized = true;
    }

    //--------------------------------------------------------------------------
    // B - METHODS
    //--------------------------------------------------------------------------
    // init Palette display (it's called Color Vector)
    private void initColorVectors() {
        for (int i = 0; i < colorVector.length; i++) {
            colorVector[i] = new JLabel();
            colorVector[i].setBackground(Color.BLACK);
            colorVector[i].setOpaque(true);
            colorVector[i].setSize(9, 9);
            colorVector[i].setBorder(new BevelBorder(BevelBorder.RAISED));
            colorPanel.add(colorVector[i], new Integer(i));
        }
    }

    // give char image rendering certain char!
    public BufferedImage giveChImg(char ch) {
        // define sampler
        final double sampler = 2.0 * multiplier * outlineWidth;
        // create the FontRenderContext object which helps us to measure the text
        // subsequently craeting the rectangle for measuring the width and height
        // and the glyph vector containing the character, of course!
        FontRenderContext frc = new FontRenderContext(null, useAntialias, true);
        Rectangle2D rect = myFont.getStringBounds(String.valueOf(ch), frc);
        rect.setRect(rect.getX(), rect.getY(), rect.getWidth() + sampler, rect.getHeight() + sampler);
        GlyphVector gv = myFont.createGlyphVector(frc, String.valueOf(ch));

        // calculating with and height and adding +1 to be correctly displayed
        int w = (int) Math.round(rect.getWidth()) + 1;
        int h = (int) Math.round(rect.getHeight()) + 1;
        
        BufferedImage chImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        // create rendering char image graphics, where rendering take place
        Graphics2D chRender = chImg.createGraphics();
        // do the first necessary translation for each
        chRender.translate(0, -rect.getY());

        // don't forget to set font!
        chRender.setFont(myFont);
        
        if (useAntialias) {
            chRender.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            chRender.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        } else {
            chRender.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
            
            chRender.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_DEFAULT);
        }
        
        if (useGradient) {
            TextLayout chLayout = new TextLayout(String.valueOf(ch), myFont, frc);
            Rectangle2D gb = chLayout.getBounds();
            GradientPaint gp = new GradientPaint(
                    0.0f, (float) gb.getMinY() - (float) (0.5f * sampler),
                    fgColor,
                    0.0f, (float) gb.getMaxY() + (float) (0.5f * sampler),
                    bgColor, false);
            chRender.setPaint(gp);
        } else {
            chRender.setColor(fgColor);
        }
        chRender.drawGlyphVector(gv, (float) (0.5f * sampler), (float) (0.5f * sampler));

        //if antialiasing is selected multiply color with it's alpha
        if (useAntialias) {
            for (int px = 0; px < chImg.getWidth(); px++) {
                for (int py = 0; py < chImg.getHeight(); py++) {
                    Color srcCol = new Color(chImg.getRGB(px, py), true);
                    if (srcCol.getAlpha() > 0) { // this if is in order to not ruin the borders around the chars
                        Color dstCol = new Color( // constructor with the three floats is called
                                (srcCol.getAlpha() / 255.0f) * (srcCol.getRed() / 255.0f),
                                (srcCol.getAlpha() / 255.0f) * (srcCol.getGreen() / 255.0f),
                                (srcCol.getAlpha() / 255.0f) * (srcCol.getBlue() / 255.0f)
                        );
                        chImg.setRGB(px, py, dstCol.getRGB());
                    }
                }
            }
        }
        // if outline is selected;
        if (outlineWidth > 0) {
            // Copy of raster of unaltered image is needed!!
            WritableRaster wr = chImg.copyData(null);
            for (int px = 0; px < chImg.getWidth(); px++) {
                for (int py = 0; py < chImg.getHeight(); py++) {
                    Color pixCol = new Color(chImg.getRGB(px, py), true);
                    // writtable raster must be associated with ARGB image!!
                    ColorSample cs = ColorSample.getSample(wr, px, py, outlineWidth);
                    if (pixCol.getAlpha() == 0 && cs.getAlpha() > 0) {
                        chImg.setRGB(px, py, outlineColor.getRGB());
                    }
                }
            }
        }

        // if user chose palette in the image, make conversion..
        if (Palette.isLoaded()) {
            IndexColorModel icm = new IndexColorModel(8, Palette.getColors().length, Palette.getColBuff(), 0, true);
            BufferedImage imageIndexed = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_INDEXED, icm);
            imageIndexed.createGraphics().drawImage(chImg, 0, 0, null);
            chImg = imageIndexed;
        }
        return chImg;
    }

    // way to preview the fonts (testing it) without actually making it (as a pk3)
    public void preview(String text) {
        // 1. validating
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Nothing to preview!",
                    "Font Preview",
                    JOptionPane.ERROR_MESSAGE
            );
        } else {
            // 2. initializing
            int totalwidth = 0;
            int maxheight = 0;
            int[] offset = new int[text.length()];
            BufferedImage[] chImgs = new BufferedImage[text.length()];
            // 3. calculating the parameters for the final (text) image
            // also gathering char images into an array
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                BufferedImage chImg = giveChImg(ch);
                chImgs[i] = chImg;
                offset[i] = totalwidth;
                totalwidth += chImg.getWidth();
                if (chImg.getHeight() > maxheight) {
                    maxheight = chImg.getHeight();
                }
            }
            // 4. Composing final text image of several char images 
            // each rendered on different offset
            BufferedImage textImg = new BufferedImage(totalwidth, maxheight, BufferedImage.TYPE_INT_ARGB);
            for (int j = 0; j < text.length(); j++) {
                BufferedImage chImg = chImgs[j];
                textImg.createGraphics().drawImage(chImg, offset[j], 0, null);
            }
            // 5. When final image i.e. textImg is ready -> preview it!
            gfp.setUp();
            gfp.preview(textImg);
        }
    }

    // create the pk3 file, the main steel method; job worker uses it!
    private void go() {
        // Since GZDoom you can make font folder with font pics.. cool!
        // Lets determine main entry of .pk3, what it should be called..
        String fontDirName;
        switch (fontFormat) {
            case "Console Font":
                fontDirName = "consolefont";
                break;
            case "Small Font":
                fontDirName = "defsmallfont";
                break;
            case "Big Font":
                fontDirName = "bigfont";
                break;
            case "Big Upper":
                fontDirName = "bigupper";
                break;
            default:
                return;
        }
        if (!fontPK3.getName().contains(".pk3")) {
            fontPK3 = new File(fontPK3.getAbsolutePath() + ".pk3");
        }
        // if file exists delete file        
        if (fontPK3.exists()) {
            fontPK3.delete();
        }
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new FileOutputStream(fontPK3));
            StringBuilder sb = new StringBuilder();
            
            sb.append("filter/");
            ZipEntry firstDirEntry = new ZipEntry(sb.toString());
            zos.putNextEntry(firstDirEntry);
            zos.closeEntry();
            
            sb.append("doom.id/");
            ZipEntry secondDirEntry = new ZipEntry(sb.toString());
            zos.putNextEntry(secondDirEntry);
            zos.closeEntry();
            
            sb.append("fonts/");
            ZipEntry thirdDirEntry = new ZipEntry(sb.toString());
            zos.putNextEntry(thirdDirEntry);
            zos.closeEntry();
            
            sb.append(fontDirName).append("/");
            ZipEntry fourthDirEntry = new ZipEntry(sb.toString());
            zos.putNextEntry(fourthDirEntry);
            zos.closeEntry();
            
            for (int i = beginChar; i <= endChar && !reqSTOP; i++) {
                BufferedImage chImg = giveChImg((char) i);

                // determine the image name
                String imgFileName = String.format("%04X", i) + ".png";
                // making entry with the image name which is inside main dir entry
                ZipEntry entry = new ZipEntry(sb.toString() + imgFileName);
                // putting the entry..
                zos.putNextEntry(entry);

                // which contains the image.. yes!
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(chImg, "png", baos);
                byte[] data = baos.toByteArray();
                zos.write(data);
                zos.closeEntry();
                // closing the entry!

                float progress = (i + 1) / (float) (endChar - beginChar + 1);
                progressBar.setValue(Math.round(100 * progress));
                progressBar.validate();
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GUILogic.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(GUILogic.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (zos != null) {
                try {
                    zos.close();
                } catch (IOException ex) {
                    Logger.getLogger(GUILogic.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        // if user demanded stop with stop signal (by pressing the button or via menu)!
        if (reqSTOP) {
            JOptionPane.showMessageDialog(
                    null,
                    "Job terminated!",
                    "Job Result",
                    JOptionPane.ERROR_MESSAGE
            );
        } else {
            JOptionPane.showMessageDialog(
                    null,
                    "Job finished successfuly!",
                    "Job Result",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
        reqSTOP = false;
    }

    // palette load wrapper
    public void loadPalette(String palette) {
        switch (palette) {
            case "Doom":
                Palette.load("DoomPalette.pal");
                break;
            case "Heretic":
                Palette.load("HereticPalette.pal");
                break;
            case "Hexen":
                Palette.load("HexenPalette.pal");
                break;
            case "6-bit RGB":
                Palette.load6bitRGB();
                break;
            case "8-bit RGB":
                Palette.load8bitRGB();
                ;
                break;
            default:
                Palette.reset();
                break;
        }
    }

    // display palette in the effects area
    public void displayPalette() {
        if (Palette.isLoaded()) {
            for (int i = 0; i < Palette.getColors().length; i++) {
                Color col = new Color(Palette.getColors()[i]);
                colorVector[i].setBackground(col);
                colorVector[i].setToolTipText("Red = " + col.getRed()
                        + ", Green = " + col.getGreen() + ", Blue = " + col.getBlue());
            }
            for (int j = Palette.getColors().length; j < colorVector.length; j++) {
                colorVector[j].setBackground(Color.BLACK);
                colorVector[j].setToolTipText(null);
            }
        } else {
            for (JLabel label : colorVector) {
                label.setBackground(Color.BLACK);
                label.setToolTipText(null);
            }
        }
    }

    // set enabled components all apart from "STOP"
    private void setEnabledComps() {
        for (JComponent comp : disCompList) {
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                button.setEnabled(!button.getText().toUpperCase().equals("STOP"));
            } else if (comp instanceof JMenuItem) {
                JMenuItem item = (JMenuItem) comp;
                item.setEnabled(!item.getText().toUpperCase().equals("STOP"));
            }
        }
    }

    // set disabled components all apart from "STOP"
    private void setDisabledComps() {
        for (JComponent comp : disCompList) {
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                button.setEnabled(button.getText().toUpperCase().equals("STOP"));
            } else if (comp instanceof JMenuItem) {
                JMenuItem item = (JMenuItem) comp;
                item.setEnabled(item.getText().toUpperCase().equals("STOP"));
            }
        }
    }

    // Asynchronous reset  - returns the logic into initial state
    public void reset() {
        myFont = new Font("Courier New", Font.PLAIN, 12);
        
        beginChar = 32;
        endChar = 127;
        
        fgColor = Color.YELLOW;
        bgColor = Color.CYAN;
        outlineColor = Color.BLUE;
        outlineWidth = 0;
        
        palette = "None";
        useGradient = false;
        useAntialias = false;
        
        reqSTOP = false;
        
        Palette.reset();
    }
    //--------------------------------------------------------------------------
    // C - GETTERS AND SETTERS
    //--------------------------------------------------------------------------

    public boolean isInitialized() {
        return initialized;
    }
    
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }
    
    public File getFontPK3() {
        return fontPK3;
    }
    
    public void setFontPK3(File fontPK3) {
        this.fontPK3 = fontPK3;
    }
    
    public Font getMyFont() {
        return myFont;
    }
    
    public void setMyFont(Font myFont) {
        this.myFont = myFont;
    }
    
    public String getFontFormat() {
        return fontFormat;
    }
    
    public void setFontFormat(String fontFormat) {
        this.fontFormat = fontFormat;
    }
    
    public int getBeginChar() {
        return beginChar;
    }
    
    public void setBeginChar(int beginChar) {
        this.beginChar = beginChar;
    }
    
    public int getEndChar() {
        return endChar;
    }
    
    public void setEndChar(int endChar) {
        this.endChar = endChar;
    }
    
    public float getMultiplier() {
        return multiplier;
    }
    
    public void setMultiplier(float multiplier) {
        this.multiplier = multiplier;
    }
    
    public Color getFgColor() {
        return fgColor;
    }
    
    public void setFgColor(Color fgColor) {
        this.fgColor = fgColor;
    }
    
    public Color getBgColor() {
        return bgColor;
    }
    
    public void setBgColor(Color bgColor) {
        this.bgColor = bgColor;
    }
    
    public Color getOutlineColor() {
        return outlineColor;
    }
    
    public void setOutlineColor(Color outlineColor) {
        this.outlineColor = outlineColor;
    }
    
    public int getOutlineWidth() {
        return outlineWidth;
    }
    
    public void setOutlineWidth(int outlineWidth) {
        this.outlineWidth = outlineWidth;
    }
    
    public String getPalette() {
        return palette;
    }
    
    public void setPalette(String palette) {
        this.palette = palette;
    }
    
    public JLabel[] getColorVector() {
        return colorVector;
    }
    
    public void setColorVector(JLabel[] colorVector) {
        this.colorVector = colorVector;
    }
    
    public JPanel getColorPanel() {
        return colorPanel;
    }
    
    public void setColorPanel(JPanel colorPanel) {
        this.colorPanel = colorPanel;
    }
    
    public JProgressBar getProgressBar() {
        return progressBar;
    }
    
    public void setProgressBar(JProgressBar progressBar) {
        this.progressBar = progressBar;
    }
    
    public List<JComponent> getDisCompList() {
        return disCompList;
    }
    
    public void setDisCompList(List<JComponent> disCompList) {
        this.disCompList = disCompList;
    }
    
    public boolean isUseGradient() {
        return useGradient;
    }
    
    public void setUseGradient(boolean useGradient) {
        this.useGradient = useGradient;
    }
    
    public boolean isUseAntialias() {
        return useAntialias;
    }
    
    public void setUseAntialias(boolean useAntialias) {
        this.useAntialias = useAntialias;
    }
    
    public Thread getJobWorker() {
        return jobWorker;
    }
    
    public void setJobWorker(Thread jobWorker) {
        this.jobWorker = jobWorker;
    }
    
    public boolean isReqSTOP() {
        return reqSTOP;
    }
    
    public void setReqSTOP(boolean reqSTOP) {
        this.reqSTOP = reqSTOP;
    }
    
    public GUIFontPreview getGfp() {
        return gfp;
    }
    
    public Object getSyncObj() {
        return syncObj;
    }
    
}
