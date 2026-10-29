package com.glaurung.batMap.gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.glaurung.batMap.vo.Exit;
import com.glaurung.batMap.vo.Room;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.VisualizationViewer;

public class ExitLabelsOnTopRendererTest {

    private SparseMultigraph<Room, Exit> graph;
    private VisualizationViewer<Room, Exit> vv;
    private List<String> drawn;

    /** records what is drawn and in which order, instead of drawing */
    private class RecordingRenderer extends ExitLabelsOnTopRenderer {
        @Override
        public void renderEdge( RenderContext<Room, Exit> rc, Layout<Room, Exit> layout, Exit exit ) {
            drawn.add( "exit " + exit.getExit() );
        }

        @Override
        public void renderVertex( RenderContext<Room, Exit> rc, Layout<Room, Exit> layout, Room room ) {
            drawn.add( "room " + room.getId() );
        }

        @Override
        public void renderVertexLabel( RenderContext<Room, Exit> rc, Layout<Room, Exit> layout, Room room ) {
        }

        @Override
        public void renderEdgeLabel( RenderContext<Room, Exit> rc, Layout<Room, Exit> layout, Exit exit ) {
            drawn.add( "label " + exit.getExit() );
        }
    }

    @BeforeClass
    public static void headless() {
        System.setProperty( "java.awt.headless", "true" );
    }

    @Before
    public void setUp() {
        graph = new SparseMultigraph<Room, Exit>();
        MapperLayout layout = new MapperLayout( graph );
        Room hall = new Room( "Hall", "hall" );
        Room yard = new Room( "Yard", "yard" );
        graph.addEdge( new Exit( "north" ), hall, yard, EdgeType.DIRECTED );
        graph.addEdge( new Exit( "south" ), yard, hall, EdgeType.DIRECTED );
        graph.addEdge( new Exit( "west" ), yard, yard, EdgeType.DIRECTED ); // loops back into the same room
        layout.setLocation( hall, new Point2D.Double( 0, 120 ) );
        layout.setLocation( yard, new Point2D.Double( 0, 0 ) );
        vv = new VisualizationViewer<Room, Exit>( layout );
        drawn = new ArrayList<String>();
    }

    private int last( String prefix ) {
        int last = - 1;
        for (int i = 0; i < drawn.size(); i++) {
            if (drawn.get( i ).startsWith( prefix )) {
                last = i;
            }
        }
        return last;
    }

    private int first( String prefix ) {
        for (int i = 0; i < drawn.size(); i++) {
            if (drawn.get( i ).startsWith( prefix )) {
                return i;
            }
        }
        return - 1;
    }

    @Test
    public void drawsExitsThenRoomsThenAllLabels() {
        new RecordingRenderer().render( vv.getRenderContext(), vv.getGraphLayout() );

        assertEquals( 3 + 2 + 3, drawn.size() );
        assertTrue( drawn.toString(), last( "exit " ) < first( "room " ) );
        assertTrue( drawn.toString(), last( "room " ) < first( "label " ) );
        assertTrue( drawn.contains( "label west" ) );
    }

    @Test
    public void mapChangedWhileDrawingIsDrawnAgainLater() {
        final boolean[] repainted = { false };
        VisualizationViewer<Room, Exit> screen = new VisualizationViewer<Room, Exit>( vv.getGraphLayout() ) {
            @Override
            public void repaint() {
                repainted[0] = true;
            }
        };
        RenderContext<Room, Exit> rc = vv.getRenderContext();
        rc.setScreenDevice( screen );

        new RecordingRenderer() {
            @Override
            public void renderVertex( RenderContext<Room, Exit> rc, Layout<Room, Exit> layout, Room room ) {
                graph.addVertex( new Room( "New", "new" + drawn.size() ) ); // mapper adds a room mid-draw
                drawn.add( "room " + room.getId() );
            }
        }.render( rc, vv.getGraphLayout() );

        assertTrue( repainted[0] );
    }

}
