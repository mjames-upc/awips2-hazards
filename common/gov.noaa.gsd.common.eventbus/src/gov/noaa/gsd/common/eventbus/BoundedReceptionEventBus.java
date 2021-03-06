/**
 * This software was developed and / or modified by the
 * National Oceanic and Atmospheric Administration (NOAA), 
 * Earth System Research Laboratory (ESRL), 
 * Global Systems Division (GSD), 
 * Information Services Branch (ISB)
 * 
 * Address: Department of Commerce Boulder Labs, 325 Broadway, Boulder, CO 80305
 */
package gov.noaa.gsd.common.eventbus;

import gov.noaa.gsd.common.utilities.IRunnableAsynchronousScheduler;

import java.util.concurrent.TimeUnit;

import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.bus.MessagePublication;
import net.engio.mbassy.bus.config.BusConfiguration;
import net.engio.mbassy.listener.Handler;

/**
 * Description: Event bus based on the {@link MBassador} class, but allowing
 * asynchronous dispatching of events in such a manner that the events are
 * received by handlers on one or more specified threads.
 * <p>
 * This class allows the duplication of the functionality provided by the Google
 * Guava project's <a
 * href="http://code.google.com/p/guava-libraries/wiki/EventBusExplained">
 * <code>EventBus</code></a> with respect to the ability to avoid reentrant
 * event posting. With <code>EventBus</code>, calling <code>post()</code> from
 * within code running as part of a handler of an event received from the same
 * <code>EventBus</code> automatically schedules the event to be dispatched to
 * handlers after the the current handler execution is completed.
 * </p>
 * <p>
 * The same behavior is possible with <code>MBassador</code> using the latter's
 * {@link MBassador#publishAsync} method, but with the caveat that the
 * asynchronously-published event is received on a different thread from that
 * used to publish it. This subclass allows events so published to be received
 * on a chosen thread or set of threads, as specified at bus creation time via
 * the {@link IRunnableAsynchronousScheduler} provided to the constructor.
 * </p>
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer     Description
 * ------------ ---------- ------------ --------------------------
 * Apr 04, 2014   3319     Chris.Golden Initial creation.
 * May 14, 2014   2925     Chris.Golden Corrected Javadoc.
 * Jul 03, 2014   4084     Chris.Golden Made shutdown() shut down the nested
 *                                      event bus as well as itself.
 * Aug 15, 2016  18376     Chris.Golden Fixed problem with notifications
 *                                      being published after shutdown.
 * Aug 19, 2016  16259     Chris.Golden Changed nested event bus to use only
 *                                      one each of dispatcher and handler
 *                                      threads, so as to avoid messages
 *                                      arriving out of order.
 * Aug 22, 2016  19537     Chris.Golden Added constants
 * </pre>
 * 
 * @author Chris.Golden
 * @version 1.0
 */
public class BoundedReceptionEventBus<T> extends MBassador<T> {

    // Private Static Constants

    /**
     * Number of dispatch threads allowed. More than one can occasionally cause
     * out-of-order reception by handlers of messages published asynchronously.
     */
    private static final int DISPATCH_THREAD_COUNT = 1;

    /**
     * Number of handler threads allowed. More than one can occasionally cause
     * out-of-order reception by handlers of messages published asynchronously.
     */
    private static final int HANDLER_THREAD_COUNT = 1;

    // Private Classes

    /**
     * Description: Event wrapper, used to receive events on a dispatcher thread
     * that were published asynchronously and republish them on the main thread.
     */
    private class EventWrapper {

        // Private Variables

        /**
         * Wrapped event.
         */
        private final T event;

        // Public Constructors

        /**
         * Construct a standard instance.
         * 
         * @param event
         *            Event to be wrapped.
         */
        public EventWrapper(T event) {
            this.event = event;
        }

        // Public Methods

        /**
         * Get the wrapped event.
         * 
         * @return Wrapped event.
         */
        public final T getEvent() {
            return event;
        }
    }

    // Private Variables

    /**
     * Scheduler used to republish events back on the appropriate receiver
     * threads.
     */
    private final IRunnableAsynchronousScheduler receiverThreadScheduler;

    /**
     * Event bus used to post events that were asynchronously published. The
     * reason that another event bus is used is because {@link EventWrapper}
     * cannot implement the generic type <code>T</code>, so there is no way to
     * have the main event bus handle both <code>EventWrapper</code> instances
     * and objects of generic type <code>T</code>. This nested event bus is
     * configured to have one each dispatcher and handler thread; previously,
     * with two or more of either, it was possible for out-of-order reception by
     * handlers of asynchronously-posted messages to occur.
     */
    private final MBassador<EventWrapper> asyncBus = new MBassador<>(
            BusConfiguration.Default(DISPATCH_THREAD_COUNT,
                    HANDLER_THREAD_COUNT, HANDLER_THREAD_COUNT));

    /**
     * Flag indicating whether or not the bus has been shut down.
     */
    private boolean shutdown = false;

    // Public Constructors

    /**
     * Construct a standard instance.
     * 
     * @param receiverThreadScheduler
     *            Scheduler to be used to republish events back on the
     *            appropriate receiver threads. The specified object must take
     *            {@link Runnable} objects and schedule them to be run
     *            asynchronously on said threads.
     */
    public BoundedReceptionEventBus(
            IRunnableAsynchronousScheduler receiverThreadScheduler) {
        super(BusConfiguration.Default(DISPATCH_THREAD_COUNT,
                HANDLER_THREAD_COUNT, HANDLER_THREAD_COUNT));
        this.receiverThreadScheduler = receiverThreadScheduler;

        /*
         * Tell the asynchronous bus that this bus wants to receive wrapped
         * events.
         */
        asyncBus.subscribe(this);
    }

    // Public Methods

    @Override
    public void publish(T message) {
        if (shutdown == false) {
            super.publish(message);
        }
    }

    @Override
    public MessagePublication publishAsync(T message) {
        if (shutdown) {
            return null;
        }
        return asyncBus.publishAsync(new EventWrapper(message));
    }

    @Override
    public MessagePublication publishAsync(T message, long timeout,
            TimeUnit unit) {
        if (shutdown) {
            return null;
        }
        return asyncBus.publishAsync(new EventWrapper(message), timeout, unit);
    }

    @Override
    public void shutdown() {
        asyncBus.shutdown();
        shutdown = true;
        super.shutdown();
    }

    // Private Methods

    /**
     * Receive an wrapped event that was posted on the {@link #asyncBus}. This
     * will be received on a dispatch thread created by said bus, not by this
     * bus.
     * 
     * @param wrapper
     *            Wrapper holding the event that was originally published
     *            asynchronously using this bus.
     */
    @Handler
    private void eventPostedAsynchronously(final EventWrapper wrapper) {

        /*
         * Schedule the synchronous publishing of the event in the wrapper,
         * synchronous because that will ensure that the event will be handled
         * on the thread upon which this runnable is executed.
         */
        receiverThreadScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                publish(wrapper.getEvent());
            }
        });
    }
}
