package com.tlabs.speechalyzer.util;

import java.awt.*;  
import javax.swing.*;  

@SuppressWarnings("serial") 
public class TestSwing2 extends JFrame {   

  public TestSwing2(String argx) { 
    if ( argx == null ) {
      argx = "a.png";
 }   
    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
    JPanel panel = new JPanel();  
    panel.setBackground(Color.CYAN);  
    ImageIcon icon = new ImageIcon(argx);  
    JLabel label = new JLabel();  
    label.setIcon(icon);  
    panel.add(label); 
    this.getContentPane().add(panel);    
    this.pack();
  } 

  public static void main(String[] args) { 
      new TestSwing2("res/images/labs_copyright_half.gif").setVisible(true);
  } 

}