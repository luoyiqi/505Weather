package com.example.l.myweather;


import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.*;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;




public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private ViewPager viewPager;
    private ArrayList<Fragment> fragmentArrayList;
    private ArrayList<Fragment> cityManagetFragment;
    private Button add;
    private MyFragmentAdapter mainAdapter;
    private MyFragmentAdapter cityManagerAdapter;
    private SQLiteDatabase db;
    private List<String> city_list;
    private List<String> cityId_list;
    private int start_count;
    private SharedPreferences startCount;
    private SharedPreferences.Editor editor;
    private TextView tip_text;
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private LinearLayout linearLayout;
    private boolean FIRST_START = false;
    private RelativeLayout relativeLayout;
    private RequestQueue mQueue;
    private ImageLoader imageLoader;
    private RelativeLayout headerLayout;
    private List<TextView> cityViewList;
    private List<TextView> weatherViewList;
    private List<NetworkImageView> networkImageViewList;
    private List<LinearLayout> linearLayoutList;
    public static int DELETE_FLAG = 0;
    private SharedPreferences sharedPreferences;
    public static Context context = MyApplication.getContext();
    private int flag = 0;
    private int pageSelected = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_main);
        initView();

        initCityList();
        setItemSelected(0);
        startService(new Intent(this, UpdateService.class));
}

    public void initView(){
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mQueue = Volley.newRequestQueue(this);
        imageLoader = new ImageLoader(mQueue,new BitmapCache());
        relativeLayout = (RelativeLayout)findViewById(R.id.relative_layout);
        headerLayout = (RelativeLayout) findViewById(R.id.header_layout);
        cityManagetFragment = new ArrayList<Fragment>();
        fragmentArrayList = new ArrayList<Fragment>();
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        add = (Button) findViewById(R.id.add);
        mainAdapter = new MyFragmentAdapter(getSupportFragmentManager(),fragmentArrayList);
        cityManagerAdapter = new MyFragmentAdapter(getSupportFragmentManager(),cityManagetFragment);
        CityDataBase cityDataBase = new CityDataBase(this,"CITY_LIST",null,1);
        db = cityDataBase.getWritableDatabase();
        city_list = new ArrayList<String>();
        cityId_list = new ArrayList<String>();
        cityViewList = new ArrayList<TextView>();
        weatherViewList = new ArrayList<TextView>();
        linearLayoutList = new ArrayList<LinearLayout>();
        networkImageViewList = new ArrayList<NetworkImageView>();

        tip_text = (TextView) findViewById(R.id.tip_text);
        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        navigationView.setNavigationItemSelectedListener(onNavigationItemSelectedListener);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);
        toolbar.setNavigationOnClickListener(onClickListener);
        add.setOnClickListener(this);
        startCount = getPreferences(MODE_APPEND);
        editor = startCount.edit();
        start_count = startCount.getInt("start_count",0);
        if (start_count == 0){
            firstStart();
        }
        start_count++;
        editor.putInt("start_count", start_count);
        editor.apply();
        viewPager.addOnPageChangeListener(onPageChangeListener);
        viewPager.setOffscreenPageLimit(10);
        linearLayout = (LinearLayout)findViewById(R.id.linear_layout);

        
    }
    public void initCityList(){
        Cursor cursor = db.query("city",null,null,null,null,null,null);
        if (cursor.moveToFirst()){
            do {
                city_list.add(cursor.getString(cursor.getColumnIndex("city")));
                cityId_list.add(cursor.getString(cursor.getColumnIndex("city_id")));
                } while (cursor.moveToNext());
        }

    }
    public void initViewPager(){

        if (city_list.size() > 0){
            for (int i = 0;i < city_list.size();i++){
                Fragment fragment = BlankFragment.newInstance(cityId_list.get(i),i);
                fragmentArrayList.add(fragment);
            }
            mainAdapter.notifyDataSetChanged();
            viewPager.setCurrentItem(0);
            toolbar.setTitle(city_list.get(0));
        } else {
            toolbar.setTitle("请添加城市");
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String city_id;
        String district;
        if (requestCode == 1){
            switch (resultCode){
                case 1:                    city_id = data.getStringExtra("return_id");
                    district = data.getStringExtra("district");
                    addData(city_id,district);
                    break;
                case 2:
                    city_id = data.getStringExtra("return_id");
                    district = data.getStringExtra("district");
                    addData(city_id,district);
                    Toast.makeText(this,"定位成功:   " + district,Toast.LENGTH_SHORT).show();
                    break;
            }
        } else if (requestCode == 2){
            switch (resultCode){
                case 1:
                    int pageSelected = data.getIntExtra("pageSelected",0);
                    viewPager.setCurrentItem(pageSelected);
                    break;
            }
        }
    }

    public void addData(final String city_id,String district){
        boolean b =true;
        Cursor cursor = db.query("city",new String[]{"city_id"},null,null,null,null,null);
        if (cursor.moveToFirst()){
            do {
                if (cursor.getString(cursor.getColumnIndex("city_id")).equals(city_id)){
                    b = false;
                    for (int i = 0;i<city_list.size();i++){
                        if (city_id.equals(cityId_list.get(i))){
                            viewPager.setCurrentItem(i);
                            break;
                        }
                    }
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        if (b) {
            addLayout();
            ContentValues values = new ContentValues();
            city_list.add(district);
            cityId_list.add(city_id);
            values.put("city", district);
            values.put("city_id", city_id);
            db.insert("city", null, values);
            Fragment fragment = BlankFragment.newInstance(city_id,city_list.size()-1);
            fragmentArrayList.add(fragment);
            mainAdapter.notifyDataSetChanged();
            viewPager.setCurrentItem(city_list.size() - 1);
            toolbar.setTitle(district);
            add.setVisibility(View.GONE);
            tip_text.setVisibility(View.GONE);

        }
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()){
            case R.id.add:
                intent = new Intent(this,SearchActivity.class);
                startActivityForResult(intent,1);
                break;
            default:
                break;
        }
    }


    private ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            toolbar.setTitle(city_list.get(position));


        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    public void firstStart(){
        final MyLocation myLocation = new MyLocation();
        myLocation.getUserLocation();

        final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("自动定位中...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                String city = myLocation.getCity();
                String district = myLocation.getDistrict();
                if (city == null ){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "定位失败", Toast.LENGTH_SHORT).show();
                        }
                    });

                }else {
                    LocationCityId locationCityId = new LocationCityId();
                    locationCityId.getLocationCityId(city, district, new LocationCallBack() {
                        @Override
                        public void onFinish(String return_id, String city_name) {
                            if (return_id != null) {
                                addData(return_id, city_name);
                            }
                        }
                    });
                }
                progressDialog.dismiss();
                }

        };
        Timer timer = new Timer();
        timer.schedule(timerTask, 2000);

    }


    public void setItemSelected(int position){
        switch(position){
            case 0:

                if (pageSelected == 0){
                    drawerLayout.closeDrawers();
                } else {
                    if (DELETE_FLAG == 0 && flag == 1){

                        viewPager.setAdapter(mainAdapter);
                        if (city_list.size() > 0){
                            toolbar.setTitle(city_list.get(0));
                        } else {
                            toolbar.setTitle("请添加城市");
                        }
                        drawerLayout.closeDrawers();
                    } else {
                        viewPager.removeAllViews();
                        mainAdapter.notifyDataSetChanged();
                        setViewToDrawerLayout();
                        viewPager.setAdapter(mainAdapter);
                        if (city_list.size() != 0 ){

                            add.setVisibility(View.GONE);
                            tip_text.setVisibility(View.GONE);
                            fragmentArrayList.clear();
                            initViewPager();
                            mainAdapter.notifyDataSetChanged();
                            toolbar.setTitle(city_list.get(0));
                            drawerLayout.closeDrawers();
                            flag = 0;
                        }else {
                            drawerLayout.closeDrawers();
                            toolbar.setTitle("请添加城市");
                            add.setVisibility(View.VISIBLE);
                            tip_text.setVisibility(View.VISIBLE);

                        }
                    }
                    viewPager.setCurrentItem(0);
                }
                navigationView.setCheckedItem(R.id.weather_show);
                pageSelected = 0;
                add.setBackgroundResource(R.drawable.ic_add_white_48dp);
                tip_text.setTextColor(Color.WHITE);
                break;
            case 1:
                break;
            case 2:
                pageSelected = 2;
                if (cityManagetFragment.size() > 0){
                    cityManagetFragment.clear();
                }
                toolbar.setTitle("城市管理");
                drawerLayout.closeDrawers();
                Fragment fragment = CityManagerFragment.newInstance();
                cityManagetFragment.add(fragment);
                viewPager.setAdapter(cityManagerAdapter);
                DELETE_FLAG = 0;
                navigationView.setCheckedItem(R.id.city_manager);
                add.setBackgroundResource(R.drawable.ic_add_black_48dp);
                tip_text.setTextColor(Color.BLACK);
                break;
        }
    }


    private NavigationView.OnNavigationItemSelectedListener onNavigationItemSelectedListener = new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(MenuItem menuItem) {
            int i = menuItem.getItemId();
            switch (i){
                case R.id.city_manager:
                    setItemSelected(2);
                    //startActivityForResult(new Intent(MainActivity.this,CityManager.class),2);
                    break;
                case R.id.add_city:
                    startActivityForResult(new Intent(MainActivity.this, SearchActivity.class), 1);
                    break;
                case R.id.settings:
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                    break;
                case R.id.exit:
                    finish();
                    break;
                case R.id.weather_show:
                    flag = 1;
                    setItemSelected(0);
                    break;
                default:
                    break;
            }
            return true;
        }
    };
    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    };


    public void setViewToDrawerLayout(){
        linearLayout.removeAllViews();
        cityViewList.clear();
        weatherViewList.clear();
        networkImageViewList.clear();
        linearLayoutList.clear();
        for (int i = 0; i < cityId_list.size(); i++){
            addLayout();  //添加layout
        }
    }

    public void addLayout(){
        final LinearLayout layout = new LinearLayout(MainActivity.this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.setPadding(20, 0, 20, 0);
        layout.setHorizontalGravity(Gravity.CENTER_HORIZONTAL);
        TextView weather_text = new TextView(MainActivity.this);
        TextView city_text = new TextView(MainActivity.this);
        NetworkImageView networkImageView = new NetworkImageView(this);
        networkImageView.setBackgroundResource(R.drawable.yuanxing);
        city_text.setGravity(Gravity.CENTER_HORIZONTAL);
        weather_text.setGravity(Gravity.CENTER_HORIZONTAL);
        weather_text.setTextColor(Color.WHITE);
        city_text.setTextColor(Color.WHITE);
        layout.addView(networkImageView);
        layout.addView(weather_text);
        layout.addView(city_text);
        linearLayout.addView(layout);
        cityViewList.add(city_text);
        weatherViewList.add(weather_text);
        networkImageViewList.add(networkImageView);
        linearLayoutList.add(layout);
    }

    public void setView(final int i,String weather,String weatherPic){
        cityViewList.get(i).setText(city_list.get(i));
        weatherViewList.get(i).setText(weather);
        networkImageViewList.get(i).setImageUrl(weatherPic, imageLoader);
        linearLayoutList.get(i).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.closeDrawers();
                viewPager.setCurrentItem(i);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unregisterReceiver(doneBroadCastReceiver);
        //unregisterReceiver(addBroadcastReceiver);
        //unregisterReceiver(removeBroadcastReceiver);
        System.exit(0);
    }


    @Override
    protected void onStart() {
        super.onStart();
        DELETE_FLAG = 0;

    }

    @Override
    protected void onResume() {
        super.onResume();
        String color = sharedPreferences.getString("style_color","青色");
        switch (color){
            case "蓝色":
                relativeLayout.setBackgroundColor(Color.parseColor("#104d8e"));
                setTheme(R.style.lanseTheme);
                headerLayout.setBackgroundColor(Color.parseColor("#104d8e"));
                break;
            case "灰色":
                relativeLayout.setBackgroundColor(Color.GRAY);
                setTheme(R.style.huiseTheme);
                headerLayout.setBackgroundColor(Color.GRAY);
                break;
            case "青色":
                relativeLayout.setBackgroundColor(Color.parseColor("#FF00786F"));
                setTheme(R.style.qingseTheme);
                headerLayout.setBackgroundColor(Color.parseColor("#FF00786F"));
                break;
            case "绿色":
                relativeLayout.setBackgroundColor(Color.parseColor("#2e8b57"));
                setTheme(R.style.lvseTheme);
                headerLayout.setBackgroundColor(Color.parseColor("#2e8b57"));
                break;
            case "黑色":
                relativeLayout.setBackgroundColor(Color.BLACK);
                setTheme(R.style.heiseTheme);
                headerLayout.setBackgroundColor(Color.BLACK);
                break;
            case "咖啡色":
                relativeLayout.setBackgroundColor(Color.parseColor("#5f4421"));
                setTheme(R.style.kafeiseTheme);
                headerLayout.setBackgroundColor(Color.parseColor("#5f4421"));
                break;
        }
        if (city_list.size() == 0){
            add.setVisibility(View.VISIBLE);
            tip_text.setVisibility(View.VISIBLE);
        } else {
            add.setVisibility(View.GONE);
            tip_text.setVisibility(View.GONE);
        }

    }

    /*public void initBroadcast(){
        IntentFilter intentFilter = new IntentFilter("com.lha.weather.DONE");
        registerReceiver(doneBroadCastReceiver,intentFilter);
        IntentFilter addIntentFilter = new IntentFilter("com.lha.weather.ADD");
        registerReceiver(addBroadcastReceiver,addIntentFilter);
    }

    private BroadcastReceiver doneBroadCastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            DELETE_FLAG = 1;
            cityId_list.clear();
            city_list.clear();
            fragmentArrayList.clear();
            viewPager.removeAllViews();
            mainAdapter.notifyDataSetChanged();
            initCityList();
            setViewToDrawerLayout();
            Intent intent1 = new Intent("com.lha.weather.UPDATE_FROM_LOCAL");
            sendBroadcast(intent1);
        }
    };

    private BroadcastReceiver addBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            addLayout();
            String city = intent.getStringExtra("city");
            String city_id = intent.getStringExtra("city_id");
            city_list.add(city);
            cityId_list.add(city_id);
            BlankFragment blankFragment = BlankFragment.newInstance(city_id, city_list.size() - 1);
            fragmentArrayList.add(blankFragment);
            mainAdapter.notifyDataSetChanged();
            Toast.makeText(context,"添加成功：" + city,Toast.LENGTH_SHORT).show();
            ContentValues values = new ContentValues();
            values.put("city", city);
            values.put("city_id", city_id);
            db.insert("city", null, values);
            values.clear();

        }
    };*/

    /*private BroadcastReceiver removeBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("Main", "onReceive");
            String city = intent.getStringExtra("city");
            db.delete("city", "city=?", new String[]{city});
            city_list.clear();
            cityId_list.clear();
            initCityList();
            fragmentArrayList.clear();
            viewPager.removeAllViews();
            initViewPager();
            setViewToDrawerLayout();
            NewAppWidget.updateWidget();
        }
    };*/



    public void deleteCity(int position) {
        city_list.remove(position);
        cityId_list.remove(position);
        //viewPager.removeAllViews();
        DELETE_FLAG = 1;
        fragmentArrayList.remove(position);
        linearLayout.removeViewAt(position);
        cityViewList.remove(position);
        weatherViewList.remove(position);
        networkImageViewList.remove(position);
        linearLayoutList.remove(position);
        sendBroadcast(new Intent("com.lha.weather.UPDATE_FROM_LOCAL"));
        if (city_list.size() == 0){
            add.setVisibility(View.VISIBLE);
            tip_text.setVisibility(View.VISIBLE);
            add.setBackgroundResource(R.drawable.ic_add_black_48dp);
            tip_text.setTextColor(Color.BLACK);
        }
    }


}




