package com.limegroup.gnutella.gui.fw6ui;

import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

public class MediaPlayerVolumePanel extends JPanel {
	
	private static final long serialVersionUID = -21533825892232966L;

	public MediaPlayerVolumePanel() {
		initializeUI();
	}
	
	private void initializeUI() {
		setLayout(new BoxLayout( this, BoxLayout.LINE_AXIS));
		add(Box.createRigidArea(new Dimension(100, 0)));
	}
}
