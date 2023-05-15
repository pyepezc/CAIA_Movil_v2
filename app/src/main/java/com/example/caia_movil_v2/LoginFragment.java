package com.example.caia_movil_v2;
/**
 * Esta clase muestra la pantalla de ingreso a la App CAIA Movil v2 (Login)
 *
 * @author Pablo Yepez Contreras <http://mailto:pyepezc@yahoo.com>
 * @version 1.0, 2022/03/07
 *
 * DHL EXPRESS ECUADOR
 */
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.caia_movil_v2.databinding.FragmentLoginBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class LoginFragment extends Fragment {

    /**
     * Constantes
     */
    private static final String TAG = "DHL";

    private static final int preLOGIN = 1001;
    private static final int codLOGIN = 1005;
    private static final int postLOGIN = 1010;

    /**
     * Widgets utilizados en la pantalla
     */
    private Handler mHandler;
    private ProgressBar progBar;
    private EditText usuarioTxt;
    private EditText passwordTxt;
    private WebView webview;

    private String usuario;
    private String password;

    private FragmentLoginBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        MainActivity main = ((MainActivity) requireActivity()); // 10 dic 2021
        if (main.menuGlobal != null )
            main.menuGlobal.findItem(R.id.action_settings).setVisible(true); // Reactiva el menu luego de regresar configuracion o guia.

        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.fecha.setText( getFecha() );

        progBar = binding.loading;
        usuarioTxt = binding.username;
        passwordTxt = binding.password;
        webview = binding.webview;

        webview.loadUrl(WebServiceCliente.getUrlS());

        binding.buttonLogin.setOnClickListener(viewE -> {

            usuario = usuarioTxt.getText().toString().trim();
            password = passwordTxt.getText().toString().trim();

            if (! usuario.isEmpty())
                handleOperaciones();

        });

    }

    /** When the user closes the application
     * onPause() will be called
     * and data will be stored
     *
     */
    @Override
    public void onResume() {
        super.onResume();

        usuarioTxt.setText("");
        passwordTxt.setText("");

        if (getContext()!=null)
            WebServiceCliente.getPreferences( getContext() );

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void handleOperaciones() {

        mHandler = new Handler(Looper.getMainLooper()) {
            String respuesta=null;

            @Override
            public void handleMessage(Message msg) {

                // Process for STATUS
                if (msg.what == preLOGIN) { // Encender ProgressBar
                    progBar.setVisibility(View.VISIBLE);
                    //Log.d(TAG, "Encender ");
                    mHandler.sendEmptyMessage(codLOGIN);
                }
                if (msg.what == codLOGIN) { // Leer los datos desde el Webservice

                    WebServiceCliente.LogonUser(usuario, password); // Llama el web service LOGON
                    int n = 0;
                    while (!WebServiceCliente.hayRespuesta()) { // Espera hasta que haya una respuesta.
                        WebServiceCliente.delay(400);
                        //Log.d(TAG, String.format("esperando %d", n));
                        if (++n > 35) break;
                    }

                    //Log.d(TAG, WebServiceCliente.getRespuesta());

                    if (WebServiceCliente.hayRespuesta()) {
                        respuesta = WebServiceCliente.getRespuesta();
                        WebServiceCliente.setUsuario(usuario);
                    }
                    else
                        respuesta = null;
                    binding.textError.setText(WebServiceCliente.getMensajeError());

                    mHandler.sendEmptyMessage(postLOGIN);  // Apagar progressbar
                }
                if (msg.what == postLOGIN) { // Desaparecer ProgressBar
                    progBar.setVisibility(View.GONE);
                    //Log.d(TAG, "Apagar status ");
                    login(respuesta);
                }

            }
        };

        mHandler.sendEmptyMessage( preLOGIN ); // Iniciar el proceso.
    }

    /**
     * Confirma los datos de usuario y transfiere a la pantalla de captura de guias
     * @param resp respuesta entregada por el webservice
     */
    private void login(String resp) {

        if (resp==null) {
            usuarioTxt.setText("");
            passwordTxt.setText("");
            Toast.makeText(getContext(),"Sin respuesta",Toast.LENGTH_LONG).show();
            usuarioTxt.requestFocus();

        } else  {
            String pre = resp.substring(0,3);
            if ("ERR".equals(pre)) {
                usuarioTxt.setText("");
                passwordTxt.setText("");
                usuarioTxt.requestFocus();
                Toast.makeText(getContext(),resp.substring(4),Toast.LENGTH_LONG).show();

            } else {

                Toast.makeText(getContext(),resp.substring(3),Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(LoginFragment.this)
                        .navigate(R.id.action_LoginFragment_to_GuiaFragment);
            }
        }
    }

    /**
     * Obtiene la fecha actual para presentar en la pantalla
     * @return el texto con la fecha actual
     */
    private String getFecha() {
        Calendar cal = Calendar.getInstance();
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm");
        return dateFormat.format(cal.getTime());
    }
}