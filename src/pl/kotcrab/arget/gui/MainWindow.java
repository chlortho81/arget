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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.apache.commons.codec.binary.Base64;

import pl.kotcrab.arget.App;
import pl.kotcrab.arget.ExitCleaner;
import pl.kotcrab.arget.Log;
import pl.kotcrab.arget.Settings;
import pl.kotcrab.arget.comm.exchange.internal.KeychainRequest;
import pl.kotcrab.arget.comm.exchange.internal.ServerInfoTransfer;
import pl.kotcrab.arget.event.ConnectionStatusEvent;
import pl.kotcrab.arget.event.Event;
import pl.kotcrab.arget.event.EventListener;
import pl.kotcrab.arget.event.MenuEvent;
import pl.kotcrab.arget.event.MenuEventType;
import pl.kotcrab.arget.event.SaveProfileEvent;
import pl.kotcrab.arget.event.UpdateContactsEvent;
import pl.kotcrab.arget.gui.components.BottomSplitPaneBorder;
import pl.kotcrab.arget.gui.components.MenuItem;
import pl.kotcrab.arget.gui.components.ServerMenuItem;
import pl.kotcrab.arget.gui.dialog.AboutDialog;
import pl.kotcrab.arget.gui.dialog.CreateContactDialog;
import pl.kotcrab.arget.gui.dialog.CreateContactDialogFinished;
import pl.kotcrab.arget.gui.dialog.CreateServerDialogFinished;
import pl.kotcrab.arget.gui.dialog.CreateServerInfoDialog;
import pl.kotcrab.arget.gui.dialog.DisplayPublicKeyDialog;
import pl.kotcrab.arget.gui.dialog.ManageServersDialog;
import pl.kotcrab.arget.gui.dialog.OptionsDialog;
import pl.kotcrab.arget.gui.dialog.VersionMismatchDialog;
import pl.kotcrab.arget.gui.notification.NotificationControler;
import pl.kotcrab.arget.gui.session.SessionWindowManager;
import pl.kotcrab.arget.profile.Profile;
import pl.kotcrab.arget.profile.ProfileIO;
import pl.kotcrab.arget.profile.ProfileOptions;
import pl.kotcrab.arget.server.ConnectionManager;
import pl.kotcrab.arget.server.ConnectionStatus;
import pl.kotcrab.arget.server.ContactInfo;
import pl.kotcrab.arget.server.ContactStatus;
import pl.kotcrab.arget.server.ServerDescriptor;
import pl.kotcrab.arget.util.GraphicsUtils;
import pl.kotcrab.arget.util.Sound;
import pl.kotcrab.arget.util.iconflasher.IconFlasher;

import com.alee.laf.button.WebToggleButton;

//TODO event bus
//TODO clear server details on disconnect
//TODO create createGUI method in every gui class
//TODO usage statistics sever
//TODO themes
//TODO add setting for file transfer save directory
public class MainWindow extends JFrame implements MainWindowCallback, EventListener, NotificationControler {
	private static final String TAG = "MainWindow";
	public static MainWindow instance;

	private Profile profile;
	private ConnectionManager connection;
	private SessionWindowManager sessionWindowManager;

	private Sound notificationSound;

	private JMenu serversMenu;
	private JTextPane statusPane;
	private JSplitPane splitPane;

	private ErrorStatusPanel errorStatusPanel;

	private ContactsPanel contactsPanel;
	private HomePanel homePanel;
	private CenterPanel logPanel;

	private IconFlasher iconFlasher;

	private WebToggleButton scrollLockToggle;

	private boolean painted;

	public MainWindow (Profile _profile) {
		if (checkAndSetInstance() == false) return;

		this.profile = _profile;

		App.eventBus.register(this);
		App.notificationService.setControler(this);

		notificationSound = new Sound("/data/notification.wav");

		createAndShowGUI();

		iconFlasher = IconFlasher.getIconFlasher(this);

		connection = new ConnectionManager(profile, sessionWindowManager, this);

		statusPane.setText("Disconnected");
		if (profile.autoconnectInfo != null) connectToServer(profile.autoconnectInfo);
	}

	private boolean checkAndSetInstance () {
		if (MainWindow.instance != null) {
			System.err.println("MainWindow already exists!");
			dispose();
			return false;
		}

		MainWindow.instance = this;
		return true;
	}

	private void createAndShowGUI () {
		setTitle(App.APP_NAME);
		setBounds(100, 100, 800, 700);
		setMinimumSize(new Dimension(500, 250));
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setIconImage(App.loadImage("/data/icon/icon.png"));

		sessionWindowManager = new SessionWindowManager(this);

		if (profile.mainWindowBounds != null && GraphicsUtils.isRectangleDisplayableOnScreen(profile.mainWindowBounds))
			setBounds(profile.mainWindowBounds);

		createMenuBars();

		contactsPanel = new ContactsPanel(profile, this);

		statusPane = new JTextPane();
		statusPane.setBorder(new EmptyBorder(1, 3, 2, 0));
		statusPane.setContentType("text/html");
		statusPane.setBackground(null);
		statusPane.setHighlighter(null);
		statusPane.setEditable(false);
		statusPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
		statusPane.setFont(new Font("Tahoma", Font.PLAIN, 13));

		statusPane.addHyperlinkListener(new HyperlinkListener() {

			@Override
			public void hyperlinkUpdate (HyperlinkEvent e) {
				if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
					String desc = e.getDescription();

					if (desc.startsWith("version-mismatch")) {
						desc = desc.substring(desc.indexOf("://") + 3);
						String[] versionInfo = desc.split("!");
						new VersionMismatchDialog(MainWindow.instance, versionInfo[0], Integer.valueOf(versionInfo[1]));
					}
				}
			}
		});

		JPanel bottomPanel = new JPanel(new BorderLayout(0, 0));

		getContentPane().add(bottomPanel, BorderLayout.SOUTH);

		scrollLockToggle = new WebToggleButton();
		scrollLockToggle.setToolTipText("Scroll lock");
		scrollLockToggle.setRolloverDecoratedOnly(true);
		scrollLockToggle.setDrawFocus(false);
		scrollLockToggle.setIcon(App.loadImageIcon("/data/scrolllock.png"));
		scrollLockToggle.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				postScrollLockUpdate();
			}
		});

		bottomPanel.add(statusPane);
		bottomPanel.add(scrollLockToggle, BorderLayout.EAST);

		splitPane = new JSplitPane();
		splitPane.setBorder(new BottomSplitPaneBorder());
		splitPane.setResizeWeight(0);
		splitPane.setContinuousLayout(true);
		splitPane.setOneTouchExpandable(true);
		getContentPane().add(splitPane, BorderLayout.CENTER);

		homePanel = new HomePanel(profile.fileName);
		logPanel = new LoggerPanel();

		errorStatusPanel = new ErrorStatusPanel();

		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.add(contactsPanel, BorderLayout.CENTER);
		leftPanel.add(errorStatusPanel, BorderLayout.SOUTH);

		splitPane.setLeftComponent(leftPanel);
		splitPane.setRightComponent(null);
		setCenterScreenTo(homePanel);

		addWindowFocusListener(new WindowAdapter() {
			@Override
			public void windowGainedFocus (WindowEvent e) {
				instance.validate();
				instance.revalidate();
				instance.repaint();

				getCenterScreen().onShow();
			}

			@Override
			public void windowLostFocus (WindowEvent e) {
				getCenterScreen().onHide();
			}

		});

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing (WindowEvent evt) {
				ExitCleaner.forceExit();
			}
		});

		setVisible(true);
	}

	@Override
	public void paint (Graphics g) {
		super.paint(g);

		if (!painted) {
			painted = true;
			splitPane.setDividerLocation(0.25);
		}
	}

	private void post (Event event) {
		App.eventBus.post(event);
	}

	private void createMenuBars () {
		JMenuBar menuBar = new JMenuBar();
		menuBar.setBorder(new EmptyBorder(0, 0, 0, 0));
		setJMenuBar(menuBar);

		JMenu argetMenu = new JMenu("Arget");
		serversMenu = new JMenu("Servers");
		JMenu contactsMenu = new JMenu("Contacts");
		JMenu viewMenu = new JMenu("View");
		JMenu helpMenu = new JMenu("Help");

		menuBar.add(argetMenu);
		menuBar.add(serversMenu);
		menuBar.add(contactsMenu);
		menuBar.add(viewMenu);
		menuBar.add(helpMenu);

		argetMenu.add(new MenuItem("Options...", MenuEventType.ARGET_EDIT_OPTIONS));
		argetMenu.add(new JSeparator());
		argetMenu.add(new MenuItem("Logout", MenuEventType.ARGET_LOGOUT));
		argetMenu.add(new MenuItem("Exit", MenuEventType.ARGET_EXIT));

		serversMenu.add(new MenuItem("Add Server...", MenuEventType.SERVERS_ADD));
		serversMenu.add(new MenuItem("Manage Servers...", MenuEventType.SERVERS_MANAGE));
		serversMenu.add(new MenuItem("Disconnect", MenuEventType.SERVERS_DISCONNECT));
		serversMenu.add(new JSeparator());

		viewMenu.add(new MenuItem("Show Home Screen", MenuEventType.VIEW_SHOW_HOME));
		viewMenu.add(new MenuItem("Show Log", MenuEventType.VIEW_SHOW_LOG));

		contactsMenu.add(new MenuItem("Show My Public Key...", MenuEventType.CONTACTS_SHOW_PUBLIC_KEY));
		contactsMenu.add(new MenuItem("Add Contact...", MenuEventType.CONTACTS_ADD));
		contactsMenu.add(new JSeparator());
		contactsMenu.add(new MenuItem("Refresh list", MenuEventType.CONTACTS_REFRESH));

		helpMenu.add(new MenuItem("About Arget", MenuEventType.HELP_ABOUT));

		addServersFromProfile();
	}

	private void rebuildServersList () {
		cleanServersMenuList();
		addServersFromProfile();
	}

	private void addServersFromProfile () {
		for (ServerDescriptor info : profile.servers)
			serversMenu.add(new ServerMenuItem(this, info));
	}

	private void cleanServersMenuList () {

		Component[] comp = serversMenu.getPopupMenu().getComponents();
		for (int i = 0; i < comp.length; i++) {
			if (comp[i] instanceof ServerMenuItem) serversMenu.getPopupMenu().remove(comp[i]);
		}

	}

	private void setConnectionStatus (ConnectionStatusEvent e) {
		if (connection.compareClient(e.eventSender)) {
			String textToSet = e.status.toPrettyString();

			if (e.status == ConnectionStatus.VERSION_MISMATCH) {
				textToSet = "<html>" + textToSet;
				textToSet += " <a href=\"version-mismatch://" + e.msg + "\">Details...</a>";
				textToSet += "<html>";
			} else if (e.msg != null) textToSet += ": " + e.msg;

			statusPane.setText(textToSet);

			if (e.status.isConnectionBroken()) resetContacts();
		}
	}

	@Override
	public void dispose () {
		if (connection.isConnected()) connection.requestDisconnect();
		if (sessionWindowManager != null) sessionWindowManager.stop();

		profile.mainWindowBounds = getBounds();
		ProfileIO.saveProfile(profile);

		MainWindow.instance = null;

		super.dispose();
	}

	@Override
	public void startChat (ContactInfo contact) {
		if (contact.status == ContactStatus.DISCONNECTED) {
			if (sessionWindowManager.showPanelForContact(contact) == false)
				JOptionPane.showMessageDialog(this,
					"Old session not found, new session cannot be created because this contact is not connected!", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (contact.status == ContactStatus.CONNECTED_SESSION) {
			sessionWindowManager.showPanelForContact(contact);
			return;
		}

		if (contact.status == ContactStatus.CONNECTED) {
			sessionWindowManager.showPanelForContactWhenReady(contact);
			connection.createSession(contact);
			return;
		}
	}

	// TODO move to contacts table
	private void resetContacts () {
		for (ContactInfo c : profile.contacts)
			c.status = ContactStatus.DISCONNECTED;

		updateContacts();
	}

	private void updateContacts () {
		if (connection.isConnected()) {
			connection.processLastKeychain(); // automatically calls contactsPanel.updateContactsTable();
			contactsPanel.updateContactsTable();
		} else
			contactsPanel.updateContactsTable();

	}

	@Override
	public boolean isKeyInContacts (String key) {
		for (ContactInfo c : profile.contacts) {
			if (c.publicProfileKey.equals(key)) return true;
		}

		return false;
	}

	@Override
	public ContactInfo getContactsByKey (String key) {
		for (ContactInfo c : profile.contacts) {
			if (c.publicProfileKey.equals(key)) return c;
		}

		return null;
	}

	@Override
	public void setCenterScreenTo (CenterPanel panel) {
		if (getCenterScreen() != null) getCenterScreen().onHide();

		// double dl = splitPane.getDividerLocation(); //why the hell this is returning int?
		// "proportional location must be between 0.0 and 1.0."
		splitPane.setRightComponent(panel);
		// splitPane.setDividerLocation(dl);
		splitPane.setDividerLocation(0.25); // TODO rember divider location

		if (panel.getTitle() == null)
			setTitle(App.APP_NAME);
		else
			setTitle(App.APP_NAME + " - " + panel.getTitle());
		panel.onShow();
	}

	@Override
	public CenterPanel getCenterScreen () {
		return (CenterPanel)splitPane.getRightComponent();
	}

	@Override
	public void connectToServer (final ServerDescriptor info) {
		if (connection.isConnected() == false)
			connection.connect(info);
		else
			JOptionPane.showMessageDialog(instance,
				"Already connected to server. Disconnect first if you want to change your current server.", "Error",
				JOptionPane.ERROR_MESSAGE);
	}

	@Override
	public void starFlasherAndSoundIfNeeded () {
		if (isFocused() == false) {
			iconFlasher.flashIcon();

			if (profile.options.mainPlaySoundNewMsg) notificationSound.play();
		}
	}

	@Override
	public void setServerInfo (ServerInfoTransfer info) {
		homePanel.setServerText(info);
	}

	@Override
	public void onEvent (Event event) {
		if (event instanceof MenuEvent) processMenuEvent((MenuEvent)event);
		if (event instanceof ConnectionStatusEvent) setConnectionStatus((ConnectionStatusEvent)event);
		if (event instanceof UpdateContactsEvent) updateContacts();
		if (event instanceof ScrollLockStatusRequestEvent) postScrollLockUpdate();

		if (event instanceof SaveProfileEvent) {
			ProfileIO.saveProfile(profile);
			rebuildServersList();
		}
	}

	private void processMenuEvent (MenuEvent event) {
		switch (event.type) {
		case ARGET_EDIT_OPTIONS:
			new OptionsDialog(this, profile);
			break;
		case ARGET_LOGOUT:
			Settings.resetAutoLogin();
			dispose();

			EventQueue.invokeLater(new Runnable() {
				@Override
				public void run () {
					new LoginFrame();
				}
			});
			break;
		case ARGET_EXIT:
			dispose();
			ExitCleaner.forceExit();
			break;

		case SERVERS_ADD:
			new CreateServerInfoDialog(instance, new CreateServerDialogFinished() {

				@Override
				public void finished (ServerDescriptor info) {
					profile.servers.add(info);
					ProfileIO.saveProfile(profile);
					rebuildServersList();
				}
			});
			break;
		case SERVERS_MANAGE:
			new ManageServersDialog(instance, profile);
			break;
		case SERVERS_DISCONNECT:
			if (connection.isConnected())
				connection.requestDisconnect();
			else
				JOptionPane.showMessageDialog(instance,
					"You need to be connected to a server to disconnect. Please connect to disconnect.", "Error",
					JOptionPane.ERROR_MESSAGE);
			break;

		case CONTACTS_SHOW_PUBLIC_KEY: {
			String key = Base64.encodeBase64String(profile.rsa.getPublicKey().getEncoded());
			new DisplayPublicKeyDialog(instance, key);
			break;
		}
		case CONTACTS_ADD: {
			String key = Base64.encodeBase64String(profile.rsa.getPublicKey().getEncoded());
			new CreateContactDialog(instance, key, new CreateContactDialogFinished() {

				@Override
				public void finished (ContactInfo contact) {
					profile.contacts.add(contact);
					ProfileIO.saveProfile(profile);
					updateContacts();
				}
			});
			break;
		}
		case CONTACTS_REFRESH:
			connection.send(new KeychainRequest());
			break;

		case VIEW_SHOW_HOME:
			setCenterScreenTo(homePanel);
			break;
		case VIEW_SHOW_LOG:
			errorStatusPanel.clear();
			setCenterScreenTo(logPanel);
			break;

		case HELP_ABOUT:
			new AboutDialog(instance);
			break;
		default:
			Log.err(TAG, "Unknown menu event type: " + event.type);
			break;
		}
	}

	private void postScrollLockUpdate () {
		post(new ScrollLockEvent(scrollLockToggle.isSelected()));
	}

	@Override
	public boolean shouldDisplayNotification () {
		return !isFocused();
	}

	@Override
	public ProfileOptions getOptions () {
		return profile.options;
	}

}
