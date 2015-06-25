package org.tasks.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.api.Filter;

import org.tasks.R;
import org.tasks.filters.FilterCounter;
import org.tasks.filters.FilterProvider;
import org.tasks.injection.InjectingFragmentActivity;

import javax.inject.Inject;

public class FilterSelectionActivity extends InjectingFragmentActivity {

    public static final String EXTRA_FILTER_NAME = "extra_filter_name";
    public static final String EXTRA_FILTER_SQL = "extra_filter_query";

    @Inject FilterProvider filterProvider;
    @Inject FilterCounter filterCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final FilterAdapter filterAdapter = new FilterAdapter(filterProvider, filterCounter, this, null, false);
        filterAdapter.populateList();

        new AlertDialog.Builder(this, R.style.Tasks_Dialog)
                .setSingleChoiceItems(filterAdapter, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final Filter selectedFilter = (Filter) filterAdapter.getItem(which);
                        setResult(RESULT_OK, new Intent() {{
                            putExtra(EXTRA_FILTER_NAME, selectedFilter.listingTitle);
                            putExtra(EXTRA_FILTER_SQL, selectedFilter.getSqlQuery());
                        }});
                        dialog.dismiss();
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                    }
                })
                .show();
    }
}
