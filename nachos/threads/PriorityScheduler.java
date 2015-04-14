package nachos.threads;

import nachos.machine.*;

import java.util.ArrayList;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }
    
    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
	return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	Lib.assertTrue(priority >= priorityMinimum &&
		   priority <= priorityMaximum);
	
	getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
	boolean intStatus = Machine.interrupt().disable();
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMaximum)
	    return false;

	setPriority(thread, priority+1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    public boolean decreasePriority() {
	boolean intStatus = Machine.interrupt().disable();
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMinimum)
	    return false;

	setPriority(thread, priority-1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
	if (thread.schedulingState == null)
	    thread.schedulingState = new ThreadState(thread);

	return (ThreadState) thread.schedulingState;
    }
	
    /**
     * 
     * Implements a heap. Since the heap must support the change of key value from outside this class,
     * we do not use java's builtin functionalities.
     *
     */
	protected class ThreadHeap{
		public ThreadHeap(){
			heap = new ArrayList<ThreadState>();
		}
		
		public void swap(int index1, int index2){
			heap.get(index1).heapIndex = index2;
			heap.get(index2).heapIndex = index1;
			ThreadState t = heap.get(index1);
			heap.set(index1, heap.get(index2));
			heap.set(index2, t);
		}
		
		public void up(int index){
			while (index > 0){
				if (heap.get(index).compareTo(heap.get((index - 1) / 2)) >= 0)
					break;
				swap(index, (index - 1) / 2);
				index = (index - 1) / 2;
			}
		}
		
		public void down(int index){
			while (index * 2 + 2 < heap.size()){
				if (heap.get(index * 2 + 1).compareTo(heap.get(index * 2 + 2)) < 0)
					if (heap.get(index * 2 + 1).compareTo(heap.get(index)) < 0){
						swap(index, index * 2 + 1);
						index = index * 2 + 1;
					}
					else
						break;
				else
					if (heap.get(index * 2 + 2).compareTo(heap.get(index)) < 0){
						swap(index, index * 2 + 2);
						index = index * 2 + 2;
					}
					else
						break;
			}
			if (index * 2 + 1 < heap.size())
				if (heap.get(index * 2 + 1).compareTo(heap.get(index)) < 0)
					swap(index, index * 2 + 1);
		}
		
		public ThreadState peek(){
			if (heap.isEmpty())
				return null;
			while (heap.get(0).refresh())
				down(0);
			return heap.get(0);
		}
		
		public ThreadState pop(){
			if (heap.isEmpty())
				return null;
			while (heap.get(0).refresh())             // force the root element of the heap to recompute
				down(0);
			ThreadState ret = heap.get(0);
			if (heap.size() == 1)
				heap.clear();
			else{
				swap(0, heap.size() - 1);
				heap.remove(heap.size() - 1);
				down(0);
			}
			return ret;
		}
		
		public void push(ThreadState state){
			state.heapIndex = heap.size();
			heap.add(state);
			up(heap.size() - 1);
		}
		
		ArrayList<ThreadState> heap = null;
	}

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
	PriorityQueue(boolean transferPriority) {
	    this.transferPriority = transferPriority;
	    heap = new ThreadHeap();
	}

	public void waitForAccess(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).waitForAccess(this);
	}

	public void acquire(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).acquire(this);
	}

	public KThread nextThread() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    if (threadHoldingResource != null){
	    	threadHoldingResource.setDirtyBit(priorityMinimum - 1);
	    	threadHoldingResource = null;
	    }
	    ThreadState t = heap.pop();
		// System.out.print("Queue " + toString() + ": thread with highest priority popped: " + (t == null ? "null" : t.thread.toString()) + "\n");
	    if (t == null)
	    	return null;
	    else{
		    t.waitQueue = null;
	    	return t.thread;
	    }
	}

	/**
	 * Return the next thread that <tt>nextThread()</tt> would return,
	 * without modifying the state of this queue.
	 *
	 * @return	the next thread that <tt>nextThread()</tt> would
	 *		return.
	 */
	protected ThreadState pickNextThread() {
		// System.out.print("Queue " + toString() + ": pick next thread\n");
		ThreadState ret = heap.peek();
		// System.out.print("Queue " + toString() + ": thread with highest priority peeked: " + (ret == null ? "null" : ret.thread.toString()) + "\n");
		return ret;
	}
	
	public void print() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    // implement me (if you want)
	}
	
	public int getEffectivePriority(){
		ThreadState t = heap.peek();
		if (t == null)
			return priorityMinimum - 1;
		else
			return t.getEffectivePriority();
	}

	/**
	 * <tt>true</tt> if this queue should transfer priority from waiting
	 * threads to the owning thread.
	 */
	public boolean transferPriority;
	
	public ThreadHeap heap = null;
	
	public ThreadState threadHoldingResource = null;
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState implements Comparable<ThreadState>{
	/**
	 * Allocate a new <tt>ThreadState</tt> object and associate it with the
	 * specified thread.
	 *
	 * @param	thread	the thread this state belongs to.
	 */
	public ThreadState(KThread thread) {
	    this.thread = thread;
	    queuesHeld = new ArrayList<PriorityQueue>();
	    
	    setPriority(priorityDefault);
	}

	/**
	 * Return the priority of the associated thread.
	 *
	 * @return	the priority of the associated thread.
	 */
	public int getPriority() {
	    return priority;
	}

	/**
	 * Return the effective priority of the associated thread.
	 *
	 * @return	the effective priority of the associated thread.
	 */
	public int getEffectivePriority() {
		refresh();
	    return priorityCache;
	}
	
	/**
	 *  recomputes the priority cache, returns true if the cache is changed.
	 *
	 */
	public boolean refresh(){
		if (!dirtyBit)
			return false;
		int k = 0;
		int old = priorityCache;
		priorityCache = priority;
		dirtyBit = false;
		while (k < queuesHeld.size()){
			if (queuesHeld.get(k).threadHoldingResource != this)       // Lazy-deletion. When as thread releases a resource, it does not
				if (k != queuesHeld.size())                            // immediately remove the corresponding queue from its record.
					queuesHeld.set(k, queuesHeld.remove(queuesHeld.size() - 1));
				else
					queuesHeld.remove(queuesHeld.size() - 1);
			else{
				int t = queuesHeld.get(k).getEffectivePriority();
				if (t > priorityCache)
					priorityCache = t;
				k++;
			}
		}
		// System.out.print("Thread " + this.thread.toString() + ": cache refreshed, effective priority " + old + " -> " + priorityCache + "\n");
		return (priorityCache != old);
	}

	/**
	 * Set the priority of the associated thread to the specified value.
	 *
	 * @param	priority	the new priority.
	 */
	public void setPriority(int priority) {
	    if (this.priority == priority)
		return;
	    
		// System.out.print("Thread " + this.thread.toString() + ": priority set to " + priority + "\n");
	    if (priority >= this.priority){                   // may only cause effective priority to increase
		    this.priority = priority;
		    receiveDonation(priority);
	    }
	    else if (this.priority == priorityCache){         // may only cause effective priority to decrease
	    	setDirtyBit(priorityCache);
	    	this.priority = priority;
	    }
	}
	
	public void receiveDonation(int priority){
		// System.out.print("Thread " + this.thread.toString() + ": donated priority " + priority + " received\n");
		if (priority > priorityCache){                              // priority cache increased
			// System.out.print("Thread " + this.thread.toString() + ": cached effective priority " + priorityCache + " -> " + priority + "\n");
			// System.out.print("Thread " + this.thread.toString() + ": dirty bit cleared\n");
			priorityCache = priority;
			dirtyBit = false;
			if (waitQueue != null){
				waitQueue.heap.up(heapIndex);
				if ((waitQueue.transferPriority) && (waitQueue.threadHoldingResource != null))
					waitQueue.threadHoldingResource.receiveDonation(priority);
			}
		}
		else if ((priority == priorityCache) && (dirtyBit)){         // priority cache not increased, but could clear dirty bit
			// System.out.print("Thread " + this.thread.toString() + ": dirty bit cleared\n");
			dirtyBit = false;
			if (waitQueue != null)
				if ((waitQueue.transferPriority) && (waitQueue.threadHoldingResource != null))
					waitQueue.threadHoldingResource.receiveDonation(priority);
		}
	}
	
	public void setDirtyBit(int priority){                           // set dirty bit only when the argument matchs the priority cache
		// System.out.print("Thread " + this.thread.toString() + ": diry bit set\n");
		if ((!dirtyBit) && ((priority == priorityCache) || (priority == priorityMinimum - 1))){
			dirtyBit = true;
			if (waitQueue != null)
				if ((waitQueue.transferPriority) && (waitQueue.threadHoldingResource != null))
					waitQueue.threadHoldingResource.setDirtyBit(priority);
		}
	}

	/**
	 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
	 * the associated thread) is invoked on the specified priority queue.
	 * The associated thread is therefore waiting for access to the
	 * resource guarded by <tt>waitQueue</tt>. This method is only called
	 * if the associated thread cannot immediately obtain access.
	 *
	 * @param	waitQueue	the queue that the associated thread is
	 *				now waiting on.
	 *
	 * @see	nachos.threads.ThreadQueue#waitForAccess
	 */
	public void waitForAccess(PriorityQueue waitQueue) {
		// System.out.print("Thread " + this.thread.toString() + ": put in queue " + waitQueue.toString() + "\n");
		time = Machine.timer().getTime();
		// System.out.print(time + "\n");
		this.waitQueue = waitQueue;
		refresh();
		waitQueue.heap.push(this);
		if ((waitQueue.transferPriority) && (waitQueue.threadHoldingResource != null)){
			waitQueue.threadHoldingResource.receiveDonation(priorityCache);
		}
	}

	/**
	 * Called when the associated thread has acquired access to whatever is
	 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
	 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
	 * <tt>thread</tt> is the associated thread), or as a result of
	 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
	 *
	 * @see	nachos.threads.ThreadQueue#acquire
	 * @see	nachos.threads.ThreadQueue#nextThread
	 */
	public void acquire(PriorityQueue waitQueue) {
		// System.out.print("Thread " + this.thread.toString() + ": aquried resource guarded by queue " + waitQueue.toString() + "\n");
		waitQueue.threadHoldingResource = this;
		ThreadState t = waitQueue.heap.peek();
		if (t != null)
			this.receiveDonation(t.getEffectivePriority());
	}	
	
	public int compareTo(ThreadState other){
		if (priorityCache > other.priorityCache)
			return -1;
		else if (priorityCache < other.priorityCache)
			return 1;
		else if (time < other.time)
			return -1;
		else if (time > other.time)
			return 1;
		else
			return thread.compareTo(other.thread);
	}

	/** The thread with which this object is associated. */	   
	protected KThread thread;
	/** The priority of the associated thread. */
	protected int priority;
	
	protected int priorityCache;
	
	protected boolean dirtyBit;
	
	// each thread should remember its position in its waitQueue's heap
	// so it can raise itself up the heap when its priority cache increases
	protected int heapIndex;
	
	protected long time;
	
	protected PriorityQueue waitQueue;
	
	// all the queues which are guarding some resource
	// that this thread is holding
	protected ArrayList<PriorityQueue> queuesHeld = null;
    }
}
