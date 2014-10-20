package com.sheikhmuneeb.demo.utilty;
import java.util.ArrayList;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import com.sheikhmuneeb.demo.model.LocationModel;

/**
 * Custom adapter for AutoCompleteTextView
 * @author Sheikh Muneeb
 *
 */
public class AutoCompleteAdapter extends ArrayAdapter<LocationModel>
		implements
			Filterable {

	private ArrayList<LocationModel> mOriginalValues;
	private ArrayList<LocationModel> fullList;
	private ArrayFilter mFilter;

	public AutoCompleteAdapter(Context context, int resource,
			int textViewResourceId, ArrayList<LocationModel> objects) {
		super(context, resource, textViewResourceId, objects);
		fullList = objects;
		mOriginalValues = new ArrayList<LocationModel>(fullList);
	}

	@Override
	public int getCount() {
		return fullList.size();
	}

	@Override
	public LocationModel getItem(int position) {
		return fullList.get(position);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return super.getView(position, convertView, parent);
	}

	@Override
	public Filter getFilter() {
		if (mFilter == null) {
			mFilter = new ArrayFilter();
		}
		return mFilter;
	}

	private class ArrayFilter extends Filter {
		private Object lock;

		@Override
		protected FilterResults performFiltering(CharSequence prefix) {
			FilterResults results = new FilterResults();

			if (mOriginalValues == null) {
				synchronized (lock) {
					mOriginalValues = new ArrayList<LocationModel>(
							fullList);
				}
			}

			if (prefix == null || prefix.length() == 0) {
//				synchronized (lock) {
					ArrayList<LocationModel> list = new ArrayList<LocationModel>(
							mOriginalValues);
					results.values = list;
					results.count = list.size();
//				}
			} else {
				final String prefixString = prefix.toString().toLowerCase();
				ArrayList<LocationModel> values = mOriginalValues;
				int count = values.size();

				ArrayList<LocationModel> newValues = new ArrayList<LocationModel>(count);

				for (int i = 0; i < count; i++) {
					LocationModel item = values.get(i);
					if (item.name.toLowerCase().contains(prefixString)) {
						newValues.add(item);
					}
				}

				results.values = newValues;
				results.count = newValues.size();
			}

			return results;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint,
				FilterResults results) {

			if (results.values != null) {
				fullList = (ArrayList<LocationModel>) results.values;
			} else {
				fullList = new ArrayList<LocationModel>();
			}
			if (results.count > 0) {
				notifyDataSetChanged();
			} else {
				notifyDataSetInvalidated();
			}
		}
	}
}