package com.limegroup.gnutella.gui.fw6ui;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JPanel;

public class MediaPlayerRowPanel extends JPanel {
	
	private static final Color BACKGROUND_COLOR = new Color(58, 103, 141);
	
	private static final long serialVersionUID = -1858398833731590955L;
	
	public MediaPlayerRowPanel() {
		
		initializeUI();
	}
	
	private void initializeUI() {
	
		setBackground(BACKGROUND_COLOR);
		setLayout(new BorderLayout());
		
		add(new MediaPlayerButtonPanel(), BorderLayout.LINE_START);
		add(new MediaPlayerProgressPanel(), BorderLayout.CENTER);
		add(new MediaPlayerVolumePanel(), BorderLayout.LINE_END);
	}

}
