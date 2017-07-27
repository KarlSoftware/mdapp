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

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class PharmaciesActivity extends AppCompatActivity
{
    private final Context mContext = this;

    private final MyTools mTools = new MyTools(mContext);

    private SQLiteDatabase mSqLiteDatabase;
    private Cursor mCursor;

    private InputMethodManager mInputMethodManager;

    private LinearLayout mToolbarSearchLayout;
    private EditText mToolbarSearchEditText;
    private FloatingActionButton mFloatingActionButton;
    private ListView mListView;
    private View mListViewEmpty;

    private boolean mActivityPaused = false;

    // Create activity
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Input manager
        mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        // Layout
        setContentView(R.layout.activity_pharmacies);

        // Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.pharmacies_toolbar);
        toolbar.setTitle(getString(R.string.pharmacies_title));

        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mToolbarSearchLayout = (LinearLayout) findViewById(R.id.pharmacies_toolbar_search_layout);
        mToolbarSearchEditText = (EditText) findViewById(R.id.pharmacies_toolbar_search);

        // Floating action button
        mFloatingActionButton = (FloatingActionButton) findViewById(R.id.pharmacies_fab);

        mFloatingActionButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                mToolbarSearchLayout.setVisibility(View.VISIBLE);
                mToolbarSearchEditText.requestFocus();

                mInputMethodManager.showSoftInput(mToolbarSearchEditText, 0);
            }
        });

        // List
        mListView = (ListView) findViewById(R.id.pharmacies_list);
        mListViewEmpty = findViewById(R.id.pharmacies_list_empty);

        // Get municipalities
        GetMunicipalitiesTask getMunicipalitiesTask = new GetMunicipalitiesTask();
        getMunicipalitiesTask.execute();
    }

    // Pause activity
    @Override
    protected void onPause()
    {
        super.onPause();

        mActivityPaused = true;
    }

    // Destroy activity
    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if(mCursor != null && !mCursor.isClosed()) mCursor.close();
        if(mSqLiteDatabase != null && mSqLiteDatabase.isOpen()) mSqLiteDatabase.close();
    }

    // Back button
    @Override
    public void onBackPressed()
    {
        if(mToolbarSearchLayout.getVisibility() == View.VISIBLE)
        {
            mToolbarSearchLayout.setVisibility(View.GONE);
            mToolbarSearchEditText.setText("");
        }
        else
        {
            super.onBackPressed();
        }
    }

    // Search button
    @Override
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event)
    {
        if(keyCode == KeyEvent.KEYCODE_SEARCH)
        {
            mToolbarSearchLayout.setVisibility(View.VISIBLE);
            mToolbarSearchEditText.requestFocus();

            mInputMethodManager.showSoftInput(mToolbarSearchEditText, 0);

            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    // Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_pharmacies, menu);
        return true;
    }

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
            case R.id.pharmacies_menu_overview:
            {
                Intent intent = new Intent(mContext, MainWebViewActivity.class);
                intent.putExtra("title", getString(R.string.pharmacies_menu_overview));
                intent.putExtra("uri", "https://legemiddelverket.no/import-og-salg/apotekdrift/apotekoversikt/");
                startActivity(intent);
                return true;
            }
            default:
            {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    // Get pharmacies
    private class GetMunicipalitiesTask extends AsyncTask<Void, Void, SimpleCursorAdapter>
    {
        @Override
        protected void onPostExecute(final SimpleCursorAdapter simpleCursorAdapter)
        {
            mListView.setAdapter(simpleCursorAdapter);
            mListView.setEmptyView(mListViewEmpty);

            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long id)
                {
                    TextView municipalityTextView = (TextView) view.findViewById(R.id.pharmacies_list_item_municipality);
                    String municipality = municipalityTextView.getText().toString();

                    Intent intent = new Intent(mContext, PharmaciesLocationActivity.class);
                    intent.putExtra("municipality", municipality);
                    startActivity(intent);
                }
            });

            mToolbarSearchEditText.addTextChangedListener(new TextWatcher()
            {
                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i2, int i3)
                {
                    simpleCursorAdapter.getFilter().filter(charSequence);
                }

                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) { }

                @Override
                public void afterTextChanged(Editable editable) { }
            });

            simpleCursorAdapter.setFilterQueryProvider(new FilterQueryProvider()
            {
                @Override
                public Cursor runQuery(CharSequence charSequence)
                {
                    if(mSqLiteDatabase != null)
                    {
                        String[] queryColumns = {SlDataSQLiteHelper.MUNICIPALITIES_COLUMN_ID, SlDataSQLiteHelper.MUNICIPALITIES_COLUMN_NAME};

                        if(charSequence.length() == 0) return mSqLiteDatabase.query(SlDataSQLiteHelper.TABLE_MUNICIPALITIES, queryColumns, null, null, null, null, SlDataSQLiteHelper.MUNICIPALITIES_COLUMN_NAME+" COLLATE NOCASE");

                        String query = charSequence.toString().trim();

                        return mSqLiteDatabase.query(SlDataSQLiteHelper.TABLE_MUNICIPALITIES, queryColumns, SlDataSQLiteHelper.MUNICIPALITIES_COLUMN_NAME+" LIKE "+mTools.sqe("%"+query+"%"), null, null, null, SlDataSQLiteHelper.MUNICIPALITIES_COLUMN_NAME+" COLLATE NOCASE");
                    }

                    return null;
                }
            });

            mFloatingActionButton.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.fab));
            mFloatingActionButton.setVisibility(View.VISIBLE);

            if(!mActivityPaused)
            {
                Handler handler = new Handler();

                handler.postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mToolbarSearchLayout.setVisibility(View.VISIBLE);
                        mToolbarSearchEditText.requestFocus();

                        mInputMethodManager.showSoftInput(mToolbarSearchEditText, 0);
                    }
                }, 500);
            }
        }

        @Override
        protected SimpleCursorAdapter doInBackground(Void... voids)
        {
            mSqLiteDatabase = new SlDataSQLiteHelper(mContext).getReadableDatabase();
            String[] queryColumns = {SlDataSQLiteHelper.MUNICIPALITIES_COLUMN_ID, SlDataSQLiteHelper.MUNICIPALITIES_COLUMN_NAME};
            mCursor = mSqLiteDatabase.query(SlDataSQLiteHelper.TABLE_MUNICIPALITIES, queryColumns, null, null, null, null, SlDataSQLiteHelper.MUNICIPALITIES_COLUMN_NAME+" COLLATE NOCASE");

            String[] fromColumns = {SlDataSQLiteHelper.MUNICIPALITIES_COLUMN_NAME};
            int[] toViews = {R.id.pharmacies_list_item_municipality};

            return new SimpleCursorAdapter(mContext, R.layout.activity_pharmacies_list_item, mCursor, fromColumns, toViews, 0);
        }
    }
}