package com.example.xyzreader.ui;

import android.app.ActivityOptions;
import android.app.LoaderManager;
import android.app.SharedElementCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = ArticleListActivity.class.toString();
    private Toolbar mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private CoordinatorLayout coordinatorLayout;
    private RecyclerView mRecyclerView;
    private String photoKey = "photo";
    private String transitionName;
    private DynamicHeightNetworkImageView sharedView;
    private int id;
    private String transitionNameKey = "transitionName";
    private String title;
    private String urlKey = "url";
    private String url;
    private String date;
    private String author;

    private String idKey = "id";

    private static final String bundleKey = "bundle";

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        /* Find the views by using their ID's */
        mToolbar = findViewById(R.id.toolbar);
        coordinatorLayout = findViewById(R.id.coordinator_layout);
        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        sharedView = findViewById(R.id.thumbnail);
        mRecyclerView = findViewById(R.id.recycler_view);

        /* Prepare and postpone the EnterTransition */
        prepareTransitions();
        postponeEnterTransition();

        getLoaderManager().initLoader(0, null, this);

        /* If there is no savedInstanceState, refresh the view */
        if (savedInstanceState == null) {
            refresh();
        }
    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        /* After the load is completed, start the postponed EnterTransition */
        startPostponedEnterTransition();

        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);
        mRecyclerView.setHasFixedSize(true);

        /* Make and show the Snackbar that informs the user that the article list has been updated */
        Snackbar snackbarRefresh = Snackbar.make(coordinatorLayout, getString(R.string.snackbar_text_update), Snackbar.LENGTH_LONG);
        int snackbarPadding = getResources().getInteger(R.integer.snackbar_padding);
        snackbarRefresh.getView().setPadding(snackbarPadding, 0, snackbarPadding, snackbarPadding);
        snackbarRefresh.show();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;

        Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sharedView = vh.thumbnailView;

                    /* Create a transitionName from the photoKey and id, and set it to the sharedView*/
                    id = vh.getAdapterPosition();
                    transitionName = photoKey + id;
                    sharedView.setTransitionName(transitionName);

                    Bundle bundle = ActivityOptions.makeSceneTransitionAnimation(
                            ArticleListActivity.this,
                            sharedView, transitionName).toBundle();
                    Intent intent = new Intent(Intent.ACTION_VIEW, ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition())));
                    intent.putExtra(bundleKey, bundle);
                    intent.putExtra(idKey, id);
                    intent.putExtra(transitionNameKey, transitionName);
                    startActivity(intent, bundle);
                }
            });
            return vh;
        }

        private Date parsePublishedDate() {
            date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            try {
                return dateFormat.parse(date);
            } catch (ParseException ex) {
                Log.e(TAG, ex.getMessage());
                Log.i(TAG, "passing today's date");
                return new Date();
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            title = mCursor.getString(ArticleLoader.Query.TITLE);
            holder.titleView.setText(title);
            Date publishedDate = parsePublishedDate();
            author = mCursor.getString(ArticleLoader.Query.AUTHOR);
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {

                holder.subtitleView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + "<br/>" + " by "
                                + author));
            } else {
                holder.subtitleView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate)
                                + "<br/>" + " by "
                                + author));
            }
            url = mCursor.getString(ArticleLoader.Query.THUMB_URL);

            holder.thumbnailView.setImageUrl(
                    url,
                    ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader());
            holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        DynamicHeightNetworkImageView thumbnailView;
        TextView titleView;
        TextView subtitleView;

        ViewHolder(View view) {
            super(view);
            thumbnailView = view.findViewById(R.id.thumbnail);
            titleView = view.findViewById(R.id.article_title);
            subtitleView = view.findViewById(R.id.article_subtitle);
        }
    }

    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);
        postponeEnterTransition();
    }

    private void prepareTransitions() {
        /* Set the ExitTransition, as well as the ExitSharedElementCallback */
        getWindow().setExitTransition(TransitionInflater.from(this).inflateTransition(R.transition.grid_exit_transition));
        setExitSharedElementCallback(new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                RecyclerView.ViewHolder viewHolder = mRecyclerView.findViewHolderForAdapterPosition(id);
                if (viewHolder == null) {
                    return;
                }
                sharedElements.put(names.get(0), viewHolder.itemView.findViewById(R.id.thumbnail));
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString(transitionNameKey, transitionName);
        savedInstanceState.putInt(idKey, id);
        savedInstanceState.putString(urlKey, url);
        super.onSaveInstanceState(savedInstanceState);
    }
}