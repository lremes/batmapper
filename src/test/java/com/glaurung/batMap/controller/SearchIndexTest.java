package com.glaurung.batMap.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.glaurung.batMap.TestMaps;
import com.glaurung.batMap.vo.Room;

public class SearchIndexTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private File base;
    private SearchIndex index;

    @Before
    public void setUp() throws IOException {
        base = temp.getRoot();
        index = new SearchIndex();
        TestMaps.saveArea( base, "arelium",
                new String[] { "a1", "Town square", "A big Fountain splashes here." },
                new String[] { "a2", "Main road", "The road leads north." } );
        TestMaps.saveArea( base, "valley",
                new String[] { "v1", "Dusty road", "Sand everywhere." } );
    }

    private List<String> ids( String text ) {
        List<String> ids = new ArrayList<String>();
        for (Room room : index.search( base.getPath(), text )) {
            ids.add( room.getId() );
        }
        return ids;
    }

    // a save within the same clock tick as the previous one must still count as a change
    private void markSavedLater( File map ) {
        map.setLastModified( map.lastModified() + 10000 );
    }

    @Test
    public void findsTextInShortAndLongDescIgnoringCase() {
        assertEquals( listOf( "a1" ), ids( "fountain" ) );
        assertEquals( listOf( "a1" ), ids( "SQUARE" ) );
        assertTrue( ids( "road" ).containsAll( listOf( "a2", "v1" ) ) );
        assertEquals( 2, ids( "road" ).size() );
    }

    @Test
    public void unchangedMapsAreNotLoadedAgain() {
        Room first = index.search( base.getPath(), "fountain" ).get( 0 );
        assertSame( first, index.search( base.getPath(), "fountain" ).get( 0 ) );
    }

    @Test
    public void savedMapIsIndexedAgain() throws IOException {
        Room before = index.search( base.getPath(), "fountain" ).get( 0 );

        File map = TestMaps.saveArea( base, "arelium",
                new String[] { "a1", "Town square", "The fountain has dried up." },
                new String[] { "a3", "Market", "Stalls full of fruit." } );
        markSavedLater( map );

        assertNotSame( before, index.search( base.getPath(), "fountain" ).get( 0 ) );
        assertEquals( listOf( "a3" ), ids( "fruit" ) );
        assertEquals( listOf( "v1" ), ids( "road" ) );
    }

    @Test
    public void newAndDeletedMapsAreNoticed() throws IOException {
        assertEquals( listOf( "v1" ), ids( "sand" ) );

        TestMaps.mapFile( base, "valley" ).delete();
        TestMaps.saveArea( base, "desert", new String[] { "d1", "Dunes", "Sand and more sand." } );

        assertEquals( listOf( "d1" ), ids( "sand" ) );
    }

    @Test
    public void unreadableMapIsSkipped() throws IOException {
        Files.write( TestMaps.mapFile( base, "broken" ).toPath(), "not a map".getBytes( StandardCharsets.UTF_8 ) );

        assertEquals( listOf( "a1" ), ids( "fountain" ) );
    }

    @Test
    public void roomsWithoutDescsAreSkipped() throws IOException {
        TestMaps.saveArea( base, "odd", new String[] { "o1", null, "A road with no name." } );

        assertEquals( 2, ids( "road" ).size() );
    }

    private static List<String> listOf( String... ids ) {
        List<String> list = new ArrayList<String>();
        for (String id : ids) {
            list.add( id );
        }
        return list;
    }

}
