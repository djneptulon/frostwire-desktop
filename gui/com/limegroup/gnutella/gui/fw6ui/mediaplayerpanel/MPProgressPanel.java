package com.limegroup.gnutella.gui.fw6ui.mediaplayerpanel;


import java.awt.BorderLayout;

import javax.swing.JPanel;

import com.frostwire.gui.mplayer.ProgressSlider;
import com.frostwire.gui.mplayer.ProgressSliderListener;
import com.frostwire.gui.player.MPlayerUIEventHandler;
import com.frostwire.gui.player.MediaPlayer;
import com.frostwire.gui.player.MediaPlayerAdapter;

public class MPProgressPanel extends JPanel {

	private static final long serialVersionUID = 2626046650466778244L;

	ProgressSlider progressSlider = null;
	
	public MPProgressPanel() {
		
		initializeUI();
		
		MediaPlayer.instance().addMediaPlayerListener(new MediaPlayerAdapter() {
			@Override public void progressChange(MediaPlayer mediaPlayer, float currentTimeInSecs) {
				progressSlider.setTotalTime(mediaPlayer.getDurationInSecs());
				progressSlider.setCurrentTime(currentTimeInSecs);
			}
		});
	}
	
	private void initializeUI() {
		setLayout(new BorderLayout());
		setOpaque(false);
		
		// title label
		
		// progress slider
		progressSlider = new ProgressSlider();
		progressSlider.addProgressSliderListener(new ProgressSliderListener() {
			@Override public void onProgressSliderTimeValueChange(float seconds) {
				MPlayerUIEventHandler.instance().onSeekToTime(seconds);
			}
			@Override public void onProgressSliderMouseDown() {
				MPlayerUIEventHandler.instance().onProgressSlideStart();
			}
			@Override public void onProgressSliderMouseUp() {
				MPlayerUIEventHandler.instance().onProgressSlideEnd();
			}
		});
		add(progressSlider, BorderLayout.CENTER);
		
	}
}
