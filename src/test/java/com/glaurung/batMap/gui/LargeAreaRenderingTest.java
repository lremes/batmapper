package com.glaurung.batMap.gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.swing.Icon;

import org.apache.commons.collections15.Transformer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.glaurung.batMap.controller.MapperEngine;
import com.glaurung.batMap.vo.Exit;
import com.glaurung.batMap.vo.Room;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;

/**
 * Emulates the mapper showing a large area zoomed out, without the batclient,
 * by building the area straight into the graph and painting the viewer into an image.
 */
public class LargeAreaRenderingTest {

    private static final int SIDE = 25;
    private static final int ROOM_SPACING = 120;

    private SparseMultigraph<Room, Exit> graph;
    private VisualizationViewer<Room, Exit> vv;
    private Map<Icon, Integer> iconsDrawn;
    private int iconRequests;

    @BeforeClass
    public static void headless() {
        System.setProperty( "java.awt.headless", "true" );
    }

    @Before
    public void setUp() {
        graph = new SparseMultigraph<Room, Exit>();
        vv = new MapperEngine( graph, null ).getVV();
        buildGridArea();
        vv.setSize( 800, 534 );

        iconsDrawn = new IdentityHashMap<Icon, Integer>();
        RenderContext<Room, Exit> rc = vv.getRenderContext();
        final Transformer<Room, Icon> icons = rc.getVertexIconTransformer();
        rc.setVertexIconTransformer( new Transformer<Room, Icon>() {
            public Icon transform( Room room ) {
                Icon icon = icons.transform( room );
                iconRequests++;
                iconsDrawn.put( icon, 1 );
                return icon;
            }
        } );
    }

    private void buildGridArea() {
        Layout<Room, Exit> layout = vv.getGraphLayout();
        Room[][] grid = new Room[SIDE][SIDE];
        for (int y = 0; y < SIDE; y++) {
            for (int x = 0; x < SIDE; x++) {
                Room room = new Room( "room " + x + "," + y, x + "," + y );
                room.setExits( new HashSet<>( Arrays.asList( "n", "s", "e", "w" ) ) );
                room.setIndoors( ( x + y ) % 3 == 0 );
                graph.addVertex( room );
                layout.setLocation( room, new Point2D.Double( x * ROOM_SPACING, y * ROOM_SPACING ) );
                grid[y][x] = room;
                if (x > 0) {
                    graph.addEdge( new Exit( "e" ), grid[y][x - 1], room, EdgeType.DIRECTED );
                    graph.addEdge( new Exit( "w" ), room, grid[y][x - 1], EdgeType.DIRECTED );
                }
                if (y > 0) {
                    graph.addEdge( new Exit( "s" ), grid[y - 1][x], room, EdgeType.DIRECTED );
                    graph.addEdge( new Exit( "n" ), room, grid[y - 1][x], EdgeType.DIRECTED );
                }
            }
        }
        grid[SIDE / 2][SIDE / 2].setCurrent( true );
    }

    private void paint() {
        BufferedImage image = new BufferedImage( 800, 534, BufferedImage.TYPE_INT_RGB );
        Graphics2D g = image.createGraphics();
        vv.paint( g );
        g.dispose();
    }

    @Test
    public void zoomedOutAreaReusesIconsBetweenRepaints() {
        // 25 rooms * 120 spacing * 0.15 = 450px, so the whole area fits in the 800x534 view
        new CrossoverScalingControl().scale( vv, 0.15f, new Point2D.Double( 0, 0 ) );

        for (int i = 0; i < 5; i++) {
            paint();
        }

        // whole area fits on screen, so every room is drawn on every repaint
        assertTrue( "rooms drawn: " + iconRequests, iconRequests >= 5 * SIDE * SIDE );
        // but they only look one of three ways: outdoor, indoor, or the current room
        assertEquals( 3, iconsDrawn.size() );
    }

    @Test
    public void zoomedOutAreaHidesExitLabels() {
        new CrossoverScalingControl().scale( vv, 0.2f, new Point2D.Double( 0, 0 ) );
        paint();

        Transformer<Exit, String> labels = vv.getRenderContext().getEdgeLabelTransformer();
        for (Exit exit : graph.getEdges()) {
            assertNull( labels.transform( exit ) );
        }
    }

    @Test
    public void normalZoomStillLabelsExits() {
        paint();

        Exit exit = graph.getEdges().iterator().next();
        assertEquals( exit.getExit(), vv.getRenderContext().getEdgeLabelTransformer().transform( exit ) );
    }

}
