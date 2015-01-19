/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package bgu.dcr.az.exen.async;

import bgu.dcr.az.exen.AbstractMailer;
import bgu.dcr.az.exen.DefaultMessageQueue;

/**
 *
 * @author bennyl
 * Message queue without Message delayer [if message delayer is added, bgu.dcr.az.exen.async.DelayedMessageQueue is instantiated]
 */
public class AsyncMailer extends AbstractMailer {
   
    @Override
    protected DefaultMessageQueue generateNewMessageQueue(int agent, String groupKey) {
        return new DefaultMessageQueue();
    }

	/* (non-Javadoc)
	 * @see bgu.dcr.az.api.exen.NonBlockingMailer#forwardTime()
	 */
	@Override
	public boolean forwardTime() {
		// TODO Auto-generated method stub
		return false;
	}
}
