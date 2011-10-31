package com.frostwire.gui.library;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.MouseInputListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.limewire.util.OSUtils;

import com.frostwire.alexandria.Playlist;
import com.frostwire.gui.bittorrent.CreateTorrentDialog;
import com.frostwire.gui.player.AudioPlayer;
import com.frostwire.gui.player.AudioSource;
import com.limegroup.gnutella.MediaType;
import com.limegroup.gnutella.gui.ButtonRow;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.iTunesMediator;
import com.limegroup.gnutella.gui.actions.LimeAction;
import com.limegroup.gnutella.gui.actions.SearchAction;
import com.limegroup.gnutella.gui.tables.LimeJTable;
import com.limegroup.gnutella.gui.themes.SkinMenu;
import com.limegroup.gnutella.gui.themes.SkinMenuItem;
import com.limegroup.gnutella.gui.themes.SkinPopupMenu;
import com.limegroup.gnutella.gui.themes.ThemeMediator;
import com.limegroup.gnutella.gui.util.GUILauncher;
import com.limegroup.gnutella.gui.util.GUILauncher.LaunchableProvider;
import com.limegroup.gnutella.util.QueryUtils;

/**
 * This class wraps the JTable that displays files in the library,
 * controlling access to the table and the various table properties.
 * It is the Mediator to the Table part of the Library display.
 */
final class LibraryInternetRadioTableMediator extends AbstractLibraryTableMediator<LibraryInternetRadioTableModel, LibraryInternetRadioTableDataLine, InternetRadioStation> {
    
    public static Action LAUNCH_ACTION;
    public static Action DELETE_ACTION;
    
    /**
     * instance, for singelton access
     */
    private static LibraryInternetRadioTableMediator INSTANCE;

    public static LibraryInternetRadioTableMediator instance() {
        if (INSTANCE == null) {
            INSTANCE = new LibraryInternetRadioTableMediator();
        }
        return INSTANCE;
    }

    /**
     * Build some extra listeners
     */
    protected void buildListeners() {
        super.buildListeners();
        
        LAUNCH_ACTION = new LaunchAction();
        DELETE_ACTION = new RemoveFromPlaylistAction();
    }

    /**
     * Set up the constants
     */
    protected void setupConstants() {
        MAIN_PANEL = null;
        DATA_MODEL = new LibraryInternetRadioTableModel();
        TABLE = new LimeJTable(DATA_MODEL);
        Action[] aa = new Action[] { LAUNCH_ACTION, DELETE_ACTION };
        BUTTON_ROW = new ButtonRow(aa, ButtonRow.X_AXIS, ButtonRow.NO_GLUE);
    }

    // inherit doc comment
    protected JPopupMenu createPopupMenu() {
        if (TABLE.getSelectionModel().isSelectionEmpty())
            return null;

        JPopupMenu menu = new SkinPopupMenu();

//        //menu.add(new SkinMenuItem(LAUNCH_ACTION));
//        if (hasExploreAction()) {
//            menu.add(new SkinMenuItem(OPEN_IN_FOLDER_ACTION));
//        }
//
//        menu.add(new SkinMenuItem(CREATE_TORRENT_ACTION));
//        menu.add(createAddToPlaylistSubMenu());
//        menu.add(new SkinMenuItem(SEND_TO_FRIEND_ACTION));
//
//        menu.add(new SkinMenuItem(SEND_TO_ITUNES_ACTION));
//
//        
//        menu.addSeparator();
//        menu.add(new SkinMenuItem(DELETE_ACTION));
//
//        int[] rows = TABLE.getSelectedRows();
//        boolean dirSelected = false;
//        boolean fileSelected = false;
//
//        for (int i = 0; i < rows.length; i++) {
//            File f = DATA_MODEL.get(rows[i]).getFile();
//            if (f.isDirectory()) {
//                dirSelected = true;
//                //				if (IncompleteFileManager.isTorrentFolder(f))
//                //					torrentSelected = true;
//            } else
//                fileSelected = true;
//
//            if (dirSelected && fileSelected)
//                break;
//        }
//        if (dirSelected) {
//            if (GUIMediator.isPlaylistVisible())
//                ENQUEUE_ACTION.setEnabled(false);
//            DELETE_ACTION.setEnabled(true);
//        } else {
//            if (GUIMediator.isPlaylistVisible() && AudioPlayer.isPlayableFile(DATA_MODEL.getFile(rows[0])))
//                ENQUEUE_ACTION.setEnabled(true);
//            DELETE_ACTION.setEnabled(true);
//        }
//
//        menu.addSeparator();
//        menu.add(new SkinMenuItem(importToPlaylistAction));
//        menu.add(new SkinMenuItem(exportPlaylistAction));
//        menu.add(new SkinMenuItem(cleanupPlaylistAction));
//        menu.add(new SkinMenuItem(refreshID3TagsAction));
//
//        menu.addSeparator();
//        LibraryPlaylistsTableDataLine line = DATA_MODEL.get(rows[0]);
//        menu.add(createSearchSubMenu(line));

        return menu;
    }
    
    @Override
    protected void addListeners() {
    	super.addListeners();

    	TABLE.addKeyListener(new KeyAdapter() {
        	@Override
        	public void keyReleased(KeyEvent e) {
        		if (LibraryUtils.isRefreshKeyEvent(e)) {
        			LibraryMediator.instance().getLibraryPlaylists().refreshSelection();
        		}        		
        	}
		});
    }

    private JMenu createSearchSubMenu(LibraryPlaylistsTableDataLine dl) {
        JMenu menu = new SkinMenu(I18n.tr("Search"));

        if (dl != null) {
            File f = dl.getFile();
            String keywords = QueryUtils.createQueryString(f.getName());
            if (keywords.length() > 2)
                menu.add(new SkinMenuItem(new SearchAction(keywords)));
        }

        if (menu.getItemCount() == 0)
            menu.setEnabled(false);

        return menu;
    }

    /**
     * Upgrade getScrolledTablePane to public access.
     */
    public JComponent getScrolledTablePane() {
        return super.getScrolledTablePane();
    }

    /* Don't display anything for this.  The LibraryMediator will do it. */
    protected void updateSplashScreen() {
    }

    /**
     * Note: This is set up for this to work.
     * Polling is not needed though, because updates
     * already generate update events.
     */
    private LibraryInternetRadioTableMediator() {
        super("LIBRARY_INTERNET_RADIO_TABLE");
        setMediaType(MediaType.getAudioMediaType());
        ThemeMediator.addThemeObserver(this);
    }

    /**
     * Sets up drag & drop for the table.
     */
    protected void setupDragAndDrop() {
        TABLE.setDragEnabled(true);
        TABLE.setDropMode(DropMode.INSERT_ROWS);
       // TABLE.setTransferHandler(new LibraryPlaylistsTableTransferHandler(this));
    }

    /**
     * there is no actual component that holds all of this table.
     * The LibraryMediator is real the holder.
     */
    public JComponent getComponent() {
        return null;
    }

    @Override
    protected void setDefaultRenderers() {
        super.setDefaultRenderers();
        TABLE.setDefaultRenderer(PlaylistItemProperty.class, new PlaylistItemPropertyRenderer());
        TABLE.setDefaultRenderer(PlaylistItemStar.class, new PlaylistItemStarRenderer());
    }

    /**
     * Sets the default editors.
     */
    protected void setDefaultEditors() {
        TableColumnModel model = TABLE.getColumnModel();
        TableColumn tc = model.getColumn(LibraryPlaylistsTableDataLine.STARRED_IDX);
        tc.setCellEditor(new PlaylistItemStarEditor());

        TABLE.addMouseMotionListener(new MouseMotionAdapter() {
            int currentCellColumn = -1;
            int currentCellRow = -1;

            @Override
            public void mouseMoved(MouseEvent e) {
                Point hit = e.getPoint();
                int hitColumn = TABLE.columnAtPoint(hit);
                int hitRow = TABLE.rowAtPoint(hit);
                if (currentCellRow != hitRow || currentCellColumn != hitColumn) {
                    if (TABLE.getCellRenderer(hitRow, hitColumn) instanceof PlaylistItemStarRenderer) {
                        TABLE.editCellAt(hitRow, hitColumn);
                    }
                    currentCellColumn = hitColumn;
                    currentCellRow = hitRow;
                }
            }
        });
    }

    /**
     * Cancels all editing of fields in the tree and table.
     */
    void cancelEditing() {
        if (TABLE.isEditing()) {
            TableCellEditor editor = TABLE.getCellEditor();
            editor.cancelCellEditing();
        }
    }

    /**
     * Adds the mouse listeners to the wrapped <tt>JTable</tt>.
     *
     * @param listener the <tt>MouseInputListener</tt> that handles mouse events
     *                 for the library
     */
    void addMouseInputListener(final MouseInputListener listener) {
        TABLE.addMouseListener(listener);
        TABLE.addMouseMotionListener(listener);
    }

    /**
     * Updates the Table based on the selection of the given table.
     * Perform lookups to remove any store files from the shared folder
     * view and to only display store files in the store view
     */
    void updateTableItems(Playlist playlist) {
//        if (playlist == null) {
//            return;
//        }
//
//        currentPlaylist = playlist;
//        List<PlaylistItem> items = currentPlaylist.getItems();
//
//        clearTable();
//        for (int i = 0; i < items.size(); i++) {
//            addUnsorted(items.get(i));
//        }
//        forceResort();
    }

    /**
     * Returns the <tt>File</tt> stored at the specified row in the list.
     *
     * @param row the row of the desired <tt>File</tt> instance in the
     *            list
     *
     * @return a <tt>File</tt> instance associated with the specified row
     *         in the table
     */
    File getFile(int row) {
        return DATA_MODEL.getFile(row);
    }

    /**
     * Accessor for the table that this class wraps.
     *
     * @return The <tt>JTable</tt> instance used by the library.
     */
    JTable getTable() {
        return TABLE;
    }

    ButtonRow getButtonRow() {
        return BUTTON_ROW;
    }

    LibraryInternetRadioTableDataLine[] getSelectedLibraryLines() {
        int[] selected = TABLE.getSelectedRows();
        LibraryInternetRadioTableDataLine[] lines = new LibraryInternetRadioTableDataLine[selected.length];
        for (int i = 0; i < selected.length; i++)
            lines[i] = DATA_MODEL.get(selected[i]);
        return lines;
    }

    /**
     * Accessor for the <tt>ListSelectionModel</tt> for the wrapped
     * <tt>JTable</tt> instance.
     */
    ListSelectionModel getSelectionModel() {
        return TABLE.getSelectionModel();
    }

    /**
     * Programatically starts a rename of the selected item.
     */
    void startRename() {
        int row = TABLE.getSelectedRow();
        if (row == -1)
            return;
        //int viewIdx = TABLE.convertColumnIndexToView(LibraryPlaylistsTableDataLine.NAME_IDX);
        //TABLE.editCellAt(row, viewIdx, LibraryTableCellEditor.EVENT);
    }

    /**
     * Shows the license window.
     */
    void showLicenseWindow() {
        //        LibraryTableDataLine ldl = DATA_MODEL.get(TABLE.getSelectedRow());
        //        if(ldl == null)
        //            return;
        //        FileDesc fd = ldl.getFileDesc();
        //        License license = fd.getLicense();
        //        URN urn = fd.getSHA1Urn();
        //        LimeXMLDocument doc = ldl.getXMLDocument();
        //        LicenseWindow window = LicenseWindow.create(license, urn, doc, this);
        //        GUIUtils.centerOnScreen(window);
        //        window.setVisible(true);
    }

    /**
     * Delete selected items from a playlist (not from disk)
     */
    public void removeSelection() {

        LibraryInternetRadioTableDataLine[] lines = getSelectedLibraryLines();

//        if (currentPlaylist != null && currentPlaylist.getId() == LibraryDatabase.STARRED_PLAYLIST_ID) {
//            for (LibraryPlaylistsTableDataLine line : lines) {
//                PlaylistItem playlistItem = line.getInitializeObject();
//                playlistItem.setStarred(false);
//                playlistItem.save();
//            }
//
//            LibraryMediator.instance().getLibraryFiles().refreshSelection();
//            
//        } else {
//
//            for (LibraryPlaylistsTableDataLine line : lines) {
//                PlaylistItem playlistItem = line.getInitializeObject();
//                playlistItem.delete();
//            }
//
//            LibraryMediator.instance().getLibraryPlaylists().reselectPlaylist();
//
//            clearSelection();
//        }
    }

    public void handleActionKey() {
        playSong();
    }

    private void playSong() {
//        LibraryInternetRadioTableDataLine line = DATA_MODEL.get(TABLE.getSelectedRow());
//        if (line == null) {
//            return;
//        }
//
//        AudioSource audioSource = new AudioSource(line.getPlayListItem());
//        if (AudioPlayer.isPlayableFile(audioSource)) {
//            AudioPlayer.instance().asyncLoadSong(audioSource, true, true, currentPlaylist, getFileView());
//        }
    }

    /**
     * Launches the associated applications for each selected file
     * in the library if it can.
     */
    void launch() {
        int[] rows = TABLE.getSelectedRows();
        if (rows.length == 0) {
            return;
        }

        File selectedFile = DATA_MODEL.getFile(rows[0]);

        if (OSUtils.isWindows()) {
            if (selectedFile.isDirectory()) {
                GUIMediator.launchExplorer(selectedFile);
                return;
            } else if (!AudioPlayer.isPlayableFile(selectedFile)) {
                GUIMediator.launchFile(selectedFile);
                return;
            }

        }

        LaunchableProvider[] providers = new LaunchableProvider[rows.length];
        for (int i = 0; i < rows.length; i++) {
            providers[i] = new FileProvider(DATA_MODEL.getFile(rows[i]));
        }
        GUILauncher.launch(providers);
    }

    /**
     * Handles the selection rows in the library window,
     * enabling or disabling buttons and chat menu items depending on
     * the values in the selected rows.
     * 
     * @param row the index of the first row that is selected
     */
    public void handleSelection(int row) {
        int[] sel = TABLE.getSelectedRows();
        if (sel.length == 0) {
            handleNoSelection();
            return;
        }

        File selectedFile = getFile(sel[0]);

//        //  always turn on Launch, Delete, Magnet Lookup, Bitzi Lookup
//        LAUNCH_ACTION.setEnabled(true);
//        DELETE_ACTION.setEnabled(true);
//        SEND_TO_ITUNES_ACTION.setEnabled(true);
//
//        if (selectedFile != null && !selectedFile.getName().endsWith(".torrent")) {
//            CREATE_TORRENT_ACTION.setEnabled(sel.length == 1);
//        }
//
//        if (selectedFile != null) {
//            SEND_TO_FRIEND_ACTION.setEnabled(sel.length == 1);
//        }
//
//        if (sel.length == 1 && selectedFile.isFile() && selectedFile.getParentFile() != null) {
//            OPEN_IN_FOLDER_ACTION.setEnabled(true);
//        } else {
//            OPEN_IN_FOLDER_ACTION.setEnabled(false);
//        }
//
//        //  turn on Enqueue if play list is visible and a selected item is playable
//        if (GUIMediator.isPlaylistVisible()) {
//            boolean found = false;
//            for (int i = 0; i < sel.length; i++)
//                if (AudioPlayer.isPlayableFile(DATA_MODEL.getFile(sel[i]))) {
//                    found = true;
//                    break;
//                }
//            ENQUEUE_ACTION.setEnabled(found);
//        } else
//            ENQUEUE_ACTION.setEnabled(false);
//
//        if (sel.length == 1) {
//            LibraryMediator.instance().getLibraryCoverArt().setFile(getSelectedLibraryLines()[0].getFile());
//        }
    }

    /**
     * Handles the deselection of all rows in the library table,
     * disabling all necessary buttons and menu items.
     */
    public void handleNoSelection() {
//        LAUNCH_ACTION.setEnabled(false);
//        OPEN_IN_FOLDER_ACTION.setEnabled(false);
//        SEND_TO_FRIEND_ACTION.setEnabled(false);
//        ENQUEUE_ACTION.setEnabled(false);
//        CREATE_TORRENT_ACTION.setEnabled(false);
//        DELETE_ACTION.setEnabled(false);
//        SEND_TO_ITUNES_ACTION.setEnabled(false);
    }

    /**
     * Refreshes the enabledness of the Enqueue button based
     * on the player enabling state. 
     */
    public void setPlayerEnabled(boolean value) {
        handleSelection(TABLE.getSelectedRow());
    }

    public boolean setPlaylistItemSelected(InternetRadioStation item) {
        int i = DATA_MODEL.getRow(item);

        if (i != -1) {
            TABLE.setSelectedRow(i);
            TABLE.ensureSelectionVisible();
            return true;
        }
        return false;
    }

    private boolean hasExploreAction() {
        return OSUtils.isWindows() || OSUtils.isMacOSX();
    }

    ///////////////////////////////////////////////////////
    //  ACTIONS
    ///////////////////////////////////////////////////////

    private final class LaunchAction extends AbstractAction {

        /**
         * 
         */
        private static final long serialVersionUID = 949208465372392591L;

        public LaunchAction() {
            putValue(Action.NAME, I18n.tr("Launch"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Launch Selected Files"));
            putValue(LimeAction.ICON_NAME, "LIBRARY_LAUNCH");
        }

        public void actionPerformed(ActionEvent ae) {
            launch();
        }
    }

    private final class OpenInFolderAction extends AbstractAction {

        /**
         * 
         */
        private static final long serialVersionUID = 1693310684299300459L;

        public OpenInFolderAction() {
            putValue(Action.NAME, I18n.tr("Open in Folder"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Open Folder Containing a Selected File"));
            putValue(LimeAction.ICON_NAME, "LIBRARY_LAUNCH");
        }

        public void actionPerformed(ActionEvent ae) {
            int[] sel = TABLE.getSelectedRows();
            if (sel.length == 0) {
                return;
            }

            File selectedFile = getFile(sel[0]);
            if (selectedFile.isFile() && selectedFile.getParentFile() != null) {
                GUIMediator.launchExplorer(selectedFile.getParentFile());
            }
        }
    }

    private final class EnqueueAction extends AbstractAction {

        /**
         * 
         */
        private static final long serialVersionUID = 9153310119076594713L;

        public EnqueueAction() {
            putValue(Action.NAME, I18n.tr("Enqueue"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Add Selected Files to the Playlist"));
            putValue(LimeAction.ICON_NAME, "LIBRARY_TO_PLAYLIST");
        }

        public void actionPerformed(ActionEvent ae) {
            //get the selected file. If there are more than 1 we add all
            int[] rows = TABLE.getSelectedRows();
            List<File> files = new ArrayList<File>();
            for (int i = 0; i < rows.length; i++) {
                int index = rows[i]; // current index to add
                File file = DATA_MODEL.getFile(index);
                if (GUIMediator.isPlaylistVisible() && AudioPlayer.isPlayableFile(file))
                    files.add(file);
            }
            //LibraryMediator.instance().addFilesToPlayList(files);
        }
    }

    private final class CreateTorrentAction extends AbstractAction {

        private static final long serialVersionUID = 1898917632888388860L;

        public CreateTorrentAction() {
            super(I18n.tr("Create New Torrent"));
            putValue(Action.LONG_DESCRIPTION, I18n.tr("Create a new .torrent file"));
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            File selectedFile = DATA_MODEL.getFile(TABLE.getSelectedRow());

            //can't create torrents out of empty folders.
            if (selectedFile.isDirectory() && selectedFile.listFiles().length == 0) {
                JOptionPane.showMessageDialog(null, I18n.tr("The folder you selected is empty."), I18n.tr("Invalid Folder"), JOptionPane.ERROR_MESSAGE);
                return;
            }

            //can't create torrents if the folder/file can't be read
            if (!selectedFile.canRead()) {
                JOptionPane.showMessageDialog(null, I18n.tr("Error: You can't read on that file/folder."), I18n.tr("Error"), JOptionPane.ERROR_MESSAGE);
                return;
            }

            CreateTorrentDialog dlg = new CreateTorrentDialog(GUIMediator.getAppFrame());
            dlg.setChosenContent(selectedFile);
            dlg.setVisible(true);

        }
    }
    
    private class SendAudioFilesToiTunes extends AbstractAction {

		private static final long serialVersionUID = 4726989286129406765L;

		public SendAudioFilesToiTunes() {
			putValue(Action.NAME, I18n.tr("Send to iTunes"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Send audio files to iTunes"));
    	}
    	
    	@Override
		public void actionPerformed(ActionEvent e) {
            int[] rows = TABLE.getSelectedRows();
            for (int i = 0; i < rows.length; i++) {
                int index = rows[i]; // current index to add
                File file = DATA_MODEL.getFile(index);
                
				iTunesMediator.instance().scanForSongs(file);                
            }
		}
    }

    private final class RemoveFromPlaylistAction extends AbstractAction {

        /**
         * 
         */
        private static final long serialVersionUID = -8704093935791256631L;

        public RemoveFromPlaylistAction() {
            putValue(Action.NAME, I18n.tr("Delete from playlist"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Delete Selected Files from this playlist"));
            putValue(LimeAction.ICON_NAME, "LIBRARY_DELETE");
        }

        public void actionPerformed(ActionEvent ae) {
            REMOVE_LISTENER.actionPerformed(ae);
        }
    }

    private static class FileProvider implements LaunchableProvider {

        private final File _file;

        public FileProvider(File file) {
            _file = file;
        }

        public File getFile() {
            return _file;
        }
    }

    private final class ImportToPlaylistAction extends AbstractAction {

        private static final long serialVersionUID = -9099898749358019734L;

        public ImportToPlaylistAction() {
            putValue(Action.NAME, I18n.tr("Import .m3u to Playlist"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Import a .m3u file into the selected playlist"));
            putValue(LimeAction.ICON_NAME, "PLAYLIST_IMPORT_TO");
        }

        public void actionPerformed(ActionEvent e) {
            //LibraryMediator.instance().getLibraryPlaylists().importM3U(currentPlaylist);
        }
    }

    private final class ExportPlaylistAction extends AbstractAction {

        private static final long serialVersionUID = 6149822357662730490L;

        public ExportPlaylistAction() {
            putValue(Action.NAME, I18n.tr("Export Playlist to .m3u"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Export this playlist into a .m3u file"));
            putValue(LimeAction.ICON_NAME, "PLAYLIST_IMPORT_NEW");
        }

        public void actionPerformed(ActionEvent e) {
            //LibraryMediator.instance().getLibraryPlaylists().exportM3U(currentPlaylist);
        }
    }

    private final class CleanupPlaylistAction extends AbstractAction {

        private static final long serialVersionUID = 8400749433148927596L;

        public CleanupPlaylistAction() {
            putValue(Action.NAME, I18n.tr("Cleanup playlist"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Remove the deleted items"));
            putValue(LimeAction.ICON_NAME, "PLAYLIST_CLEANUP");
        }

        public void actionPerformed(ActionEvent e) {
            //LibraryUtils.cleanup(currentPlaylist);
            LibraryMediator.instance().getLibraryPlaylists().refreshSelection();
        }
    }
    
    private final class RefreshID3TagsAction extends AbstractAction {

        private static final long serialVersionUID = 758150680592618044L;
        
        public RefreshID3TagsAction() {
            putValue(Action.NAME, I18n.tr("Refresh Audio Properties"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Refresh the audio properties based on ID3 tags"));
            putValue(LimeAction.ICON_NAME, "PLAYLIST_REFRESHID3TAGS");
        }

        public void actionPerformed(ActionEvent e) {
            //LibraryPlaylistsTableDataLine[] lines = getSelectedLibraryLines();
//            List<PlaylistItem> items = new ArrayList<PlaylistItem>(lines.length);
//            for (LibraryPlaylistsTableDataLine line : lines) {
//                items.add(line.getInitializeObject());
//            }
            //LibraryUtils.refreshID3Tags(currentPlaylist, items);
        }
    }

    @Override
    public List<AudioSource> getFileView() {
        int size = DATA_MODEL.getRowCount();
        List<AudioSource> result = new ArrayList<AudioSource>(size);
        for (int i = 0; i < size; i++) {
            try {
                //result.add(new AudioSource(DATA_MODEL.get(i).getPlayListItem()));
            } catch (Exception e) {
                return Collections.emptyList();
            }
        }
        return result;
    }
    
    @Override
    protected void sortAndMaintainSelection(int columnToSort) {
        super.sortAndMaintainSelection(columnToSort);
        resetAudioPlayerFileView();
    }

    private void resetAudioPlayerFileView() {
        Playlist playlist = AudioPlayer.instance().getCurrentPlaylist();
//        if (playlist != null && playlist.equals(currentPlaylist)) {
//            if (AudioPlayer.instance().getPlaylistFilesView() != null) {
//                AudioPlayer.instance().setPlaylistFilesView(getFileView());
//            }
//        }
    }
}