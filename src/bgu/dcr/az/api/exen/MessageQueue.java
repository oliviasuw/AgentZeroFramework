/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bgu.dcr.az.api.exen;

import java.util.concurrent.PriorityBlockingQueue;

import bgu.dcr.az.api.Message;

/**
 *
 * @author bennyl
 */
public interface MessageQueue extends NonBlockingMessageQueue {

    void waitForNewMessages() throws InterruptedException;

    boolean isNotEmpty();

    /**
     * will cause the agent that is waiting for new messages to awake and take the message 'null'
     */
    void releaseBlockedAgent();
    
    /**
     * will get called when the agent finish
     */
    void onAgentFinish();

	/**
	 * @author Olivia
	 * @return
	 */
	boolean delayedQueueIsEmpty();
	
    /**
     * @author Olivia
     * @return
     */
    public PriorityBlockingQueue<Message> getDelayedQueue();
}
