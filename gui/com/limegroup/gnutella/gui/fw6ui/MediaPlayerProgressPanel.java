package com.limegroup.gnutella.gui.fw6ui;


import javax.swing.BoxLayout;
import javax.swing.JPanel;

public class MediaPlayerProgressPanel extends JPanel {

	private static final long serialVersionUID = 2626046650466778244L;

	public MediaPlayerProgressPanel() {
		initializeUI();
	}
	
	private void initializeUI() {
		setLayout(new BoxLayout( this, BoxLayout.LINE_AXIS));
		
	}
}
