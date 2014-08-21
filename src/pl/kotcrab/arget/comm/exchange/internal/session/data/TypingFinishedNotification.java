
package pl.kotcrab.arget.comm.exchange.internal.session.data;

import java.util.UUID;

/** Notifies that remote finished (or stopped) typing message
 * @author Pawel Pastuszak */
public class TypingFinishedNotification extends InternalSessionExchange {
	@Deprecated
	public TypingFinishedNotification () {
		super(null);
	}

	public TypingFinishedNotification (UUID id) {
		super(id);
	}
}
