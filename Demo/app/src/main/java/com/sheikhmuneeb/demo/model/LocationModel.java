package com.sheikhmuneeb.demo.model;

import com.google.gson.Gson;

public class LocationModel {
    public String name;
    public GeoPosition geo_position;

    public class GeoPosition
    {
        public double latitude;
        public double longitude;
    }

	public static LocationModel[] fromJson(String json) {
		return new Gson().fromJson(json, LocationModel[].class);
	}

    @Override
    public String toString() {
        return name;
    }
}