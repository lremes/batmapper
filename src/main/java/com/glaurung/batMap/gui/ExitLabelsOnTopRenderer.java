package com.glaurung.batMap.gui;

import java.util.ConcurrentModificationException;

import com.glaurung.batMap.vo.Exit;
import com.glaurung.batMap.vo.Room;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.renderers.BasicRenderer;

/**
 * Draws exits, then rooms, then exit labels. Jung's own order draws each label right after its exit,
 * so exits drawn later and all rooms could cover it, and labels of exits looping back into the same
 * room were always hidden under the room.
 */
public class ExitLabelsOnTopRenderer extends BasicRenderer<Room, Exit> {

    @Override
    public void render( RenderContext<Room, Exit> renderContext, Layout<Room, Exit> layout ) {
        try {
            for (Exit exit : layout.getGraph().getEdges()) {
                renderEdge( renderContext, layout, exit );
            }
            for (Room room : layout.getGraph().getVertices()) {
                renderVertex( renderContext, layout, room );
                renderVertexLabel( renderContext, layout, room );
            }
            for (Exit exit : layout.getGraph().getEdges()) {
                renderEdgeLabel( renderContext, layout, exit );
            }
        } catch (ConcurrentModificationException cme) {
            // the map changed while drawing, like jung does, just draw it again
            renderContext.getScreenDevice().repaint();
        }
    }

}
