package com.glaurung.batMap.gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.awt.Shape;

import org.apache.commons.collections15.functors.ConstantTransformer;
import org.junit.Test;

import com.glaurung.batMap.vo.Exit;
import com.glaurung.batMap.vo.Room;

import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.visualization.util.VertexShapeFactory;

public class RoomShapeTest {

    @Test
    public void shapeIsReusedForAllRooms() {
        RoomShape roomShape = new RoomShape( new SparseMultigraph<Room, Exit>() );
        assertSame( roomShape.transform( new Room( "a", "1" ) ), roomShape.transform( new Room( "b", "2" ) ) );
    }

    @Test
    public void shapeMatchesJungRectangleOfRoomSize() {
        Room room = new Room( "a", "1" );
        VertexShapeFactory<Room> factory = new VertexShapeFactory<Room>(
                new ConstantTransformer( DrawingUtils.ROOM_SIZE ), new ConstantTransformer( 1.0f ) );
        Shape expected = factory.getRectangle( room );

        Shape actual = new RoomShape( new SparseMultigraph<Room, Exit>() ).transform( room );

        assertEquals( expected.getBounds2D(), actual.getBounds2D() );
    }

}
