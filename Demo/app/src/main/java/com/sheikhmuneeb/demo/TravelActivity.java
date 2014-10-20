package com.sheikhmuneeb.demo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.sheikhmuneeb.demo.model.LocationModel;
import com.sheikhmuneeb.demo.utilty.AutoCompleteAdapter;
import com.sheikhmuneeb.demo.utilty.DatePickerFragment;
import com.sheikhmuneeb.demo.utilty.DemoUtility;
import com.sheikhmuneeb.demo.utilty.DemoUtility.ErrorDialogFragment;
import com.sheikhmuneeb.demo.utilty.NetworkUtility;
import com.sheikhmuneeb.demo.utilty.PositionComparator;

/*
 * This demo application assumes that the location and networks settings are enabled
 * and does not show any visual prompt if they are disabled. Additionally, this demo
 * also handles the use case of out of order arrival of suggested positions due to
 * to which older positions list can override the data arrived.
 */
public class TravelActivity extends FragmentActivity
		implements
			GooglePlayServicesClient.ConnectionCallbacks,
			GooglePlayServicesClient.OnConnectionFailedListener,
			DatePickerDialog.OnDateSetListener {

	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

	private List<RequestLocation> requestLocationsList;
	private LocationClient mLocationClient;
	private Location currentLocation = null;
	private SparseBooleanArray booleanArray;

	private AutoCompleteTextView pickupLocation;
	private AutoCompleteTextView dropOffLocation;
	private TextView departureDate;
	private Button search;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_travel);

		pickupLocation = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewPickup);
		pickupLocation.setThreshold(1);
		addTextChangeListener(pickupLocation);

		dropOffLocation = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewDropOff);
		dropOffLocation.setThreshold(1);
		addTextChangeListener(dropOffLocation);

		booleanArray = new SparseBooleanArray();
		booleanArray.put(pickupLocation.getId(), false);
		booleanArray.put(dropOffLocation.getId(), false);

		departureDate = (TextView) findViewById(R.id.textViewDatePicker);
		departureDate.setText(DateFormat.getDateFormat(this).format(
				Calendar.getInstance().getTime()));

		search = (Button) findViewById(R.id.buttonSearch);
		requestLocationsList = new ArrayList<RequestLocation>();
	}

	@Override
	public void onStop() {
		mLocationClient.disconnect();
		super.onStop();
	}

	@Override
	public void onStart() {
		super.onStart();
		mLocationClient = new LocationClient(this, this, this);
		requestUserLocation();
	}

	@Override
	public void onResume() {
		super.onResume();
		// request for user current location
		requestUserLocation();
	}
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		ErrorDialogFragment errorFragment = new ErrorDialogFragment();
		errorFragment.setDialog(GooglePlayServicesUtil.getErrorDialog(
				connectionResult.getErrorCode(), this,
				CONNECTION_FAILURE_RESOLUTION_REQUEST));
		errorFragment
				.show(getFragmentManager(), getString(R.string.dialog_tag));
	}

	@Override
	public void onConnected(Bundle bundle) {
		Location receivedLocation = mLocationClient.getLastLocation();
		if (receivedLocation != null)
			currentLocation = receivedLocation;
	}

	@Override
	public void onDisconnected() {
		// Location services disconnected
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		switch (requestCode) {
			case CONNECTION_FAILURE_RESOLUTION_REQUEST :
				switch (resultCode) {
				// If Google Play services resolved the problem
					case Activity.RESULT_OK :
						requestUserLocation();
						break;
				}
		}
	}

	@Override
	public void onDateSet(DatePicker datePicker, int year, int month, int day) {
		// set the date on the view
		departureDate.setText(DateFormat.getDateFormat(this).format(
				datePicker.getCalendarView().getDate()));
	}
	
	public void search(View view) {
		Toast.makeText(TravelActivity.this,
				getString(R.string.search_not_implemented), Toast.LENGTH_SHORT)
				.show();
	}

	// shows the date picker dialog
	public void displayDatePicker(View view) {
		DatePickerFragment dialogFragment = new DatePickerFragment(this);
		dialogFragment.show(getSupportFragmentManager(), "datePicker");
	}

	// listen for text changes in the auto complete listener
	private void addTextChangeListener(
			final AutoCompleteTextView autoCompleteTextView) {
		TextWatcher textWatcher = new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable text) {
				if (text.length() > 0) {
					// request the network for the suggested positions
					requestAutoCompleteList(autoCompleteTextView, text);
					booleanArray.put(autoCompleteTextView.getId(), true);

					// if both locations are filled, enable the search button 
					for (int index = 0; index < booleanArray.size(); index++) {
						if (!booleanArray.valueAt(index))
							return;
					}
					search.setEnabled(true);
				} else {
					booleanArray.put(autoCompleteTextView.getId(), false);
					search.setEnabled(false);
				}
			}
		};
		autoCompleteTextView.addTextChangedListener(textWatcher);
	}

	private void requestAutoCompleteList(
			AutoCompleteTextView autoCompleteTextView, Editable inputText) {
		RequestLocation requestLocation = new RequestLocation(
				inputText.toString(), autoCompleteTextView);
		// add in the list to keep track of all the executing tasks
		requestLocationsList.add(requestLocation);
		requestLocation.execute();

		// Remove previous tasks that are in execution phase
		// Since user has updated the input.
		if (requestLocationsList.size() > 1) {
			for (int index = 0; index < requestLocationsList.size() - 2; index++) {
				RequestLocation requestLocationAsyncTask = requestLocationsList.get(index);
				requestLocationAsyncTask.cancel(true);
				requestLocationsList.remove(index);
			}
		}
	}

	//request for user location using fused location api
	private void requestUserLocation() {
		if (DemoUtility.servicesConnected(TravelActivity.this)) {
			if (!mLocationClient.isConnected())
				mLocationClient.connect();
		}
	}

	private class RequestLocation extends AsyncTask<Void, Void, AutoCompleteAdapter> {

		private String position;
		private AutoCompleteTextView autoCompleteTextView;

		public RequestLocation(String position, AutoCompleteTextView autoCompleteTextView) {
			this.position = position;
			this.autoCompleteTextView = autoCompleteTextView;
		}

		protected AutoCompleteAdapter doInBackground(Void... params) {
			String response = NetworkUtility.getSuggestedPositions(position);

			if (isCancelled()) {
				Log.d(TravelActivity.class.getName(), "Task Cancelled");
				return null;
			}

			LocationModel arrayModels[] = LocationModel.fromJson(response);

			if (arrayModels != null) {

                ArrayList<LocationModel> locationModels = new ArrayList<LocationModel>();
                locationModels.addAll(Arrays.asList(arrayModels));

				if (currentLocation != null)
					Collections.sort(locationModels, new PositionComparator(
							currentLocation));

				AutoCompleteAdapter arrayAdapter = new AutoCompleteAdapter(
						TravelActivity.this,
						android.R.layout.simple_dropdown_item_1line,
						android.R.id.text1, locationModels);

				return arrayAdapter;
			}
			return null;
		}

		protected void onPostExecute(AutoCompleteAdapter arrayAdapter) {
			if (arrayAdapter != null && !isCancelled()) {
				autoCompleteTextView.setAdapter(arrayAdapter);
			}
		}
	}
}
