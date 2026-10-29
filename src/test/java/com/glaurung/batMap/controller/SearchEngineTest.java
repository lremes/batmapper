package com.glaurung.batMap.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.glaurung.batMap.gui.MapperLayout;
import com.glaurung.batMap.gui.search.SearchPanel;
import com.glaurung.batMap.io.AreaDataPersister;
import com.glaurung.batMap.vo.Area;
import com.glaurung.batMap.vo.Exit;
import com.glaurung.batMap.vo.Room;

import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;

public class SearchEngineTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private SearchEngine engine;
    private JButton saveButton;

    @BeforeClass
    public static void headless() {
        System.setProperty( "java.awt.headless", "true" );
    }

    @Before
    public void setUp() throws IOException {
        new File( temp.getRoot(), "conf" ).mkdirs(); // the client always has its conf dir
        saveArea( "trader's house" );
        saveArea( "arelium" );
        engine = new SearchEngine( null );
        engine.setBaseDir( temp.getRoot().getPath() );
        SearchPanel panel = (SearchPanel) engine.getPanel();
        panel.componentResized( null ); // lays out and adds the buttons
        saveButton = findSaveButton( panel );
    }

    private void saveArea( String name ) throws IOException {
        Area area = new Area( name );
        SparseMultigraph<Room, Exit> graph = new SparseMultigraph<Room, Exit>();
        MapperLayout layout = new MapperLayout( graph );
        Room first = new Room( "First room of " + name, name + "-1", area );
        Room second = new Room( "Second room of " + name, name + "-2", area );
        graph.addEdge( new Exit( "e" ), first, second, EdgeType.DIRECTED );
        layout.setLocation( first, new Point2D.Double( 0, 0 ) );
        layout.setLocation( second, new Point2D.Double( 120, 0 ) );
        AreaDataPersister.save( temp.getRoot().getPath(), graph, layout );
    }

    // the search panel's own save button sits directly on it, the room desc panel inside has another "Save"
    private JButton findSaveButton( Container panel ) {
        for (Component component : panel.getComponents()) {
            if (component instanceof JButton && "Save".equals( ( (JButton) component ).getText() )) {
                return (JButton) component;
            }
        }
        throw new AssertionError( "search panel has no save button" );
    }

    @Test
    public void showAreaFromListLoadsTheMap() {
        engine.showArea( "arelium" );

        assertEquals( 2, engine.getGraph().getVertexCount() );
        assertEquals( "arelium", engine.getGraph().getVertices().iterator().next().getArea().getName() );
    }

    @Test
    public void areaOpenInMapperCannotBeSavedFromSearchEvenWithApostrophe() {
        engine.setMapperArea( "trader's house" );

        // the area list shows the file name, which drops the apostrophe
        engine.showArea( "traders house" );

        assertFalse( saveButton.isEnabled() );
    }

    @Test
    public void otherAreasCanBeSaved() {
        engine.setMapperArea( "trader's house" );

        engine.showArea( "arelium" );

        assertTrue( saveButton.isEnabled() );
    }

    @Test
    public void missingAreaKeepsCurrentMap() {
        engine.showArea( "arelium" );

        engine.showArea( "no such area" );

        assertEquals( "arelium", engine.getGraph().getVertices().iterator().next().getArea().getName() );
    }

}
