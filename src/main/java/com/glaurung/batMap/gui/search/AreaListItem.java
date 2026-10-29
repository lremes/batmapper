package com.glaurung.batMap.gui.search;

public class AreaListItem {
    private String areaName;

    public AreaListItem( String areaName ) {
        this.areaName = areaName;
    }

    public String getAreaName() {
        return areaName;
    }

    @Override
    public String toString() {
        return this.areaName;
    }
}
