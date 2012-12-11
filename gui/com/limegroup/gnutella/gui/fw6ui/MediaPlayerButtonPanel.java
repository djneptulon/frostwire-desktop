package com.limegroup.gnutella.gui.fw6ui;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.themes.ThemeSettings;

public class MediaPlayerButtonPanel extends Box {
	
	private static final long serialVersionUID = 85874961592690100L;
	
	private static int PANEL_HEIGHT = 70;
	private static int BUTTON_PANEL_WIDTH = 200;
	private static int OUTER_SPACER = 15;
	
	private CardLayout playPauseCardLayout = null;
	private JPanel playPausePanel = null;
	
	public MediaPlayerButtonPanel() {
		
		super(BoxLayout.LINE_AXIS);
		
		initializeUI();
	}
	
	private void initializeUI() {
		
		// initialize buttons
		// --------------------
		JButton playButton = createMediaButton("fc_play", I18n.tr("Play"));
		playButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
			}
		});
		
		JButton pauseButton = createMediaButton("fc_pause", I18n.tr("Pause"));
		pauseButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
			}
		});
		
		JButton nextButton = createMediaButton("fc_next", I18n.tr("Next"));
		nextButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
			}
		});
		
		JButton prevButton = createMediaButton("fc_previous", I18n.tr("Previous"));
		prevButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
			}
		});


		// initialize UI elements
		// ------------------------
		
		setMinimumSize(new Dimension(BUTTON_PANEL_WIDTH, PANEL_HEIGHT));
		setMaximumSize(new Dimension(BUTTON_PANEL_WIDTH, PANEL_HEIGHT));
		setPreferredSize(new Dimension(BUTTON_PANEL_WIDTH, PANEL_HEIGHT));
		
		// ensure height
		add(Box.createRigidArea(new Dimension(0,PANEL_HEIGHT)));
		
		add(Box.createRigidArea(new Dimension(OUTER_SPACER,0)));
		
		add(prevButton);
		add(Box.createHorizontalGlue());
		
		playPauseCardLayout = new CardLayout();
		playPausePanel = new JPanel( playPauseCardLayout );
		playPausePanel.add(playButton, "PLAY");
		playPausePanel.add(pauseButton, "PAUSE");
		playPausePanel.setOpaque(false);
		playPauseCardLayout.show(playPausePanel, "PLAY");
		add(playPausePanel);
		
		add(Box.createHorizontalGlue());
		add(nextButton);
		
		add(Box.createRigidArea(new Dimension(OUTER_SPACER,0)));
		
	}
	
	private JButton createMediaButton(String name, String tipText ) {
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
}
