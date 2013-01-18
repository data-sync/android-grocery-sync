package com.ownmydata.grocerysync;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.*;
import com.android.data.*;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.ektorp.ViewQuery;

import java.io.Serializable;
import java.util.Map;

import static com.android.data.DataService.GROUPS;
import static com.android.data.DataService.REMOTE_DB;
import static com.google.common.collect.Sets.newHashSet;
import static com.ownmydata.grocerysync.Item.USERS;

public class AndroidGrocerySyncActivity extends Activity {
    public static final String TAG = "GrocerySync";
    //splash screen
    protected SplashScreenDialog splashDialog;

    //main screen
    protected EditText addItemEditText;
    protected ListView itemListView;
    private DataServiceConnection serviceConnection;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        addItemEditText = (EditText) findViewById(R.id.addItemEditText);
        itemListView = (ListView) findViewById(R.id.itemListView);

        showSplashScreen();
        setup();
    }

    private void setup() {
        super.onResume();
        Intent intent = intentWithExtras(DataService.class, intentExtrasBuilder()
                .put(GROUPS, newHashSet(USERS))
                .put(REMOTE_DB, "http://10.0.2.2:5984/data-test")
                .build());

        serviceConnection = new DataServiceConnection() {
            @Override
            protected void onDataConnection(DataStore dataStore) {
                onConnection(dataStore);
            }
        };
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        unbindService(serviceConnection);
        super.onPause();
    }

    private void onConnection(DataStore dataStore) {
        removeSplashScreen();
        ItemRepository repository = new ItemRepository(dataStore);
        ViewQuery viewQuery = repository.buildViewQuery("byCreatedAt").descending(true);
        itemListView.setAdapter(new ItemDataListAdapter(repository, viewQuery));
        itemListView.setOnItemClickListener(clickListener(repository));
        itemListView.setOnItemLongClickListener(longClickListener(repository));
        addItemEditText.setOnKeyListener(keyListener(repository));
    }

    private View.OnKeyListener keyListener(final Repository<Item> repository) {
        return new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN)
                        && (keyCode == KeyEvent.KEYCODE_ENTER)) {

                    String inputText = addItemEditText.getText().toString();
                    if (StringUtils.isNotBlank(inputText)) {
                        Item item = new Item(inputText, false);
                        repository.add(item);
                        Log.d(TAG, "Added item with id: " + item.getId());
                    }
                    addItemEditText.setText("");
                    return true;
                }
                return false;
            }
        };
    }

    private AdapterView.OnItemClickListener clickListener(final Repository<Item> repository) {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Item item = (Item) parent.getItemAtPosition(position);
                item.toggleCheck();
                repository.update(item);
            }
        };
    }

    private AdapterView.OnItemLongClickListener longClickListener(final Repository<Item> repository) {
        return new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final Item item = (Item) parent.getItemAtPosition(position);
                String itemText = item.getText();

                AlertDialog.Builder builder = new AlertDialog.Builder(AndroidGrocerySyncActivity.this);
                AlertDialog alert = builder.setTitle("Delete Item?")
                        .setMessage("Are you sure you want to delete \"" + itemText + "\"?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                repository.remove(item);
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        })
                        .create();

                alert.show();
                return true;
            }
        };
    }

    private void removeSplashScreen() {
        if (splashDialog != null) {
            splashDialog.dismiss();
            splashDialog = null;
        }
    }

    private void showSplashScreen() {
        splashDialog = new SplashScreenDialog(this);
        splashDialog.show();
    }

    private ImmutableMap.Builder<String, Serializable> intentExtrasBuilder() {
        return new ImmutableMap.Builder<String, Serializable>();
    }

    private Intent intentWithExtras(Class<DataService> serviceClass, ImmutableMap<String, Serializable> intentExtras) {
        Intent intent = new Intent(this, serviceClass);
        for (Map.Entry<String, Serializable> intentExtra : intentExtras.entrySet()) {
            intent.putExtra(intentExtra.getKey(), intentExtra.getValue());
        }
        return intent;
    }

    private static class ItemDataListAdapter extends DataListAdapter<Item> {
        public ItemDataListAdapter(Repository<Item> repository, ViewQuery viewQuery) {
            super(repository, viewQuery, R.layout.grocery_list_item, true);
        }

        @Override
        public void populateView(View view, Item item, int position) {
            TextView label = (TextView) view.findViewById(R.id.label);
            ImageView icon = (ImageView) view.findViewById(R.id.icon);

            label.setText(item.getText());
            icon.setImageResource(item.isChecked() ? R.drawable.list_area___checkbox___checked : R.drawable.list_area___checkbox___unchecked);
        }
    }

    private static class ItemRepository extends Repository<Item> {
        public ItemRepository(DataStore dataStore) {
            super(Item.class, dataStore);
            defineViewBy("createdAt");
        }
    }
}
