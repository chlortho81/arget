/*******************************************************************************
    Copyright 2014 Pawel Pastuszak
 
    This file is part of Arget.

    Arget is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Arget is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Arget.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package pl.kotcrab.arget.comm.exchange.internal;

import org.apache.commons.codec.binary.Base64;

/** Transfer of a public profile key.
 * @author Pawel Pastuszak */
public class ProfilePublicKeyTransfer implements InternalExchange {
	public String key;

	@Deprecated
	public ProfilePublicKeyTransfer () {

	}

	public ProfilePublicKeyTransfer (byte[] key) {
		this.key = Base64.encodeBase64String(key);
	}
}
