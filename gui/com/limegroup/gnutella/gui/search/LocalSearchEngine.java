package com.limegroup.gnutella.gui.search;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderCallBackInterface;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderFactory;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.limewire.util.FileUtils;

import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTSuggestPiece;
import com.frostwire.JsonEngine;
import com.frostwire.bittorrent.websearch.WebSearchResult;
import com.frostwire.gui.bittorrent.TorrentUtil;
import com.frostwire.gui.filters.SearchFilter;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.gui.search.db.SmartSearchDB;
import com.limegroup.gnutella.gui.search.db.TorrentDBPojo;
import com.limegroup.gnutella.gui.search.db.TorrentFileDBPojo;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.util.FrostWireUtils;
import com.limegroup.gnutella.util.FrostWireUtils.IndexedMapFunction;

public class LocalSearchEngine {

	private static final int DEEP_SEARCH_DELAY = 1000;
	private static final int MAXIMUM_TORRENTS_TO_SCAN = 25;
	private static final int DEEP_SEARCH_ROUNDS = 3;

	private static LocalSearchEngine INSTANCE;

	private static final int LOCAL_SEARCH_RESULTS_LIMIT = 128; 

	/**
	 * We'll keep here every info hash we've already processed during the
	 * session
	 */
	private HashSet<String> KNOWN_INFO_HASHES = new HashSet<String>();
	private SmartSearchDB DB;
	private JsonEngine JSON_ENGINE;

	public LocalSearchEngine() {
		DB = new SmartSearchDB(
				SearchSettings.SMART_SEARCH_DATABASE_FOLDER.getValue());
		JSON_ENGINE = new JsonEngine();
	}

	public static LocalSearchEngine instance() {
		if (INSTANCE == null) {
			INSTANCE = new LocalSearchEngine();
		}

		return INSTANCE;
	}

	public final static HashSet<String> IGNORABLE_KEYWORDS;

	static {
		IGNORABLE_KEYWORDS = new HashSet<String>();
		IGNORABLE_KEYWORDS.addAll(Arrays.asList("me", "you", "he", "she",
				"they", "them", "we", "us", "my", "your", "yours", "his",
				"hers", "theirs", "ours", "the", "of", "in", "on", "out", "to",
				"at", "as", "and", "by", "not", "is", "are", "am", "was",
				"were", "will", "be", "for"));
	}

	/**
	 * Avoid possible SQL errors due to escaping. Cleans all double spaces and
	 * trims.
	 * 
	 * @param str
	 * @return
	 */
	private final static String stringSanitize(String str) {
		str = str.replace("\\", "").replace("%", "").replace("_", "")
				.replace(";", "").replace("'", "''");

		while (str.indexOf("  ") != -1) {
			str = str.replace("  ", " ");
		}
		return str;
	}

	/**
	 * @param builder
	 * @param lastIndex
	 * @param uniqueQueryTokensArray
	 * @param columns
	 * @return
	 */
	private static String getWhereClause(final String[] uniqueQueryTokensArray,
			String... columns) {
		final StringBuilder builder = new StringBuilder();
		final int lastIndex = columns.length - 1;

		FrostWireUtils.map(Arrays.asList(columns),
				new IndexedMapFunction<String>() {
					// Create a where clause that considers all the given
					// columns for each of the words in the query.
					public void map(int i, String column) {

						int size = uniqueQueryTokensArray.length;

						for (int j = 0; j < size; j++) {
							String token = uniqueQueryTokensArray[j];
							builder.append(column
									+ " LIKE '%"
									+ token
									+ "%' "
									+ ((i <= lastIndex || j < size) ? " OR "
											: ""));
						}
					}
				});

		String str = builder.toString();
		int index = str.lastIndexOf(" OR");

		return index > 0 ? str.substring(0, index) : str;
	}

	public final static String getOrWhereClause(String query, String... columns) {
		String[] queryTokens = stringSanitize(query).split(" ");

		// Let's make sure we don't send repeated tokens to SQL Engine
		Set<String> uniqueQueryTokens = new TreeSet<String>();
		for (int i = 0; i < queryTokens.length; i++) {
			String token = queryTokens[i];

			if (token.length() == 1 || uniqueQueryTokens.contains(token)
					|| IGNORABLE_KEYWORDS.contains(token.toLowerCase())) {
				continue;
			}

			uniqueQueryTokens.add(token);
		}

		String[] uniqueQueryTokensArray = uniqueQueryTokens
				.toArray(new String[] {});

		return getWhereClause(uniqueQueryTokensArray, columns);
	}

	/**
	 * Perform a simple Database Search, immediate results should be available
	 * if there are matches.
	 */
	public List<SmartSearchResult> search(String query) {
		query = query.toLowerCase();
		String orWhereClause = getOrWhereClause(query, "fileName");

		String sql = "SELECT Torrents.json, Files.json, torrentName, fileName FROM Torrents JOIN Files ON Torrents.torrentId = Files.torrentId WHERE ("
				+ orWhereClause + ") ORDER BY seeds DESC LIMIT " + LOCAL_SEARCH_RESULTS_LIMIT;

		long start = System.currentTimeMillis();
		List<List<Object>> rows = DB.query(sql);
		long delta = System.currentTimeMillis() - start;
		System.out.print("Found " + rows.size() + " local results in " + delta
				+ "ms. ");

		//no query should ever take this long.
		if (delta > 1000) {
			System.out.println("\nWarning: Results took too long, there's something wrong with the database, you might want to delete your 'search_db' folder inside the FrostWire preferences folder.");
		}

		List<SmartSearchResult> results = new ArrayList<SmartSearchResult>();
		Map<Integer, SearchEngine> searchEngines = SearchEngine
				.getSearchEngineMap();

		// GUBENE
		String torrentJSON = null;
		for (List<Object> row : rows) {
			try {
				torrentJSON = (String) row.get(0);
				torrentJSON = torrentJSON.replace("\'", "'");

				String fileJSON = (String) row.get(1);
				fileJSON = fileJSON.replace("\'", "'");

				String torrentName = (String) row.get(2);
				String fileName = (String) row.get(3);

				if (new MatchLogic(query, torrentName, fileName).matchResult()) {

					TorrentDBPojo torrentPojo = JSON_ENGINE.toObject(
							torrentJSON, TorrentDBPojo.class);

					if (!searchEngines.get(torrentPojo.searchEngineID)
							.isEnabled()) {
						continue;
					}

					TorrentFileDBPojo torrentFilePojo = JSON_ENGINE.toObject(
							fileJSON, TorrentFileDBPojo.class);

					results.add(new SmartSearchResult(torrentPojo,
							torrentFilePojo));
					KNOWN_INFO_HASHES.add(torrentPojo.hash);
				}
			} catch (Exception e) {
				// keep going dude
				System.out.println("Issues with POJO deserialization -> " + torrentJSON);
				e.printStackTrace();
				System.out.println("=====================");
			}
		}
		
		System.out.println("Ended up with "+ results.size() +" results");

		return results;
	}

	public List<DeepSearchResult> deepSearch(byte[] guid, String query,
			SearchInformation info) {
		ResultPanel rp = null;

		// Let's wait for enough search results from different search engines.
		sleep();

		// Wait for enough results or die if the ResultPanel has been closed.
		int tries = DEEP_SEARCH_ROUNDS;

		for (int i = tries; i > 0; i--) {
			if ((rp = SearchMediator.getResultPanelForGUID(new GUID(guid))) == null) {
				return null;
			}

			scanAvailableResults(guid, query, info, rp);

			sleep();
		}

		// did they close rp? nothing left to do.
		if (rp == null) {
			return null;
		}

		return null;
	}

	public void sleep() {
		try {
			Thread.sleep(DEEP_SEARCH_DELAY);
		} catch (InterruptedException e1) {
		}
	}

	public void scanAvailableResults(byte[] guid, String query,
			SearchInformation info, ResultPanel rp) {
		
		int foundTorrents = 0;
		
		for (int i = 0; i < rp.getSize() && foundTorrents < MAXIMUM_TORRENTS_TO_SCAN; i++) {
			TableLine line = rp.getLine(i);
			
			if (line.getInitializeObject() instanceof SearchEngineSearchResult) {
				foundTorrents++;
				
				WebSearchResult webSearchResult = line.getSearchResult()
						.getWebSearchResult();
	
				if (!KNOWN_INFO_HASHES.contains(webSearchResult.getHash())) {
					KNOWN_INFO_HASHES.add(webSearchResult.getHash());
					SearchEngine searchEngine = line.getSearchEngine();
					scanDotTorrent(webSearchResult, guid, query, searchEngine, info);
				}
			}
		}
	}

	/**
	 * Will decide wether or not to fetch the .torrent from the DHT.
	 * 
	 * If it has to download it, it will use a
	 * LocalSearchTorrentDownloaderListener to start scanning, if the torrent
	 * has already been fetched, it will perform an immediate search.
	 * 
	 * @param webSearchResult
	 * @param searchEngine
	 * @param info
	 */
	private void scanDotTorrent(WebSearchResult webSearchResult, byte[] guid,
			String query, SearchEngine searchEngine, SearchInformation info) {
		if (!torrentHasBeenIndexed(webSearchResult.getHash())) {
			// download the torrent
			String saveDir = SearchSettings.SMART_SEARCH_DATABASE_FOLDER
					.getValue().getAbsolutePath();

			ResultPanel rp = SearchMediator
					.getResultPanelForGUID(new GUID(guid));
			if (rp != null) {
				rp.incrementSearchCount();
			}
			TorrentDownloaderFactory.create(
					new LocalSearchTorrentDownloaderListener(guid, query,
							webSearchResult, searchEngine, info),
					TorrentUtil.getMagnet(webSearchResult.getHash()), null,
					saveDir).start();
		}
	}

	private boolean torrentHasBeenIndexed(String infoHash) {
		List<List<Object>> rows = DB
				.query("SELECT * FROM Torrents WHERE infoHash LIKE '"
						+ infoHash + "'");
		return rows.size() > 0;
	}

	private void indexTorrent(WebSearchResult searchResult,
			TOTorrent theTorrent, SearchEngine searchEngine) {
		TorrentDBPojo torrentPojo = new TorrentDBPojo();
		torrentPojo.creationTime = searchResult.getCreationTime();
		torrentPojo.fileName = stringSanitize(searchResult.getFileName());
		torrentPojo.hash = searchResult.getHash();
		torrentPojo.searchEngineID = searchEngine.getId();
		torrentPojo.seeds = searchResult.getSeeds();
		torrentPojo.size = searchResult.getSize();
		torrentPojo.torrentDetailsURL = searchResult.getTorrentDetailsURL();
		torrentPojo.torrentURI = searchResult.getTorrentURI();
		torrentPojo.vendor = searchResult.getVendor();

		String torrentJSON = JSON_ENGINE.toJson(torrentPojo);
		torrentJSON = torrentJSON.replace("'", "\'");

		int torrentID = DB
				.insert("INSERT INTO Torrents (infoHash, timestamp, torrentName, seeds, json) VALUES ('"
						+ torrentPojo.hash
						+ "', "
						+ ""
						+ System.currentTimeMillis()
						+ ", '"
						+ torrentPojo.fileName.toLowerCase()
						+ "', "
						+ torrentPojo.seeds + ", '"
						+ torrentJSON + "')");

		TOTorrentFile[] files = theTorrent.getFiles();

		for (TOTorrentFile f : files) {
			TorrentFileDBPojo tfPojo = new TorrentFileDBPojo();
			tfPojo.relativePath = stringSanitize(f.getRelativePath());
			tfPojo.size = f.getLength();

			String fileJSON = JSON_ENGINE.toJson(tfPojo);
			fileJSON = fileJSON.replace("'", "\'");

			DB.insert("INSERT INTO Files (torrentId, fileName, json) VALUES ("
					+ torrentID + ", '" + tfPojo.relativePath.toLowerCase()
					+ "', '" + fileJSON + "')");
			// System.out.println("INSERT INTO Files (torrentId, fileName, json) VALUES ("+torrentID+", '"+tfPojo.relativePath+"', '"+fileJSON+"')");
		}

	}

	private class LocalSearchTorrentDownloaderListener implements
			TorrentDownloaderCallBackInterface {

		private AtomicBoolean finished = new AtomicBoolean(false);
		private byte[] guid;
		private List<String> query;
		private SearchEngine searchEngine;
		private WebSearchResult webSearchResult;
		private SearchInformation info;
		private List<String> substractedQuery;
		private List<String> tokensTorrent;

		public LocalSearchTorrentDownloaderListener(byte[] guid, String query,
				WebSearchResult webSearchResult, SearchEngine searchEngine,
				SearchInformation info) {
			this.guid = guid;
			this.query = Arrays.asList(query.toLowerCase().split(" "));
			this.searchEngine = searchEngine;
			this.webSearchResult = webSearchResult;
			this.info = info;

			initSubstractedQuery();
		}

		private void initSubstractedQuery() {
			// substract the query keywords that are already in the
			// webSearchResult title.

			String torrentFileNameNoExtension = webSearchResult
					.getFilenameNoExtension();
			List<String> tokensQuery = new ArrayList<String>(query);
			tokensTorrent = Arrays.asList(torrentFileNameNoExtension
					.toLowerCase().split(" "));

			tokensQuery.removeAll(tokensTorrent);
			this.substractedQuery = tokensQuery;
		}

		@Override
		public void TorrentDownloaderEvent(int state, TorrentDownloader inf) {

			// index the torrent (insert it's structure in the local DB)
			if (state == TorrentDownloader.STATE_FINISHED
					&& finished.compareAndSet(false, true)) {
				try {
					File torrentFile = inf.getFile();
					TOTorrent theTorrent = TorrentUtils.readFromFile(
							torrentFile, false);
					torrentFile.delete();

					indexTorrent(webSearchResult, theTorrent, searchEngine);

					// search right away on this torrent.
					matchResults(theTorrent);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			switch (state) {
			case TorrentDownloader.STATE_FINISHED:
			case TorrentDownloader.STATE_ERROR:
			case TorrentDownloader.STATE_DUPLICATE:
			case TorrentDownloader.STATE_CANCELLED:
				ResultPanel rp = SearchMediator.getResultPanelForGUID(new GUID(
						guid));
				if (rp != null) {
					rp.decrementSearchCount();
				}
				break;
			}
		}

		private void matchResults(TOTorrent theTorrent) {

			if (!searchEngine.isEnabled()) {
				return;
			}

			ResultPanel rp = SearchMediator
					.getResultPanelForGUID(new GUID(guid));

			// user closed the tab.
			if (rp == null) {
				return;
			}

			SearchFilter filter = SearchMediator.getSearchFilterFactory()
					.createFilter();

			TOTorrentFile[] fs = theTorrent.getFiles();
			for (int i = 0; i < fs.length; i++) {
				DeepSearchResult result = new DeepSearchResult(fs[i],
						webSearchResult, searchEngine, info);

				if (!filter.allow(result))
					continue;

				boolean foundMatch = true;

				List<String> selectedQuery = substractedQuery;

				// if all tokens happened to be on the title of the torrent,
				// we'll just use the full query.
				if (substractedQuery.size() == 0) {
					selectedQuery = query;
				}

				// Steve Jobs style first (like iTunes search logic)
				for (String token : selectedQuery) {
					if (!result.getFileName().toLowerCase().contains(token)) {
						foundMatch = false;
						break;
					}
				}

				// best match ever, Steve Jobs style.
				if (foundMatch) {
					SearchMediator.getSearchResultDisplayer().addQueryResult(
							guid, result, rp);
					return;
				}

				// if Steve Jobs is too good for ya...
				// we'll remove the tokens of the torrent title ONCE from the
				// search result name
				// and we'll perform a match on what's left.
				String resultName = result.getFileName().toLowerCase();

				HashSet<String> torrentTokenSet = new HashSet<String>(
						tokensTorrent);
				for (String torrentToken : torrentTokenSet) {
					try {
						torrentToken = torrentToken.replace("(", "")
								.replace(")", "").replace("[", "")
								.replace("]", "");
						resultName = resultName.replaceFirst(torrentToken, "");
					} catch (Exception e) {
						// shhh
					}
				}

				foundMatch = true; // optimism!

				for (String token : selectedQuery) {
					if (!resultName.contains(token)) {
						foundMatch = false;
						break;
					}
				}

				if (foundMatch) {
					SearchMediator.getSearchResultDisplayer().addQueryResult(
							guid, result, rp);
					return;
				}

			}

		}
	}

	private class MatchLogic {

		private List<String> query;
		private String torrentName;
		private String fileName;
		private List<String> substractedQuery;
		private List<String> tokensTorrent;

		public MatchLogic(String query, String torrentName, String fileName) {
			this.query = Arrays.asList(query.toLowerCase().split(" "));
			this.torrentName = torrentName.toLowerCase();
			this.fileName = fileName.toLowerCase();

			initSubstractedQuery();
		}

		private void initSubstractedQuery() {
			// substract the query keywords that are already in the
			// webSearchResult title.

			String torrentFileNameNoExtension = torrentName;
			List<String> tokensQuery = new ArrayList<String>(query);
			tokensTorrent = Arrays.asList(torrentFileNameNoExtension
					.toLowerCase().split(" "));

			tokensQuery.removeAll(tokensTorrent);
			this.substractedQuery = tokensQuery;
		}

		public boolean matchResult() {

			boolean foundMatch = true;

			List<String> selectedQuery = substractedQuery;

			// if all tokens happened to be on the title of the torrent,
			// we'll just use the full query.
			if (substractedQuery.size() == 0) {
				selectedQuery = query;
			}

			// Steve Jobs style first (like iTunes search logic)
			for (String token : selectedQuery) {
				if (!fileName.contains(token)) {
					foundMatch = false;
					break;
				}
			}

			// best match ever, Steve Jobs style.
			if (foundMatch) {
				return true;
			}

			// if Steve Jobs is too good for ya...
			// we'll remove the tokens of the torrent title ONCE from the
			// search result name
			// and we'll perform a match on what's left.
			String resultName = fileName;

			HashSet<String> torrentTokenSet = new HashSet<String>(tokensTorrent);
			for (String torrentToken : torrentTokenSet) {
				try {
					torrentToken = torrentToken.replace("(", "")
							.replace(")", "").replace("[", "").replace("]", "");
					resultName = resultName.replaceFirst(torrentToken, "");
				} catch (Exception e) {
					// shhh
				}
			}

			foundMatch = true; // optimism!

			for (String token : selectedQuery) {
				if (!resultName.contains(token)) {
					foundMatch = false;
					break;
				}
			}

			if (foundMatch) {
				return true;
			}

			return false;
		}
	}

	public void shutdown() {
		DB.close();
	}

	public int getTotalTorrents() {
		List<List<Object>> query = DB.query("SELECT COUNT(*) FROM Torrents");
		return (Integer) query.get(0).get(0);
	}
	
	public int getTotalFiles() {
		List<List<Object>> query = DB.query("SELECT COUNT(*) FROM Files");
		return (Integer) query.get(0).get(0);
	}

	public synchronized void resetDB() {
		DB.close();
		File value = SearchSettings.SMART_SEARCH_DATABASE_FOLDER.getValue();
		FileUtils.deleteRecursive(value);
		DB =  new SmartSearchDB(SearchSettings.SMART_SEARCH_DATABASE_FOLDER.getValue());
	}
}