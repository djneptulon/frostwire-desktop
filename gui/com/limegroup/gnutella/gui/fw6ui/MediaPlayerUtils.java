package com.limegroup.gnutella.gui.fw6ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.themes.ThemeSettings;

public class MediaPlayerUtils {
	private MediaPlayerUtils() {} // to protect against instantiation
	
	public static JButton createMediaButton(String name, String tipText ) {
		
		JButton button = new JButton();
		
		button.setContentAreaFilled(false);
		button.setBorderPainted(ThemeSettings.isNativeOSXTheme());
		button.setRolloverEnabled(true);
		button.setIcon(GUIMediator.getThemeImage(name));
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setPreferredSize(new Dimension( button.getIcon().getIconWidth(), 
											   button.getIcon().getIconHeight()));
		button.setFocusable(false);
		button.setMargin(new Insets(0,0,0,0));
		button.setToolTipText(tipText);  
		
		return button;
	}
	
	public static JToggleButton createMediaToggleButton(String onName, String offName, String toolTipText) {
		
		JToggleButton toggleButton = new JToggleButton();
		
		toggleButton.setBorderPainted(false);
		toggleButton.setContentAreaFilled(false);
		toggleButton.setBackground(null);
		toggleButton.setOpaque(false);
		toggleButton.setFocusable(false);
		toggleButton.setIcon(GUIMediator.getThemeImage(offName));
		toggleButton.setSelectedIcon(GUIMediator.getThemeImage(onName));
		toggleButton.setToolTipText(I18n.tr(toolTipText));
		
		return toggleButton;
	}
	
	public static JSeparator createSeparator( int direction) {
		JSeparator sep = new JSeparator(direction);
		sep.setForeground(Color.RED);
		sep.setBackground(Color.BLACK);
		sep.setSize(10, 100);
		sep.setOpaque(true);
		
		return sep;
	}
}
