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

import java.io.Serializable;

import peersim.config.Configuration;
import peersim.core.Cleanable;
import peersim.core.CommonState;
import peersim.core.Fallible;
import peersim.core.Network;
import peersim.core.Node;
import peersim.core.Protocol;

/**
 * This is the default {@link Node} class that is used to compose the
 * {@link Network}.
 */
public class MyGeneralNode implements Node, Serializable {
  // ================= fields ========================================
  // =================================================================
  private static final long serialVersionUID = -1012544929392718404L;
  /** used to generate unique IDs */
  private static long counterID = -1;
  /**
   * The protocols on this node.
   */
  transient protected Protocol[] protocol = null;
  /**
   * The current index of this node in the node list of the {@link Network}. It
   * can change any time. This is necessary to allow the implementation of
   * efficient graph algorithms.
   */
  private int index;
  /**
   * The fail state of the node.
   */
  protected int failstate = Fallible.OK;
  /**
   * The ID of the node. It should be final, however it can't be final because
   * clone must be able to set it.
   */
  private long ID;
  
  // public final String prefix;
  // ================ constructor and initialization =================
  // =================================================================
  /**
   * Used to construct the prototype node. This class currently does not have
   * specific configuration parameters and so the parameter <code>prefix</code>
   * is not used. It reads the protocol components (components that have type
   * {@value peersim.core.Node#PAR_PROT}) from the configuration.
   * 
   * @param prefix
   *          useless
   */
  public MyGeneralNode(final String prefix) {
    // this.prefix = prefix;
    final String[] names = Configuration.getNames(PAR_PROT);
    CommonState.setNode(this);
    ID = nextID();
    protocol = new Protocol[names.length];
    for (int i = 0; i < names.length; i++) {
      CommonState.setPid(i);
      final Protocol p = (Protocol) Configuration.getInstance(names[i]);
      protocol[i] = p;
    }
  }
  
  // -----------------------------------------------------------------
  @Override public Object clone() {
    MyGeneralNode result = null;
    try {
      result = (MyGeneralNode) super.clone();
    } catch (final CloneNotSupportedException e) {/* never happens */
      throw new RuntimeException(e);
    }
    result.protocol = new Protocol[protocol.length];
    CommonState.setNode(result);
    result.ID = nextID();
    for (int i = 0; i < protocol.length; ++i) {
      CommonState.setPid(i);
      result.protocol[i] = (Protocol) protocol[i].clone();
    }
    return result;
  }
  
  // -----------------------------------------------------------------
  /** returns the next unique ID */
  private static long nextID() {
    return counterID++;
  }
  
  // =============== public methods ==================================
  // =================================================================
  @Override public void setFailState(final int failState) {
    // after a node is dead, all operations on it are errors by definition
    if (failstate == DEAD && failState != DEAD) {
      throw new IllegalStateException("Cannot change fail state: node is already DEAD");
    }
    switch (failState) {
      case OK:
        failstate = OK;
        break;
      case DEAD:
        // protocol = null;
        index = -1;
        failstate = DEAD;
        CommonState.setNode(this);
        for (int i = 0; i < protocol.length; ++i) {
          CommonState.setPid(i);
          if (protocol[i] instanceof Cleanable) {
            ((Cleanable) protocol[i]).onKill();
          }
        }
        break;
      case DOWN:
        failstate = DOWN;
        break;
      default:
        throw new IllegalArgumentException("failState=" + failState);
    }
  }
  
  // -----------------------------------------------------------------
  @Override public int getFailState() {
    return failstate;
  }
  
  // ------------------------------------------------------------------
  @Override public boolean isUp() {
    return failstate == OK;
  }
  
  // -----------------------------------------------------------------
  @Override public Protocol getProtocol(final int i) {
    return protocol[i];
  }
  
  public void setProtocol(final int i, final Protocol p) {
    protocol[i] = p;
  }
  
  // ------------------------------------------------------------------
  @Override public int protocolSize() {
    return protocol.length;
  }
  
  // ------------------------------------------------------------------
  @Override public int getIndex() {
    return index;
  }
  
  // ------------------------------------------------------------------
  @Override public void setIndex(final int index) {
    this.index = index;
  }
  
  // ------------------------------------------------------------------
  /**
   * Returns the ID of this node. The IDs are generated using a counter (i.e.
   * they are not random).
   */
  @Override public long getID() {
    return ID;
  }
  
  // ------------------------------------------------------------------
  @Override public String toString() {
    final StringBuffer buffer = new StringBuffer();
    buffer.append("ID: " + ID + " index: " + index + "\n");
    for (int i = 0; i < protocol.length; ++i) {
      buffer.append("protocol[" + i + "]=" + protocol[i] + "\n");
    }
    return buffer.toString();
  }
  
  // ------------------------------------------------------------------
  /** Implemented as <code>(int)getID()</code>. */
  @Override public int hashCode() {
    return (int) getID();
  }
  
  public static void resetIDs() {
    counterID = -1;
  }
}
