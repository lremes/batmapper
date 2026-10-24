package com.glaurung.batMap.gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.util.Arrays;
import java.util.HashSet;

import javax.swing.Icon;

import org.junit.Before;
import org.junit.Test;

import com.glaurung.batMap.vo.Room;

public class RoomIconTransformerTest {

    private RoomIconTransformer transformer;

    @Before
    public void setUp() {
        transformer = new RoomIconTransformer();
    }

    private Room room( String id, String... exits ) {
        Room room = new Room( "short desc", id );
        room.setExits( new HashSet<>( Arrays.asList( exits ) ) );
        return room;
    }

    @Test
    public void sameRoomGetsSameIconOnRepeatedCalls() {
        Room room = room( "1", "n", "s" );
        assertSame( transformer.transform( room ), transformer.transform( room ) );
    }

    @Test
    public void roomsThatLookTheSameShareAnIcon() {
        assertSame( transformer.transform( room( "1", "n", "s" ) ), transformer.transform( room( "2", "south", "north" ) ) );
    }

    @Test
    public void differentExitsGetDifferentIcons() {
        assertNotSame( transformer.transform( room( "1", "n" ) ), transformer.transform( room( "2", "n", "e" ) ) );
        assertNotSame( transformer.transform( room( "3", "n", "u" ) ), transformer.transform( room( "4", "n", "d" ) ) );
        assertNotSame( transformer.transform( room( "5", "n" ) ), transformer.transform( room( "6", "n", "climb rope" ) ) );
    }

    @Test
    public void changingHowRoomLooksGivesNewIcon() {
        Room room = room( "1", "n", "s" );
        Icon plain = transformer.transform( room );

        room.setIndoors( true );
        Icon indoors = transformer.transform( room );
        assertNotSame( plain, indoors );

        room.setColor( RoomColors.BLUE );
        Icon blue = transformer.transform( room );
        assertNotSame( indoors, blue );

        room.setColor( null );
        assertSame( indoors, transformer.transform( room ) );
    }

    @Test
    public void exploringAllExitsGivesNewIcon() {
        Room room = room( "1", "n", "s" );
        Icon unexplored = transformer.transform( room );
        room.useExit( "n" );
        assertSame( unexplored, transformer.transform( room ) );
        room.useExit( "s" );
        assertNotSame( unexplored, transformer.transform( room ) );
    }

    @Test
    public void pickedAndCurrentRoomsGetBorderedIcons() {
        Room room = room( "1", "n" );
        assertEquals( 90, transformer.transform( room ).getIconWidth() );

        room.setPicked( true );
        assertEquals( 96, transformer.transform( room ).getIconWidth() );

        room.setCurrent( true );
        assertEquals( 102, transformer.transform( room ).getIconWidth() );

        room.setPicked( false );
        Icon current = transformer.transform( room );
        assertEquals( 102, current.getIconWidth() );

        room.setCurrent( false );
        assertEquals( 90, transformer.transform( room ).getIconWidth() );
    }

}
