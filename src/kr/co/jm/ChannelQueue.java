package kr.co.jm;

import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

public class ChannelQueue {
	
	private LinkedList<SocketChannel> queue;
	Selector readSelector;
	
	public ChannelQueue() {
		queue = new LinkedList<SocketChannel>();
	}
	
	public void registerSelector(Selector rs) {
		readSelector = rs;
	}
	
	public void enqueue(SocketChannel sc) {
		queue.add(sc);
		readSelector.wakeup();
	}
	
	public SocketChannel dequeue() {
		return queue.poll();
	}

}
