/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package carskit.gui;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;

/**
 *
 * @author sihc
 */
public class OpenFile {
    
    public String path = "";

    public void openFile() throws IOException {
        JFileChooser chooser = new JFileChooser();
        Scanner in = null;
        File workingDirectory = new File(System.getProperty("user.dir"));
        chooser.setCurrentDirectory(workingDirectory);
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            path = selectedFile.toPath().toString();
        }
    }
    
    public static void main(String[] args) {
        OpenFile a = new OpenFile();
        try {
            a.openFile();
        } catch (IOException ex) {
            Logger.getLogger(OpenFile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
