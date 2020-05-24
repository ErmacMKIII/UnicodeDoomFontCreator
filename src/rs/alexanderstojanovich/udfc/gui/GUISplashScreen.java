/* 
 * Copyright (C) 2020 Alexander Stojanovich <coas91@rocketmail.com>
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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class GUISplashScreen extends JWindow implements Runnable {

    // path to splash screen image
    private static final String SPLASH_FILE_NAME = "udfc_splash.png";

    // label which contains splash image
    private JLabel splashImgLbl = new JLabel();
    // progress bar (the progress is dependant on the GUI initialization progress)
    private JProgressBar progressBar = new JProgressBar(0, 100);

    //--------------------------------------------------------------------------
    // A - CONSTRUCTORS 
    //--------------------------------------------------------------------------
    public GUISplashScreen() {
        URL splashImgURL = this.getClass().getResource(GUI.RESOURCES_DIR + SPLASH_FILE_NAME);
        ImageIcon splashImgIcon = new ImageIcon(splashImgURL);
        this.splashImgLbl.setSize(splashImgIcon.getIconWidth(), splashImgIcon.getIconHeight());
        this.splashImgLbl.setIcon(splashImgIcon);
        this.progressBar.setForeground(new Color(255, 150, 35));
    }

    //--------------------------------------------------------------------------
    // B - METHODS
    //--------------------------------------------------------------------------
    // Center the JFrame which is splash screen into center of the screen
    private void setUpPosition() {
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width / 2 - this.getWidth() / 2, dim.height / 2 - this.getHeight() / 2);
    }

    // call this method after constructor to add components and finish the effects
    public void setUp() {
        this.setLayout(new BorderLayout());
        this.setSize(splashImgLbl.getWidth(), splashImgLbl.getHeight());
        this.setUpPosition();
        this.getContentPane().add(splashImgLbl, BorderLayout.CENTER);
        this.getContentPane().add(progressBar, BorderLayout.SOUTH);
        this.setVisible(true);
        this.pack();
    }

    // this what this Runnable things to, updates it's progress bar depending on
    // the GUI initialization tasks
    @Override
    public void run() {
        while (GUI.getProgress() < 100) {
            this.progressBar.setValue(GUI.getProgress());
            this.progressBar.validate();
        }
        this.progressBar.setValue(GUI.getProgress());
        this.progressBar.validate();
        this.dispose();
    }

    //--------------------------------------------------------------------------
    // C - GETTERS
    //--------------------------------------------------------------------------
    public JLabel getSplashImgLbl() {
        return splashImgLbl;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

}
