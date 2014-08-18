
package pl.kotcrab.arget.test;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import pl.kotcrab.arget.App;
import pl.kotcrab.arget.comm.Msg;
import pl.kotcrab.arget.global.session.gui.SessionPanel;
import pl.kotcrab.arget.global.session.gui.TextMessage;

public class SessionPanelTest extends JFrame {

	private JPanel contentPane;

	/** Launch the application. */
	public static void main (String[] args) {
		App.init();
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run () {
				try {
					SessionPanelTest frame = new SessionPanelTest();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/** Create the frame. */
	public SessionPanelTest () {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 700);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		final SessionPanel panel = new SessionPanel(null, null, null);
		contentPane.add(panel);
		setContentPane(contentPane);

		panel.addMessage(new TextMessage(Msg.LEFT, "Lorem ipsum dolor sit amet"));
		panel.addMessage(new TextMessage(Msg.RIGHT, "Lorem ipsum dolor sit amet"));
		panel.addMessage(new TextMessage(Msg.SYSTEM, "System msg"));
		panel.addMessage(new TextMessage(Msg.ERROR, "System error msg"));
		// FIXME too long text without spaces break layouts
		panel
			.addMessage(new TextMessage(
				Msg.RIGHT,
				"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nam tincidunt, eros id convallis ultricies, nulla mi eleifend velit,"
					+ " vel placerat ante urna interdum velit. Nunc tincidunt eros ac consectetur varius. Aenean a pretium est, id tincidunt eros."));

		panel.addMessage(new TextMessage(Msg.RIGHT, "Linki: https://www.kotcrab.pl/ Formatowanie: *Lorem* _ipsum_"));
		panel.addMessage(new TextMessage(Msg.RIGHT, "Linki: https://www.youtube.com/watch?v=3vI_7os2V_o"));

		panel
			.addMessage(new TextMessage(
				Msg.RIGHT,
				"Linki: http://mashable.com/2014/08/14/watch-surgery-on-the-oculus-rift-but-maybe-do-it-after-lunch/?utm_cid=mash-com-G+-main-link"));

		panel.addMessage(new TextMessage(Msg.LEFT, "Obrazki w okienku rozmowy i przesy�anie plik�w"));
// panel.addMessage(new ImageMessage(Msg.LEFT, ImageUitls.read(new File("avatar.jpg")), ""));

		// panel.addMessage(new FileTransferMessage(new SendFileTask(null, new File("test.txt"), false)));
		// panel.addMessage(new TextMessage(Msg.SYSTEM, "Combined test: _abc *abc* a_"));
		// panel.addMessage(new TextMessage(Msg.SYSTEM, "Error test: *abc ab *c _a*"));

		panel.showTyping();

// new Thread(new Runnable() {
//
// @Override
// public void run () {
// ThreadUtils.sleep(1000);
// panel.addMessage(new TextMessage(Msg.SYSTEM_ERROR, "Lorem ipsum dolor sit amet"));
// ThreadUtils.sleep(3000);
// panel.hideTyping();
// }
// }).start();

	}
}
