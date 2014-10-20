package com.sheikhmuneeb.demo.utilty;

import android.location.Location;

import com.sheikhmuneeb.demo.model.LocationModel;

import java.util.Comparator;

/**
 * Sort the locations retrieved from server based on the user current location
 * 
 * @author Sheikh Muneeb
 */
public class PositionComparator implements Comparator<LocationModel> {
	private Location currentLocation;

	public PositionComparator(Location currentLocation) {
		this.currentLocation = currentLocation;
	}

	@Override
	public int compare(LocationModel lhs, LocationModel rhs) {

		float leftResults[] = new float[3];
		float rightResults[] = new float[3];

		Location.distanceBetween(currentLocation.getLatitude(),
				currentLocation.getLongitude(), lhs.geo_position.latitude,
				lhs.geo_position.longitude, leftResults);

		Location.distanceBetween(currentLocation.getLatitude(),
				currentLocation.getLongitude(), rhs.geo_position.latitude,
				rhs.geo_position.longitude, rightResults);

		if (leftResults[0] < rightResults[0]) {
			return -1;
		} else if (leftResults[0] > rightResults[0]) {
			return 1;
		}
		return 0;
	}

}
