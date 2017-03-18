package kr.co.jm;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Iterator;

public class RequestProcessor extends Thread {
	private ChannelQueue cQueue;
	private Selector readSelector;
	
	private ByteBuffer readBuffer;
	private Charset iso8859;
	private CharsetDecoder iso8859Decoder;
	private Charset euckr;
	private CharsetEncoder euckrEncoder;
	
	private ByteBuffer headerBuffer;
	
	public RequestProcessor(ChannelQueue cq) throws IOException {
		this.cQueue = cq;
		readSelector = Selector.open();
		cq.registerSelector(readSelector);
		
		readBuffer = ByteBuffer.allocate(1024);	
		
		// Initialize Character sets
		iso8859 = Charset.forName("iso-8859-1");
		iso8859Decoder = iso8859.newDecoder();
		euckr = Charset.forName("euc-kr");
		euckrEncoder = euckr.newEncoder();
		
		createHeaderBuffer();
	}
	
	private void createHeaderBuffer() throws CharacterCodingException {
		CharBuffer chars = CharBuffer.allocate(88);
		chars.put("HTTP/1.1 200 OK\n");
		chars.put("Connection: close\n");
		chars.put("Server: testServer\n");
		chars.put("Content-Type: text/html\n");
		chars.put("\n");
		chars.flip();
		headerBuffer = euckrEncoder.encode(chars);
	}
	
	public void run() {
		while(true) {
			try {
				processChannelQueue();
				
				System.out.println("processing");
				// process
				
				int numKeys = readSelector.select();
				if (numKeys > 0) {
					processRequest();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void processChannelQueue() throws IOException {
		SocketChannel socketChannel = null;
		while ((socketChannel = cQueue.dequeue()) != null) {
			socketChannel.configureBlocking(false);
			socketChannel.register(readSelector, SelectionKey.OP_READ, new StringBuffer());
		}
	}
	
	private void processRequest() {
		System.out.println("Processing request");
		Iterator<SelectionKey> iter = readSelector.selectedKeys().iterator();
		while (iter.hasNext()) {
			SelectionKey key = iter.next();
			iter.remove();
			
			SocketChannel socketChannel = (SocketChannel) key.channel();
			
			try {
				socketChannel.read(readBuffer);
				readBuffer.flip();
				
				String res = iso8859Decoder.decode(readBuffer).toString();
				StringBuffer requestString = (StringBuffer) key.attachment();
				requestString.append(res);
				System.out.println(requestString);
				
				readBuffer.clear();
				
				if (res.endsWith("\n\n") || res.endsWith("\r\n\r\n")) {
					completeRequest(requestString.toString(), socketChannel);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void completeRequest(String requestData, SocketChannel socketChannel) {
		try {
			headerBuffer.rewind();
			
			CharBuffer bodyBuffer = CharBuffer.allocate(64);
			bodyBuffer.put("a\n");
			bodyBuffer.flip();
			ByteBuffer body = euckrEncoder.encode(bodyBuffer);
			
			socketChannel.write(headerBuffer);
			socketChannel.write(body);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				socketChannel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}	
	}
}
