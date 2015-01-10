package net.olejon.mdapp;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import org.json.JSONArray;

public class PharmaciesLocationActivity extends ActionBarActivity
{
    private final Context mContext = this;

    private SQLiteDatabase mSqLiteDatabase;
    private Cursor mCursor;

    private Toolbar mToolbar;
    private RecyclerView mRecyclerView;

    // Create activity
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Intent
        Intent intent = getIntent();
        final long locationId = intent.getLongExtra("id", 0);

        // Layout
        setContentView(R.layout.activity_pharmacies_location);

        // Toolbar
        mToolbar = (Toolbar) findViewById(R.id.pharmacies_location_toolbar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Recycler view
        mRecyclerView = (RecyclerView) findViewById(R.id.pharmacies_location_cards);

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(new PharmaciesLocationAdapter(mContext, new JSONArray()));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));

        // Get pharmacies
        GetPharmaciesTask getPharmaciesTask = new GetPharmaciesTask();
        getPharmaciesTask.execute(locationId);
    }

    // Destroy activity
    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if(mCursor != null && !mCursor.isClosed()) mCursor.close();
        if(mSqLiteDatabase != null && mSqLiteDatabase.isOpen()) mSqLiteDatabase.close();
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

    // Get pharmacies
    private class GetPharmaciesTask extends AsyncTask<Long, Void, Void>
    {
        @Override
        protected void onPostExecute(Void success)
        {
            if(mCursor.moveToFirst())
            {
                try
                {
                    mToolbar.setTitle(mCursor.getString(mCursor.getColumnIndexOrThrow("location")));

                    mRecyclerView.setAdapter(new PharmaciesLocationAdapter(mContext, new JSONArray(mCursor.getString(mCursor.getColumnIndexOrThrow("details")))));
                }
                catch(Exception e)
                {
                    Log.e("PharmaciesLocationActivity", Log.getStackTraceString(e));
                }
            }
        }

        @Override
        protected Void doInBackground(Long... longs)
        {
            mSqLiteDatabase = new FelleskatalogenSQLiteHelper(mContext).getReadableDatabase();

            mCursor = mSqLiteDatabase.query(FelleskatalogenSQLiteHelper.TABLE_PHARMACIES, null, FelleskatalogenSQLiteHelper.PHARMACIES_COLUMN_ID+" = "+longs[0], null, null, null, null);

            return null;
        }
    }
}