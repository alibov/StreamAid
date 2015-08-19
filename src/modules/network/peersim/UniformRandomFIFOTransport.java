/* Copyright (c) 2003-2005 The BISON Project
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 675 Mass Ave, Cambridge, MA 02139, USA. */
package modules.network.peersim;

import java.util.HashMap;
import java.util.Map;

import peersim.config.Configuration;
import peersim.config.IllegalParameterException;
import peersim.core.CommonState;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import peersim.transport.Transport;
import utils.Utils;

/**
 * Implement a transport layer that reliably delivers messages with a random
 * delay, that is drawn from the configured interval according to the uniform
 * distribution.
 *
 * @author Alberto Montresor
 * @version $Revision: 1.14 $
 */
public final class UniformRandomFIFOTransport implements Transport {
  // ---------------------------------------------------------------------
  // Parameters
  // ---------------------------------------------------------------------
  /**
   * String name of the parameter used to configure the minimum latency.
   *
   * @config
   */
  private static final String PAR_MINDELAY = "mindelay";
  /**
   * String name of the parameter used to configure the maximum latency.
   * Defaults to {@value #PAR_MINDELAY}, which results in a constant delay.
   *
   * @config
   */
  private static final String PAR_MAXDELAY = "maxdelay";
  // ---------------------------------------------------------------------
  // Fields
  // ---------------------------------------------------------------------
  /** Minimum delay for message sending */
  private final long min;
  /**
   * Difference between the max and min delay plus one. That is, max delay is
   * min+range-1.
   */
  private final long range;

  // ---------------------------------------------------------------------
  // Initialization
  // ---------------------------------------------------------------------
  /**
   * Reads configuration parameter.
   */
  public UniformRandomFIFOTransport(final String prefix) {
    min = Configuration.getLong(prefix + "." + PAR_MINDELAY);
    final long max = Configuration.getLong(prefix + "." + PAR_MAXDELAY, min);
    if (max < min) {
      throw new IllegalParameterException(prefix + "." + PAR_MAXDELAY,
          "The maximum latency cannot be smaller than the minimum latency");
    }
    range = max - min + 1;
  }

  // ---------------------------------------------------------------------
  /**
   * Returns <code>this</code>. This way only one instance exists in the system
   * that is linked from all the nodes. This is because this protocol has no
   * node specific state.
   */
  @Override public Object clone() {
    return this;
  }

  // ---------------------------------------------------------------------
  // Methods
  // ---------------------------------------------------------------------
  private final Map<Node, Map<Node, Long>> sentTimes = new HashMap<Node, Map<Node, Long>>();

  /**
   * Delivers the message with a random delay, that is drawn from the configured
   * interval according to the uniform distribution.
   */
  @Override public void send(final Node src, final Node dest, final Object msg, final int pid) {
    // avoid calling nextLong if possible
    long newMin = min;
    long newRange = range;
    if (sentTimes.containsKey(src) && sentTimes.get(src).containsKey(dest)) {
      final long minTime = sentTimes.get(src).get(dest);
      if (minTime < Utils.getTime() + min) {
        sentTimes.get(src).remove(dest);
        if (sentTimes.get(src).isEmpty()) {
          sentTimes.remove(src);
        }
      } else {
        newMin += minTime - Utils.getTime() - min + 1;
        newRange -= minTime - Utils.getTime() - min + 1;
      }
    }
    final long delay = (newRange <= 1 ? newMin : newMin + CommonState.r.nextLong(newRange));
    EDSimulator.add(delay, msg, dest, pid);
    Utils.checkExistence(sentTimes, src, new HashMap<Node, Long>());
    sentTimes.get(src).put(dest, Utils.getTime() + delay);
  }

  /**
   * Returns a random delay, that is drawn from the configured interval
   * according to the uniform distribution.
   */
  @Override public long getLatency(final Node src, final Node dest) {
    return (range == 1 ? min : min + CommonState.r.nextLong(range));
  }
}
