package com.glaurung.batMap.gui;

import org.apache.commons.collections15.Transformer;

import com.glaurung.batMap.vo.Exit;
import com.glaurung.batMap.vo.Room;

import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.MultiLayerTransformer;
import edu.uci.ics.jung.visualization.VisualizationViewer;

/**
 * Labels exits with their name, but hides the labels when the map is zoomed out so far
 * that they would only clutter the map and slow down drawing.
 */
public class ExitLabeller implements Transformer<Exit, String> {

    public static final double MIN_SCALE_FOR_LABELS = 0.5;

    private VisualizationViewer<Room, Exit> vv;

    public ExitLabeller( VisualizationViewer<Room, Exit> vv ) {
        this.vv = vv;
    }

    public String transform( Exit exit ) {
        if (getScale() < MIN_SCALE_FOR_LABELS) {
            return null;
        }
        return exit.toString();
    }

    private double getScale() {
        MultiLayerTransformer transformer = vv.getRenderContext().getMultiLayerTransformer();
        return transformer.getTransformer( Layer.LAYOUT ).getScale() * transformer.getTransformer( Layer.VIEW ).getScale();
    }

}
