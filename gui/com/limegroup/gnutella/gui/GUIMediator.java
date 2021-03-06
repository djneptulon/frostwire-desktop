/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.limegroup.gnutella.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;

import jd.plugins.FilePackage;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.i18n.I18nMarker;
import org.limewire.service.ErrorService;
import org.limewire.service.Switch;
import org.limewire.setting.IntSetting;
import org.limewire.setting.StringSetting;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.util.OSUtils;
import org.limewire.util.StringUtils;
import org.limewire.util.VersionUtils;

import com.frostwire.bittorrent.websearch.WebSearchResult;
import com.frostwire.gui.ChatMediator;
import com.frostwire.gui.HideExitDialog;
import com.frostwire.gui.bittorrent.BTDownloadMediator;
import com.frostwire.gui.library.LibraryMediator;
import com.frostwire.gui.player.MediaSource;
import com.frostwire.gui.player.InternetRadioAudioSource;
import com.frostwire.gui.player.MediaPlayer;
import com.frostwire.gui.tabs.Tab;
import com.limegroup.gnutella.MediaType;
import com.limegroup.gnutella.UpdateInformation;
import com.limegroup.gnutella.gui.actions.AbstractAction;
import com.limegroup.gnutella.gui.bugs.FatalBugManager;
import com.limegroup.gnutella.gui.notify.NotifyUserProxy;
import com.limegroup.gnutella.gui.options.OptionsMediator;
import com.limegroup.gnutella.gui.search.SearchMediator;
import com.limegroup.gnutella.gui.search.SoundcloudSearchResult;
import com.limegroup.gnutella.gui.shell.FrostAssociations;
import com.limegroup.gnutella.gui.shell.ShellAssociationManager;
import com.limegroup.gnutella.gui.themes.ThemeSettings;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.QuestionsHandler;
import com.limegroup.gnutella.settings.StartupSettings;
import com.limegroup.gnutella.util.FrostWireUtils;
import com.limegroup.gnutella.util.LaunchException;
import com.limegroup.gnutella.util.Launcher;

/**
 * This class acts as a central point of access for all gui components, a sort
 * of "hub" for the frontend. This should be the only common class that all
 * frontend components have access to, reducing the overall dependencies and
 * therefore increasing the modularity of the code.
 * 
 * <p>
 * Any functions or services that should be accessible to multiple classes
 * should be added to this class. These currently include such functions as
 * easily displaying standardly-formatted messages to the user, obtaining
 * locale-specific strings, and obtaining image resources, among others.
 * 
 * <p>
 * All of the methods in this class should be called from the event- dispatch
 * (Swing) thread.
 */
public final class GUIMediator {

    /**
     * Flag for whether or not a message has been displayed to the user --
     * useful in deciding whether or not to display other dialogues.
     */
    private static boolean _displayedMessage;

    /**
     * Message key for the disconnected message
     */
    private static final String DISCONNECTED_MESSAGE = I18nMarker
            .marktr("Your machine does not appear to have an active Internet connection or a firewall is blocking FrostWire from accessing the internet. FrostWire will automatically keep trying to connect you to the network unless you select \"Disconnect\" from the File menu.");

    /**
     * Singleton for easy access to the mediator.
     */
    private static GUIMediator _instance;

    private boolean _remoteDownloadsAllowed;

    public static enum Tabs {
        SEARCH(I18n.tr("&Search")), LIBRARY(I18n.tr("&Library")),
                CHAT(I18n.tr("C&hat"));

        private Action navAction;

        private String name;
        
        public boolean navigatedTo;

        private final PropertyChangeSupport propertyChangeSupport;

        private Tabs(String nameWithAmpers) {
            this.name = GUIUtils.stripAmpersand(nameWithAmpers);
            navAction = new NavigationAction(nameWithAmpers, I18n.tr("Display the {0} Screen", name));
            this.propertyChangeSupport = new PropertyChangeSupport(this);
        }

        private Tabs(StringSetting nameSetting) {
            this(nameSetting.getValue());
            nameSetting.addSettingListener(new SettingListener() {
                public void settingChanged(final SettingEvent evt) {
                    if (evt.getEventType() == SettingEvent.EventType.VALUE_CHANGED) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                setName(evt.getSetting().getValueAsString());
                            }
                        });
                    }
                }
            });
        }

        private Tabs(String nameWithAmpers, String url) {
            this(nameWithAmpers);
            this.navAction = new BrowseAction(nameWithAmpers, url);
        }

        void setName(String newName) {
            String oldName = name;
            this.name = GUIUtils.stripAmpersand(newName);
            navAction.putValue(Action.NAME, newName);
            navAction.putValue(Action.LONG_DESCRIPTION, I18n.tr("Display the {0} Screen", name));
            propertyChangeSupport.firePropertyChange("name", oldName, name);
        }

        void setEnabled(boolean enabled) {
            navAction.setEnabled(enabled);
        }

        public Action getNavigationAction() {
            return navAction;
        }

        public String getName() {
            return name;
        }

        private class NavigationAction extends AbstractAction {
            /**
             * 
             */
            private static final long serialVersionUID = -575503118703093157L;

            public NavigationAction(String name, String description) {
                super(name);
                putValue(Action.LONG_DESCRIPTION, description);
            }

            public void actionPerformed(ActionEvent e) {
                instance().setWindow(Tabs.this);
            }
        }

        /**
         * The tabs are also used on the View Menu, sometimes, you don't need to
         * add a tab, but instead point to a website when the user wants to view
         * the action shown on the menu, in this case, we use a BrowseAction.
         * 
         * A BrowseAction will be used with the Third Tab enum constructor,
         * which takes an url as the third parameter.
         */
        private class BrowseAction extends AbstractAction {
            /**
             * 
             */
            private static final long serialVersionUID = -6546640610645484649L;
            private String url;

            public BrowseAction(String name, String url) {
                super(name);
                this.url = url;
            }

            public void actionPerformed(ActionEvent e) {
                GUIMediator.openURL(FrostWireUtils.addLWInfoToUrl(this.url, new byte[0]));
            }
        }

        // To make sure we're always working with te same instances
        // so that the actions update all the components listening to them.
        private static Tabs[] OPTIONAL_TABS = null;

        public static Tabs[] getOptionalTabs() {
            if (OPTIONAL_TABS == null) {
                OPTIONAL_TABS = new Tabs[] { LIBRARY, CHAT };
            }

            return OPTIONAL_TABS;
        }

        public void addPropertyChangeListener(PropertyChangeListener listener) {
            propertyChangeSupport.addPropertyChangeListener(listener);
        }

    }

    /**
     * The main <tt>JFrame</tt> for the application.
     */
    private static JFrame FRAME;

    /**
     * The popup menu on the icon in the sytem tray.
     */
    private static PopupMenu TRAY_MENU;

    /**
     * <tt>List</tt> of <tt>RefreshListener</tt> classes to notify of UI refresh
     * events.
     */
    private static final List<RefreshListener> REFRESH_LIST = new ArrayList<RefreshListener>();

    /**
     * String to be displayed in title bar of LW client.
     */
    private static final String APP_TITLE = I18n.tr("FrostWire: Share Big Files");

    /**
     * Handle to the <tt>OptionsMediator</tt> class that is responsible for
     * displaying customizable options to the user.
     */
    private static OptionsMediator OPTIONS_MEDIATOR;

    /**
     * The shell association manager.
     */
    private static ShellAssociationManager ASSOCIATION_MANAGER;

    /**
     * Constant handle to the <tt>MainFrame</tt> instance that handles
     * constructing all of the primary gui components.
     */
    private MainFrame MAIN_FRAME;

    /**
     * Constant handle to the <tt>DownloadMediator</tt> class that is
     * responsible for displaying active downloads to the user.
     */
    private BTDownloadMediator BT_DOWNLOAD_MEDIATOR;

    /**
     * Constant handle to the <tt>LibraryMediator</tt> class that is responsible
     * for displaying files in the user's repository.
     */
    private LibraryMediator LIBRARY_MEDIATOR;

    /**
     * Constant handle to the <tt>ChatMediator</tt> class that is responsible
     * for displaying the user chat.
     */
    private ChatMediator CHAT_MEDIATOR;

    /**
     * Media Player Mediator
     */
    private MPlayerMediator MPLAYER_MEDIATOR;
    
    /**
     * Constant handle to the <tt>DownloadView</tt> class that is responsible
     * for displaying the status of the network and connectivity to the user.
     */
    private StatusLine STATUS_LINE;

	private long _lastConnectivityCheckTimestamp;

	/** How long until you check the internet connection status in milliseconds. */
	private long _internetConnectivityInterval = 5000;

	private boolean _wasInternetReachable;

    /**
     * Flag for whether or not the app has ever been made visible during this
     * session.
     */
    private static boolean _visibleOnce = false;

    /**
     * Flag for whether or not the app is allowed to become visible.
     */
    private static boolean _allowVisible = false;

    /**
     * Private constructor to ensure that this class cannot be constructed from
     * another class.
     */
    private GUIMediator() {
        MAIN_FRAME = new MainFrame(getAppFrame());
        OPTIONS_MEDIATOR = MAIN_FRAME.getOptionsMediator();
        
        _remoteDownloadsAllowed = true;
        
        
    }

    /**
     * Singleton accessor for this class.
     * 
     * @return the <tt>GUIMediator</tt> instance
     */
    public static synchronized GUIMediator instance() {
        if (_instance == null)
            _instance = new GUIMediator();
        return _instance;
    }

    /**
     * Accessor for whether or not the GUIMediator has been constructed yet.
     */
    public static boolean isConstructed() {
        return _instance != null;
    }

    /**
     * Notification that the the core has been initialized.
     */
    public void coreInitialized() {
        startTimer();
    }

    private final void startTimer() {
        RefreshTimer timer = new RefreshTimer();
        timer.startTimer();
    }

    /**
     * Returns a boolean specifying whether or not the wrapped <tt>JFrame</tt>
     * is visible or not.
     * 
     * @return <tt>true</tt> if the <tt>JFrame</tt> is visible, <tt>false</tt>
     *         otherwise
     */
    public static final boolean isAppVisible() {
        return getAppFrame().isShowing();
    }

    /**
     * Specifies whether or not the main application window should be visible or
     * not.
     * 
     * @param visible
     *            specifies whether or not the application should be made
     *            visible or not
     */
    public static final void setAppVisible(final boolean visible) {
        safeInvokeLater(new Runnable() {
            public void run() {
                try {
                    if (visible)
                        getAppFrame().toFront();
                    getAppFrame().setVisible(visible);
                } catch (NullPointerException npe) {
                    System.out.println("GUIMediator - NULL POINTER EXCEPTION HAPPENED");
                    // NPE being thrown on WinXP sometimes. First try
                    // reverting to the limewire theme. If NPE still
                    // thrown, tell user to change LimeWire's Windows
                    // compatibility mode to Win2k.
                    // null pointer found
                    // Update: no idea if the NPE also happens on vista, use the
                    // workaround
                    // just in case.
                    if (OSUtils.isNativeThemeWindows()) {
                        try {
                            if (ThemeSettings.isWindowsTheme()) {
                                // System.out.println("GUIMediator - setting frostwire theme for windows...");
                                //ThemeMediator.changeTheme(ThemeSettings.FROSTWIRE_THEME_FILE);
                                try {
                                    if (visible)
                                        getAppFrame().toFront();
                                    getAppFrame().setVisible(visible);
                                } catch (NullPointerException npe2) {
                                    GUIMediator
                                            .showError(I18n
                                                    .tr("FrostWire has encountered a problem during startup and cannot proceed. You may be able to fix this problem by changing FrostWire\'s Windows Compatibility. Right-click on the FrostWire icon on your Desktop and select \'Properties\' from the popup menu. Click the \'Compatibility\' tab at the top, then click the \'Run this program in compatibility mode for\' check box, and then select \'Windows 2000\' in the box below the check box. Then click the \'OK\' button at the bottom and restart FrostWire."));
                                    System.exit(0);
                                }
                            } else {
                                GUIMediator
                                        .showError(I18n
                                                .tr("FrostWire has encountered a problem during startup and cannot proceed. You may be able to fix this problem by changing FrostWire\'s Windows Compatibility. Right-click on the FrostWire icon on your Desktop and select \'Properties\' from the popup menu. Click the \'Compatibility\' tab at the top, then click the \'Run this program in compatibility mode for\' check box, and then select \'Windows 2000\' in the box below the check box. Then click the \'OK\' button at the bottom and restart FrostWire."));
                                System.exit(0);
                            }
                        } catch (Throwable t) {
                            if (visible)
                                FatalBugManager.handleFatalBug(npe);
                            else
                                ErrorService.error(npe);
                        }
                    } else {
                        if (visible)
                            FatalBugManager.handleFatalBug(npe);
                        else
                            ErrorService.error(npe);
                    }
                } catch (Throwable t) {
                    if (visible)
                        FatalBugManager.handleFatalBug(t);
                    else
                        ErrorService.error(t);
                }
                if (visible) {
                    SearchMediator.requestSearchFocus();
                    // forcibily revalidate the FRAME
                    // after making it visible.
                    // on Java 1.5, it does not validate correctly.
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            getAppFrame().getContentPane().invalidate();
                            getAppFrame().getContentPane().validate();
                        }
                    });
                }
                // If the app has already been made visible, don't display extra
                // dialogs. We could display the pro dialog here, but it causes
                // some odd issues when LimeWire is brought back up from the
                // tray
                if (visible && !_visibleOnce) {
                    // Show the startup dialogs in the swing thread.
                    showDialogsForFirstVisibility();
                    _visibleOnce = true;
                }
            }
        });
    }

    /**
     * Displays various dialog boxes that should only be shown the first time
     * the application is made visible.
     */
    private static final void showDialogsForFirstVisibility() {
        if (_displayedMessage)
            return;
        _displayedMessage = true;

        getAssociationManager().checkAndGrab(true);

        if (TipOfTheDayMessages.hasLocalizedMessages() && StartupSettings.SHOW_TOTD.getValue()) {
            // Construct it first...
            TipOfTheDayMediator.instance();

            ThreadExecutor.startThread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            TipOfTheDayMediator.instance().displayTipWindow();
                        }
                    });
                }
            }, "TOTD");
        }

        JDialog dialog = JavaVersionNotice.getUpgradeRecommendedDialog(VersionUtils.getJavaVersion());
        if (dialog != null) {
            dialog.setVisible(true);
        }
    }

    /**
     * Displays a dialog the first time a user performs a download. Returns true
     * iff the user selects 'Yes'; returns false otherwise.
     */
    /*
     * public static boolean showFirstDownloadDialog() { if (DialogOption.YES ==
     * showYesNoCancelMessage(I18n.tr(
     * "FrostWire is unable to find a license for this file. Download the file anyway?\n\nPlease note: FrostWire cannot monitor or control the content of the Gnutella network. Please respect your local copyright laws."
     * ), QuestionsHandler.SKIP_FIRST_DOWNLOAD_WARNING)) return true; return
     * false; }
     */
    /**
     * Closes any dialogues that are displayed at startup and sets the flag to
     * indicate that we've displayed a message.
     */
    public static void closeStartupDialogs() {
        if (SplashWindow.instance().isShowing())
            SplashWindow.instance().toBack();
        if (TipOfTheDayMediator.isConstructed())
            TipOfTheDayMediator.instance().hide();
    }

    /**
     * Returns a <tt>Dimension</tt> instance containing the dimensions of the
     * wrapped JFrame.
     * 
     * @return a <tt>Dimension</tt> instance containing the width and height of
     *         the wrapped JFrame
     */
    public static final Dimension getAppSize() {
        return getAppFrame().getSize();
    }

    /**
     * Returns a <tt>Point</tt> instance containing the x, y position of the
     * wrapped <ttJFrame</tt> on the screen.
     * 
     * @return a <tt>Point</tt> instance containting the x, y position of the
     *         wrapped JFrame
     */
    public static final Point getAppLocation() {
        return getAppFrame().getLocation();
    }

    /**
     * Returns the <tt>MainFrame</tt> instance. <tt>MainFrame</tt> maintains
     * handles to all of the major gui classes.
     * 
     * @return the <tt>MainFrame</tt> instance
     */
    public final MainFrame getMainFrame() {
        return MAIN_FRAME;
    }
    
    public final MPlayerMediator getMPlayerMediator() {
    	if (MPLAYER_MEDIATOR == null) {
    		MPLAYER_MEDIATOR = MPlayerMediator.instance();
    	}
    	return MPLAYER_MEDIATOR;
    }

    /**
     * Returns the main application <tt>JFrame</tt> instance.
     * 
     * @return the main application <tt>JFrame</tt> instance
     */
    public static final JFrame getAppFrame() {
        if (FRAME == null) {
            FRAME = new LimeJFrame();
            FRAME.setTitle(APP_TITLE);
        }
        return FRAME;
    }

    /**
     * Returns the popup menu on the icon in the system tray.
     * 
     * @return The tray popup menu
     */
    public static final PopupMenu getTrayMenu() {
        if (TRAY_MENU == null) {
            TRAY_MENU = new PopupMenu();
        }
        return TRAY_MENU;
    }

    /**
     * Returns the status line instance for other classes to access
     */
    public StatusLine getStatusLine() {
        if (STATUS_LINE == null) {
            STATUS_LINE = getMainFrame().getStatusLine();
        }
        return STATUS_LINE;
    }

    /**
     * Refreshes the various gui components that require refreshing.
     */
    public final void refreshGUI() {
        for (int i = 0; i < REFRESH_LIST.size(); i++) {
            try {
                REFRESH_LIST.get(i).refresh();
            } catch (Throwable t) {
                // Show the error for each RefreshListener individually
                // so that we continue refreshing the other items.
                ErrorService.error(t);
            }
        }

        // update the status panel
        int quality = getConnectionQuality();

        if (quality != StatusLine.STATUS_DISCONNECTED) {
            hideDisposableMessage(DISCONNECTED_MESSAGE);
        }

        updateConnectionUI(quality);
    }

    /**
     * Returns the connectiong quality.
     */
    public int getConnectionQuality() {
        if (isInternetReachable()) {
            return StatusLine.STATUS_TURBOCHARGED;
        } else {
            return StatusLine.STATUS_DISCONNECTED;
        }
    }

    /**
     * Sets the visibility state of the options window.
     * 
     * @param visible
     *            the visibility state to set the window to
     */
    public void setOptionsVisible(boolean visible) {
        if (OPTIONS_MEDIATOR == null)
            return;
        OPTIONS_MEDIATOR.setOptionsVisible(visible);
    }

    /**
     * Sets the visibility state of the options window, and sets the selection
     * to a option pane associated with a given key.
     * 
     * @param visible
     *            the visibility state to set the window to
     * @param key
     *            the unique identifying key of the panel to show
     */
    public void setOptionsVisible(boolean visible, final String key) {
        if (OPTIONS_MEDIATOR == null)
            return;
        OPTIONS_MEDIATOR.setOptionsVisible(visible, key);
    }

    /**
     * Returns whether or not the options window is visible
     * 
     * @return <tt>true</tt> if the options window is visible, <tt>false</tt>
     *         otherwise
     */
    public static boolean isOptionsVisible() {
        if (OPTIONS_MEDIATOR == null)
            return false;
        return OPTIONS_MEDIATOR.isOptionsVisible();
    }

    /**
     * Gets a handle to the options window main <tt>JComponent</tt> instance.
     * 
     * @return the options window main <tt>JComponent</tt>, or <tt>null</tt> if
     *         the options window has not yet been constructed (the window is
     *         guaranteed to be constructed if it is visible)
     */
    public static Component getMainOptionsComponent() {
        if (OPTIONS_MEDIATOR == null)
            return null;
        return OPTIONS_MEDIATOR.getMainOptionsComponent();
    }

    /**
     * @return the <tt>ShellAssociationManager</tt> instance.
     */
    public static ShellAssociationManager getAssociationManager() {
        if (ASSOCIATION_MANAGER == null) {
            ASSOCIATION_MANAGER = new ShellAssociationManager(FrostAssociations.getSupportedAssociations());
        }

        return ASSOCIATION_MANAGER;
    }

    /**
     * Sets the tab pane to display the given tab.
     * 
     * @param index
     *            the index of the tab to display
     */
    public void setWindow(GUIMediator.Tabs tab) {
        getMainFrame().setSelectedTab(tab);
        selectFinishedDownloadsOnLibraryFirstTime(tab);
    }

    /**
     * If the window to be shown is the Library tab, we automatically select "Finished Downloads"
     * so the users have a clue of what they can do with the Library, and so that they see their
     * finished downloads in case they came here the first time to see what they downloaded. 
     * @param tab
     */
    private void selectFinishedDownloadsOnLibraryFirstTime(GUIMediator.Tabs tab) {
        if (!tab.navigatedTo && tab.equals(GUIMediator.Tabs.LIBRARY)) {
            LibraryMediator.instance().getLibraryExplorer().selectFinishedDownloads();
            tab.navigatedTo = true;
        }
    }
    
    public GUIMediator.Tabs getSelectedTab() {
        return getMainFrame().getSelectedTab();
    }


    /**
     * Sets the connected/disconnected visual status of the client.
     * 
     * @param connected
     *            the connected/disconnected status of the client
     */
    private void updateConnectionUI(int quality) {
        getStatusLine().setConnectionQuality(quality);

        boolean connected = quality != StatusLine.STATUS_DISCONNECTED;
        if (!connected)
            this.setSearching(false);
    }

    /**
     * Returns the total number of currently active uploads.
     * 
     * @return the total number of currently active uploads
     */
    public int getCurrentUploads() {
        return getBTDownloadMediator().getActiveUploads();
    }

    /**
     * Returns the total number of downloads for this session.
     * 
     * @return the total number of downloads for this session
     */
    public final int getTotalDownloads() {
        return getBTDownloadMediator().getTotalDownloads();
    }

    public final int getCurrentDownloads() {
        return getBTDownloadMediator().getActiveDownloads();
    }
    
    public final void openTorrentSearchResult(WebSearchResult webSearchResult, boolean partialDownload) {
        openTorrentSearchResult(webSearchResult, partialDownload, null);
    }

    public final void openTorrentSearchResult(WebSearchResult webSearchResult, boolean partialDownload, ActionListener postPartialDownloadAction) {
        getBTDownloadMediator().openTorrentSearchResult(webSearchResult, partialDownload, postPartialDownloadAction);
        setWindow(GUIMediator.Tabs.SEARCH);
    }

    public final void openTorrentFile(File torrentFile, boolean partialSelection) {
        getBTDownloadMediator().openTorrentFile(torrentFile, partialSelection);
        setWindow(GUIMediator.Tabs.SEARCH);
    }

    public void openTorrentForSeed(File torrentFile, File saveDir) {
        getBTDownloadMediator().openTorrentFileForSeed(torrentFile, saveDir);
        setWindow(GUIMediator.Tabs.SEARCH);
    }

    public final void openTorrentURI(String uri, boolean partialDownload) {
        getBTDownloadMediator().openTorrentURI(uri, partialDownload, null);
        setWindow(GUIMediator.Tabs.SEARCH);
    }

    /**
     * Determines whether or not the PlaylistMediator is being used this
     * session.
     */
    public static boolean isPlaylistVisible() {
//        // If we are not constructed yet, then make our best guess as
//        // to visibility. It is actually VERY VERY important that this
//        // returns the same thing throughout the entire course of the program,
//        // otherwise exceptions can pop up.
//        if (!isConstructed())
//            return PlayerSettings.PLAYER_ENABLED.getValue();
//        else
//            return getPlayList() != null && PlayerSettings.PLAYER_ENABLED.getValue();
        return true;
    }

    /**
     * Runs the appropriate methods to start LimeWire up hidden.
     */
    public static void startupHidden() {
        // sends us to the system tray on windows, ignored otherwise.
        GUIMediator.showTrayIcon();
        // If on OSX, we must set the framestate appropriately.
        if (OSUtils.isMacOSX())
            GUIMediator.hideView();
    }

    /**
     * Notification that visibility is now allowed.
     */
    public static void allowVisibility() {
        if (!_allowVisible && OSUtils.isAnyMac())
            MacEventHandler.instance().enablePreferences();
        _allowVisible = true;
    }

    /**
     * Notification that loading is finished. Updates the status line and bumps
     * the AWT thread priority.
     */
    public void loadFinished() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Thread awt = Thread.currentThread();
                awt.setPriority(awt.getPriority() + 1);
                getStatusLine().loadFinished();
            }
        });
    }

    /**
     * Handles a 'reopen' event appropriately. Used primarily for allowing
     * LimeWire to be made visible after it was started from system startup on
     * OSX.
     */
    public static void handleReopen() {
        // Do not do anything
        // if visibility is not allowed yet, as initialization
        // is not yet finished.
        if (_allowVisible) {
            if (!_visibleOnce)
                restoreView(); // First make sure it's not minimized
            setAppVisible(true); // Then make it visible
            // Otherwise (if the above operations were reversed), a tiny
            // LimeWire icon would appear in the 'minimized' area of the dock
            // for a split second, and the Console would report strange errors
        }
    }

    /**
     * Hides the GUI by either sending it to the System Tray or minimizing the
     * window. Mimimize behavior occurs on platforms which do not support the
     * System Tray.
     * 
     * @see restoreView
     */
    public static void hideView() {
        getAppFrame().setState(Frame.ICONIFIED);

        if (OSUtils.supportsTray() && ResourceManager.instance().isTrayIconAvailable()) {
            GUIMediator.setAppVisible(false);
        }
    }

    /**
     * Makes the GUI visible by either restoring it from the System Tray or the
     * task bar.
     * 
     * @see hideView
     */
    public static void restoreView() {
        // Frame must be visible for setState to work. Make visible
        // before restoring.

        if (OSUtils.supportsTray() && ResourceManager.instance().isTrayIconAvailable()) {
            // below is a little hack to get around odd windowing
            // behavior with the system tray on windows. This enables
            // us to get LimeWire to the foreground after it's run from
            // the startup folder with all the nice little animations
            // that we want

            // cache whether or not to use our little hack, since setAppVisible
            // changes the value of _visibleOnce
            boolean doHack = false;
            if (!_visibleOnce)
                doHack = true;
            GUIMediator.setAppVisible(true);
            if (ApplicationSettings.DISPLAY_TRAY_ICON.getValue())
                GUIMediator.showTrayIcon();
            else
                GUIMediator.hideTrayIcon();
            if (doHack)
                restoreView();
        }

        getAppFrame().setState(Frame.NORMAL);
    }

    /**
     * Determines the appropriate shutdown behavior based on user settings. This
     * implementation decides between exiting the application immediately, or
     * exiting after all file transfers in progress are complete.
     */
    public static void close(boolean fromFrame) {

        boolean minimizeToTray = ApplicationSettings.MINIMIZE_TO_TRAY.getValue();

        if (!OSUtils.isMacOSX() && ApplicationSettings.SHOW_HIDE_EXIT_DIALOG.getValue()) {
            HideExitDialog dlg = new HideExitDialog(getAppFrame());
            dlg.setVisible(true);
            int result = dlg.getResult();
            if (result == HideExitDialog.NONE) {
                return;
            } else {
                minimizeToTray = result == HideExitDialog.HIDE ? true : false;
            }
        }

        if (minimizeToTray) {
            if (OSUtils.supportsTray()) {
                if (ResourceManager.instance().isTrayIconAvailable()) {
                    applyWindowSettings();
                    GUIMediator.showTrayIcon();
                    hideView();
                }
            } 
        } else if (OSUtils.isMacOSX() && fromFrame) {
            // If on OSX, don't close in response to clicking on the 'X'
            // as that's not normal behavior. This can only be done on Java14
            // though, because we need access to the
            // com.apple.eawt.ApplicationListener.handleReOpenApplication event
            // in order to restore the GUI.
            GUIMediator.setAppVisible(false);
        } else {
            shutdown();
        }
    }

    /**
     * Shutdown the program cleanly.
     */
    public static void shutdown() {
        Finalizer.shutdown();
    }

    public static void flagUpdate(String toExecute) {
        Finalizer.flagUpdate(toExecute);
    }

    /**
     * Shows the "About" menu with more information about the program.
     */
    public static final void showAboutWindow() {
        new AboutWindow().showDialog();
    }

    /**
     * Shows the user notification area. The user notification icon and tooltip
     * created by the NotifyUser object are not modified.
     */
    public static void showTrayIcon() {
        NotifyUserProxy.instance().showTrayIcon();
    }

    /**
     * Hides the user notification area.
     */
    public static void hideTrayIcon() {
        // Do not use hideNotify() here, since that will
        // create multiple tray icons.
        NotifyUserProxy.instance().hideTrayIcon();
    }

    /**
     * Sets the window height, width and location properties to remember the
     * next time the program is started.
     */
    public static void applyWindowSettings() {
        ApplicationSettings.RUN_ONCE.setValue(true);
        if (GUIMediator.isAppVisible()) {
            if ((GUIMediator.getAppFrame().getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
                ApplicationSettings.MAXIMIZE_WINDOW.setValue(true);
            } else {
                // set the screen size and location for the
                // next time the application is run.
                Dimension dim = GUIMediator.getAppSize();
                // only save reasonable sizes to get around a bug on
                // OS X that could make the window permanently
                // invisible
                if ((dim.height > 100) && (dim.width > 100)) {
                    Point loc = GUIMediator.getAppLocation();
                    ApplicationSettings.APP_WIDTH.setValue(dim.width);
                    ApplicationSettings.APP_HEIGHT.setValue(dim.height);
                    ApplicationSettings.WINDOW_X.setValue(loc.x);
                    ApplicationSettings.WINDOW_Y.setValue(loc.y);
                }
            }
        }
    }

    /**
     * 
     * @param tabs (tabs is actually a Singular word, it referes to the Tabs enum)
     * @return
     */
    public Tab getTab(Tabs tabs) {
        return MAIN_FRAME.getTab(tabs);
    }
    
    /**
     * Serves as a single point of access for any icons used in the program.
     * 
     * @param imageName
     *            the name of the icon to return without path information, as in
     *            "plug"
     * @return the <tt>ImageIcon</tt> object specified in the param string
     */
    public static final ImageIcon getThemeImage(final String name) {
        return ResourceManager.getThemeImage(name);
    }

    /**
     * Returns an ImageIcon for the specified resource.
     */
    public static final ImageIcon getImageFromResourcePath(final String loc) {
        return ResourceManager.getImageFromResourcePath(loc);
    }

    /**
     * Returns a new <tt>URL</tt> instance for the specified file name. The file
     * must be located in the org/limewire/gui/resources directory, or this will
     * return <tt>null</tt>.
     * 
     * @param FILE_NAME
     *            the name of the file to return a url for without path
     *            information, as in "about.html"
     * @return the <tt>URL</tt> instance for the specified file, or
     *         <tt>null</tt> if the <tt>URL</tt> could not be loaded
     */
    public static URL getURLResource(final String FILE_NAME) {
        return ResourceManager.getURLResource(FILE_NAME);
    }

    /**
     * Resets locale options.
     */
    public static void resetLocale() {
        ResourceManager.resetLocaleOptions();
        GUIUtils.resetLocale();
    }

    /**
     * Return ResourceBundle for use with specific xml schema
     * 
     * @param schemaname
     *            the name of schema (not the URI but name returned by
     *            LimeXMLSchema.getDisplayString)
     * @return a ResourceBundle matching the passed in param
     */
    public static final ResourceBundle getXMLResourceBundle(final String schemaname) {
        return ResourceManager.getXMLResourceBundle(schemaname);
    }

    /**
     * Acts as a proxy for the <tt>MessageService</tt> class. Displays a
     * locale-specific question message to the user with an input field
     * <p>
     * 
     * The <tt>messageKey</tt> parameter must be the key for a locale- specific
     * message <tt>String</tt> and not a hard-coded value.
     * 
     * @param message
     *            the locale-specific message to display
     * @param initialValue
     *            the initial input value
     * @return a String containing the user input
     */
    public static final String showInputMessage(final String message, String initialValue) {
        return MessageService.instance().showInputMessage(message, initialValue);
    }

    /**
     * Acts as a proxy for the <tt>MessageService</tt> class. Displays a
     * locale-specific message to the user in the form of a yes or no question.
     * <p>
     * 
     * The <tt>messageKey</tt> parameter must be the key for a locale- specific
     * message <tt>String</tt> and not a hard-coded value.
     * 
     * @param message
     *            the locale-specific message to display
     * @return an integer indicating a yes or a no response from the user
     */
    public static final DialogOption showYesNoMessage(final String message, final DialogOption defaultOption) {
        return MessageService.instance().showYesNoMessage(message, defaultOption);
    }

    /**
     * Acts as a proxy for the <tt>MessageService</tt> class. Displays a
     * locale-specific message to the user in the form of a yes or no question.
     * <p>
     * 
     * The <tt>messageKey</tt> parameter must be the key for a locale- specific
     * message <tt>String</tt> and not a hard-coded value.
     * 
     * @param message
     *            the locale-specific message to display
     * @param defaultValue
     *            the IntSetting to store/retrieve the default value
     * @return an integer indicating a yes or a no response from the user
     */
    public static final DialogOption showYesNoMessage(final String message, final IntSetting defaultValue, final DialogOption defaultOption) {
        return MessageService.instance().showYesNoMessage(message, defaultValue, defaultOption);
    }

    public static final DialogOption showYesNoTitledMessage(final String message, final String title, final DialogOption defaultOption) {
        return MessageService.instance().showYesNoMessage(message, title, defaultOption);
    }

    /**
     * Acts as a proxy for the <tt>MessageService</tt> class. Displays a
     * locale-specific message to the user. Below a non-selectable list is
     * shown. This is in the form of a yes or no or cancel question.
     * <p>
     * 
     * The <tt>messageKey</tt> parameter must be the key for a locale- specific
     * message <tt>String</tt> and not a hard-coded value.
     * 
     * @param message
     *            the locale-specific message to display
     * @param listModel
     *            the array of object to be displayed in the list
     * @param messageType
     *            either {@link JOptionPane#YES_NO_OPTION},
     *            {@link JOptionPane#YES_NO_CANCEL_OPTION} or
     *            {@link JOptionPane#OK_CANCEL_OPTION}.
     * @param listRenderer
     *            optional list cell rendere, can be <code>null</code>
     * 
     * @return an integer indicating a yes or a no or cancel response from the
     *         user, see
     *         {@link JOptionPane#showConfirmDialog(Component, Object, String, int)}
     */
    public static final int showConfirmListMessage(final String message, final Object[] listModel, int messageType, final ListCellRenderer listRenderer) {
        return MessageService.instance().showConfirmListMessage(message, listModel, messageType, listRenderer);
    }

    /**
     * Displays a locale-specific message to the user in the form of a
     * yes/no/{other} question.
     * <p>
     * 
     * The <tt>messageKey</tt> parameter must be the key for a locale- specific
     * message <tt>String</tt> and not a hard-coded value.
     * 
     * @param message
     *            the locale-specific message to display
     * @param defaultValue
     *            the IntSetting to store/retrieve the default value
     * @param otherOptions
     *            the name of the other option
     * @return an integer indicating a yes or a no response from the user
     */
    public static final DialogOption showYesNoOtherMessage(final String message, final IntSetting defaultValue, String otherOptions) {
        return MessageService.instance().showYesNoOtherMessage(message, defaultValue, otherOptions);
    }

    /**
     * Acts as a proxy for the <tt>MessageService</tt> class. Displays a
     * locale-specific message to the user in the form of a yes or no or cancel
     * question.
     * <p>
     * 
     * The <tt>messageKey</tt> parameter must be the key for a locale- specific
     * message <tt>String</tt> and not a hard-coded value.
     * 
     * @param message
     *            the locale-specific message to display
     * @return an integer indicating a yes or a no response from the user
     */
    public static final DialogOption showYesNoCancelMessage(final String message) {
        return MessageService.instance().showYesNoCancelMessage(message);
    }

    /**
     * Acts as a proxy for the <tt>MessageService</tt> class. Displays a
     * locale-specific message to the user in the form of a yes or no or cancel
     * question.
     * <p>
     * 
     * The <tt>messageKey</tt> parameter must be the key for a locale- specific
     * message <tt>String</tt> and not a hard-coded value.
     * 
     * @param message
     *            for the locale-specific message to display
     * @param defaultValue
     *            the IntSetting to store/retrieve the default value
     * @return an integer indicating a yes or a no response from the user
     */
    public static final DialogOption showYesNoCancelMessage(final String message, final IntSetting defaultValue) {
        return MessageService.instance().showYesNoCancelMessage(message, defaultValue);
    }

    /**
     * Acts as a proxy for the <tt>MessageService</tt> class. Displays a
     * locale-specific message to the user.
     * <p>
     * 
     * The <tt>messageKey</tt> parameter must be the key for a locale- specific
     * message <tt>String</tt> and not a hard-coded value.
     * 
     * @param messageKey
     *            the key for the locale-specific message to display
     */
    public static final void showMessage(final String messageKey) {
        MessageService.instance().showMessage(messageKey);
    }

    /**
     * Acts as a proxy for the <tt>MessageService</tt> class. Displays a
     * locale-specific message to the user.
     * <p>
     * 
     * The <tt>messageKey</tt> parameter must be the key for a locale- specific
     * message <tt>String</tt> and not a hard-coded value.
     * 
     * @param message
     *            the locale-specific message to display
     * @param ignore
     *            the BooleanSetting that stores/retrieves whether or not to
     *            display this message.
     */
    public static final void showMessage(final String message, final Switch ignore) {
        MessageService.instance().showMessage(message, ignore);
    }

    /**
     * Acts as a proxy for the <tt>MessageService</tt> class. Displays a
     * locale-specific disposable message to the user.
     * <p>
     * 
     * The <tt>messageKey</tt> parameter must be the key for a locale- specific
     * message <tt>String</tt> and not a hard-coded value.
     * 
     * @param messageKey
     *            the key for the locale-specific message to display
     * @param ignore
     *            the BooleanSetting that stores/retrieves whether or not to
     *            display this message.
     * @param msgType
     *            The <tt>JOptionPane</tt> message type. @see
     *            javax.swing.JOptionPane.
     * @param msgTitle
     *            The title of the message window.
     */
    public static final void showDisposableMessage(final String messageKey, final String message, final Switch ignore, int msgType) {
        MessageService.instance().showDisposableMessage(messageKey, message, ignore, msgType);
    }

    /**
     * Acts as a proxy for the <tt>MessageService</tt> class. Hides a
     * locale-specific disposable message.
     * <p>
     * 
     * The <tt>messageKey</tt> parameter must be the key for a locale- specific
     * message <tt>String</tt> and not a hard-coded value.
     * 
     * @param messageKey
     *            the key for the locale-specific message to display
     */
    public static final void hideDisposableMessage(final String messageKey) {
        MessageService.instance().hideDisposableMessage(messageKey);
    }

    /**
     * Acts as a proxy for the <tt>MessageService</tt> class. Displays a
     * confirmation message to the user.
     * <p>
     * 
     * The <tt>messageKey</tt> parameter must be the key for a locale- specific
     * message <tt>String</tt> and not a hard-coded value.
     * 
     * @param message
     *            the locale-specific message to display
     */
    public static final void showConfirmMessage(final String message) {
        MessageService.instance().showConfirmMessage(message);
    }

    /**
     * Acts as a proxy for the <tt>MessageService</tt> class. Displays a
     * confirmation message to the user.
     * <p>
     * 
     * The <tt>messageKey</tt> parameter must be the key for a locale- specific
     * message <tt>String</tt> and not a hard-coded value.
     * 
     * @param message
     *            the locale-specific message to display
     * @param ignore
     *            the BooleanSetting for that stores/retrieves whether or not to
     *            display this message.
     */
    public static final void showConfirmMessage(final String message, final Switch ignore) {
        MessageService.instance().showConfirmMessage(message, ignore);
    }

    /**
     * Acts as a proxy for the <tt>MessageService</tt> class. Displays a
     * locale-specific message to the user.
     * <p>
     * 
     * The <tt>messageKey</tt> parameter must be the key for a locale- specific
     * message <tt>String</tt> and not a hard-coded value.
     * 
     * @param message
     *            the locale-specific message to display.
     */
    public static final void showError(final String message) {
        closeStartupDialogs();
        MessageService.instance().showError(message);
    }

    /**
     * Acts as a proxy for the <tt>MessageService</tt> class. Displays a
     * locale-specific message to the user.
     * <p>
     * 
     * The <tt>messageKey</tt> parameter must be the key for a locale- specific
     * message <tt>String</tt> and not a hard-coded value.
     * 
     * @param message
     *            the key for the locale-specific message to display.
     * @param ignore
     *            the BooleanSetting for that stores/retrieves whether or not to
     *            display this message.
     */
    public static final void showError(final String message, final Switch ignore) {
        closeStartupDialogs();
        MessageService.instance().showError(message, ignore);
    }

    /**
     * Acts as a proxy for the <tt>MessageService</tt> class. Displays a
     * locale-specific warning message to the user.
     * <p>
     * 
     * The <tt>messageKey</tt> parameter must be the key for a locale- specific
     * message <tt>String</tt> and not a hard-coded value.
     * 
     * @param message
     *            the locale-specific message to display.
     * @param ignore
     *            the BooleanSetting for that stores/retrieves whether or not to
     *            display this message.
     */
    public static final void showWarning(final String message, final Switch ignore) {

        closeStartupDialogs();
        MessageService.instance().showWarning(message, ignore);
    }

    /**
     * Acts as a proxy for the <tt>MessageService</tt> class. Displays a
     * locale-specific warning message to the user.
     * <p>
     * 
     * The <tt>messageKey</tt> parameter must be the key for a locale- specific
     * message <tt>String</tt> and not a hard-coded value.
     * 
     * @param message
     *            the locale-specific message to display.
     */
    public static final void showWarning(final String message) {
        closeStartupDialogs();
        MessageService.instance().showWarning(message);
    }

    /**
     * Acts as a proxy for the Launcher class so that other classes only need to
     * know about this mediator class.
     * 
     * <p>
     * Opens the specified url in a browser.
     * 
     * @param url
     *            the url to open
     * @return an int indicating the success of the browser launch
     */
    public static final int openURL(String url) {
        try {
            return Launcher.openURL(url);
        } catch (IOException ioe) {
            GUIMediator.showError(I18n.tr("FrostWire could not locate your web browser to display the following webpage: {0}.", url));
            return -1;
        }
    }

    /**
     * Acts as a proxy for the Launcher class so that other classes only need to
     * know about this mediator class.
     * 
     * <p>
     * Launches the file specified in its associated application.
     * 
     * @param file
     *            a <tt>File</tt> instance denoting the abstract pathname of the
     *            file to launch
     * @throws IOException
     *             if the file cannot be launched do to an IO problem
     */
    public static final void launchFile(File file) {
        try {
            Launcher.launchFile(file);
        } catch (SecurityException se) {
            showError(I18n.tr("FrostWire will not launch the specified file for security reasons."));
        } catch (LaunchException e) {
            GUIMediator
                    .showError(I18n.tr("FrostWire could not launch the specified file.\n\nExecuted command: {0}.", StringUtils.explode(e.getCommand(), " ")));
        } catch (IOException e) {
            showError(I18n.tr("FrostWire could not launch the specified file."));
        }
    }

    /**
     * Acts as a proxy for the Launcher class so that other classes only need to
     * know about this mediator class.
     * 
     * <p>
     * Opens <tt>file</tt> in a platform specific file manager.
     * 
     * @param file
     *            a <tt>File</tt> instance denoting the abstract pathname of the
     *            file to launch
     * @throws IOException
     *             if the file cannot be launched do to an IO problem
     */
    public static final void launchExplorer(File file) {
        try {
            Launcher.launchExplorer(file);
        } catch (SecurityException e) {
            showError(I18n.tr("FrostWire will not launch the specified file for security reasons."));
        } catch (LaunchException e) {
            GUIMediator
                    .showError(I18n.tr("FrostWire could not launch the specified file.\n\nExecuted command: {0}.", StringUtils.explode(e.getCommand(), " ")));
        } catch (IOException e) {
            showError(I18n.tr("FrostWire could not launch the specified file."));
        }
    }

    /**
     * Returns a <tt>Component</tt> standardly sized for horizontal separators.
     * 
     * @return the constant <tt>Component</tt> used as a standard horizontal
     *         separator
     */
    public static final Component getHorizontalSeparator() {
        return Box.createRigidArea(new Dimension(6, 0));
    }

    /**
     * Returns a <tt>Component</tt> standardly sized for vertical separators.
     * 
     * @return the constant <tt>Component</tt> used as a standard vertical
     *         separator
     */
    public static final Component getVerticalSeparator() {
        return Box.createRigidArea(new Dimension(0, 6));
    }

    /**
     * Connects the user from the network.
     */
    public void connect() {
        //GuiCoreMediator.getConnectionServices().connect();
    }

    /**
     * Disconnects the user to the network.
     */
    public void disconnect() {
        //GuiCoreMediator.getConnectionServices().disconnect();
    }

    /**
     * Notifies the user that LimeWire is disconnected
     */
    public static void disconnected() {
        showDisposableMessage(
                DISCONNECTED_MESSAGE,
                I18n.tr("Your machine does not appear to have an active Internet connection or a firewall is blocking FrostWire from accessing the internet. FrostWire will automatically keep trying to connect you to the network unless you select \"Disconnect\" from the File menu."),
                QuestionsHandler.NO_INTERNET_RETRYING, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Modifies the text displayed to the user in the splash screen to provide
     * application loading information.
     * 
     * @param text
     *            the text to display
     */
    public static void setSplashScreenString(String text) {
        if (!_allowVisible)
            SplashWindow.instance().setStatusText(text);
        else if (isConstructed())
            instance().getStatusLine().setStatusText(text);
    }

    /**
     * Returns the point for the placing the specified component on the center
     * of the screen.
     * 
     * @param comp
     *            the <tt>Component</tt> to use for getting the relative center
     *            point
     * @return the <tt>Point</tt> for centering the specified <tt>Component</tt>
     *         on the screen
     */
    public static Point getScreenCenterPoint(Component comp) {
        final Dimension COMPONENT_DIMENSION = comp.getSize();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int appWidth = Math.min(screenSize.width, COMPONENT_DIMENSION.width);
        // compare against a little bit less than the screen size,
        // as the screen size includes the taskbar
        int appHeight = Math.min(screenSize.height - 40, COMPONENT_DIMENSION.height);
        return new Point((screenSize.width - appWidth) / 2, (screenSize.height - appHeight) / 2);
    }

    /**
     * Adds the <tt>FinalizeListener</tt> class to the list of classes that
     * should be notified of finalize events.
     * 
     * @param fin
     *            the <tt>FinalizeListener</tt> class that should be notified
     */
    public static void addFinalizeListener(FinalizeListener fin) {
        Finalizer.addFinalizeListener(fin);
    }

    /**
     * Sets the searching or not searching status of the application.
     * 
     * @param searching
     *            the searching status of the application
     */
    public void setSearching(boolean searching) {
        getMainFrame().setSearching(searching);
    }

    /**
     * Adds the specified <tt>RefreshListener</tt> instance to the list of
     * listeners to be notified when a UI refresh event occurs.
     * 
     * @param the
     *            new <tt>RefreshListener</tt> to add
     */
    public static void addRefreshListener(RefreshListener listener) {
        if (!REFRESH_LIST.contains(listener))
            REFRESH_LIST.add(listener);
    }

    /**
     * Removes the specified <tt>RefreshListener</tt> instance from the list of
     * listeners to be notified when a UI refresh event occurs.
     * 
     * @param the
     *            <tt>RefreshListener</tt> to remove
     */
    public static void removeRefreshListener(RefreshListener listener) {
        REFRESH_LIST.remove(listener);
    }

    /**
     * Returns the <tt>Locale</tt> instance currently in use.
     * 
     * @return the <tt>Locale</tt> instance currently in use
     */
    public static Locale getLocale() {
        return ResourceManager.getLocale();
    }

    /**
     * Returns true if the current locale is English.
     */
    public static boolean isEnglishLocale() {
        return LanguageUtils.isEnglishLocale(getLocale());
    }

    /**
     * Launches the specified audio song in the player.
     * 
     * @param song
     *            - song to play now
     * 
     */
    public void launchAudio(MediaSource song) {

        if (MediaPlayer.instance().getCurrentMedia() != null)
            try {
            	MediaPlayer.instance().stop();
                // it needs to pause for a bit, otherwise it'll play the same song.
                // must be a sync bug somewhere, but this fixes it
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }

        //MediaPlayer.instance().loadSong(song);
        boolean playNextSong = !song.getClass().equals(InternetRadioAudioSource.class);
        if ( song.getFile() != null && MediaType.getVideoMediaType().matches(song.getFile().getAbsolutePath()) ) {
        	    playNextSong = false;
        }
		
        MediaPlayer.instance().asyncLoadMedia(song, true, playNextSong);
    }

    /**
     * Attempts to stop a song if playing on the frost player.
     * 
     */
    public boolean attemptStopAudio() {
    	MediaPlayer mediaPlayer = MediaPlayer.instance();
        mediaPlayer.stop();
        return true;
    }

    /**
     * Makes the update message show up in the status panel
     */
    public void showUpdateNotification(final UpdateInformation info) {
        safeInvokeAndWait(new Runnable() {
            public void run() {
                getStatusLine().showUpdatePanel(true, info);
            }
        });
    }

    /**
     * Trigger a search based on a string.
     * 
     * @param query
     *            the query <tt>String</tt>
     * @return the GUID of the query sent to the network. Used mainly for
     *         testing
     */
    public byte[] triggerSearch(String query) {
        getMainFrame().setSelectedTab(GUIMediator.Tabs.SEARCH);
        return SearchMediator.triggerSearch(query);
    }

    /**
     * Notification that the button state has changed.
     */
    public void buttonViewChanged() {
        IconManager.instance().wipeButtonIconCache();
        updateButtonView(getAppFrame());
    }

    /**
     * Notification that the smileys state has been changed.
     */
    public void smileysChanged(boolean newstatus) {
        ChatMediator.instance().changesmileys(newstatus);
        updateButtonView(getAppFrame());
    }

    private void updateButtonView(Component c) {
        if (c instanceof IconButton) {
            ((IconButton) c).updateUI();
        }
        Component[] children = null;
        if (c instanceof Container) {
            children = ((Container) c).getComponents();
        }
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                updateButtonView(children[i]);
            }
        }
    }

    /**
     * safely run code synchronously in the event dispatching thread.
     */
    public static void safeInvokeAndWait(Runnable runnable) {
        if (EventQueue.isDispatchThread())
            runnable.run();
        else {
            try {
                SwingUtilities.invokeAndWait(runnable);
            } catch (InvocationTargetException ite) {
                Throwable t = ite.getTargetException();
                if (t instanceof Error)
                    throw (Error) t;
                else if (t instanceof RuntimeException)
                    throw (RuntimeException) t;
                else
                    ErrorService.error(t);
            } catch (InterruptedException ignored) {
            }
        }
    }

    /**
     * InvokesLater if not already in the dispatch thread.
     */
    public static void safeInvokeLater(Runnable runnable) {
        if (EventQueue.isDispatchThread())
            runnable.run();
        else
            SwingUtilities.invokeLater(runnable);
    }

    /** Tells CHAT_MEDIATOR to try to start the IRC Chat */
    public void tryToStartAndAddChat() {
        getChatMediator().tryToStartAndAddChat();
    }

    /** Changes the nick on the Chat */
    public void setIRCNick(String newNick) {
        getChatMediator().nick(newNick);
    }

    /**
     * Sets the cursor on limewire's frame.
     * 
     * @param cursor
     *            the cursor that should be shown on the frame and all its child
     *            components that don't have their own cursor set
     */
    public void setFrameCursor(Cursor cursor) {
        getAppFrame().setCursor(cursor);
    }

    public static void openURL(final String link, final long delay) {
        if (delay > 0) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(delay);
                    } catch (Throwable e) {
                        // ignore
                    }
                    openURL(link);
                }
            }).start();
        } else {
            openURL(link);
        }
    }

    public BTDownloadMediator getBTDownloadMediator() {
        if (BT_DOWNLOAD_MEDIATOR == null) {
            BT_DOWNLOAD_MEDIATOR = getMainFrame().getBTDownloadMediator();
        }
        return BT_DOWNLOAD_MEDIATOR;
    }

    public LibraryMediator getLibraryMediator() {
        if (LIBRARY_MEDIATOR == null) {
            LIBRARY_MEDIATOR = getMainFrame().getLibraryMediator();
        }
        return LIBRARY_MEDIATOR;
    }

    private ChatMediator getChatMediator() {
        if (CHAT_MEDIATOR == null) {
            CHAT_MEDIATOR = getMainFrame().getChatMediator();
        }
        return CHAT_MEDIATOR;
    }

    public static void setClipboardContent(String str) {
        try {
            StringSelection data = new StringSelection(str);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(data, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    
    private boolean isInternetReachable() {
    	
    	long now = System.currentTimeMillis();
    	
    	if (now - _lastConnectivityCheckTimestamp < _internetConnectivityInterval) {
    		return _wasInternetReachable;
    	}
    	
    	_lastConnectivityCheckTimestamp = now;
    	
    	
    	
    	try {
	    	Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
	    	
	    	if (interfaces == null) {
	    		_wasInternetReachable = false;
	    		return false;
	    	}

	    	while (interfaces.hasMoreElements()) {
	    	  NetworkInterface iface = interfaces.nextElement();
	    	  //System.out.println(iface);
	    	  if (iface.isUp() && !iface.isLoopback()) {
	    		  _wasInternetReachable = true;
	    	    return true;
	    	  }
	    	}
    	} catch (Exception e) {
    		_wasInternetReachable = false;
    		return false;
    	}
    	
    	_wasInternetReachable = false;    	
    	return false;
    }


    public boolean isRemoteDownloadsAllowed() {
        return _remoteDownloadsAllowed;
    }

    public void setRemoteDownloadsAllowed(boolean remoteDownloadsAllowed) {
        _remoteDownloadsAllowed = remoteDownloadsAllowed;
    }

	public void openTorrentSearchResult(WebSearchResult item,
			String relativePath) {
        getBTDownloadMediator().openTorrentURI(item.getTorrentURI(), item.getDetailsUrl(), relativePath, item.getHash(), null);
        setWindow(GUIMediator.Tabs.SEARCH);
	}
	
	public void openYouTubeVideoUrl(String videoUrl) {
	    getBTDownloadMediator().openYouTubeVideoUrl(videoUrl);
        setWindow(GUIMediator.Tabs.SEARCH);
	}
	
	public void openSoundcloudTrackUrl(String trackUrl, String title) {
        getBTDownloadMediator().openSoundcloudTrackUrl(trackUrl, title, null);
        setWindow(GUIMediator.Tabs.SEARCH);
    }
	
	public void openSoundcloudTrackUrl(String trackUrl, String title, SoundcloudSearchResult sr) {
        getBTDownloadMediator().openSoundcloudTrackUrl(trackUrl, title, sr);
        setWindow(GUIMediator.Tabs.SEARCH);
    }

    public void openYouTubeItem(FilePackage filePackage) {
        getBTDownloadMediator().openYouTubeItem(filePackage);
        setWindow(GUIMediator.Tabs.SEARCH);
    }
}
