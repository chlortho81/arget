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

package pl.kotcrab.arget.gui;

import java.awt.BorderLayout;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;

import pl.kotcrab.arget.App;
import pl.kotcrab.arget.Log;
import pl.kotcrab.arget.LoggerListener;
import pl.kotcrab.arget.event.LoggerPanelEvent;
import pl.kotcrab.arget.event.LoggerPanelEvent.Type;

public class LoggerPanel extends CenterPanel {
	private JTextArea textArea;

	public LoggerPanel () {
		setLayout(new BorderLayout(0, 0));

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBorder(null);
		add(scrollPane);

		textArea = new JTextArea();
		scrollPane.setViewportView(textArea);

		Log.setListener(new LoggerListener() {

			@Override
			public void log (String msg) {
				appendToLog(msg);

				if (msg.contains("WARNING")) App.eventBus.post(new LoggerPanelEvent(Type.WARNING));
			}

			@Override
			public void err (String msg) {
				appendToLog(msg);

				App.eventBus.post(new LoggerPanelEvent(Type.ERROR));
			}

			@Override
			public void exception (String stacktrace) {
				appendToLog("EXCEPTION: " + stacktrace);

				App.eventBus.post(new LoggerPanelEvent(Type.EXCEPTION));

			}
		});
	}

	private void appendToLog (String msg) {
		try {
			textArea.getDocument().insertString(textArea.getDocument().getLength(), msg + "\n", null);
		} catch (BadLocationException e) {
		}
	}

	@Override
	public String getTitle () {
		return "Log";
	}

}
