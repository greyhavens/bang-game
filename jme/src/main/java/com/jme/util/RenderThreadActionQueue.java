package com.jme.util;

import java.util.ArrayList;

/**
 * 
 * @author Joshua Slack
 * 
 * @deprecated Replaced by {@link GameTaskQueue}
 */
@Deprecated
public class RenderThreadActionQueue {

    /**
     * @deprecated Replaced by {@link GameTaskQueue}
     */
    @Deprecated
	protected static final ArrayList<RenderThreadExecutable> queue = new ArrayList<RenderThreadExecutable>();

    /**
     * @deprecated Replaced by {@link GameTaskQueue}
     * @param qItem -
     */
    @Deprecated
	public static void addToQueue(RenderThreadExecutable qItem) {
        queue.add(qItem);
    }

    /**
     * @deprecated v
     * @return -
     */
    @Deprecated
	public static boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * @deprecated Replaced by {@link GameTaskQueue}
     */
    @Deprecated
	public static void processQueueItem() {
        if (!isEmpty())
            queue.remove(0).doAction();
    }
}
