package com.limegroup.gnutella.gui.fw6ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;

import com.frostwire.gui.components.GoogleSearchField;
import com.limegroup.gnutella.MediaType;
import com.limegroup.gnutella.gui.actions.FileMenuActions;
import com.limegroup.gnutella.gui.search.SearchInformation;
import com.limegroup.gnutella.gui.search.SearchMediator;

public class ApplicationHeader extends JPanel {
	
	private static final long serialVersionUID = 8237124807777940537L;

	private final GoogleSearchField SEARCH_FIELD = new GoogleSearchField();
	private final ActionListener SEARCH_LISTENER = new ApplicationHeader.SearchListener();
	
	public ApplicationHeader() {
		
		initializeUI();
	}
	
	private void initializeUI() {
		setLayout(new BorderLayout());
	}
	
	private class SearchListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String query = SEARCH_FIELD.getText();
            
            //start a download from the search box by entering a URL.
            if (FileMenuActions.openMagnetOrTorrent(query)) {
                SEARCH_FIELD.setText("");
                SEARCH_FIELD.hidePopup();
                return;
            }
            
            final SearchInformation info = SearchInformation.createTitledKeywordSearch(query, null, MediaType.getTorrentMediaType(), query);

            // If the search worked, store & clear it.
            if (SearchMediator.triggerSearch(info) != null) {
                if (info.isKeywordSearch()) {
                    
                        SEARCH_FIELD.addToDictionary();

                    // Clear the existing search.
                    SEARCH_FIELD.setText("");
                    SEARCH_FIELD.hidePopup();
                }
            }
        }
    }
}
