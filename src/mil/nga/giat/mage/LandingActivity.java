package mil.nga.giat.mage;

import mil.nga.giat.mage.help.HelpFragment;
import mil.nga.giat.mage.login.LoginActivity;
import mil.nga.giat.mage.map.MapFragment;
import mil.nga.giat.mage.navigation.DrawerItem;
import mil.nga.giat.mage.newsfeed.NewsFeedFragment;
import mil.nga.giat.mage.newsfeed.PeopleFeedFragment;
import mil.nga.giat.mage.preferences.PublicPreferencesFragment;
import mil.nga.giat.mage.sdk.utils.UserUtility;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;

/**
 * FIXME: Currently a mock of what a landing page might look like. Could be
 * replaced entirely if need be. Menu options do exist.
 * 
 * 
 */
public class LandingActivity extends Activity implements ListView.OnItemClickListener {	
	
	private DrawerItem[] drawerItems;
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerToggle;
    private DrawerItem currentActivity;
    private int activeTimeFilter = 0;
    private String currentTitle = "";
    private DrawerItem mapItem;
    private boolean switchFragment;
    private DrawerItem itemToSwitchTo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

		MAGE mage = (MAGE) getApplication();

		// Start location services
		mage.initLocationService();

		// Start fetching and pushing observations and locations
		mage.startFetching();
		mage.startPushing();

		// Pull static layers and features just once
		mage.pullStaticFeaturesOneTime();
 		
 		DrawerItem viewHeader = new DrawerItem(-1, "Views");
 		viewHeader.isHeader(true);
 		DrawerItem extraHeader = new DrawerItem(-1, "Extra");
 		extraHeader.isHeader(true);
 		mapItem = new DrawerItem(0, "Map", R.drawable.ic_globe_white, new MapFragment());

 		drawerItems = new DrawerItem[] {
	        mapItem,
	        new DrawerItem(1, "Observations", R.drawable.ic_map_marker_white, new NewsFeedFragment()),
	        new DrawerItem(2, "People", R.drawable.ic_users_white, new PeopleFeedFragment()),
	        new DrawerItem(3, "Settings", R.drawable.ic_settings_white, new PublicPreferencesFragment()),
	        new DrawerItem(4, "Help", R.drawable.ic_question_circle_white, new HelpFragment()),
	        new DrawerItem(5, "Logout", R.drawable.ic_power_off_white)
 		};

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.left_drawer);

        // Set the adapter for the list view
        drawerList.setAdapter(new ArrayAdapter<DrawerItem>(this, R.layout.drawer_list_item, drawerItems) {
        	@Override
        	public View getView (int position, View view, ViewGroup parent) {
        		if (view == null) {
        			LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        			view = inflater.inflate(R.layout.drawer_list_item, null);
        		}
        		DrawerItem item = getItem(position);
        		
		        if (item.isHeader()) {
		        	view.findViewById(R.id.drawer_divider).setVisibility(View.GONE);
		        	view.findViewById(R.id.header_divider).setVisibility(View.VISIBLE);
		        } else {
		        	view.findViewById(R.id.header_divider).setVisibility(View.GONE);
		        	view.findViewById(R.id.drawer_divider).setVisibility(View.VISIBLE);
		        }
        		TextView text = (TextView)view.findViewById(R.id.drawer_item_text);
        		text.setText(item.getItemText());
        		if (item.getDrawableId() != null) {
	        		ImageView iv = (ImageView)view.findViewById(R.id.drawer_item_icon);
	        		iv.setImageResource(item.getDrawableId());
        		}
        		TextView countView = (TextView)view.findViewById(R.id.drawer_count);
        		if (item.getCount() != 0) {
        			countView.setVisibility(View.VISIBLE);
        			countView.setText("" + item.getCount());
        		} else {
        			countView.setVisibility(View.GONE);
        		}
        		
        		return view;
        	}
        });
        
        // Set the list's click listener
        drawerList.setOnItemClickListener(this);
        
        actionbarToggleHandler();
        
        goToMap();
    }
    
    private void goToMap() {
        FragmentManager fragmentManager = getFragmentManager();
	    fragmentManager.beginTransaction()
	                   .replace(R.id.content_frame, mapItem.getFragment())
	                   .commit();
	    getActionBar().setTitle("MAGE");
	    currentActivity = mapItem;
    }
    
    private void actionbarToggleHandler() {  
        getActionBar().setHomeButtonEnabled(true);  
        getActionBar().setDisplayHomeAsUpEnabled(true);  
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,  
                  R.drawable.ic_drawer, R.string.drawer_open,  
                  R.string.drawer_close) {  
             @Override  
             public void onDrawerClosed(View drawerView) {
            	 super.onDrawerClosed(drawerView);
            	 getActionBar().setTitle(currentTitle);
    
            	 if (drawerView.getId() == R.id.filter_drawer) {
            		 setFilter(); 
            	 } else if (drawerView.getId() == R.id.left_drawer && switchFragment) {
            		 switchFragment = false;
         	
         	        // Insert the fragment by replacing any existing fragment
         	        FragmentManager fragmentManager = getFragmentManager();
         	        fragmentManager.beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
         	                       .add(R.id.content_frame, itemToSwitchTo.getFragment())
         	                       .commit();
         	        currentActivity = itemToSwitchTo;
         	        getActionBar().setTitle(itemToSwitchTo.getItemText());
            	 }
            	 invalidateOptionsMenu();
             }  
             @Override  
             public void onDrawerOpened(View drawerView) { 
            	 super.onDrawerOpened(drawerView);
                  invalidateOptionsMenu();
                  currentTitle = (String) getActionBar().getTitle();
                  if (drawerView.getId() == R.id.left_drawer) {
                	  getActionBar().setTitle("Navigation");
                  } else if (drawerView.getId() == R.id.filter_drawer) {
                	  getActionBar().setTitle("Filter");
                	  RadioGroup rg = (RadioGroup)findViewById(R.id.time_filter_radio_gorup);
                	  int checkedFilter = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(getResources().getString(R.string.activeTimeFilterKey), R.id.none_rb);
                	  rg.check(checkedFilter); 
                  }
             }  
        };
        drawerLayout.setDrawerListener(drawerToggle);
   }
    
    public void filterOkClick(View v) {
    	//setFilter();
    	drawerLayout.closeDrawer(findViewById(R.id.filter_drawer));
    }
    
    public void setFilter() {
    	RadioGroup rg = (RadioGroup)findViewById(R.id.time_filter_radio_gorup);
		 if (activeTimeFilter != rg.getCheckedRadioButtonId()) {
			 activeTimeFilter = rg.getCheckedRadioButtonId();
			 PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putInt(getResources().getString(R.string.activeTimeFilterKey), rg.getCheckedRadioButtonId()).commit();
		 }
    }
    
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
    	drawerToggle.syncState();
    	super.onPostCreate(savedInstanceState);
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		MAGE mage = (MAGE) getApplication();
		mage.destroyFetching();
		mage.destroyPushing();
		mage.destroyLocationService();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if ( keyCode == KeyEvent.KEYCODE_MENU ) {
	    	boolean drawerOpen = drawerLayout.isDrawerOpen(drawerList);
	    	if (!drawerOpen) {
	    		drawerLayout.openDrawer(drawerList);
	    	} else {
	    		drawerLayout.closeDrawer(drawerList);
	    	}
	        //Put the code for an action menu from the top here
	        return true;
	    } else if (keyCode == KeyEvent.KEYCODE_BACK) {
	    	if (currentActivity != mapItem) {
	    		goToMap();
	    		return true;
	    	}
	    }
	    return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (drawerToggle.onOptionsItemSelected(item)) {
			// drawer handled the event
			return true;
		}
		switch(item.getItemId()) {
		case R.id.filter_button:
	    	DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
	    	View filterDrawer = findViewById(R.id.filter_drawer);
	    	if (!drawerLayout.isDrawerOpen(filterDrawer)) {
	    		drawerLayout.openDrawer(filterDrawer);
	    	} else {
	    		drawerLayout.closeDrawer(filterDrawer);
	    	}
	    	break;
		}
		
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Takes you to the home screen
	 */
	@Override
	public void onBackPressed() {
		Intent startMain = new Intent(Intent.ACTION_MAIN);
		startMain.addCategory(Intent.CATEGORY_HOME);
		startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(startMain);
	}

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
    	
    	ArrayAdapter<DrawerItem> adapter = (ArrayAdapter<DrawerItem>) adapterView.getAdapter();
    	itemToSwitchTo = adapter.getItem(position);
        if (itemToSwitchTo.getFragment() == null) {
	        switch (itemToSwitchTo.getId()) {
	            case 5: {
	                // TODO : wipe user certs, really just wipe out the token from shared preferences
	                UserUtility.getInstance(getApplicationContext()).clearTokenInformation();
	                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
	                finish();
	                return;
	            }
	            default: {
	                // TODO not sure what to do here, if anything (fix your code)
	            	// could just be unclickable
	            }
	        }
        }
        if (currentActivity != itemToSwitchTo && itemToSwitchTo.getFragment() != null) {
        	switchFragment = true;
        	
        	FragmentManager fragmentManager = getFragmentManager();
 	        fragmentManager.beginTransaction()
 	                       .remove(currentActivity.getFragment())
 	                       .commit();
        }

        // Highlight the selected item, update the title, and close the drawer
        drawerList.setItemChecked(position, true);
        drawerLayout.closeDrawer(drawerList);        
    }
}