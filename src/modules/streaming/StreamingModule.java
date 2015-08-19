package modules.streaming;

import ingredients.streaming.mandatory.DefaultChunkInitIngredient;
import interfaces.NodeConnectionAlgorithm;

import java.util.Random;

import logging.ObjectLogger;
import logging.logObjects.ChunkReceiveLog;
import messages.ChunkMessage;
import messages.Message;
import modules.P2PClient;
import modules.overlays.OverlayModule;
import utils.Common;
import utils.Utils;

public abstract class StreamingModule extends NodeConnectionAlgorithm {
  static private final ObjectLogger<ChunkReceiveLog> chunkRecLog = ObjectLogger.getLogger("chunkRec");
  protected final P2PClient client;
  public long latestChunk = Long.MAX_VALUE;
  public long fromChunk = 0;
  public OverlayModule<?> mainOverlay;
  
  @Override public void setConfNumber(final int confNumber) {
    super.setConfNumber(confNumber);
    mainOverlay.setConfNumber(confNumber);
  }
  
  @Override public void deactivate() {
    super.deactivate();
    if (mainOverlay != null) {
      mainOverlay.deactivate();
    }
  }
  
  public StreamingModule(final P2PClient client, final OverlayModule<?> overlay, final Random r) {
    super(client.network, r);
    this.client = client;
    mainOverlay = overlay;
    addIngredient(new DefaultChunkInitIngredient(new Random(r.nextLong())), client);
  }
  
  public boolean isOverlayConnected() {
    return mainOverlay.isOverlayConnected();
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    if (!client.player.bufferFromFirstChunk && client.player.getVs() == null && Utils.getMovieTime() >= 0) {
      final long movieStartFrame = Math.max(1L, Utils.getMovieTime() / Common.currentConfiguration.cycleLength + 1);
      client.player.initVS(movieStartFrame);
    }
    mainOverlay.nextCycle();
  }
  
  @Override public void setServerMode() {
    super.setServerMode();
    mainOverlay.setServerMode();
  }
  
  @Override public void handleMessage(final Message message) {
    super.handleMessage(message);
    if (message instanceof ChunkMessage) {
      if (network.isServerMode()) {
        throw new RuntimeException("server received chunks message: " + message);
      }
      chunkRecLog.logObject(new ChunkReceiveLog((ChunkMessage) message));
    }
  }
  
  /**
   * this function is called by the P2P client when there is too much
   * consecutive lag
   */
  public void handleConsecutiveLag() {
    if (mainOverlay.isOverlayConnected()) {
      mainOverlay.reConnect();
    }
  }
  
  public void setLatestChunk(final long chunk) {
    if (latestChunk == Long.MAX_VALUE) {
      latestChunk = chunk;
    }
  }
  
  public void setEarliestChunk(final long chunk) {
    fromChunk = chunk;
  }
  
  public boolean isActive() {
    if (latestChunk == Long.MAX_VALUE || client.player.getVs() == null) {
      return true;
    }
    // TODO debug
    return client.player.getVs().windowOffset < latestChunk + 20;
  }
}
