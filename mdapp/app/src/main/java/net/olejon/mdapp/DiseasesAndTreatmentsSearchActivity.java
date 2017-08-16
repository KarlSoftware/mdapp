package net.olejon.mdapp;

/*

Copyright 2017 Ole Jon Bjørkum

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see http://www.gnu.org/licenses/.

*/

import android.app.assist.AssistContent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;

public class DiseasesAndTreatmentsSearchActivity extends AppCompatActivity
{
	private final Context mContext = this;

	private final MyTools mTools = new MyTools(mContext);

	private Toolbar mToolbar;
	private ProgressBar mProgressBar;
	private SwipeRefreshLayout mSwipeRefreshLayout;
	private RecyclerView mRecyclerView;

	private String mSearchLanguage;
	private String mSearchString;

	// Create activity
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Connected?
		if(!mTools.isDeviceConnected())
		{
			mTools.showToast(getString(R.string.device_not_connected), 1);

			finish();

			return;
		}

		// Intent
		Intent intent = getIntent();

		mSearchLanguage = intent.getStringExtra("language");

		mSearchString = intent.getStringExtra("string");

		// Layout
		setContentView(R.layout.activity_diseases_and_treatments_search);

		// Toolbar
		mToolbar = (Toolbar) findViewById(R.id.diseases_and_treatments_search_toolbar);
		mToolbar.setTitle(getString(R.string.diseases_and_treatments_search_search, mSearchString));

		TextView mToolbarTextView = (TextView) mToolbar.getChildAt(1);
		mToolbarTextView.setEllipsize(TextUtils.TruncateAt.MIDDLE);

		setSupportActionBar(mToolbar);
		if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// Progress bar
		mProgressBar = (ProgressBar) findViewById(R.id.diseases_and_treatments_search_toolbar_progressbar);
		mProgressBar.setVisibility(View.VISIBLE);

		// Refresh
		mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.diseases_and_treatments_search_swipe_refresh_layout);
		mSwipeRefreshLayout.setColorSchemeResources(R.color.accent_blue, R.color.accent_purple, R.color.accent_teal);

		mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
		{
			@Override
			public void onRefresh()
			{
				search(mSearchString);
			}
		});

		// Recycler view
		mRecyclerView = (RecyclerView) findViewById(R.id.diseases_and_treatments_search_cards);

		mRecyclerView.setHasFixedSize(true);
		mRecyclerView.setAdapter(new DiseasesAndTreatmentsSearchAdapter(new JSONArray()));
		mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));

		// Search
		search(mSearchString);

		// Correct
		try
		{
			final RequestQueue requestQueue = new RequestQueue(new DiskBasedCache(getCacheDir(), 0), new BasicNetwork(new HurlStack()));

			requestQueue.start();

			JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, mTools.getApiUri()+"api/1/correct/?search="+URLEncoder.encode(mSearchString, "utf-8"), null, new Response.Listener<JSONObject>()
			{
				@Override
				public void onResponse(JSONObject response)
				{
					requestQueue.stop();

					try
					{
						final String correctSearchString = response.getString("correct");

						if(!correctSearchString.equals(""))
						{
							new MaterialDialog.Builder(mContext).title(R.string.correct_dialog_title).content(getString(R.string.correct_dialog_message, correctSearchString)).positiveText(R.string.correct_dialog_positive_button).negativeText(R.string.correct_dialog_negative_button).onPositive(new MaterialDialog.SingleButtonCallback()
							{
								@Override
								public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction)
								{
									ContentValues contentValues = new ContentValues();
									contentValues.put(DiseasesAndTreatmentsSQLiteHelper.COLUMN_STRING, correctSearchString);

									SQLiteDatabase sqLiteDatabase = new DiseasesAndTreatmentsSQLiteHelper(mContext).getWritableDatabase();

									sqLiteDatabase.delete(DiseasesAndTreatmentsSQLiteHelper.TABLE, DiseasesAndTreatmentsSQLiteHelper.COLUMN_STRING+" = "+mTools.sqe(mSearchString)+" COLLATE NOCASE", null);
									sqLiteDatabase.insert(DiseasesAndTreatmentsSQLiteHelper.TABLE, null, contentValues);

									sqLiteDatabase.close();

									mToolbar.setTitle(getString(R.string.diseases_and_treatments_search_search, correctSearchString));

									mProgressBar.setVisibility(View.VISIBLE);

									search(correctSearchString);
								}
							}).contentColorRes(R.color.black).positiveColorRes(R.color.dark_blue).negativeColorRes(R.color.black).show();
						}
					}
					catch(Exception e)
					{
						Log.e("DiseasesAndTreatments", Log.getStackTraceString(e));
					}
				}
			}, new Response.ErrorListener()
			{
				@Override
				public void onErrorResponse(VolleyError error)
				{
					requestQueue.stop();

					Log.e("DiseasesAndTreatments", error.toString());
				}
			});

			jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

			requestQueue.add(jsonObjectRequest);
		}
		catch(Exception e)
		{
			Log.e("DiseasesAndTreatments", Log.getStackTraceString(e));
		}
	}

	// Menu
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case android.R.id.home:
			{
				NavUtils.navigateUpFromSameTask(this);
				return true;
			}
			default:
			{
				return super.onOptionsItemSelected(item);
			}
		}
	}

	// Assistant
	@Override
	public void onProvideAssistContent(AssistContent assistContent)
	{
		super.onProvideAssistContent(assistContent);

		try
		{
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
			{
				String structuredJson = new JSONObject().put("@type", "MedicalCondition").put("name", mSearchString).toString();

				assistContent.setStructuredData(structuredJson);
			}
		}
		catch(Exception e)
		{
			Log.e("DiseasesAndTreatments", Log.getStackTraceString(e));
		}
	}

	// Search
	private void search(final String searchString)
	{
		try
		{
			final RequestQueue requestQueue = new RequestQueue(new DiskBasedCache(getCacheDir(), 0), new BasicNetwork(new HurlStack()));

			requestQueue.start();

			JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(mTools.getApiUri()+"api/1/diseases-and-treatments/"+mSearchLanguage+"/?search="+URLEncoder.encode(searchString, "utf-8"), new Response.Listener<JSONArray>()
			{
				@Override
				public void onResponse(JSONArray response)
				{
					requestQueue.stop();

					mProgressBar.setVisibility(View.GONE);
					mSwipeRefreshLayout.setRefreshing(false);

					if(mTools.isTablet())
					{
						mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
					}

					mRecyclerView.setAdapter(new DiseasesAndTreatmentsSearchAdapter(response));

					ContentValues contentValues = new ContentValues();
					contentValues.put(DiseasesAndTreatmentsSQLiteHelper.COLUMN_STRING, searchString);

					SQLiteDatabase sqLiteDatabase = new DiseasesAndTreatmentsSQLiteHelper(mContext).getWritableDatabase();

					sqLiteDatabase.delete(DiseasesAndTreatmentsSQLiteHelper.TABLE, DiseasesAndTreatmentsSQLiteHelper.COLUMN_STRING+" = "+mTools.sqe(searchString)+" COLLATE NOCASE", null);
					sqLiteDatabase.insert(DiseasesAndTreatmentsSQLiteHelper.TABLE, null, contentValues);

					sqLiteDatabase.close();
				}
			}, new Response.ErrorListener()
			{
				@Override
				public void onErrorResponse(VolleyError error)
				{
					requestQueue.stop();

					mProgressBar.setVisibility(View.GONE);
					mSwipeRefreshLayout.setRefreshing(false);

					mTools.showToast(getString(R.string.diseases_and_treatments_search_could_not_search), 1);

					Log.e("DiseasesAndTreatments", error.toString());

					finish();
				}
			});

			jsonArrayRequest.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

			requestQueue.add(jsonArrayRequest);
		}
		catch(Exception e)
		{
			Log.e("DiseasesAndTreatments", Log.getStackTraceString(e));
		}
	}

	// Adapter
	class DiseasesAndTreatmentsSearchAdapter extends RecyclerView.Adapter<DiseasesAndTreatmentsSearchAdapter.DiseasesAndTreatmentsSearchViewHolder>
	{
		final JSONArray mResults;

		DiseasesAndTreatmentsSearchAdapter(JSONArray results)
		{
			mResults = results;
		}

		class DiseasesAndTreatmentsSearchViewHolder extends RecyclerView.ViewHolder
		{
			final CardView card;
			final ImageView icon;
			final TextView title;
			final TextView text;

			DiseasesAndTreatmentsSearchViewHolder(View view)
			{
				super(view);

				card = (CardView) view.findViewById(R.id.diseases_and_treatments_search_card);
				icon = (ImageView) view.findViewById(R.id.diseases_and_treatments_search_card_icon);
				title = (TextView) view.findViewById(R.id.diseases_and_treatments_search_card_title);
				text = (TextView) view.findViewById(R.id.diseases_and_treatments_search_card_text);
			}
		}

		@Override
		public DiseasesAndTreatmentsSearchViewHolder onCreateViewHolder(ViewGroup viewGroup, int i)
		{
			View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.activity_diseases_and_treatments_search_card, viewGroup, false);
			return new DiseasesAndTreatmentsSearchViewHolder(view);
		}

		@Override
		public void onBindViewHolder(DiseasesAndTreatmentsSearchViewHolder viewHolder, int i)
		{
			try
			{
				JSONObject result = mResults.getJSONObject(i);

				final String title = result.getString("title");
				final String uri = result.getString("uri");
				String type = result.getString("type");
				String text = result.getString("text");

				viewHolder.title.setText(title);

				switch(type)
				{
					case "pubmed":
					{
						viewHolder.icon.setImageResource(R.drawable.pubmed);
						viewHolder.text.setText(text);
						break;
					}
					case "webofscience":
					{
						viewHolder.icon.setImageResource(R.drawable.webofscience);
						viewHolder.text.setText(text);
						break;
					}
					case "medlineplus":
					{
						viewHolder.icon.setImageResource(R.drawable.medlineplus);
						viewHolder.text.setText(text);
						break;
					}
					case "wikipedia":
					{
						viewHolder.icon.setImageResource(R.drawable.wikipedia);
						viewHolder.text.setText(text);
						break;
					}
					case "uptodate":
					{
						viewHolder.icon.setImageResource(R.drawable.uptodate);
						viewHolder.text.setText(text);
						break;
					}
					case "bmj":
					{
						viewHolder.icon.setImageResource(R.drawable.bmj);
						viewHolder.text.setText(text);
						break;
					}
					case "ao_surgery":
					{
						viewHolder.icon.setImageResource(R.drawable.ao_surgery);
						viewHolder.text.setText(text);
						break;
					}
					case "nhi":
					{
						viewHolder.icon.setImageResource(R.drawable.nhi);
						viewHolder.text.setText(text);
						break;
					}
					case "sml":
					{
						viewHolder.icon.setImageResource(R.drawable.sml);
						viewHolder.text.setText(text);
						break;
					}
					case "forskning":
					{
						viewHolder.icon.setImageResource(R.drawable.forskning);
						viewHolder.text.setText(text);
						break;
					}
					case "legehandboka":
					{
						viewHolder.icon.setImageResource(R.drawable.legehandboka);
						viewHolder.text.setText(text);
						break;
					}
					case "helsebiblioteket":
					{
						viewHolder.icon.setImageResource(R.drawable.helsebiblioteket);
						viewHolder.text.setText(text);
						break;
					}
					case "tidsskriftet":
					{
						viewHolder.icon.setImageResource(R.drawable.tidsskriftet);
						viewHolder.text.setText(text);
						break;
					}
					case "oncolex":
					{
						viewHolder.icon.setImageResource(R.drawable.oncolex);
						viewHolder.text.setText(text);
						break;
					}
					case "brukerhandboken":
					{
						viewHolder.icon.setImageResource(R.drawable.brukerhandboken);
						viewHolder.text.setText(text);
						break;
					}
					case "analyseoversikten":
					{
						viewHolder.icon.setImageResource(R.drawable.analyseoversikten);
						viewHolder.text.setText(text);
						break;
					}
					case "helsenorge":
					{
						viewHolder.icon.setImageResource(R.drawable.helsenorge);
						viewHolder.text.setText(text);
						break;
					}
					default:
					{
						viewHolder.icon.setImageResource(R.mipmap.ic_launcher);
						viewHolder.text.setText(text);
						break;
					}
				}

				viewHolder.card.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View view)
					{
						Intent intent = new Intent(mContext, DiseasesAndTreatmentsSearchWebViewActivity.class);
						intent.putExtra("title", title);
						intent.putExtra("uri", uri);
						intent.putExtra("search", mSearchString);
						mContext.startActivity(intent);
					}
				});
			}
			catch(Exception e)
			{
				Log.e("DiseasesAndTreatments", Log.getStackTraceString(e));
			}
		}

		@Override
		public int getItemCount()
		{
			return mResults.length();
		}
	}
}