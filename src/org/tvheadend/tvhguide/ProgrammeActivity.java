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

import java.util.Locale;

import org.tvheadend.tvhguide.R.string;
import org.tvheadend.tvhguide.htsp.HTSListener;
import org.tvheadend.tvhguide.htsp.HTSService;
import org.tvheadend.tvhguide.intent.SearchEPGIntent;
import org.tvheadend.tvhguide.intent.SearchIMDbIntent;
import org.tvheadend.tvhguide.model.Channel;
import org.tvheadend.tvhguide.model.Programme;
import org.tvheadend.tvhguide.model.SeriesInfo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

/**
 * 
 * @author john-tornblom
 */
public class ProgrammeActivity extends Activity implements HTSListener {

	private Programme programme;
	private ImageView state;
	private Channel channel;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		TVHGuideApplication app = (TVHGuideApplication) getApplication();
		channel = app.getChannel(getIntent().getLongExtra("channelId", 0));
		if (channel == null) {
			finish();
			return;
		}

		long eventId = getIntent().getLongExtra("eventId", 0);
		for (Programme p : channel.epg) {
			if (p.id == eventId) {
				programme = p;
				break;
			}
		}

		if (programme == null) {
			finish();
			return;
		}

		setContentView(R.layout.programme_layout);

		TextView text = (TextView) findViewById(R.id.pr_title);
		text.setText(programme.title);

		text = (TextView) findViewById(R.id.pr_channel);
		text.setText(channel.name);

		text = (TextView) findViewById(R.id.pr_airing);
		text.setText(DateFormat.getLongDateFormat(text.getContext()).format(
				programme.start)
				+ "   "
				+ DateFormat.getTimeFormat(text.getContext()).format(
						programme.start)
				+ " - "
				+ DateFormat.getTimeFormat(text.getContext()).format(
						programme.stop));

		if (programme.summary.length() == 0
				&& programme.description.length() == 0) {
			View v = findViewById(R.id.pr_summay_and_desc_layout);
			v.setVisibility(View.GONE);
		} else {
			text = (TextView) findViewById(R.id.pr_summary);
			text.setText(programme.summary);
			if (programme.summary.length() == 0)
				text.setVisibility(View.GONE);

			text = (TextView) findViewById(R.id.pr_desc);
			text.setText(programme.description);
			if (programme.description.length() == 0)
				text.setVisibility(View.GONE);
		}

		String s = buildSeriesInfoString(programme.seriesInfo);
		if (s.length() > 0) {
			text = (TextView) findViewById(R.id.pr_series_info);
			text.setText(s);
		} else {
			View v = findViewById(R.id.pr_series_info_row);
			v.setVisibility(View.GONE);
			v = findViewById(R.id.pr_series_info_sep);
			v.setVisibility(View.GONE);
		}

		SparseArray<String> contentTypes = TVHGuideApplication
				.getContentTypes(this.getResources());
		s = contentTypes.get(programme.contentType, "");
		if (s.length() > 0) {
			text = (TextView) findViewById(R.id.pr_content_type);
			text.setText(s);
		} else {
			View v = findViewById(R.id.pr_content_type_row);
			v.setVisibility(View.GONE);
			v = findViewById(R.id.pr_content_type_sep);
			v.setVisibility(View.GONE);
		}

		if (programme.starRating > 0) {
			RatingBar starRating = (RatingBar) findViewById(R.id.pr_star_rating);
			starRating.setRating((float) programme.starRating / 10.0f);

			text = (TextView) findViewById(R.id.pr_star_rating_txt);
			text.setText("(" + programme.starRating + "/" + 100 + ")");
		} else {
			View v = findViewById(R.id.pr_star_rating_row);
			v.setVisibility(View.GONE);
		}

		state = (ImageView) findViewById(R.id.pr_state);
		update(programme);
	}

	protected void update(Programme p) {
		RecordUtil.applyRecording(p.recording, state);
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
	public void onMessage(String action, final Object obj) {
		if (action.equals(TVHGuideApplication.ACTION_PROGRAMME_UPDATE)) {
			runOnUiThread(new Runnable() {

				public void run() {
					Programme p = (Programme) obj;
					update(p);
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
			if (info.seasonCount > 0)
				s += String.format("/%02d", info.seasonCount);
		}
		if (info.episodeNumber > 0) {
			if (s.length() > 0)
				s += ", ";
			s += String.format("%s %02d",
					episode.toLowerCase(Locale.getDefault()),
					info.episodeNumber);
			if (info.episodeCount > 0)
				s += String.format("/%02d", info.episodeCount);
		}
		if (info.partNumber > 0) {
			if (s.length() > 0)
				s += ", ";
			s += String.format("%s %d", part.toLowerCase(Locale.getDefault()),
					info.partNumber);
			if (info.partCount > 0)
				s += String.format("/%02d", info.partCount);
		}

		if (s.length() > 0) {
			s = s.substring(0, 1).toUpperCase(Locale.getDefault())
					+ s.substring(1);
		}

		return s;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem item = null;

		if (programme.title != null) {
			item = menu.add(Menu.NONE, android.R.string.search_go, Menu.NONE,
					android.R.string.search_go);
			item.setIntent(new SearchEPGIntent(this, programme.title));
			item.setIcon(android.R.drawable.ic_menu_search);

			item = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, "IMDb");
			item.setIntent(new SearchIMDbIntent(this, programme.title));
			item.setIcon(android.R.drawable.ic_menu_info_details);
		}

		Intent intent = new Intent(this, HTSService.class);

		if (programme.recording == null) {
			intent.setAction(HTSService.ACTION_DVR_ADD);
			intent.putExtra("eventId", programme.id);
			intent.putExtra("channelId", programme.channel.id);
			item = menu.add(Menu.NONE, R.string.menu_record, Menu.NONE,
					R.string.menu_record);
			item.setIcon(android.R.drawable.ic_menu_save);
		} else if (programme.isRecording() || programme.isScheduled()) {
			intent.setAction(HTSService.ACTION_DVR_CANCEL);
			intent.putExtra("id", programme.recording.id);
			item = menu.add(Menu.NONE, R.string.menu_record_cancel, Menu.NONE,
					R.string.menu_record_cancel);
			item.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		} else {
			intent.setAction(HTSService.ACTION_DVR_DELETE);
			intent.putExtra("id", programme.recording.id);
			item = menu.add(Menu.NONE, R.string.menu_record_remove, Menu.NONE,
					R.string.menu_record_remove);
			item.setIcon(android.R.drawable.ic_menu_delete);
		}

		if (getActionBar() != null && channel != null) {
			getActionBar().setTitle(channel.name);

			if (channel.iconBitmap != null) {
				getActionBar().setIcon(
						new BitmapDrawable(getResources(), channel.iconBitmap));
			}
		}

		item.setIntent(intent);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean rebuild = false;
		if (programme.recording == null) {
			rebuild = menu.findItem(R.string.menu_record) == null;
		} else if (programme.isRecording() || programme.isScheduled()) {
			rebuild = menu.findItem(R.string.menu_record_cancel) == null;
		} else {
			rebuild = menu.findItem(R.string.menu_record_remove) == null;
		}

		if (rebuild) {
			menu.clear();
			return onCreateOptionsMenu(menu);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.string.menu_record_remove:
		case R.string.menu_record_cancel:
		case R.string.menu_record:
			startService(item.getIntent());
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
