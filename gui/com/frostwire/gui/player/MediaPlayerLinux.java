package com.frostwire.gui.player;

import java.io.File;

import org.limewire.util.OSUtils;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;

public class MediaPlayerLinux extends MediaPlayer {

	private static final String MPLAYER_DEFAULT_LINUX_PATH = "/usr/bin/mplayer";

	@Override
	protected String getPlayerPath() {
        File f = new File(MPLAYER_DEFAULT_LINUX_PATH);
        if (!f.exists()) {
            
            GUIMediator.safeInvokeLater(new Runnable() {
                @Override
                public void run() {
                    
                    String instructions = I18n
                            .tr("<br><br>To Install <b>mplayer</b> in Ubuntu open a terminal window and type \"<b>sudo apt-get install mplayer</b>\".<br><br>If you have installed mplayer already at a custom location, <b>make sure to have a symlink pointing to your mplayer executable</b> at <b><font color=\"blue\">"
                                    + MPLAYER_DEFAULT_LINUX_PATH + "</font></b>");
                    
                    if (!OSUtils.isUbuntu()) {
                        instructions = "";
                    }
                    
                    GUIMediator.showError(I18n.tr("<html><b>FrostWire requires Mplayer to play your media</b> but I could not find it in your computer.<br><br>If you want to use FrostWire as a media player <b>Please install mplayer and restart FrostWire.</b>")
                            + I18n.tr(instructions));
                            
                }
            });
        }
        return MPLAYER_DEFAULT_LINUX_PATH;
    }
}
