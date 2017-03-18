package kr.co.jm;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;

public class HttpServer extends Thread {
	
	private int port;
	private ChannelQueue cQueue;
	
	private Selector acceptSelector;
	private ServerSocketChannel ssc;
	
	public HttpServer(ChannelQueue cq, int port) throws IOException {
		this.port = port;
		this.cQueue = cq;
		
		acceptSelector = Selector.open();
		ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);
		
		InetSocketAddress address = new InetSocketAddress(port);
		ssc.socket().bind(address);
		ssc.register(acceptSelector, SelectionKey.OP_ACCEPT); // ServerSocketChannel can register only OP_ACCEPT (because it is server socket!)
	}
	
	public void run() {
		try {
			while (true) {
				System.out.println("running..");
				int numKeys = acceptSelector.select(); // blocked until new event will come.
				if (numKeys > 0) {
					Iterator<SelectionKey> iter = acceptSelector.selectedKeys().iterator();
					
					while (iter.hasNext()) {
						SelectionKey key = iter.next();
						iter.remove();
						
						ServerSocketChannel readyChannel = (ServerSocketChannel) key.channel();
						SocketChannel incomingChannel = readyChannel.accept();
						
						System.out.println("connected");
						
						cQueue.enqueue(incomingChannel);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				ssc.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		int port = sc.nextInt();
		ChannelQueue cq = new ChannelQueue();
		
		try {
			HttpServer hs = new HttpServer(cq, port);
			RequestProcessor pr = new RequestProcessor(cq);
			
			hs.start();
			pr.start();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
