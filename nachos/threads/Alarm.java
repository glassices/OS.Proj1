package nachos.threads;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
    sleepingThreads = new java.util.PriorityQueue<TimeThreadPair>();
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
    boolean intStatus = Machine.interrupt().disable();
    
    TimeThreadPair p = sleepingThreads.peek();
    long time = Machine.timer().getTime();
    while ((p != null) && (p.time <= time)){
    	p.thread.ready();
    	sleepingThreads.poll();
    	p = sleepingThreads.peek();
    }
	KThread.yield();
	
    Machine.interrupt().restore(intStatus);
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
    boolean intStatus = Machine.interrupt().disable();
    
    long wakeTime = Machine.timer().getTime() + x;
    sleepingThreads.add(new TimeThreadPair(wakeTime, KThread.currentThread()));
    KThread.sleep();
	
    Machine.interrupt().restore(intStatus);
    }
    
    private class TimeThreadPair implements Comparable<TimeThreadPair>{
    	public TimeThreadPair(long time, KThread thread){
    		this.time = time;
    		this.thread = thread;
    	}
    	
		public int compareTo(TimeThreadPair other) {
			if (time < other.time)
				return -1;
			else if (time > other.time)
				return 1;
			else
				return thread.compareTo(other.thread);
		}
    	
    	public long time;
    	public KThread thread;
    }
   
    private java.util.PriorityQueue<TimeThreadPair> sleepingThreads = null;
}
