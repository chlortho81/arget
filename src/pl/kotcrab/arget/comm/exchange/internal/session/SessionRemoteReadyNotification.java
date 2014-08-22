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

package pl.kotcrab.arget.comm.exchange.internal.session;

import java.util.UUID;

/** Notifies requester that cipher init was successful.
 * @author Pawel Pastuszak */
public class SessionRemoteReadyNotification extends SessionExchange {

	@Deprecated
	public SessionRemoteReadyNotification () {
		super(null);
	}

	public SessionRemoteReadyNotification (UUID id) {
		super(id);
	}
}
