package kr.ac.konkuk.cya_cu;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import noman.googleplaces.NRPlaces;
import noman.googleplaces.Place;
import noman.googleplaces.PlaceType;
import noman.googleplaces.PlacesListener;
import noman.googleplaces.PlacesException;


public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback, PlacesListener{

    private GoogleMap mMap;
    private GpsTracker gpsTracker;


    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS  = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

   
    Button add ; //위치 추가
    Button middle; //중간지점 찾기
    Button weather; //날씨버튼
    Button gps; //현위치 찾기


    //검색창
    ImageButton search;
    EditText searchtv;

    //위도 경도를 저장 할 변수들
    Double lon1, lat1;
    Double lon2, lat2;
    Double middlelat, middlelon;

   
    //add한 위도, 경도 값을 담을 List
    List<Double> latlist = new ArrayList<Double>();
    List<Double> lonlist = new ArrayList<Double>();

    List<Marker> previous_marker = null;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (!checkLocationServicesStatus()) {

            showDialogForLocationServiceSetting();
        }else {

            checkRunTimePermission();
        }

        add = (Button)findViewById(R.id.btn_addlocation);
        middle = (Button)findViewById(R.id.btn_findmiddle);
        weather = (Button)findViewById(R.id.btn_weather);
        gps = (Button)findViewById(R.id.btn_crrentlocation);


        search = (ImageButton)findViewById(R.id.btn_search);
        searchtv = (EditText)findViewById(R.id.tv_search);

        //검색버튼이 클릭되었을때
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 검색창에서 텍스트를 가져온다
                String searchText = searchtv.getText().toString();

                Geocoder geocoder = new Geocoder(getBaseContext());
                List<Address> addresses = null;

                try {
                    addresses = geocoder.getFromLocationName(searchText, 3);
                    if (addresses != null && !addresses.equals(" ")) {
                        search(addresses); //검색된 지명의 주소를 찾아온다.
                    }
                } catch(Exception e) {

                }
            }
        });


        //GPS 즉 현위치 버튼을 클릭했을때, 현 위치로 이동
        gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gpsTracker = new GpsTracker(MainActivity.this);
                lon1 = gpsTracker.getLongitude();
                lat1 = gpsTracker.getLatitude();

                String address = getCurrentAddress(lat1, lon1);

                LatLng CURRENT = new LatLng(lat1,lon1);
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(CURRENT);
                mMap.addMarker(markerOptions);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(CURRENT, 11));

                //현 위치에서 add버튼이 클리되었을때
                add.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        latlist.add(lat1);
                        lonlist.add(lon1);

                        //추가된 것의 marker을 변화

                        mMap.addMarker(markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));

                    }
                });


            }
        });

        //날씨 버튼이 클릭되었을 때
        weather.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Weather.class);
                startActivity(intent);

            }
        });


        //중간버튼을 클릭하면 중간의 위치에 마크를 찍는다
        middle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               toMiddle(latlist,lonlist);
            }
        });



    }//onCreate


    //지명을 가지고 지도에서 검색
    protected void search(List<Address> addresses) {
        Address address = addresses.get(0);

        Double latitude = address.getLatitude();
        Double longitude = address.getLongitude();

        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

        String addressText = String.format(
                "%s, %s",
                address.getMaxAddressLineIndex() > 0 ? address
                        .getAddressLine(0) : " ", address.getFeatureName());
        
        MarkerOptions mOptions = new MarkerOptions();
        mOptions.position(latLng);
        mOptions.title(addressText);

        mMap.clear();
        mMap.addMarker(mOptions);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        // 해당 위치로 이동, 마커도 찍힌다


        //여기서 추가 버튼이 클릭되었을때 --> 앞으로는 메소드를 그냥 하나 만들자
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                lonlist.add(longitude);
                latlist.add(latitude);

                mMap.addMarker(mOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));

            }
        });
    }




    //Default 값으로 서울을 해놓음 지도가 맨 처음 실행되었을때
    @Override
    public void onMapReady(final GoogleMap googleMap) {

        mMap = googleMap;

        LatLng SEOUL = new LatLng(37.56, 126.97);

        //표시 마커를 만드는것
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(SEOUL);//마커를 이곳에 위치시켜라!
        //MarkerOptions.position(Current);
        //markerOptions.title("서울");
        //markerOptions.snippet("한국의 수도");
        //mMap.addMarker(markerOptions);


        //그 쪽으로 이동--> 현위치, 축척 정도
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(SEOUL, 11));

        //지도에서 위치가 클릭되었을 때 --> 마커 생성, 그 위치 받음
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                MarkerOptions mOptions = new MarkerOptions();

                mOptions.title("마커좌표");
                Double latitude = latLng.latitude;
                Double longitude = latLng.longitude;

                mOptions.snippet(latitude.toString()+","+longitude.toString());

                mOptions.position(new LatLng(latitude,longitude));
                googleMap.addMarker(mOptions);


                //add버튼을 클릭했을때 이벤트 -> 위치 저장, 마커 아이콘 변경
                add.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        lonlist.add(longitude);
                        latlist.add(latitude);

                        //위치를 등록했을 때, 파랑으로 변화
                        googleMap.addMarker(mOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));


                    }
                });
            }
        });

    } //onMapReady

    //middle 버튼을 클릭하면 중간지점을 찾고, 그 장소로 줌인 한다.
    public void toMiddle(List<Double> latlist, List<Double> lonlist){
        middlelat = ((Double)latlist.get(0) + (Double)latlist.get(1))/2;
        middlelon = ((Double)lonlist.get(0) + (Double)lonlist.get(1))/2;

        MarkerOptions mOptions = new MarkerOptions();
        LatLng MIDDLE = new LatLng(middlelat, middlelon);
        mOptions.position(MIDDLE);
        mMap.addMarker(mOptions);
        //중간 아이콘이 클릭되면 밑에 코드 실행, 또는 다른 지도 upload함 새로운 페이지
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(MIDDLE, 12));


/* 이 부분은 icon을 변경한 것! 이미지로!!
        BitmapDrawable bitmapdraw=(BitmapDrawable)getResources().getDrawable(R.drawable.marker);
        Bitmap b=bitmapdraw.getBitmap();
        Bitmap smallMarker = Bitmap.createScaledBitmap(b, 150, 150, false);
        mOptions.icon(BitmapDescriptorFactory.fromBitmap(smallMarker));
*/
       

        mMap.addMarker(mOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        //중간지점은 마크를 노랑으로


        //중간 마커를 클릭하면 클릭한 위치가 줌됨
        //TODO: 버튼들 변경, 구글 place적용하기,
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                MarkerOptions mOptions = new MarkerOptions();

                //mOptions.title("마커좌표");
                LatLng MIDDLEZOOM = new LatLng(middlelat, middlelon);
                mOptions.position(MIDDLEZOOM);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(MIDDLEZOOM, 16));



                //mMap.clear();

                //버튼을 지운다
                //add.setVisibility(View.GONE);

                //버튼들의 이름이 변화된다.
                add.setText("CAFE");
                middle.setText("RESTAURANT");
                weather.setText("SHOPPING");
                gps.setText("BAR");


                /*
                 이 부분은 icon을 변경한 것! 이미지로!!
        BitmapDrawable bitmapdraw=(BitmapDrawable)getResources().getDrawable(R.drawable.marker);
        Bitmap b=bitmapdraw.getBitmap();
        Bitmap smallMarker = Bitmap.createScaledBitmap(b, 150, 150, false);
        mOptions.icon(BitmapDescriptorFactory.fromBitmap(smallMarker));
*/


                //mMap.addMarker(mOptions);
                //카페, 등등 표시 여기부터 시작
                previous_marker = new ArrayList<Marker>();

                //카페정보
                add.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showCafeInformation(MIDDLEZOOM);
                                           }
                });

                //식당정보
                middle.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showRestaurantInformation(MIDDLEZOOM);
                    }
                });

                //쇼핑 정보
                weather.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showShoppingInformation(MIDDLEZOOM);
                    }
                });

                //술집 정보
                gps.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showBarInformation(MIDDLEZOOM);
                    }
                });


                return false;
            }
        });



    }

    @Override
    public void onPlacesFailure(PlacesException e) {
    }

    @Override
    public void onPlacesStart() {
    }

    @Override
    public void onPlacesSuccess(final List<Place> places) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (noman.googleplaces.Place place:places){
                    LatLng latLng = new LatLng(place.getLatitude(),place.getLongitude());

                    String markerSnippet =
                            getCurrentAddress(place.getLatitude(),place.getLongitude());


                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.title(place.getName());//장소의 이름
                    markerOptions.snippet(markerSnippet);//장소의 주소
                    Marker item = mMap.addMarker(markerOptions);
                    previous_marker.add(item);



                }
                //중복 마커 제거
                HashSet<Marker> hashSet = new HashSet<Marker>();
                hashSet.addAll(previous_marker);
                previous_marker.clear();
                previous_marker.addAll(hashSet);
            }
        });

    }

    @Override
    public void onPlacesFinished() {

    }

    /*
    * 여기서부터는 places들에 대한 정보들 불러오기!
    * */

    //식당정보를 불러온다
    public void showRestaurantInformation(LatLng location)
    {
        mMap.clear();//지도 클리어

        if (previous_marker != null)
            previous_marker.clear();//지역정보 마커 클리어


        new NRPlaces.Builder()
                .listener(MainActivity.this)
                .key("Google Place") //내 Google Place API키 TODO: 제출할 때 꼭 지워라
                .latlng(location.latitude, location.longitude)//현재 위치
                .radius(500) //500 미터 내에서 검색
                .type(PlaceType.RESTAURANT) //음식점
                .build()
                .execute();

    }

    //쇼핑할 장소 정보를 불러온다
    public void showShoppingInformation(LatLng location)
    {
        mMap.clear();//지도 클리어

        if (previous_marker != null)
            previous_marker.clear();//지역정보 마커 클리어


        new NRPlaces.Builder()
                .listener(MainActivity.this)
                .key("Google Place")//Google Place API key입력
                .latlng(location.latitude, location.longitude)//현재 위치
                .radius(500) //500 미터 내에서 검색
                .type(PlaceType.CLOTHING_STORE) //음식점
                .build()
                .execute();
    }

    //카페 정보를 불러온다.
    public void showCafeInformation(LatLng location)
    {
        mMap.clear();//지도 클리어

        if (previous_marker != null)
            previous_marker.clear();//지역정보 마커 클리어


        new NRPlaces.Builder()
                .listener(MainActivity.this)
                .key("Google Place")//Google Place API key입력
                .latlng(location.latitude, location.longitude)//현재 위치
                .radius(500) //500 미터 내에서 검색
                .type(PlaceType.CAFE) //음식점
                .build()
                .execute();
    }

    //술집 정보를 불러온다
    public void showBarInformation(LatLng location)
    {
        mMap.clear();//지도 클리어

        if (previous_marker != null)
            previous_marker.clear();//지역정보 마커 클리어


        new NRPlaces.Builder()
                .listener(MainActivity.this)
                .key("Google Place")
                .latlng(location.latitude, location.longitude)//현재 위치
                .radius(500) //500 미터 내에서 검색
                .type(PlaceType.BAR) //바
                .build()
                .execute();
    }

    //지하철역 정보를 불러온다 --> 지금은 사용 안함
    public void showsubwayInformation(LatLng location)
    {
        mMap.clear();//지도 클리어

        if (previous_marker != null)
            previous_marker.clear();//지역정보 마커 클리어


        new NRPlaces.Builder()
                .listener(MainActivity.this)
                .key("Google Place")//Google Place API key입력
                .latlng(location.latitude, location.longitude)//현재 위치
                .radius(500) //500 미터 내에서 검색
                .type(PlaceType.SUBWAY_STATION) //지하철역
                .build()
                .execute();
    }






    //뒤로가기 버튼이 눌렸을 때 1, 등록한 것 취소
    public void onBackPressedcanceladd(){

        onMapReady(mMap);
        /*
        * 1,마크 빨간색으로 바꾸기
        * 2, list에서 삭제하기
        * */

        super.onBackPressed();

    }




    /*
     * ActivityCompat.requestPermissions를 사용한 퍼미션 요청의 결과를 리턴받는 메소드
     */
    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {

        if ( permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {

            // 요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면

            boolean check_result = true;


            // 모든 퍼미션을 허용했는지 체크합

            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }


            if ( check_result ) {

                //위치 값을 가져올 수 있음
                ;
            }
            else {
                // 거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명해주고 앱을 종료합.2 가지 경우

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {

                    Toast.makeText(MainActivity.this, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.", Toast.LENGTH_LONG).show();
                    finish();
                }else {

                    Toast.makeText(MainActivity.this, "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ", Toast.LENGTH_LONG).show();

                }
            }

        }
    }

    void checkRunTimePermission(){

        //런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION);


        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {

            // 2. 이미 퍼미션을 가지고 있다면
            // ( 안드로이드 6.0 이하 버전은 런타임 퍼미션이 필요없기 때문에 이미 허용된 걸로 인식)

            // 3.  위치 값을 가져올 수 있음



        } else {  //2. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요합니다. 2가지 경우(3-1, 4-1)가 있습니다.

            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, REQUIRED_PERMISSIONS[0])) {

                // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
                Toast.makeText(MainActivity.this, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                // 3-3. 사용자게에 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);


            } else {
                // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }

        }

    }


    public String getCurrentAddress( double latitude, double longitude) {

        //지오코더... GPS를 주소로 변환
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses;

        try {

            addresses = geocoder.getFromLocation(
                    latitude,
                    longitude,
                    7);
        } catch (IOException ioException) {
            //네트워크 문제
            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show();
            return "지오코더 서비스 사용불가";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();
            return "잘못된 GPS 좌표";

        }



        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
            return "주소 미발견";

        }

        Address address = addresses.get(0);
        return address.getAddressLine(0).toString()+"\n";

    }




    //여기부터는 GPS 활성화를 위한 메소드들
    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하실래요?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case GPS_ENABLE_REQUEST_CODE:

                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {

                        Log.d("@@@", "onActivityResult : GPS 활성화 되있음");
                        checkRunTimePermission();
                        return;
                    }
                }

                break;
        }
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }



}//Public class
