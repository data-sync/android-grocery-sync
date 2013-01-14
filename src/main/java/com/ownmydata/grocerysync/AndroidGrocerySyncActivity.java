package com.ownmydata.grocerysync;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.*;
import com.android.data.DataListAdapter;
import com.android.data.DataStore;
import com.android.data.Repository;
import com.android.data.services.DataService;
import org.apache.commons.lang3.StringUtils;
import org.ektorp.ViewQuery;

public class AndroidGrocerySyncActivity extends Activity {
    public static final String TAG = "GrocerySync";
    //splash screen
    protected SplashScreenDialog splashDialog;

    //main screen
    protected EditText addItemEditText;
    protected ListView itemListView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        addItemEditText = (EditText) findViewById(R.id.addItemEditText);
        itemListView = (ListView) findViewById(R.id.itemListView);

        showSplashScreen();
        setup();
    }

    private void setup() {
        boolean isServiceBound = getApplicationContext().bindService(new Intent(this, DataService.class), new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                DataService service = ((DataService.DataServiceBinder) binder).getService();
                onConnection(service.getDataStore());
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "Service Disconnected");
            }
        }, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "Service bound:" + isServiceBound);
    }

    private void onConnection(DataStore dataStore) {
        removeSplashScreen();
        Repository<Item> repository = new Repository<Item>(Item.class, dataStore);
        repository.defineViewBy("createdAt");
        ViewQuery viewQuery = repository.buildViewQuery("byCreatedAt").descending(true);
        DataListAdapter<Item> itemListViewAdapter = new DataListAdapter<Item>(repository, viewQuery, R.layout.grocery_list_item, true) {
            @Override
            public void populateView(View view, Item item) {
                TextView label = (TextView) view.findViewById(R.id.label);
                ImageView icon = (ImageView) view.findViewById(R.id.icon);

                label.setText(item.getText());
                icon.setImageResource(item.isChecked() ? R.drawable.list_area___checkbox___checked : R.drawable.list_area___checkbox___unchecked);
            }
        };
        itemListView.setAdapter(itemListViewAdapter);
        itemListView.setOnItemClickListener(clickListener(repository));
        itemListView.setOnItemLongClickListener(longClickListener(repository));
        addItemEditText.setOnKeyListener(keyListener(repository));
//        startReplications();
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

    protected void removeSplashScreen() {
        if (splashDialog != null) {
            splashDialog.dismiss();
            splashDialog = null;
        }
    }

    protected void showSplashScreen() {
        splashDialog = new SplashScreenDialog(this);
        splashDialog.show();
    }
}