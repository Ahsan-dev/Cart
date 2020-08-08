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
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
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

import java.util.List;
import java.util.Objects;

public class DriverMapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private Location lastLocation;
    private LocationRequest lRequest;
    private FirebaseAuth mAuth;

    private FirebaseUser currentUser;
    private String driverId;
    private Button settingsBtn, logoutBtn;
    private Boolean driverLogoutStatus = false;
    private DatabaseReference assignedCustomerRef, AssignedCustomerPickupRef;
    Marker pickUpMarker;
    private String customerId = "", userId;
    private ValueEventListener AssignedCustomerPickupRefListener;

    private CircleImageView customerImage;
    private TextView customerNameTxt, customerPhnTxt, driverCarTxt;
    private ImageView phoneBtn;
    private RelativeLayout driverMapRelativeLayout;
    private String phone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_maps);

        mAuth = FirebaseAuth.getInstance();

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        currentUser = mAuth.getCurrentUser();
        settingsBtn = findViewById(R.id.DriversettingsBtnId);
        logoutBtn = findViewById(R.id.driver_logout_button);

        driverMapRelativeLayout = findViewById(R.id.driver_map_relative_layout_Id);
        customerImage = findViewById(R.id.driver_map_profile_imgId);
        customerNameTxt = findViewById(R.id.driver_map_driver_name_txt);
        customerPhnTxt = findViewById(R.id.driver_map_driver_phone_txt);
        phoneBtn = findViewById(R.id.driver_map_phone_image_button);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                driverLogoutStatus = true;
                mAuth.signOut();
                driverlogout();
                logOutDriver();

            }
        });

        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsIntent = new Intent(DriverMapsActivity.this,SettingsActivity.class);
                settingsIntent.putExtra("settingCheck","Drivers");
                startActivity(settingsIntent);
            }
        });

        getAssignedCustomerRequest();

    }

    private void getAssignedCustomerRequest() {

        assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId)
                .child("customerRideId");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(snapshot.exists()){

                    customerId = snapshot.getValue().toString();
                    getAssignedCustomerPickupLocation();
                    driverMapRelativeLayout.setVisibility(View.VISIBLE);
                    getCustomerInfos();

                }

                else {
                    customerId = "";

                    if(pickUpMarker != null){
                        pickUpMarker.remove();
                    }

                    if(AssignedCustomerPickupRefListener != null){
                        AssignedCustomerPickupRef.removeEventListener(AssignedCustomerPickupRefListener);
                    }

                    driverMapRelativeLayout.setVisibility(View.GONE);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void getAssignedCustomerPickupLocation() {

        AssignedCustomerPickupRef = FirebaseDatabase.getInstance().getReference().child("Customer Requests").child(customerId)
                .child("l");
        AssignedCustomerPickupRefListener = AssignedCustomerPickupRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(snapshot.exists()){

                    List<Object> customerLocationList = (List<Object>) snapshot.getValue();
                    double LocationLat = 0;
                    double LocationLng = 0;

                    if(customerLocationList.get(0) != null){

                        LocationLat = Double.parseDouble(customerLocationList.get(0).toString());

                    }

                    if(customerLocationList.get(1) != null){

                        LocationLng = Double.parseDouble(customerLocationList.get(1).toString());

                    }

                    LatLng customerLatLng = new LatLng(LocationLat,LocationLng);
                    pickUpMarker = mMap.addMarker(new MarkerOptions().position(customerLatLng).title("Pick up the customer").icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));

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

        buildGoogleAPIClient();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        mMap.setMyLocationEnabled(true);


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
    public void onLocationChanged( Location location) {

       if(getApplicationContext() != null){
           lastLocation = location;
           LatLng  latLng = new LatLng(location.getLatitude(),location.getLongitude());
           mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
           mMap.animateCamera(CameraUpdateFactory.zoomTo(13));

           //String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

           DatabaseReference driverAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("Drivers Available");
           GeoFire geoFireAvailability = new GeoFire(driverAvailabilityRef);

           DatabaseReference driverWorkingRef = FirebaseDatabase.getInstance().getReference().child("Drivers Working");
           GeoFire geoFireWorking = new GeoFire(driverWorkingRef);

           switch (customerId){
               case "":
                   geoFireWorking.removeLocation(userId);
                   geoFireAvailability.setLocation(userId, new GeoLocation(location.getLatitude(),location.getLongitude()));
                   break;
               default:
                   geoFireAvailability.removeLocation(userId);
                   geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(),location.getLongitude()));
                   break;
           }



       }


    }

    protected synchronized void buildGoogleAPIClient(){
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

        if(!driverLogoutStatus){
            driverlogout();
        }
    }

    private void driverlogout() {


//        DatabaseReference driverAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("Drivers Available");
//
//        GeoFire geoFire = new GeoFire(driverAvailabilityRef);
//        geoFire.removeLocation(userId);
        DatabaseReference driverAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("Drivers Available");
        GeoFire geoFireAvailability = new GeoFire(driverAvailabilityRef);

        assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId)
                .child("customerRideId");

        DatabaseReference driverWorkingRef = FirebaseDatabase.getInstance().getReference().child("Drivers Working");
        GeoFire geoFireWorking = new GeoFire(driverWorkingRef);

       if(customerId != null) {

           geoFireWorking.removeLocation(userId);
           assignedCustomerRef.removeValue();
       }

               geoFireAvailability.removeLocation(userId);
       

        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient,this);

    }

    private void logOutDriver() {

        Intent welcomeIntent = new Intent(DriverMapsActivity.this, WelcomeActivity.class);
        welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(welcomeIntent);
        finish();

    }

    private void getCustomerInfos(){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers")
                .child(customerId);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(snapshot.exists() && snapshot.getChildrenCount() > 0){

                    String name = snapshot.child("name").getValue().toString();
                    customerNameTxt.setText(name);
                    phone = snapshot.child("phone").getValue().toString();
                    customerPhnTxt.setText(phone);

                    if(snapshot.hasChild("image")){

                        String image = snapshot.child("image").getValue().toString();
                        Picasso.get().load(image).placeholder(R.drawable.profile).into(customerImage);

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