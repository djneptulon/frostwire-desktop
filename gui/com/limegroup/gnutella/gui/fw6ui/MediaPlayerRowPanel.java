package com.limegroup.gnutella.gui.fw6ui;

import java.awt.Color;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class MediaPlayerRowPanel extends JPanel {
	
	private static final Color BACKGROUND_COLOR = new Color(58, 103, 141);
	
	private static final long serialVersionUID = -1858398833731590955L;
	
	public MediaPlayerRowPanel() {
		
		initializeUI();
	}
	
	private void initializeUI() {
	
		setBackground(BACKGROUND_COLOR);
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		setOpaque(true);
		
		add(new MediaPlayerButtonPanel());
		add(MediaPlayerUtils.createSeparator(SwingConstants.VERTICAL));
		add(new MediaPlayerProgressPanel());
		add(MediaPlayerUtils.createSeparator(SwingConstants.VERTICAL));
		add(new MediaPlayerVolumePanel());
	}

}
