/*
 *  Copyright (C) 2011 John Törnblom
 *
 * This file is part of TVHGuide.
 *
 * TVHGuide is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TVHGuide is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TVHGuide.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tvheadend.tvhguide;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.tvheadend.tvhguide.R.string;
import org.tvheadend.tvhguide.htsp.HTSListener;
import org.tvheadend.tvhguide.htsp.HTSService;
import org.tvheadend.tvhguide.intent.SearchEPGIntent;
import org.tvheadend.tvhguide.intent.SearchIMDbIntent;
import org.tvheadend.tvhguide.model.Channel;
import org.tvheadend.tvhguide.model.Programme;
import org.tvheadend.tvhguide.model.Recording;
import org.tvheadend.tvhguide.model.SeriesInfo;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

/**
 * 
 * @author john-tornblom
 */
public class ProgrammeListActivity extends ListActivity implements HTSListener {

	private ProgrammeListAdapter prAdapter;
	private Channel channel;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		TVHGuideApplication app = (TVHGuideApplication) getApplication();
		channel = app.getChannel(getIntent().getLongExtra("channelId", 0));

		if (channel == null) {
			finish();
			return;
		}

		Button btn = new Button(this);
		btn.setText(R.string.pr_get_more);
		btn.setOnClickListener(new OnClickListener() {

			public void onClick(View view) {
				Programme p = null;

				Iterator<Programme> it = channel.epg.iterator();
				long nextId = 0;

				while (it.hasNext()) {
					p = it.next();
					if (p.id != nextId && nextId != 0) {
						break;
					}
					nextId = p.nextId;
				}
				if (p == null)
					return;

				if (nextId == 0) {
					nextId = p.nextId;
				}
				if (nextId == 0) {
					nextId = p.id;
				}
				Intent intent = new Intent(ProgrammeListActivity.this,
						HTSService.class);
				intent.setAction(HTSService.ACTION_GET_EVENTS);
				intent.putExtra("eventId", nextId);
				intent.putExtra("channelId", channel.id);
				intent.putExtra("count", 10);
				startService(intent);
			}
		});

		getListView().addFooterView(btn);

		List<Programme> prList = new ArrayList<Programme>();
		prList.addAll(channel.epg);
		prAdapter = new ProgrammeListAdapter(this, prList);
		prAdapter.sort();
		setListAdapter(prAdapter);

		registerForContextMenu(getListView());
	}

	@Override
	protected void onResume() {
		super.onResume();
		TVHGuideApplication app = (TVHGuideApplication) getApplication();
		app.addListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		TVHGuideApplication app = (TVHGuideApplication) getApplication();
		app.removeListener(this);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Programme p = (Programme) prAdapter.getItem(position);

		Intent intent = new Intent(this, ProgrammeActivity.class);
		intent.putExtra("eventId", p.id);
		intent.putExtra("channelId", p.channel.id);
		startActivity(intent);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.string.menu_record:
		case R.string.menu_record_cancel:
		case R.string.menu_record_remove: {
			startService(item.getIntent());
			return true;
		}
		default: {
			return super.onContextItemSelected(item);
		}
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		Programme p = prAdapter.getItem(info.position);

		menu.setHeaderTitle(p.title);

		Intent intent = new Intent(this, HTSService.class);

		MenuItem item = null;

		if (p.recording == null) {
			intent.setAction(HTSService.ACTION_DVR_ADD);
			intent.putExtra("eventId", p.id);
			intent.putExtra("channelId", p.channel.id);
			item = menu.add(ContextMenu.NONE, R.string.menu_record,
					ContextMenu.NONE, R.string.menu_record);
		} else if (p.isRecording() || p.isScheduled()) {
			intent.setAction(HTSService.ACTION_DVR_CANCEL);
			intent.putExtra("id", p.recording.id);
			item = menu.add(ContextMenu.NONE, R.string.menu_record_cancel,
					ContextMenu.NONE, R.string.menu_record_cancel);
		} else {
			intent.setAction(HTSService.ACTION_DVR_DELETE);
			intent.putExtra("id", p.recording.id);
			item = menu.add(ContextMenu.NONE, R.string.menu_record_remove,
					ContextMenu.NONE, R.string.menu_record_remove);
		}

		item.setIntent(intent);

		item = menu.add(ContextMenu.NONE, R.string.search_hint,
				ContextMenu.NONE, R.string.search_hint);
		item.setIntent(new SearchEPGIntent(this, p.title));

		item = menu.add(ContextMenu.NONE, ContextMenu.NONE, ContextMenu.NONE,
				"IMDb");
		item.setIntent(new SearchIMDbIntent(this, p.title));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem item = null;
		Intent intent = null;

		item = menu.add(Menu.NONE, android.R.string.search_go, Menu.NONE,
				android.R.string.search_go);
		item.setIcon(android.R.drawable.ic_menu_search);

		intent = new Intent(ProgrammeListActivity.this, PlaybackActivity.class);
		intent.putExtra("channelId", channel.id);

		item = menu.add(Menu.NONE, R.string.ch_play, Menu.NONE,
				R.string.ch_play);
		item.setIcon(android.R.drawable.ic_menu_view);
		item.setIntent(intent);

		if (getActionBar() != null && channel != null) {
			getActionBar().setTitle(channel.name);

			if (channel.iconBitmap != null) {
				getActionBar().setIcon(
						new BitmapDrawable(getResources(), channel.iconBitmap));
			}
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.string.search_go: {
			onSearchRequested();
			return true;
		}
		default: {
			return super.onOptionsItemSelected(item);
		}
		}
	}

	@Override
	public boolean onSearchRequested() {
		Bundle bundle = new Bundle();
		bundle.putLong("channelId", channel.id);
		startSearch(null, false, bundle, false);
		return true;
	}

	public void onMessage(String action, final Object obj) {
		if (action.equals(TVHGuideApplication.ACTION_PROGRAMME_ADD)) {
			runOnUiThread(new Runnable() {

				public void run() {
					Programme p = (Programme) obj;
					if (channel != null && p.channel.id == channel.id) {
						prAdapter.add(p);
						prAdapter.notifyDataSetChanged();
						prAdapter.sort();
					}
				}
			});
		} else if (action.equals(TVHGuideApplication.ACTION_PROGRAMME_DELETE)) {
			runOnUiThread(new Runnable() {

				public void run() {
					Programme p = (Programme) obj;
					prAdapter.remove(p);
					prAdapter.notifyDataSetChanged();
				}
			});
		} else if (action.equals(TVHGuideApplication.ACTION_PROGRAMME_UPDATE)) {
			runOnUiThread(new Runnable() {

				public void run() {
					Programme p = (Programme) obj;
					prAdapter.updateView(getListView(), p);
				}
			});
		} else if (action.equals(TVHGuideApplication.ACTION_DVR_UPDATE)) {
			runOnUiThread(new Runnable() {

				public void run() {
					Recording rec = (Recording) obj;
					for (Programme p : prAdapter.list) {
						if (rec == p.recording) {
							prAdapter.updateView(getListView(), p);
							return;
						}
					}
				}
			});
		}
	}

	public String buildSeriesInfoString(SeriesInfo info) {
		if (info.onScreen != null && info.onScreen.length() > 0)
			return info.onScreen;

		String s = "";
		String season = this.getResources().getString(string.pr_season);
		String episode = this.getResources().getString(string.pr_episode);
		String part = this.getResources().getString(string.pr_part);

		if (info.onScreen.length() > 0) {
			return info.onScreen;
		}

		if (info.seasonNumber > 0) {
			if (s.length() > 0)
				s += ", ";
			s += String.format("%s %02d",
					season.toLowerCase(Locale.getDefault()), info.seasonNumber);
		}
		if (info.episodeNumber > 0) {
			if (s.length() > 0)
				s += ", ";
			s += String.format("%s %02d",
					episode.toLowerCase(Locale.getDefault()),
					info.episodeNumber);
		}
		if (info.partNumber > 0) {
			if (s.length() > 0)
				s += ", ";
			s += String.format("%s %d", part.toLowerCase(Locale.getDefault()),
					info.partNumber);
		}

		if (s.length() > 0) {
			s = s.substring(0, 1).toUpperCase(Locale.getDefault())
					+ s.substring(1);
		}

		return s;
	}

	class ProgrammeListAdapter extends ArrayAdapter<Programme> {

		Activity context;
		List<Programme> list;

		ProgrammeListAdapter(Activity context, List<Programme> list) {
			super(context, R.layout.programme_list_widget, list);
			this.context = context;
			this.list = list;
		}

		public void sort() {
			sort(new Comparator<Programme>() {

				public int compare(Programme x, Programme y) {
					return x.compareTo(y);
				}
			});
		}

		public void updateView(ListView listView, Programme programme) {
			for (int i = 0; i < listView.getChildCount(); i++) {
				View view = listView.getChildAt(i);
				int pos = listView.getPositionForView(view);
				Programme pr = (Programme) listView.getItemAtPosition(pos);

				if (view.getTag() == null || pr == null) {
					continue;
				}

				if (programme.id != pr.id) {
					continue;
				}

				ProgrammeListViewWrapper wrapper = (ProgrammeListViewWrapper) view
						.getTag();
				wrapper.repaint(programme);
			}
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			ProgrammeListViewWrapper wrapper = null;

			if (row == null) {
				LayoutInflater inflater = context.getLayoutInflater();
				row = inflater.inflate(R.layout.programme_list_widget, null,
						false);

				wrapper = new ProgrammeListViewWrapper(row);
				row.setTag(wrapper);

			} else {
				wrapper = (ProgrammeListViewWrapper) row.getTag();
			}

			Programme p = getItem(position);
			wrapper.repaint(p);
			return row;
		}
	}
}
