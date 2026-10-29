package com.glaurung.batMap.gui.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.glaurung.batMap.TestMaps;
import com.glaurung.batMap.controller.SearchEngine;

public class SearchPanelTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private File mapDir;
    private SearchPanel panel;

    @BeforeClass
    public static void headless() {
        System.setProperty( "java.awt.headless", "true" );
    }

    @Before
    public void setUp() {
        mapDir = new File( new File( temp.getRoot(), "conf" ), "batMapAreas" );
        mapDir.mkdirs();
        SearchEngine engine = new SearchEngine( null );
        engine.setBaseDir( temp.getRoot().getPath() );
        panel = new SearchPanel( engine );
    }

    // not a real map, so listing fails if it ever tries to load a map
    private void unreadableMap( String areaName ) throws IOException {
        Files.write( new File( mapDir, areaName + ".batmap" ).toPath(), "not a map".getBytes( StandardCharsets.UTF_8 ) );
    }

    @Test
    public void listsAreasWithoutLoadingMaps() throws IOException {
        unreadableMap( "arelium" );
        unreadableMap( "traders house" );

        panel.populateAreaList();

        assertEquals( new HashSet<>( Arrays.asList( "arelium", "traders house" ) ), new HashSet<>( panel.listedAreas() ) );
    }

    @Test
    public void listStartsWithTitleOnly() {
        assertEquals( 0, panel.listedAreas().size() );
    }

    @Test
    public void refreshPicksUpNewAreas() throws IOException {
        unreadableMap( "arelium" );
        panel.populateAreaList();

        unreadableMap( "valley" );
        panel.populateAreaList();

        assertEquals( new HashSet<>( Arrays.asList( "arelium", "valley" ) ), new HashSet<>( panel.listedAreas() ) );
    }

    @Test
    public void unchangedListIsNotRebuilt() throws IOException {
        unreadableMap( "arelium" );
        panel.populateAreaList();
        Object item = getListItem( 1 );

        panel.populateAreaList();

        assertSame( item, getListItem( 1 ) );
    }

    @Test
    public void searchFindsRoomsInSavedMaps() throws IOException {
        TestMaps.saveArea( temp.getRoot(), "arelium",
                new String[] { "a1", "Town square", "A big fountain splashes here." },
                new String[] { "a2", "Main road", "The road leads north." } );

        List<String> found = panel.searchForRoomsWith( "fountain" );

        assertEquals( Arrays.asList( "arelium: Town square" ), found );
        assertEquals( 1, panel.listedResults().size() );
        assertEquals( "a1", panel.listedResults().get( 0 ).getRoom().getId() );
    }

    @Test
    public void tooShortTextFindsNothing() throws IOException {
        TestMaps.saveArea( temp.getRoot(), "arelium", new String[] { "a1", "Town square", "An ox stands here." } );

        assertTrue( panel.searchForRoomsWith( "ox" ).isEmpty() );
        assertTrue( panel.searchForRoomsWith( " o  " ).isEmpty() );
        assertEquals( 0, panel.listedResults().size() );
        assertEquals( 1, panel.searchForRoomsWith( "an ox" ).size() );
    }

    @Test
    public void roomsWithSameDescAreListedButReportedOnce() throws IOException {
        TestMaps.saveArea( temp.getRoot(), "forest",
                new String[] { "f1", "Dark forest", "Trees all around." },
                new String[] { "f2", "Dark forest", "Trees all around." },
                new String[] { "f3", "Clearing", "Trees surround a clearing." } );

        List<String> found = panel.searchForRoomsWith( "trees" );

        assertEquals( 3, panel.listedResults().size() );
        assertEquals( 2, found.size() );
        assertTrue( found.containsAll( Arrays.asList( "forest: Dark forest", "forest: Clearing" ) ) );
    }

    @Test
    public void newSearchReplacesOldResults() throws IOException {
        TestMaps.saveArea( temp.getRoot(), "arelium",
                new String[] { "a1", "Town square", "A big fountain splashes here." },
                new String[] { "a2", "Main road", "The road leads north." } );
        panel.searchForRoomsWith( "fountain" );

        panel.searchForRoomsWith( "road" );

        assertEquals( 1, panel.listedResults().size() );
        assertEquals( "a2", panel.listedResults().get( 0 ).getRoom().getId() );
    }

    private Object getListItem( int index ) {
        try {
            java.lang.reflect.Field field = SearchPanel.class.getDeclaredField( "listAllModel" );
            field.setAccessible( true );
            return ( (javax.swing.ComboBoxModel) field.get( panel ) ).getElementAt( index );
        } catch (ReflectiveOperationException e) {
            throw new AssertionError( e );
        }
    }

}
