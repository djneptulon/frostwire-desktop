package com.limegroup.gnutella.gui.fw6ui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.frostwire.gui.player.MPlayerUIEventHandler;
import com.frostwire.gui.player.MediaPlayer;
import com.frostwire.gui.player.MediaPlayerAdapter;
import com.frostwire.gui.player.MediaPlayerListener;
import com.frostwire.gui.player.MediaSource;
import com.frostwire.gui.player.RepeatMode;
import com.frostwire.mplayer.MediaPlaybackState;
import com.limegroup.gnutella.gui.GUIMediator;

public class MediaPlayerVolumePanel extends JPanel {
	
	private static final long serialVersionUID = -21533825892232966L;

	private static int PANEL_HEIGHT = 70;
	private static int PANEL_WIDTH = 200;
	
	private JSlider volumeSlider;
	
	public MediaPlayerVolumePanel() {
				
		// initialize UI
		initializeUI();
		
		// initialize media player handler
		MediaPlayer.instance().addMediaPlayerListener( new MediaPlayerAdapter() {
			@Override public void volumeChange(MediaPlayer mediaPlayer, double currentVolume) {
				volumeSlider.setValue( (int)(currentVolume * 100.0f) );
			}
		});
	}
	
	private void initializeUI() {
		
		setLayout(new BoxLayout( this, BoxLayout.PAGE_AXIS));
		
		setOpaque(false);
		setBorder( BorderFactory.createEmptyBorder(10,0,10,0));
		
		setMinimumSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
		setMaximumSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
		setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
		
		
		// shuffle / loop panel
		// ---------------------
		JPanel shuffleButtonPanel = new JPanel();
		shuffleButtonPanel.setOpaque(false);
		shuffleButtonPanel.setLayout( new BoxLayout(shuffleButtonPanel, BoxLayout.LINE_AXIS) );
		
		// shuffle button
		JToggleButton shuffleButton = MediaPlayerUtils.createMediaToggleButton("shuffle_on", "shuffle_off", "Shuffle Songs");
		shuffleButton.setSelected(MediaPlayer.instance().isShuffle());
		shuffleButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent arg0) {
				MediaPlayer.instance().
					setShuffle(((JToggleButton)arg0.getSource()).isSelected());
			}
		});
		shuffleButtonPanel.add(shuffleButton);
		
		// loop button
		JToggleButton loopButton = MediaPlayerUtils.createMediaToggleButton("loop_on", "loop_off", "Repeat Songs");
		loopButton.setSelected(MediaPlayer.instance().getRepeatMode() == RepeatMode.All);
		loopButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent arg0) {
				MediaPlayer.instance().
					setRepeatMode(((JToggleButton)arg0.getSource()).isSelected() ? RepeatMode.All : RepeatMode.None);
			}
		});
		shuffleButtonPanel.add(loopButton);
		add(shuffleButtonPanel);
		
		// volume slider panel
		// ---------------------
		JPanel volumeSliderPanel = new JPanel();
		volumeSliderPanel.setOpaque(false);
		volumeSliderPanel.setLayout( new BoxLayout(volumeSliderPanel, BoxLayout.LINE_AXIS) );
		volumeSliderPanel.setBorder(BorderFactory.createEmptyBorder(0,20,0,20));
		
		// volume off image
		ImageIcon volMinIcon = GUIMediator.getThemeImage("fc_volume_off");
		JLabel volMinLabel = new JLabel( volMinIcon );
		volMinLabel.setOpaque(false);
		volMinLabel.setSize( volMinIcon.getIconWidth(), volMinIcon.getIconHeight());
		volMinLabel.setAlignmentY(0.5f);
        volumeSliderPanel.add(volMinLabel);
        
		// volume slider
        volumeSlider = new JSlider();
        volumeSlider.setOpaque(false);
        volumeSlider.setFocusable(false);
        volumeSlider.addChangeListener( new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				MPlayerUIEventHandler.instance().
					onVolumeChanged(((JSlider)e.getSource()).getValue() / 100.0f);
			}
        });
        volumeSlider.setAlignmentY(0.5f);
		volumeSliderPanel.add(volumeSlider);
		
		// volume on image
        ImageIcon volMaxIcon = GUIMediator.getThemeImage("fc_volume_on");
		JLabel volMaxLabel = new JLabel( volMaxIcon );
		volMaxLabel.setSize( volMaxIcon.getIconWidth(), volMaxIcon.getIconHeight());
		volMaxLabel.setAlignmentY(0.5f);
        volumeSliderPanel.add(volMaxLabel);
        
        add(volumeSliderPanel);
		
	}
}
