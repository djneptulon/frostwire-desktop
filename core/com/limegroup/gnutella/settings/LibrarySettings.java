/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011, 2012, FrostWire(TM). All rights reserved.
 *
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

package com.limegroup.gnutella.settings;

import java.io.File;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.FileSetSetting;
import org.limewire.setting.FileSetting;
import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.util.FrostWireUtils;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
public class LibrarySettings extends LimeProps {

    private LibrarySettings() {
    }

    public static final File LIBRARY_DATABASE = new File(CommonUtils.getUserSettingsDir(), "library_db");

    public static final File DEFAULT_LIBRARY_FROM_DEVICE_DATA_DIR = new File(FrostWireUtils.getFrostWireRootFolder(), "From Device");

    /**
     * The include directories. 
     */
    public static final FileSetSetting DIRECTORIES_TO_INCLUDE = FACTORY.createFileSetSetting("DIRECTORIES_TO_INCLUDE_FOR_FILES", new File[0]);

    public static final FileSetSetting DIRECTORIES_NOT_TO_INCLUDE = FACTORY.createFileSetSetting("DIRECTORIES_NOT_TO_INCLUDE", new File[0]);

    public static final FileSetSetting DIRECTORIES_TO_INCLUDE_FROM_FROSTWIRE4 = FACTORY.createFileSetSetting("DIRECTORIES_TO_INCLUDE_FROM_FROSTWIRE4", new File[0]);

    public static final FileSetting USER_MUSIC_FOLDER = FACTORY.createFileSetting("USER_MUSIC_FOLDER", FrostWireUtils.getUserMusicFolder());

    public static final FileSetting LIBRARY_FROM_DEVICE_DATA_DIR_SETTING = FACTORY.createFileSetting("LIBRARY_FROM_DEVICE_DATA_DIR_SETTING", DEFAULT_LIBRARY_FROM_DEVICE_DATA_DIR).setAlwaysSave(true);

    public static final BooleanSetting LIBRARY_WIFI_SHARING_ENABLED = FACTORY.createBooleanSetting("LIBRARY_WIFI_SHARING_ENABLED", true);
}
