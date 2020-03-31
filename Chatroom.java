package chatroom;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Chatroom extends Frame {
	private static final long serialVersionUID = 6551293412586971141L;
	
//	public static final String Client_IP = "192.168.5.3";
	public static final String Client_IP = "127.0.0.1";
	public static final int Server_Port = 6666;
	public static final String Log_Storage_Path = "D:\\javase\\client\\log.txt";
//	public static final String Log_Storage_Path = "/Users/kaiqiangzhan/Documents/Java/log.txt";
	private static final String String_Shake = "%shake";
	
	private TextField ipTxt;
	private Button sendBtn;
	private Button clearBtn;
	private Button shakeBtn;
	private TextArea viewTxt;
	private TextArea sendTxt;
	
	private Client client;
	private Server server;

	public static void main(String[] args) {
		new Chatroom();
	}
	
	public Chatroom() {
		// gui
		this.init();
		this.addWindowListeners();
		this.addBtnListeners();
		// 加载聊天记录
		try {
			this.loadLog();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// 网络
		new Thread(this.server = new Server()).start();
		this.client = new Client("聊天室");
	}
	
	private void init() {
		this.setSize(400, 500);
		this.setLocation(100, 100);
		this.setLayout(new BorderLayout());
		this.createSouthPanel();
		this.createNorthPanel();
		this.setVisible(true);
	}
	
	private void createNorthPanel() {
		Panel p = new Panel();
		p.setLayout(new BorderLayout());
		this.viewTxt = new TextArea();
		this.viewTxt.setEditable(false);
		this.sendTxt = new TextArea();
		p.add(this.sendTxt, BorderLayout.SOUTH);
		p.add(this.viewTxt, BorderLayout.CENTER);
		this.add(p, BorderLayout.CENTER);
	}
	
	private void createSouthPanel() {
		Panel p = new Panel();
		this.ipTxt = new TextField(Chatroom.Client_IP, 10);
		this.sendBtn = new Button("发送");
		this.clearBtn = new Button("清屏");
		this.shakeBtn = new Button("抖动");
		p.add(ipTxt);
		p.add(this.sendBtn);
		p.add(this.clearBtn);
		p.add(this.shakeBtn);
		this.add(p, BorderLayout.SOUTH);
	}
	
	private void addWindowListeners() {
		this.addWindowListener(new WindowAdapter() {
			// 退出
			@Override
			public void windowClosing(WindowEvent e) {
				try {
					Chatroom.this.client.close();
					Chatroom.this.server.close();
				} catch (Exception e2) {

				}
				System.exit(0);
			}
		});
	}
	
	private void addBtnListeners() {
		this.sendBtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				Chatroom.this.send(null);
			}
		});
		this.clearBtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				Chatroom.this.clear();
			}
		});
		this.shakeBtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				try {
					Chatroom.this.send(Chatroom.String_Shake);
				} catch (Exception e2) {

				}
			}
		});
	}
	
	//===========================================================
	
	// 发送信息
	private void send(String msg) {
		if (msg == null)
			msg = this.sendTxt.getText();
		if (msg.equals(""))
			return;
		String ip = this.ipTxt.getText();
		if (ip.equals("")) 
			ip = "255:255:255:255";
		try {
			this.client.send(ip, Chatroom.Server_Port, msg);
			this.sendTxt.setText("");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void receive(String msg) {
		this.viewTxt.append(msg + "\r\n\r\n");
		try {
			this.storeLog(msg);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private void clear() {
		this.viewTxt.setText("");
	}
	
	private void shake() throws InterruptedException {
		int[] offsetArr = {10,-10,8,-8,6,-6,4,-4,2,-2,0};
		Point p = this.getLocation();
		int originX = p.x;
		int originY = p.y;
		for (int offset : offsetArr) {
			this.setLocation(originX, originY + offset);
			Thread.sleep(20);
		}
	}
	
	private File getLogFile() {
		File file = new File(Chatroom.Log_Storage_Path);
		if (!file.exists() || !file.isFile())
			return null;
		return file;
	}
	
	private void storeLog(String msg) throws FileNotFoundException {
		File file = this.getLogFile();
		if (file == null)
			return;
		PrintStream ps = new PrintStream(new FileOutputStream(file, true));
		ps.println(msg + "\r\n");
		ps.close();
	}
	
	private void loadLog() throws IOException {
		File file = this.getLogFile();
		if (file == null)
			return;
		BufferedReader br = new BufferedReader(new FileReader(file));
		StringBuilder log = new StringBuilder();
		String line = null;
		while ((line = br.readLine()) != null) {
			log.append(line + "\r\n");
		}
		this.viewTxt.setText(log.toString());
		br.close();
	}
	
	//========================= server =============================
	
	public class Server implements Runnable {
		
		private ServerSocket server;
		
		public void run() {
			try {
				this.server = new ServerSocket(Chatroom.Server_Port);
				while (true) {
					Socket socket = this.server.accept();
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								BufferedReader bis = new BufferedReader(new InputStreamReader(socket.getInputStream()));
								String line = null;
								StringBuilder builder = new StringBuilder();
								while ((line = bis.readLine()) != null) {
									builder.append(line + "\r\n");
								}
								String msg = builder.toString();
								if (msg.contains(Chatroom.String_Shake)) {
									Chatroom.this.shake();
									return;
								}
								Chatroom.this.receive(msg);
							} catch (IOException e) {
								e.printStackTrace();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}).start();
				}
			} catch (Exception e) {

			}
		}
		
		public void close() throws IOException {
			this.server.close();
			this.server = null;
		}

	}
	
	//=========================== client =============================
	
	public class Client {
		private Socket socket;
		private String clientName;
		
		public Client(String clientName) {
			this.clientName = clientName;
		}
		
		public void send(String ip, int port, String msg) throws Exception{
			this.socket = new Socket(ip, port);
			PrintStream ps = new PrintStream(this.socket.getOutputStream());
			String wrappedMsg = this.wrapMsg(ip, port, msg);
			ps.println(wrappedMsg);
			if (!msg.equals(Chatroom.String_Shake)) {
				Chatroom.this.storeLog(wrappedMsg);				
			}
			this.socket.close();
			this.socket = null;
		}
		
		private String wrapMsg(String ip, int port, String msg) {
			return this.getDateStr() + " " 
					+ this.clientName + " -> server: " + ip + ":" + port + "\n" 
					+ msg;
		}
		
		private String getDateStr() {
			Calendar c = new GregorianCalendar();
			int hour = c.get(Calendar.HOUR_OF_DAY);
			int min = c.get(Calendar.MINUTE);
			int sec = c.get(Calendar.SECOND);
			return (hour < 10 ? "0" + hour : "" + hour) + ":"
					+ (min < 10 ? "0" + min : "" + min) + ":"
					+ (sec < 10 ? "0" + sec : "" + sec);
		}
		
		private void close() throws IOException {
			if (this.socket != null && this.socket.isConnected()) {
				this.socket.close();
				this.socket = null;
			}
		}
	}
}
