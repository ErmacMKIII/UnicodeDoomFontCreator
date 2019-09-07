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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JWindow;

/**
 *
 * @author Coa
 */
public class GUIFontPreview extends JWindow {

    // label which contains the font preview image
    private JLabel imagePreviewLabel = new JLabel();
    // button which closes the window
    private JButton closeButton = new JButton("Close");

    //--------------------------------------------------------------------------
    // A - CONSTRUCTORS (NONE EXPLICIT, USE DEFAULT ONE)
    //--------------------------------------------------------------------------    
    //--------------------------------------------------------------------------
    // B - METHODS
    //--------------------------------------------------------------------------
    // Center the JWindow which is splash screen into center of the screen
    private void setUpPosition() {
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width / 2 - this.getWidth() / 2, dim.height / 2 - this.getHeight() / 2);
    }

    // call this method after constructor to add components and finish the effects
    public void setUp() {
        this.setLayout(new BorderLayout());
        this.setUpPosition();
        this.getContentPane().add(imagePreviewLabel, BorderLayout.CENTER);
        this.getContentPane().add(closeButton, BorderLayout.SOUTH);
        this.setVisible(true);

        this.closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tearDown();
            }
        });
    }

    // preview text by rendering it to the image
    public void preview(BufferedImage finalTextImg) {
        ImageIcon splashImgIcon = new ImageIcon(finalTextImg, "Test preview of the font");
        this.imagePreviewLabel.setSize(splashImgIcon.getIconWidth(), splashImgIcon.getIconHeight());
        this.imagePreviewLabel.setIcon(splashImgIcon);
        this.pack();
    }

    // method for disposing the Window "GUIFontPreview", 
    // call it after user finished analyzing the preview..
    public void tearDown() {
        this.dispose();
    }
    //--------------------------------------------------------------------------
    // C - GETTERS
    //--------------------------------------------------------------------------

    public JLabel getImagePreviewLabel() {
        return imagePreviewLabel;
    }

    public JButton getCloseButton() {
        return closeButton;
    }

}
