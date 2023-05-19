package com.example.caia_movil_v2;
/*
 * Esta es la clase base que sostiene a las otras pantallas(fragmentos) de la App CAIA Movil v2
 *
 * @author Pablo Yepez Contreras <http://mailto:pyepezc@yahoo.com>
 * @version 1.0, 2022/03/07
 * @version 1.1, 2023/05/17
 *
 * DHL EXPRESS ECUADOR
 */

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.caia_movil_v2.databinding.ActivityMainBinding;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.security.ProviderInstaller;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity
    implements ProviderInstaller.ProviderInstallListener {

    private AppBarConfiguration appBarConfiguration;


    public Menu menuGlobal; // Para acceder desde Configfragment y guiaFragment.

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ActivityMainBinding binding;

        super.onCreate(savedInstanceState);

        ProviderInstaller.installIfNeededAsync(this, this);//Install Google service security provider
        Log.d("DHL", "installIfNeededAsync ");

        binding = ActivityMainBinding.inflate( getLayoutInflater() );
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menuGlobal = menu; // Guardar el menu para desactivar en el Config Fragment y en el Guia Fragment.
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            // Sirve para ir a la pantalla de Configuracion
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
                   navController.navigate(R.id.action_LoginFragment_to_ConfigFragment);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    /*
        Google Play services provides. Security Provider
    */
    private static final int ERROR_DIALOG_REQUEST_CODE = 1;

    //private boolean retryProviderInstall;

    @Override
    public void onProviderInstallFailed(int errorCode, Intent recoveryIntent) {
        GoogleApiAvailability availability = GoogleApiAvailability.getInstance();
        int status_code = availability.isGooglePlayServicesAvailable(this);
        Log.d("DHL", "onProviderInstallFailed "+status_code);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ERROR_DIALOG_REQUEST_CODE) {
            // Adding a fragment via GoogleApiAvailability.showErrorDialogFragment
            // before the instance state is restored throws an error. So instead,
            // set a flag here, which causes the fragment to delay until
            // onPostResume.
            Log.d("DHL", "onActivityResult "+ERROR_DIALOG_REQUEST_CODE);
            //retryProviderInstall = true;
        }
    }

    @Override
    public void onProviderInstalled() {
        // Provider is up to date; app can make secure network calls.
        //Log.d("DHL", "onProviderInstalled ");
    }

    //private void onProviderInstallerNotAvailable() {
        // provider can't be updated .
    //}

}