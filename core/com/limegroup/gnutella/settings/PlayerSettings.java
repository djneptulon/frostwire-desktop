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

package com.limegroup.gnutella.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.FloatSetting;

/**
 * Settings for Music Player
 */
public class PlayerSettings extends LimeProps {

    private PlayerSettings() {
    }

    public static BooleanSetting LOOP_PLAYLIST = FACTORY.createBooleanSetting("LOOP_PLAYLIST", true);

    public static BooleanSetting SHUFFLE_PLAYLIST = FACTORY.createBooleanSetting("SHUFFLE_PLAYLIST", false);

    public static FloatSetting PLAYER_VOLUME = FACTORY.createFloatSetting("PLAYER_VOLUME", 0.5f);

    public static BooleanSetting USE_OS_DEFAULT_PLAYER = FACTORY.createBooleanSetting("USE_OS_DEFAULT_PLAYER", false);
}
