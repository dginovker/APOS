package com.aposbot;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.Map;

import javax.imageio.ImageIO;

import com.aposbot._default.IAutoLogin;
import com.aposbot._default.IClient;
import com.aposbot._default.IClientInit;
import com.aposbot._default.IJokerFOCR;
import com.aposbot._default.IPaintListener;
import com.aposbot._default.IScriptListener;
import com.aposbot.applet.AVStub;

public final class BotFrame extends Frame {

	private static final long serialVersionUID = -2847514806687135697L;
	private Checkbox loginCheck;
	private Checkbox gfxCheck;
	private Button startButton;
	private Debugger debugger;
	private ScriptFrame scriptFrame;
	private Choice worldChoice;
	private AVStub stub;
	private IClient client;

	BotFrame(IClientInit init, final TextArea cTextArea, String account) {
		super("APOS (" + account + ")");
		setFont(Constants.UI_FONT);
		setIconImages(Constants.ICONS);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				quit();
			}
		});

		if (SystemTray.isSupported()) {
			final TrayIcon icon = new TrayIcon(Constants.ICON_16, "APOS (" + account + ")");
			icon.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					final BotFrame t = BotFrame.this;
					t.setVisible(!t.isVisible());
				}
			});
			try {
				SystemTray.getSystemTray().add(icon);
			} catch (final Throwable t) {
			}
		}

		//final int defaultWorld = Constants.RANDOM.nextBoolean() ? 2 : 3;
		final int defaultWorld = 2 + Constants.RANDOM.nextInt(4);

		final String str = "http://classic" + defaultWorld + ".runescape.com/";

		final String rsc_page;
		try {
			byte[] b = HTTPClient.load(str +
				"plugin.js?param=o0,a1,s0", str, true);
			rsc_page = new String(b, Constants.UTF_8);
		} catch (final Throwable t) {
				System.out.println("Error fetching RSC page: " + t.toString());
				dispose();
				return;
		}

		Map<String, String> params = HTTPClient.getParameters(rsc_page);

		client = init.createClient(this);
		((Component)client).setBackground(Color.BLACK);

		try {
			final URL url = new URL(str);
			stub = new AVStub((Applet) client, url, url, params);
		} catch (final Throwable t) {
			t.printStackTrace();
			dispose();
			return;
		}

		client.setStub(stub);

		BufferedImage image = null;
		try {
			image = ImageIO.read(new File("." + File.separator + "lib" + File.separator + "logo.png"));
		} catch (final Throwable t) {
			System.out.println("Error loading logo: " + t.toString());
		}

		final Panel sidePanel = new ImagePanel(image);
		setColours(sidePanel, true);
		sidePanel.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));

		final Dimension buttonSize = new Dimension(120, 23);

		final Choice worldChoice = new Choice();
		worldChoice.setPreferredSize(buttonSize);
		worldChoice.setForeground(SystemColor.textText);
		worldChoice.setBackground(SystemColor.text);
		worldChoice.add("World # 1");
		worldChoice.add("World # 2");
		worldChoice.add("World # 3");
		worldChoice.add("World # 4");
		worldChoice.add("World # 5");
		worldChoice.select(defaultWorld - 1);
		worldChoice.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent event) {
				updateWorld(worldChoice.getSelectedIndex() + 1);
			}
		});
		this.worldChoice = worldChoice;

		final Button chooseButton = new Button("Choose script");
		chooseButton.setPreferredSize(buttonSize);
		setButtonColours(chooseButton);
		chooseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (scriptFrame == null) {
					scriptFrame = new ScriptFrame(client);
				}
				scriptFrame.setLocationRelativeTo(BotFrame.this);
				scriptFrame.setVisible(true);
			}
		});

		startButton = new Button("Start script");
		startButton.setPreferredSize(buttonSize);
		setButtonColours(startButton);
		startButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (client.getScriptListener().isScriptRunning()) {
					stopScript();
				} else {
					startScript();
				}
			}
		});

		final Button debugButton = new Button("Debugger");
		debugButton.setPreferredSize(buttonSize);
		setButtonColours(debugButton);
		debugButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (debugger == null) {
					debugger = new Debugger(client);
				}
				debugger.setLocationRelativeTo(BotFrame.this);
				debugger.setVisible(true);
			}
		});

		final Button scrButton = new Button("Screenshot");
		scrButton.setPreferredSize(buttonSize);
		setButtonColours(scrButton);
		scrButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						takeScreenshot(String.valueOf(System.currentTimeMillis()));
					}
				}, "ScreenshotThread").start();
			}
		});

		final Button exitButton = new Button("Exit");
		exitButton.setPreferredSize(buttonSize);
		setButtonColours(exitButton);
		exitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				quit();
			}
		});

		sidePanel.add(worldChoice);
		sidePanel.add(chooseButton);
		sidePanel.add(startButton);
		sidePanel.add(debugButton);
		sidePanel.add(scrButton);
		sidePanel.add(exitButton);

		final Panel checkPanel = new Panel();
		setColours(checkPanel, true);
		checkPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 0));

		loginCheck = new Checkbox("Autologin");
		setColours(loginCheck, true);
		loginCheck.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				final IAutoLogin al = client.getAutoLogin();
				al.setEnabled(loginCheck.getState());
			}
		});

		gfxCheck = new Checkbox("Rendering", true);
		setColours(gfxCheck, true);
		gfxCheck.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				client.setRendering(gfxCheck.getState());
			}
		});
		gfxCheck.setEnabled(false);

		final Checkbox paintCheck = new Checkbox("Show bot layer",
		    true);
		setColours(paintCheck, true);
		paintCheck.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				final IPaintListener paint = client.getPaintListener();
				paint.setPaintingEnabled(paintCheck.getState());
			}
		});

		final Checkbox r3d = new Checkbox("Plain 3D", true);
		setColours(r3d, true);
		r3d.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				final IPaintListener paint = client.getPaintListener();
				paint.setRenderSolid(r3d.getState());
			}
		});

		final Checkbox t3d = new Checkbox("Textured 3D", true);
		setColours(t3d, true);
		t3d.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				final IPaintListener paint = client.getPaintListener();
				paint.setRenderTextures(t3d.getState());
			}
		});

		checkPanel.add(loginCheck);
		checkPanel.add(gfxCheck);
		checkPanel.add(paintCheck);
		checkPanel.add(r3d);
		checkPanel.add(t3d);

		((Component)client)
		    .addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				int w = ((Component)client).getWidth();
				int h = ((Component)client).getHeight();
				client.getPaintListener().doResize(w, h);
			}
		});

		add((Component)client, BorderLayout.CENTER);
		add(sidePanel, BorderLayout.EAST);

		if (cTextArea != null) {
			final Panel bottomPanel = new Panel();
			setColours(bottomPanel, true);
			bottomPanel.setLayout(new BorderLayout());
			cTextArea.setPreferredSize(new Dimension(0, 150));
			bottomPanel.add(cTextArea, BorderLayout.CENTER);
			bottomPanel.add(checkPanel, BorderLayout.SOUTH);
			add(bottomPanel, BorderLayout.SOUTH);
		} else {
			add(checkPanel, BorderLayout.SOUTH);
		}

		pack();
		setMinimumSize(getSize());

		client.init();
		stub.setActive(true);
		client.start();
	}

	private void quit() {
		client.getScriptListener().setScriptRunning(false);
		if (stub != null) {
			stub.setActive(false);
		}
		if (client != null) {
			client.stop();
		}
		final IJokerFOCR joker = client.getJoker();
		if (joker.isLibraryLoaded()) {
			joker.close();
		}
		dispose();
		System.exit(0);
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			setLocationRelativeTo(null);
			toFront();
			requestFocus();
		}
		super.setVisible(visible);
	}

	static void setColours(Component c, boolean invert) {
		c.setFont(Constants.UI_FONT);
		if (invert) {
			c.setBackground(Color.BLACK);
			c.setForeground(Color.WHITE);
		} else {
			c.setBackground(Color.WHITE);
			c.setForeground(Color.BLACK);
		}
	}

	private static void setButtonColours(Button b) {
		b.setFont(Constants.UI_FONT);
		b.setForeground(SystemColor.controlText);
		b.setBackground(SystemColor.control);
	}

	void startScript() {
		final IScriptListener listener = client.getScriptListener();
		if (listener.hasScript()) {
			listener.setScriptRunning(true);
			startButton.setLabel("Stop script");
			System.out.println(listener.getScriptName() + " started.");
		} else {
			System.out.println("No script selected!");
		}
	}

	public void stopScript() {
		final IScriptListener listener = client.getScriptListener();
		listener.setScriptRunning(false);
		startButton.setLabel("Start script");
		client.setKeysDisabled(false);
		System.out.println(listener.getScriptName() + " stopped.");
	}

	public void takeScreenshot(String fileName) {
		final String name = "." + File.separator + "Screenshots" + File.separator + fileName + ".png";
		final Image image = client.getImage();
		final BufferedImage b = new BufferedImage(image.getWidth(null),
		image.getHeight(null), BufferedImage.TYPE_INT_RGB);
		final Graphics g = b.createGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		try {
			ImageIO.write(b, "png", new File(name));
			System.out.println("Saved " + name + ".");
		} catch (final Throwable t) {
			System.out.println("Error taking screenshot: " + t.toString());
		}
	}

	public String getCodeBase() {
		return stub.getCodeBase().toString();
	}

	public void updateWorld(int i) {
		try {
			final URL url = new URL("http://classic" + i + ".runescape.com/");
			stub.setDocumentBase(url);
			stub.setCodeBase(url);
		} catch (final Throwable t) {
		}
		stub.setParameter("nodeid", String.valueOf(5000 + i));
		stub.setParameter("servertype", i == 1 ? "3" : "1");
		worldChoice.select(i - 1);
	}

	public void setAutoLogin(boolean b) {
		loginCheck.setState(b);
		client.getAutoLogin().setEnabled(b);
	}

	public void enableRenderingToggle() {
		gfxCheck.setEnabled(true);
	}
}
