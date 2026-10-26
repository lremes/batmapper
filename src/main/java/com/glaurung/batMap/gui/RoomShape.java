package com.glaurung.batMap.gui;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import org.apache.commons.collections15.Transformer;

import com.glaurung.batMap.vo.Room;

public class RoomShape implements Transformer<Room, Shape> {

    // every room is the same size, and jung asks for the shape many times per repaint, so build it only once
    private final Shape roomGfx = new Rectangle2D.Float( - DrawingUtils.ROOM_SIZE / 2f, - DrawingUtils.ROOM_SIZE / 2f,
                                                          DrawingUtils.ROOM_SIZE, DrawingUtils.ROOM_SIZE );

    public Shape transform( Room room ) {
        return roomGfx;
    }


}
