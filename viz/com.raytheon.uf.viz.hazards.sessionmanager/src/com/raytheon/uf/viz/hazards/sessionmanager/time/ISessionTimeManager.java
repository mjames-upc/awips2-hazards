/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.viz.hazards.sessionmanager.time;

import gov.noaa.gsd.common.utilities.ICurrentTimeProvider;

import java.util.Date;

import com.raytheon.uf.common.time.TimeRange;
import com.raytheon.uf.viz.hazards.sessionmanager.originator.IOriginator;

/**
 * Manages selected, current, and visible times for a session.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 21, 2013 1257       bsteffen     Initial creation
 * May 12, 2014 2925       Chris.Golden Added originator to visible time
 *                                      range change notification, and
 *                                      added current time provider and
 *                                      getter.
 * Nov 18, 2014 4124       Chris.Golden Reorganized and changed over to
 *                                      use a SelectedTime object to
 *                                      represent both single selected
 *                                      time instances, and selected
 *                                      time ranges.
 * Nov 10, 2015 12762      Chris.Golden Added ability to schedule arbitrary
 *                                      tasks to run at regular intervals.
 * Apr 01, 2016 16225      Chris.Golden Added ability to cancel tasks that
 *                                      are scheduled to run at regular
 *                                      intervals.
 * Apr 25, 2016 18129      Chris.Golden Changed time-interval-triggered
 *                                      tasks to be triggered close to the
 *                                      instant when the CAVE current time
 *                                      ticks over to a new minute.
 * Oct 19, 2016 21873      Chris.Golden Removed unneeded constant definition.
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */

public interface ISessionTimeManager {

    // Public Methods

    /**
     * Get the current time provider.
     * 
     * @return Current time provider.
     */
    public ICurrentTimeProvider getCurrentTimeProvider();

    /**
     * Get the current system time, as an epoch time in milliseconds.
     * 
     * @return Current system time, as an epoch time in milliseconds.
     */
    public long getCurrentTimeInMillis();

    /**
     * Get the current system time.
     * 
     * @return Current system time.
     */
    public Date getCurrentTime();

    /**
     * Get the lower bound of the selected time range, as an epoch time in
     * milliseconds.
     * 
     * @return Lower bound of the selected time range, as an epoch time in
     *         milliseconds.
     */
    public long getLowerSelectedTimeInMillis();

    /**
     * Get the upper bound of the selected time range, as an epoch time in
     * milliseconds.
     * 
     * @return Upper bound of the selected time range, as an epoch time in
     *         milliseconds.
     */
    public long getUpperSelectedTimeInMillis();

    /**
     * Get the selected time.
     * 
     * @return Selected time.
     */
    public SelectedTime getSelectedTime();

    /**
     * Set the selected time.
     * 
     * @param selectedTime
     *            New selected time.
     * @param originator
     *            Originator of the action.
     */
    public void setSelectedTime(SelectedTime selectedTime,
            IOriginator originator);

    /**
     * Get the lower bound of the visible time range, as an epoch time in
     * milliseconds.
     * 
     * @return Lower bound of the visible time range, as an epoch time in
     *         milliseconds.
     */
    public long getLowerVisibleTimeInMillis();

    /**
     * Get the upper bound of the visible time range, as an epoch time in
     * milliseconds.
     * 
     * @return Upper bound of the visible time range, as an epoch time in
     *         milliseconds.
     */
    public long getUpperVisibleTimeInMillis();

    /**
     * Get the range of times that should be visible to the user.
     * 
     * @return Range of times that should be visible to the user.
     */
    public TimeRange getVisibleTimeRange();

    /**
     * Set the range of times that should be visible to the user.
     * 
     * @param timeRange
     *            New range of times that should be visible to the user.
     * @param originator
     *            Originator of the action.
     */
    public void setVisibleTimeRange(TimeRange timeRange, IOriginator originator);

    /**
     * Execute the specified runnable at the given regular intervals.
     * <p>
     * Note that any runnable provided will be executed in the following
     * circumstances once this method is called:
     * </p>
     * <ol>
     * <li>Shortly after this method is executed (initial run).</li>
     * <li>Whenever the CAVE clock time is changed (frozen, unfrozen, or set to
     * a new value).</li>
     * <li>If the CAVE clock time is not frozen, at the specified regular
     * intervals starting from the last occurrence of either (1) or (2).</li>
     * </ol>
     * 
     * @param task
     *            Task to be executed at regular intervals.
     * @param intervalInMinutes
     *            Interval in minutes between executions.
     */
    public void runAtRegularIntervals(Runnable task, int intervalInMinutes);

    /**
     * Cancel the execution at regular intervals of the specified runnable, the
     * latter of which was previously scheduled using the
     * {@link #runAtRegularIntervals(Runnable, long)} method.
     * 
     * @param task
     *            Task to have its regular execution cancelled.
     */
    public void cancelRunAtRegularIntervals(Runnable task);

    /**
     * Execute any shutdown needed.
     */
    public void shutdown();

}
