package com.glaurung.batMap.gui.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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
