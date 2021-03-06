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

package pl.kotcrab.arget.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import pl.kotcrab.arget.Log;
import pl.kotcrab.arget.comm.exchange.internal.session.SessionAlreadyExistNotification;
import pl.kotcrab.arget.comm.exchange.internal.session.SessionCloseNotification;
import pl.kotcrab.arget.comm.exchange.internal.session.SessionCreateRequest;
import pl.kotcrab.arget.comm.exchange.internal.session.SessionDoesNotExist;
import pl.kotcrab.arget.comm.exchange.internal.session.SessionExchange;
import pl.kotcrab.arget.comm.exchange.internal.session.SessionInvalidIDNotification;
import pl.kotcrab.arget.comm.exchange.internal.session.SessionInvalidReciever;
import pl.kotcrab.arget.comm.exchange.internal.session.SessionRemoteAcceptRequest;
import pl.kotcrab.arget.comm.exchange.internal.session.SessionTargetKeyNotFound;
import pl.kotcrab.arget.comm.exchange.internal.session.SessionUnrecoverableBroken;
import pl.kotcrab.arget.server.ArgetServer;
import pl.kotcrab.arget.server.ResponseServer;
import pl.kotcrab.arget.util.ProcessingQueue;

public class ServerSessionManager extends ProcessingQueue<ServerSessionUpdate> {
	private static final String TAG = "SessionManager";

	private Collection<ResponseServer> servers;
	private List<ServerSession> sessions;

	public ServerSessionManager (Collection<ResponseServer> servers) {
		super("ServerSessionManager");
		this.servers = servers;

		sessions = new ArrayList<ServerSession>();
	}

	@Override
	protected void processQueueElement (ServerSessionUpdate update) {
		if (update instanceof ServerSessionInternalCloseRequest) {
			ResponseServer server = update.receiver;

			Iterator<ServerSession> itr = sessions.iterator();
			while (itr.hasNext()) {
				ServerSession session = itr.next();

				if (session.requester == server) closeInvalidSession(itr, session, session.target);
				if (session.target == server) closeInvalidSession(itr, session, session.requester);
			}

			return;
		}

		SessionExchange ex = update.exchange;

		if (ex instanceof SessionCreateRequest) {
			SessionCreateRequest request = (SessionCreateRequest)ex;

			if (isIDInUse(request.id)) {
				update.receiver.send(new SessionInvalidIDNotification(request.id));
				return;
			}

			ResponseServer target = getServerForKey(request.targetKey);
			if (target == null) {
				update.receiver.send(new SessionTargetKeyNotFound(request.id));
				return;
			}

			for (ServerSession ses : sessions) {
				if ((ses.requester == target && ses.target == update.receiver)
					|| (ses.requester == update.receiver && ses.target == target)) {
					update.receiver.send(new SessionAlreadyExistNotification(request.id));
					return;
				}

			}

			target.send(new SessionRemoteAcceptRequest(request.id, update.receiver.getProfilePublicKey()));
			sessions.add(new ServerSession(request.id, update.receiver, target));
			Log.l(TAG, "Created: " + request.id);
			return;
		}

		ServerSession session = getSessionByID(ex);

		if (isValidUpdateForSession(session, update)) {
			if (ex instanceof SessionUnrecoverableBroken) {
				Log.l(TAG, "Closed: " + session.id);
				sendToOposite(session, update);
				sessions.remove(session);
				return;
			}

			sendToOposite(session, update);
		}

	}

	private void sendToOposite (ServerSession session, ServerSessionUpdate update) {
		if (session.requester == update.receiver)
			session.target.send(update.exchange);
		else
			session.requester.send(update.exchange);
	}

	private ServerSession getSessionByID (SessionExchange ex) {
		for (ServerSession ses : sessions) {
			if (ses.id.compareTo(ex.id) == 0) return ses;
		}

		return null;
	}

	private boolean isValidUpdateForSession (ServerSession session, ServerSessionUpdate update) {
		UUID id = update.exchange.id;

		if (session == null) {
			Log.w(TAG, "Could not found session by UUID: " + id + " Receiver ID: " + update.receiver.getId());
			update.receiver.send(new SessionDoesNotExist(id));
			return false;
		}

		if (session.requester == update.receiver || session.target == update.receiver)
			return true;
		else {
			update.receiver.send(new SessionInvalidReciever(id));
			Log.w(TAG, "Not valid update for session UUID: " + id + " Receiver ID: " + update.receiver.getId());
			return false;
		}
	}

	private boolean isIDInUse (UUID id) {
		for (ServerSession s : sessions) {
			if (s.id.compareTo(id) == 0) return true;
		}

		return false;
	}

	private ResponseServer getServerForKey (String key) {
		for (ResponseServer s : servers) {
			if (s.getProfilePublicKey().equals(key)) return s;
		}

		return null;
	}

	public void closeSessionsForServer (final ResponseServer respServer) {
		// because this method was called from interrupted thread, queue for this session manager would throw InterutptedException
		// and not process our request, but if we call it from different thread everything will be fine
		new Thread(new Runnable() {
			@Override
			public void run () {
				processLater(new ServerSessionInternalCloseRequest(respServer));
			}
		}).start();
	}

	private void closeInvalidSession (Iterator<ServerSession> it, ServerSession session, ResponseServer targetToSendNotif) {
		Log.l(TAG, "Closed no longer valid session: " + session.id);
		targetToSendNotif.send(new SessionCloseNotification(session.id));
		it.remove();
	}

	/** This is special update that is inserted into {@link ServerSessionManager} queue by {@link ArgetServer}. It tells
	 * {@link ServerSessionManager} to close all session associated with provided {@link ResponseServer}
	 * @author Pawel Pastuszak */
	private class ServerSessionInternalCloseRequest extends ServerSessionUpdate {

		public ServerSessionInternalCloseRequest (ResponseServer respServer) {
			super(respServer, null);
		}
	}

}
