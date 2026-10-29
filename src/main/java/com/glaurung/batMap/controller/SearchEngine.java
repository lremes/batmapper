package com.glaurung.batMap.controller;

import java.awt.event.ItemListener;
import java.io.IOException;
import java.sql.Savepoint;
import java.util.List;

import com.glaurung.batMap.gui.search.SearchPanel;
import com.glaurung.batMap.io.AreaDataPersister;
import com.glaurung.batMap.vo.AreaSaveObject;
import com.glaurung.batMap.vo.Exit;
import com.glaurung.batMap.vo.Room;

import edu.uci.ics.jung.graph.SparseMultigraph;

public class SearchEngine extends MapperEngine implements ItemListener {

    private String areaName = "";
    private String mapperArea;
    private boolean savingAlreadyDisabled = false;
    private final SearchIndex searchIndex = new SearchIndex();

    public SearchEngine( SparseMultigraph<Room, Exit> graph, MapperPlugin plugin ) {
        this(plugin);
        this.graph = graph;
        this.mapperLayout.setGraph( graph );
    }

    public SearchEngine(MapperPlugin plugin) {
        super(plugin);
        panel = new SearchPanel( this );
    }

    /**
     * Rooms in any saved map whose short or long desc contains the text, ignoring case.
     */
    public List<Room> searchRooms( String text ) {
        return searchIndex.search( baseDir, text );
    }

    /**
     * Indexes the saved maps in the background, so the first search doesn't have to wait for it.
     */
    public void warmUpSearchIndex() {
        final String dir = baseDir;
        Thread indexer = new Thread( () -> searchIndex.refresh( dir ), "batMap search index" );
        indexer.setDaemon( true );
        indexer.setPriority( Thread.MIN_PRIORITY );
        indexer.start();
    }

    public void moveToRoom( Room room, boolean highlight ) {
        moveToArea( room.getArea().getName() );
        showRoom( room, highlight );
    }

    /**
     * Shows an area picked from the area list, with the descs of its first room.
     */
    public void showArea( String areaName ) {
        if (loadArea( areaName ) && graph.getVertexCount() > 0) {
            showRoom( graph.getVertices().iterator().next(), false );
        }
    }

    private void showRoom( Room room, boolean highlight ) {
        for (Room aRoom : graph.getVertices()) {

            if (aRoom.equals( room ) && highlight) {
                currentRoom = aRoom;
                aRoom.setPicked( true );
                aRoom.setCurrent( true );
            } else {
                aRoom.setPicked( false );
                aRoom.setCurrent( false );
            }
        }

        singleRoomPicked(room);
//        pickedRoom = room;
//        this.panel.setTextForDescs( pickedRoom.getShortDesc(), pickedRoom.getLongDesc(), makeExitsStringFromPickedRoom(), pickedRoom );
        repaint();
        moveMapToStayWithCurrentRoom();
    }

    public void save() {
        try {
            if (this.area != null) {
                AreaDataPersister.save( baseDir, graph, mapperLayout );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void setPanel( SearchPanel panel ) {
        this.panel = panel;
    }

    public void moveToArea( String areaName ) {
        loadArea( areaName );
    }

    private boolean loadArea( String areaName ) {
        AreaSaveObject areaSaveObject = null;
        try {
            areaSaveObject = AreaDataPersister.loadData( baseDir, areaName );
        } catch (ClassNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        this.graph = areaSaveObject.getGraph();
        this.area = this.graph.getVertices().iterator().next().getArea();
        // take the name stored in the map, file names (and so the area list) drop ' and / from it
        this.areaName = this.area.getName();
        mapperLayout.displayLoadedData( areaSaveObject );
        if (! this.areaName.equalsIgnoreCase( this.mapperArea )) {
            savingAlreadyDisabled = false;
        }
        checkIfCanSaveMap();
        return true;
    }


    public void setBaseDir( String baseDir ) {
        this.baseDir = baseDir;

    }

    public String getBaseDir() {
        return this.baseDir;
    }

    public void checkIfCanSaveMap() {
        if (this.areaName.equalsIgnoreCase( this.mapperArea ) || savingAlreadyDisabled) {
            SearchPanel tempPanel = (SearchPanel) this.panel;
            tempPanel.toggleSaveAbility( false );
            savingAlreadyDisabled = true;
        } else {
            SearchPanel tempPanel = (SearchPanel) this.panel;
            tempPanel.toggleSaveAbility( true );
        }

    }

    public void setMapperArea( String areaName ) {
        this.mapperArea = areaName;
        checkIfCanSaveMap();
    }

}
