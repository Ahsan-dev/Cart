package com.example.cart;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import de.hdodenhof.circleimageview.CircleImageView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.renderscript.RenderScript;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryDataEventListener;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

public class CustomerMapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private Location lastLocation;
    private LatLng customerPickUpLocation;
    private LocationRequest lRequest;
    private FirebaseAuth mAuth;
    private String currentUserId ,customerId;
    private FirebaseUser currentUser;
    private Button settingsBtn, logoutBtn, callACartBtn;
    private DatabaseReference customerAvailabilityRef, driverAvailableRef,DriverRef,driverLocationRef, CustomerDatabaseRef;
    private GeoFire geoFire;
    private Boolean customerLogOutStatus = false, requestType = false;
    private int radius = 1;
    private Boolean driverFound = false;
    private String driverFoundId;
    Marker driverMarker, pickUpMarker;
    GeoQuery geoQuery;
    private ValueEventListener driverLocationRefListener;
    private CircleImageView driverImage;
    private TextView driverNameTxt, driverPhnTxt, driverCarTxt;
    private ImageView phoneBtn;
    private RelativeLayout customerMapRelativeLayout;
    private String phone;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_maps);

        mAuth = FirebaseAuth.getInstance();
        customerAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("Customers Available");
        driverAvailableRef = FirebaseDatabase.getInstance().getReference().child("Drivers Available");
        driverLocationRef = FirebaseDatabase.getInstance().getReference().child("Drivers Working");
        CustomerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Customer Requests");
        currentUserId = mAuth.getCurrentUser().getUid();
        currentUser = mAuth.getCurrentUser();
        customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        settingsBtn = findViewById(R.id.customer_settingsBtnId);
        logoutBtn = findViewById(R.id.customer_logout_button);
        callACartBtn = findViewById(R.id.customer_call_a_cart_btn);
        customerMapRelativeLayout = findViewById(R.id.customer_map_relative_layout_Id);
        driverImage = findViewById(R.id.customer_map_profile_imgId);
        driverNameTxt = findViewById(R.id.customer_map_driver_name_txt);
        driverPhnTxt = findViewById(R.id.customer_map_driver_phone_txt);
        driverCarTxt = findViewById(R.id.customer_map_driver_car_txt);
        phoneBtn = findViewById(R.id.customer_map_phone_image_button);




        //Obtain the SupportMapFragment and get notified when the map is ready to be used.....
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                logOutLocation();
                customerLogOutStatus = true;
                goToWelcomeActivity();
            }
        });

        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsIntent = new Intent(CustomerMapsActivity.this,SettingsActivity.class);
                settingsIntent.putExtra("settingCheck","Customers");
                startActivity(settingsIntent);
            }
        });

        callACartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(requestType){

                    requestType = false;
                    geoQuery.removeAllListeners();
                    driverLocationRef.removeEventListener(driverLocationRefListener);

                    if(driverFound != null){

                        DriverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("customerRideId");
                        DriverRef.removeValue();
                        driverFoundId = null;

                    }

                    driverFound = false;
                    radius = 1;
                    geoFire = new GeoFire(CustomerDatabaseRef);
                    geoFire.removeLocation(customerId);

                    if(pickUpMarker != null){
                        pickUpMarker.remove();
                    }
                    if(driverMarker != null){
                        driverMarker.remove();
                    }
                    callACartBtn.setText("Call A Cart");

                    customerMapRelativeLayout.setVisibility(View.GONE);


                }else {

                    requestType = true;

                    geoFire = new GeoFire(CustomerDatabaseRef);
                    geoFire.setLocation(currentUserId,new GeoLocation(lastLocation.getLatitude(),lastLocation.getLongitude()));
                    customerPickUpLocation = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                    pickUpMarker = mMap.addMarker(new MarkerOptions().position(customerPickUpLocation).title("My location").icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));

                    callACartBtn.setText("Getting Your Driver......");
                    getClosestDriverCart();



                }




            }
        });
    }

    private void getClosestDriverCart() {

        GeoFire geoFire = new GeoFire(driverAvailableRef);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(customerPickUpLocation.latitude,customerPickUpLocation.longitude),radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                if(!driverFound && requestType) {
                    driverFound = true;
                    driverFoundId = key;

                    DriverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId);
                    HashMap driverMap = new HashMap();
                    driverMap.put("customerRideId", customerId);
                    DriverRef.updateChildren(driverMap);

                    GettingDriverLocation();
                    callACartBtn.setText("Looking for driver location...");

                }
            }

            @Override
            public void onKeyExited(String key) {



            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if(!driverFound){

                    radius = radius + 1;
                    getClosestDriverCart();

                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void GettingDriverLocation(){

        driverLocationRefListener = driverLocationRef.child(driverFoundId).child("l")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if(snapshot.exists() && requestType){
                            List<Object> driverLocationList = (List<Object>) snapshot.getValue();
                            double LocationLat = 0;
                            double LocationLng = 0;
                            callACartBtn.setText("Driver found");

                            customerMapRelativeLayout.setVisibility(View.VISIBLE);
                            getDriverInfos();



                            if(driverLocationList.get(0) != null){

                                LocationLat = Double.parseDouble(driverLocationList.get(0).toString());

                            }

                            if(driverLocationList.get(1) != null){

                                LocationLng = Double.parseDouble(driverLocationList.get(1).toString());

                            }

                            LatLng driverLatLng = new LatLng(LocationLat,LocationLng);
                            if(driverMarker != null){
                                driverMarker.remove();
                            }

                            Location locationCus = new Location("");
                            locationCus.setLatitude(customerPickUpLocation.latitude);
                            locationCus.setLongitude(customerPickUpLocation.longitude);

                            Location locationDri = new Location("");
                            locationDri.setLatitude(driverLatLng.latitude);
                            locationDri.setLongitude(driverLatLng.longitude);

                            float distance = locationCus.distanceTo(locationDri);

                            if(distance<90){

                                callACartBtn.setText("Driver is reached");
                               // getDriverInfos();

                            }else {

                                callACartBtn.setText("Driver found at "+ distance/1000 + " Km far away...");
                                //getDriverInfos();

                            }


                            driverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Your driver is here...").icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));

                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        buildGoogleApiClient();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        mMap.setMyLocationEnabled(true);

        // Add a marker in Sydney and move the camera

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        lRequest = new LocationRequest();
        lRequest.setInterval(1000);
        lRequest.setFastestInterval(1000);
        lRequest.setPriority(lRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, lRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

        lastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12));




    }

    protected synchronized void buildGoogleApiClient(){
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(!customerLogOutStatus){
            logOutLocation();
        }
    }



    private void goToWelcomeActivity() {

        Intent welcomeIntent = new Intent(CustomerMapsActivity.this, WelcomeActivity.class);
        welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(welcomeIntent);
        finish();
    }

    private void logOutLocation() {

        geoFire = new GeoFire(customerAvailabilityRef);
        geoFire.removeLocation(currentUserId);

    }

    private void getDriverInfos(){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers")
                .child(driverFoundId);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(snapshot.exists() && snapshot.getChildrenCount() > 0){

                    String name = snapshot.child("name").getValue().toString();
                    driverNameTxt.setText(name);
                    phone = snapshot.child("phone").getValue().toString();
                    driverPhnTxt.setText(phone);

                    if(snapshot.hasChild("car")){

                        String car = snapshot.child("car").getValue().toString();
                        driverCarTxt.setText(car);

                    }

                    if(snapshot.hasChild("image")){

                        String image = snapshot.child("image").getValue().toString();
                        Picasso.get().load(image).placeholder(R.drawable.profile).into(driverImage);

                    }

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        phoneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.CALL_PHONE ) != PackageManager.PERMISSION_GRANTED){
                    requestPermissions(new String[]{Manifest.permission.CALL_PHONE},1);
                }else {

                    String s = "tel:+88"+phone;
                    Intent intent = new Intent(Intent.ACTION_CALL);
                    intent.setData(Uri.parse(s));
                    startActivity(intent);

                }
            }
        });
    }
}