package com.glaurung.batMap.gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Paint;
import java.awt.geom.Point2D;

import org.junit.Before;
import org.junit.Test;

import com.glaurung.batMap.vo.Exit;
import com.glaurung.batMap.vo.Room;

import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.visualization.VisualizationViewer;

public class ExitPaintTransformerTest {

    private SparseMultigraph<Room, Exit> graph;
    private MapperLayout layout;
    private ExitPaintTransformer transformer;

    @Before
    public void setUp() {
        graph = new SparseMultigraph<Room, Exit>();
        layout = new MapperLayout( graph );
        transformer = new ExitPaintTransformer( new VisualizationViewer<Room, Exit>( layout ) );
    }

    private Exit connect( boolean fromIndoors, boolean toIndoors ) {
        Room from = new Room( "from", "from" + graph.getVertexCount() );
        from.setIndoors( fromIndoors );
        Room to = new Room( "to", "to" + graph.getVertexCount() );
        to.setIndoors( toIndoors );
        layout.setLocation( from, new Point2D.Double( 0, 0 ) );
        layout.setLocation( to, new Point2D.Double( 120, 0 ) );
        Exit exit = new Exit( "e" );
        graph.addEdge( exit, from, to, EdgeType.DIRECTED );
        return exit;
    }

    @Test
    public void sameKindRoomsGetSolidColor() {
        Paint outdoor = transformer.transform( connect( false, false ) );
        Paint indoor = transformer.transform( connect( true, true ) );
        assertTrue( outdoor instanceof Color );
        assertTrue( indoor instanceof Color );
        assertTrue( ! outdoor.equals( indoor ) );
    }

    @Test
    public void mixedRoomsStillGetGradient() {
        Paint paint = transformer.transform( connect( false, true ) );
        assertTrue( paint instanceof GradientPaint );
        GradientPaint gradient = (GradientPaint) paint;
        assertEquals( transformer.transform( connect( false, false ) ), gradient.getColor1() );
        assertEquals( transformer.transform( connect( true, true ) ), gradient.getColor2() );
    }

}
