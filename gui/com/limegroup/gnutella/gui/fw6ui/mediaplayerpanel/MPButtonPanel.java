package com.limegroup.gnutella.gui.fw6ui.mediaplayerpanel;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import com.frostwire.gui.player.MPlayerUIEventHandler;
import com.frostwire.gui.player.MediaPlayer;
import com.frostwire.gui.player.MediaPlayerAdapter;
import com.frostwire.mplayer.MediaPlaybackState;
import com.limegroup.gnutella.gui.I18n;

public class MPButtonPanel extends Box {
	
	private static final long serialVersionUID = 85874961592690100L;
	
	private static int PANEL_HEIGHT = 70;
	private static int BUTTON_PANEL_WIDTH = 200;
	private static int OUTER_SPACER = 15;
	
	private CardLayout playPauseCardLayout = null;
	private JPanel playPausePanel = null;
	
	public MPButtonPanel() {
		
		super(BoxLayout.LINE_AXIS);
		
		// initialize UI elements
		initializeUI();
		
		// initialize media player state handling
		MediaPlayer.instance().addMediaPlayerListener( new MediaPlayerAdapter() {
			@Override public void stateChange(MediaPlayer mediaPlayer, MediaPlaybackState state) {
				switch( state ) {
				case Playing:
					playPauseCardLayout.show(playPausePanel, "PAUSE");
					break;
				case Paused:
					playPauseCardLayout.show(playPausePanel, "PLAY");
					break;
				case Closed:
					playPauseCardLayout.show(playPausePanel, "PLAY");
					break;
				default:
					break;
				}
			}
		});
	}
	
	private void initializeUI() {
		
		// initialize buttons
		// --------------------
		JButton playButton = MPUtils.createMediaButton("fc_play", I18n.tr("Play"));
		playButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent arg0) {
				MPlayerUIEventHandler.instance().onPlayPressed();
			}
		});
		
		JButton pauseButton = MPUtils.createMediaButton("fc_pause", I18n.tr("Pause"));
		pauseButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent arg0) {
				MPlayerUIEventHandler.instance().onPausePressed();
			}
		});
		
		JButton nextButton = MPUtils.createMediaButton("fc_next", I18n.tr("Next"));
		nextButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent arg0) {
				MPlayerUIEventHandler.instance().onFastForwardPressed();
			}
		});
		
		JButton prevButton = MPUtils.createMediaButton("fc_previous", I18n.tr("Previous"));
		prevButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent arg0) {
				MPlayerUIEventHandler.instance().onRewindPressed();
			}
		});


		// initialize UI elements
		// ------------------------
		
		setMinimumSize(new Dimension(BUTTON_PANEL_WIDTH, PANEL_HEIGHT));
		setMaximumSize(new Dimension(BUTTON_PANEL_WIDTH, PANEL_HEIGHT));
		setPreferredSize(new Dimension(BUTTON_PANEL_WIDTH, PANEL_HEIGHT));
		
		setBorder(BorderFactory.createEmptyBorder(0, OUTER_SPACER, 0, OUTER_SPACER));
		
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
		
	}

}
