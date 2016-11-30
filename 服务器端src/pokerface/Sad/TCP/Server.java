package pokerface.Sad.TCP;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class Server {
	static String heatbeatMsg = "heartbeating";
	ServerSocket server = null;
	Socket pcClient = null;
	Socket webClient = null;
	boolean pcConnectState = false;
	public static void main(String[] args) throws IOException {
		Server s = new Server();
		System.out.println("wait for Web part...");
		s.acceptWeb();//�ȴ�Web����
		//������������
		System.out.println("JARVIS Service Start Up Normally......");
		s.acceptPC();//�ȴ�PC����
		Thread webMonitorThread = new Thread(new WebMonitor(s));
		Thread clientMonitorThead = new Thread(new ClientMonitor(s));
		webMonitorThread.start();
		clientMonitorThead.start();
		while(true)
		{
			if((!webMonitorThread.isAlive())&&(!clientMonitorThead.isAlive()))
			{
				s.acceptPC(); //�ȴ����߳���ֹ��PC���ѶϿ����ӣ��ȴ�PC�ٴ�����
				webMonitorThread = new Thread(new WebMonitor(s));
				clientMonitorThead = new Thread(new ClientMonitor(s));
				webMonitorThread.start();
				clientMonitorThead.start();
			}
		}
		
	}
	public Server() throws IOException {
		server = new ServerSocket(10001);
	}
	//�ȴ�PC����
	public void acceptPC() throws IOException{
		System.out.println("wait for PC connect......");
		pcClient = server.accept();
		System.out.println("PC :"+pcClient.getInetAddress()+" connect");
		pcConnectState = true;
	}
	//�ȴ�WebӦ������
	public void acceptWeb() throws IOException{
		webClient = server.accept();
		System.out.println("Web :"+webClient.getInetAddress()+" connect");
	}
	public void sendOrder(String msg) throws IOException{
		OutputStream os = this.pcClient.getOutputStream();
		os.write(msg.getBytes());
	}
	
	//��webӦ�ô���������,�����ȴ�ָ��ֱ��PC�˶Ͽ�Ϊֹ
	public String receiveOrder() throws IOException{
		this.webClient.setSoTimeout(5000);//���ó�ʱ���ԣ���ֹ������read������
		InputStream is = this.webClient.getInputStream();
		byte[] buf = new byte[1024];
		Integer len = null;
		String msg = null;
		//��PC�������ӣ���һֱ�ȴ�Web�˴���ָ��
		while(this.pcConnectState)
		{
			try {
				if((len=is.read(buf))!=-1)
				{
					msg = new String(buf, 0, len);
					return msg;
				}
			} catch (SocketTimeoutException e) {
				//ÿ��������һ���ж�PC���Ƿ�����
			}
		}
		return null;
	}
	public void close(){
		if(this.pcClient!=null)
		{
			try {
				this.pcClient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			this.server.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public boolean isConnected(){
        try{
        	/*
        	 * �˷��ͽ������ݵķ�����Windows�����»�����쳣
        	 * this.pcClient.sendUrgentData(0xff);
        	 * */
        	this.sendOrder(Server.heatbeatMsg);
        	return true;
        }catch(Exception e){
            return false;
        }
}
}
//�ȴ�Web�����߳�
class WebMonitor implements Runnable{
	Server Server = null;
	public WebMonitor(Server Server) {
		this.Server = Server;
	}
	public void run() {
		try {
			
			String order = null;
			
			while(true)
			{
				if(!Server.isConnected())
				{
					//��PC���ѶϿ�����ֹ�߳�
					return;
				}
				
				//��������Web��ָ��ֱ��PC�˶Ͽ��򷵻�null
				order = Server.receiveOrder();
				if(order==null) 
				{
					System.out.println("�ȴ�Web�����߳���ֹ");
					return; //��PC���ѶϿ�����ֹ�߳�
				}
				Server.sendOrder(order);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
//�ȴ��ͻ��˹ر��߳�
class ClientMonitor implements Runnable{

	Server Server = null;
	public ClientMonitor(Server s) {
		this.Server = s;
	}
	public void run() {

		while(Server.isConnected()){
			//���PC���Ƿ�����
			try {
				Thread.currentThread().sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}	
		System.out.println("PC��������");
		this.Server.pcConnectState = false;
		System.out.println("�ȴ��ͻ��˹ر��߳���ֹ");
	}
}
