package com.glaurung.batMap.gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.awt.geom.Point2D;

import org.junit.Before;
import org.junit.Test;

import com.glaurung.batMap.vo.Exit;
import com.glaurung.batMap.vo.Room;

import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;

public class ExitLabellerTest {

    private VisualizationViewer<Room, Exit> vv;
    private ExitLabeller labeller;

    @Before
    public void setUp() {
        vv = new VisualizationViewer<Room, Exit>( new MapperLayout( new SparseMultigraph<Room, Exit>() ) );
        labeller = new ExitLabeller( vv );
    }

    private void zoom( float amount ) {
        new CrossoverScalingControl().scale( vv, amount, new Point2D.Double( 0, 0 ) );
    }

    @Test
    public void labelsShownAtNormalZoom() {
        assertEquals( "climb rope", labeller.transform( new Exit( "climb rope" ) ) );
    }

    @Test
    public void labelsShownWhenZoomedIn() {
        zoom( 3f );
        assertEquals( "n", labeller.transform( new Exit( "n" ) ) );
    }

    @Test
    public void labelsShownAtThreshold() {
        zoom( (float) ExitLabeller.MIN_SCALE_FOR_LABELS + 0.01f );
        assertEquals( "n", labeller.transform( new Exit( "n" ) ) );
    }

    @Test
    public void labelsHiddenWhenZoomedOutPastThreshold() {
        zoom( (float) ExitLabeller.MIN_SCALE_FOR_LABELS - 0.01f );
        assertNull( labeller.transform( new Exit( "n" ) ) );
    }

    @Test
    public void currentExitKeepsLabelWhenZoomedOut() {
        zoom( 0.2f );
        Exit current = new Exit( "climb rope" );
        current.setCurrentExit( true );

        assertEquals( "climb rope", labeller.transform( current ) );
        assertNull( labeller.transform( new Exit( "n" ) ) );
    }

    @Test
    public void labelsReturnWhenZoomingBackIn() {
        zoom( 0.2f );
        assertNull( labeller.transform( new Exit( "n" ) ) );
        zoom( 5f );
        assertEquals( "n", labeller.transform( new Exit( "n" ) ) );
    }

}
