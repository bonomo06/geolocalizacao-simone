package com.example.geolocalizacao;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private Button btnObterLocalizacao;
    private ProgressBar progressBar;
    private TextView tvLatitude;
    private TextView tvLongitude;
    private TextView tvEndereco;
    private TextView tvStatus;

    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnObterLocalizacao = findViewById(R.id.btnObterLocalizacao);
        progressBar = findViewById(R.id.progressBar);
        tvLatitude = findViewById(R.id.tvLatitude);
        tvLongitude = findViewById(R.id.tvLongitude);
        tvEndereco = findViewById(R.id.tvEndereco);
        tvStatus = findViewById(R.id.tvStatus);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                fusedLocationClient.removeLocationUpdates(locationCallback);
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    showCoordinates(location);
                    reverseGeocode(location);
                } else {
                    showError("Não foi possível obter a localização.");
                    setLoading(false);
                }
            }
        };

        btnObterLocalizacao.setOnClickListener(v -> solicitarLocalizacao());
    }

    private void solicitarLocalizacao() {
        tvStatus.setText("");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    PERMISSION_REQUEST_CODE
            );
            return;
        }

        obterLocalizacao();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;

            if (granted) {
                obterLocalizacao();
            } else {
                showError("Permissão de localização negada. "
                        + "Habilite nas configurações do aplicativo.");
            }
        }
    }

    @SuppressWarnings("MissingPermission")
    private void obterLocalizacao() {
        setLoading(true);

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 0)
                .setMaxUpdates(1)
                .setWaitForAccurateLocation(false)
                .build();

        fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
        );
    }

    private void showCoordinates(Location location) {
        tvLatitude.setText(String.format(Locale.getDefault(),
                "Latitude: %.6f", location.getLatitude()));
        tvLongitude.setText(String.format(Locale.getDefault(),
                "Longitude: %.6f", location.getLongitude()));
    }

    private void reverseGeocode(Location location) {
        executor.execute(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            String resultado;
            try {
                List<Address> addresses = geocoder.getFromLocation(
                        location.getLatitude(), location.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(address.getAddressLine(i));
                    }
                    resultado = sb.toString();
                } else {
                    resultado = "Endereço não encontrado.";
                }
            } catch (IOException e) {
                resultado = "Erro ao obter endereço: " + e.getMessage();
            }

            final String enderecoFinal = resultado;
            runOnUiThread(() -> {
                tvEndereco.setText("Endereço: " + enderecoFinal);
                setLoading(false);
            });
        });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnObterLocalizacao.setEnabled(!isLoading);
    }

    private void showError(String message) {
        tvStatus.setText(message);
        setLoading(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
