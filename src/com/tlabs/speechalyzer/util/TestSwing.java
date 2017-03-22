package com.tlabs.speechalyzer.util;

import java.awt.Image;
import java.awt.Point;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.felix.util.KeyValues;
import com.felix.util.SwingUtil;
import com.felix.util.SwingUtil.ImagePanel;

public class TestSwing extends JFrame {
	private KeyValues _config, _agenderConfig;
	private ImagePanel _mainPanel;
	private String _resolution = "";
	private boolean _firststart = true;

	/**
	 * 
	 */
	public TestSwing() {
		super();
		loadConfig();
		_resolution = _config.getString("resolution");
		try {
			Image[] images = new Image[1];
//			ImageIcon image = new ImageIcon("res/images/labs_copyright_half.gif");
			String path = _config.getString("imageDir")
					+ _resolution + "/" + _config.getString("startImage");
			System.out.println(path);
			ImageIcon image = new ImageIcon(path);
			images[0] = image.getImage();
//			_mainPanel = new SwingUtil.ImagePanel(images);
			_mainPanel = new SwingUtil.ImagePanel(images);
			
//			JLabel label = new JLabel();
//			label.setIcon(new ImageIcon("res/images/labs_copyright_half.gif"));
//			_mainPanel = new JPanel();
//			_mainPanel.add(label);
			getContentPane().add(_mainPanel);
			pack();
			setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// testFrame();
	}

	public void testFrame() {
		/*
		 * Erzeugung eines neuen Frames mit dem Titel "Beispiel JFrame "
		 */
		JFrame meinFrame = new JFrame("Beispiel JFrame");
		/*
		 * Wir setzen die Breite und die Höhe unseres Fensters auf 200 Pixel
		 */
		meinFrame.setSize(200, 200);
		// Wir lassen unseren Frame anzeigen
		meinFrame.setVisible(true);

	}

	public static void main(String[] args) {
		new TestSwing();
	}

	private void loadConfig() {
		try {
			_config = new KeyValues("res/sbcDemo.properties", "=");
			_agenderConfig = new KeyValues("res/aGenderDemo.properties", "=");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}