/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package bgu.dcr.az.exen.async;

import bgu.dcr.az.api.Agent;
import bgu.dcr.az.api.Message;
import bgu.dcr.az.api.exen.MessageQueue;
import bgu.dcr.az.api.exp.InternalErrorException;
import bgu.dcr.az.api.exen.mdef.MessageDelayer;
import bgu.dcr.az.api.tools.IdleDetector;
import bgu.dcr.az.exen.DefaultMessageQueue;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author bennyl
 * Message queue when adding Message delayer [if not added, bgu.dcr.az.exen.async.AsyncMailer is instantiate]
 */
public class DelayedMessageQueue implements MessageQueue {
	
	/* debug */
	boolean debug = false;

    private PriorityBlockingQueue<Message> futureQ;
    private PriorityBlockingQueue<Message> q;
    private Semaphore count = new Semaphore(0);
    private MessageDelayer dman;
    private boolean agentFinished = false;
    private IdleDetector timeSwitchDetector;
    private AsyncDelayedMailer parent;
    private String groupKey;
    private int agent;

    public DelayedMessageQueue(final MessageDelayer dman, AsyncDelayedMailer parent, IdleDetector timeSwitchDetector, String groupKey, int agent) {
        this.agent = agent;
        this.parent = parent;
        this.groupKey = groupKey;
        this.timeSwitchDetector = timeSwitchDetector;
        this.dman = dman;
        MessageTimeComparator mtc = new MessageTimeComparator(dman);

        this.q = new PriorityBlockingQueue<Message>(1000, mtc);
        this.futureQ = new PriorityBlockingQueue<Message>(1000, mtc);
    }

    @Override
    public void onAgentFinish() {
    	if(debug){
    		System.out.println("onAgentFinish");
    	}
        agentFinished = true;
        parent.updateAgentActiveGroup(agent, groupKey);
        timeSwitchDetector.notifyAgentIdle();
    }

    /**
     * release from the internal queue all the messages with the time lower or
     * equal then the given time
     *
     * @param time
     */
    public void release(long time) {
        Message peeked;
        while ((peeked = futureQ.peek()) != null && dman.extractTime(peeked) <= time) {
            Message msg = futureQ.poll();
            if (msg == null) {
                return;
            }

            long mtime = dman.extractTime(msg);
            if (mtime <= time) {
                add(msg);
            } else {
                futureQ.offer(msg);
                return;
            }
        }
    }

    @Override
    public Message take() throws InterruptedException {
        parent.updateAgentActiveGroup(agent, groupKey);
        waitForNewMessages(); //also handles idle detection..
        Message ret = q.take();
        count.acquire();
        if (ret == DefaultMessageQueue.SYSTEM_RELEASE_MESSAGE) {
            return null;
        }
        return ret;
    }

    /**
     * will try to add the message if the time is right else will put it in the
     * internal q
     * 
     * @param e
     * @param time
     * Modified by Olivia
     * Do not delay the system msgs: SYS_TERMINATION_MESSAGE
     */
    public void tryAdd(Message e, long time) {

        String msgName = e.getName();
        if(!msgName.equals("__TERMINATE__") &&
        		!msgName.equals("__TICK__") &&
        		!msgName.equals("__TIMEOUT__") &&
        		!msgName.equals("DONE") &&
        		!msgName.equals("SET_CHILD") &&
        		!msgName.equals("ADD_ANCESTORS") &&
        		!msgName.equals("SET_PSAUDO_CHILD") &&
        		!msgName.equals("REFUSE_VISIT") 
        		){
        	long mtime = dman.extractTime(e);
            if (mtime <= time) {
                add(e);
            } else {
                futureQ.offer(e);
            }

        }
        else{
        	if(debug){
        		System.out.println("ADD MSG: " + msgName + "from agent " + e.getSender());
        	}
        	add(e);
        }

    }

    @Override
    public void add(Message e) {

        if (!q.offer(e)) {
            throw new InternalErrorException("cannot insert message " + e + " to agent queue");
        }

        count.release();
    }
    
    /**
     * @author Olivia
     * @return
     */
    public PriorityBlockingQueue<Message> getDelayedQueue(){
    	return futureQ;
    }

    @Override
    public int availableMessages() {
        if (agentFinished) {
            return 0;
        }
        return q.size();
    }

    @Override
    public void waitForNewMessages() throws InterruptedException {
        if (isEmpty()) {
            timeSwitchDetector.notifyAgentIdle();
            count.acquire();
            count.release();
            timeSwitchDetector.notifyAgentWorking();
        }
    }

    @Override
    public boolean isEmpty() {
        return this.q.isEmpty();
    }

    @Override
    public boolean isNotEmpty() {
        return !this.q.isEmpty();
    }

    /**
     * @author Olivia
     */
    @Override
    public boolean delayedQueueIsEmpty() {
        return this.futureQ.isEmpty();
    }
    /**
     * @return from all the messages in the innerq the minimum message time - or
     * null if the innerq is empty.
     */
    public Long minimumMessageTime() {
        if (agentFinished) {
            return null;
        }
        Message m = futureQ.peek();
        if (m == null) {
            return null;
        }

        return dman.extractTime(m);
    }

    @Override
    public void releaseBlockedAgent() {
        add(DefaultMessageQueue.SYSTEM_RELEASE_MESSAGE);
    }

    public static class MessageTimeComparator implements Comparator<Message> {

        MessageDelayer dman;

        public MessageTimeComparator(MessageDelayer dman) {
            this.dman = dman;
        }

        @Override
        public int compare(Message o1, Message o2) {
            if (o1 == DefaultMessageQueue.SYSTEM_RELEASE_MESSAGE) {
                return -1;
            }
            if (o2 == DefaultMessageQueue.SYSTEM_RELEASE_MESSAGE) {
                return 1;
            }
            if ((o1 == o2) && (o1 == DefaultMessageQueue.SYSTEM_RELEASE_MESSAGE)) {
                return 0;
            }

            Long time1 = null, time2 = null;
            try {
                time1 = dman.extractTime(o1);
            } catch (Exception ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                } else {
                    throw new InternalErrorException("Cannot take time from message " + o1 + " cannot perform message delay.", ex);
                }
            }

            try {
                time2 = dman.extractTime(o2);
            } catch (Exception ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                } else {
                    throw new InternalErrorException("Cannot take time from message " + o2 + " cannot perform message delay.", ex);
                }
            }

            return (int) (time1 - time2);
        }
    }
}
