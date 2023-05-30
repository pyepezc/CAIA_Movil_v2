package com.example.caia_movil_v2;
/*
 * Esta clase muestra la pantalla de Captura de Guias de la App CAIA Movil v2
 *
 * @author Pablo Yepez Contreras <http://mailto:pyepezc@yahoo.com>
 * @version 1.0, 2022/03/07
 *
 * DHL EXPRESS ECUADOR
 */

import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
//import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.caia_movil_v2.databinding.FragmentGuiaBinding;


public class GuiaFragment extends Fragment {

    /**
     * Constantes
     */

    private static final int preSALIDA = 1001;
    private static final int codSALIDA = 1005;
    private static final int postSALIDA = 1010;

    private static final int preIMPRESION = 2001;
    private static final int codIMPRESION = 2005;
    private static final int postIMPRESION = 2010;

    /**
     * Atributos
     */
    private String guia;
    private boolean esperando;

    /**
     * Widgets utilizados en la pantalla
     */
    private TextView impresoraText;
    private TextView textMensaje;
    private Handler mHandler;
    private ProgressBar progBar;
    private EditText guiaTxt;
    private Button botonRegistrar;
    private MediaPlayer mpAlarma, mpOk;

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch salidaSwitch;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch impresionSwitch;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch bloqueoSwitch;

    private FragmentGuiaBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        mpAlarma = MediaPlayer.create(this.getContext(), R.raw.alarma);
        mpOk = MediaPlayer.create(this.getContext(), R.raw.ok);

        MainActivity main = ((MainActivity) requireActivity()); // 10 dic 2021
        main.menuGlobal.findItem(R.id.action_settings).setVisible(false); // Apagar esta opcion en la Guia

        binding = FragmentGuiaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        impresoraText = binding.impresoraText;
        textMensaje = binding.textMensaje;
        salidaSwitch = binding.salidaSwitch;
        impresionSwitch = binding.impresionSwitch;
        bloqueoSwitch = binding.bloqueoSwitch;
        guiaTxt = binding.guia;
        botonRegistrar = binding.botonRegistrar;
        botonRegistrar.setEnabled(false);
        progBar = binding.loading;

        bloqueoSwitch.setChecked(true); // Se inicia bloqueado
        salidaSwitch.setEnabled(false);
        impresionSwitch.setEnabled(false);

        salidaSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            WebServiceCliente.setSalidaAduana(isChecked);
            habilitarbotonRegistrar();
        });

        impresionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            WebServiceCliente.setImpresionImagenes(isChecked);
            habilitarbotonRegistrar();
        });

        bloqueoSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked)
                bloqueoSwitch.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock, 0, 0, 0);

            else
                bloqueoSwitch.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_unlock, 0, 0, 0);
            salidaSwitch.setEnabled(!isChecked);
            impresionSwitch.setEnabled(!isChecked);
        });

        botonRegistrar.setOnClickListener(e -> sendGuia() );

        guiaTxt.addTextChangedListener(new TextWatcher() { // Manejador de Eventos del texto con el numero de la guia

            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            public void afterTextChanged(Editable s) {
//

                habilitarbotonRegistrar();
                if (s.length() == 10) {
                    guiaTxt.requestFocus();
                    guiaTxt.selectAll();

                    sendGuia();
                }
                //Log.d(TAG, "afterTextChanged " + ss);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        impresoraText.setText(WebServiceCliente.getImpresoraActual());

        salidaSwitch.setChecked(WebServiceCliente.getSalidaAduana());
        impresionSwitch.setChecked(WebServiceCliente.getImpresionImagenes());

        guiaTxt.requestFocus();

    }

    /** When the user closes the application
     * onPause() will be called
     * and data will be stored
     *
     */
    @Override
    public void onPause() {
        super.onPause();

        if (getContext() != null)
            WebServiceCliente.savePreferences(getContext());

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Activar o desactivar el boton OK
     */
    private void habilitarbotonRegistrar() {
        botonRegistrar.setEnabled((salidaSwitch.isChecked() || impresionSwitch.isChecked()) && guiaTxt.getText().length() == 10);
    }

    /**
     * Atiende las operaciones de la pantalla de Configuracion
     * Registrar la salida de una guia
     * Imprimir documentos de una guia
     * @param cod_inicio   Codigo de operacion
     */
    private void handleOperaciones(int cod_inicio) {

        mHandler = new Handler(Looper.getMainLooper()) {
            String respuesta = null;

            @Override
            public void handleMessage(Message msg) {

                // Process for STATUS
                if (msg.what == preSALIDA) { // Encender ProgressBar
                    progBar.setVisibility(View.VISIBLE);
                    textMensaje.setText(""); // Limpiar mensaje.
                    //Log.d(TAG, "Encender ");
                    esperando=true;
                    guiaTxt.setEnabled(false);
                    mHandler.sendEmptyMessage(codSALIDA);
                }
                if (msg.what == preIMPRESION) { // Encender ProgressBar
                    progBar.setVisibility(View.VISIBLE);
                    //Log.d(TAG, "Encender ");
                    esperando=true;
                    mHandler.sendEmptyMessage(codIMPRESION);
                }
                if (msg.what == codSALIDA) { // Leer los datos desde el Webservice

                    WebServiceCliente.SalidaAduanas(guia); // Llama el web service SalidaAduanas
                    int n = 0;
                    while (!WebServiceCliente.hayRespuesta()) { // Espera hasta que haya una respuesta.
                        WebServiceCliente.delay(250);
                        //Log.d(TAG, String.format("esperando %d", n));
                        if (++n > 40) break;
                    }
                   // Log.d(TAG, "ya");

                    if (WebServiceCliente.hayRespuesta()) {

                        //Log.d(TAG, WebServiceCliente.getRespuesta());
                        respuesta = WebServiceCliente.getRespuesta();

                        String texto = ((respuesta==null) ? "" : respuesta) + "\n";
                        textMensaje.setText(texto); // Actualizar mensaje.
                    } else
                        respuesta = null;

                    binding.textError.setText(WebServiceCliente.getMensajeError() );

                    mHandler.sendEmptyMessage(postSALIDA);  // Apagar progressbar
                }

                if (msg.what == codIMPRESION) { // Leer los datos desde el Webservice

                    WebServiceCliente.ImprimirImagenesGuia(guia); // Llama el web service SalidaAduanas
                    int n = 0;
                    while (!WebServiceCliente.hayRespuesta()) { // Espera hasta que haya una respuesta.
                        WebServiceCliente.delay(200);
                        //Log.d(TAG, String.format("esperando %d", n));
                        if (++n > 40) break;
                    }
                    //Log.d(TAG, "ya");

                    if (WebServiceCliente.hayRespuesta()) {
                        //Log.d(TAG, WebServiceCliente.getRespuesta());
                        WebServiceCliente.setMensajeError(WebServiceCliente.getRespuesta());
                        respuesta = WebServiceCliente.getRespuesta();
                    }
                    else
                        respuesta = null;

                    String texto = textMensaje.getText() + ((respuesta==null) ? "" : respuesta);
                    textMensaje.setText(texto); // Actualizar mensaje.
                    binding.textError.setText(WebServiceCliente.getMensajeError());

                    mHandler.sendEmptyMessage(postIMPRESION);  // Apagar progressbar
                }
                if (msg.what == postSALIDA) { // Desaparecer ProgressBar
                    respuesta = WebServiceCliente.getRespuesta();
                    if (respuesta != null && respuesta.length()>4) {
                        String pre = respuesta.substring(0, 3);
                        if ("ERR".equals(pre))
                            mpAlarma.start();
                        else
                            mpOk.start();
                    }

                    progBar.setVisibility(View.GONE);
                    esperando=false;
                    guiaTxt.setEnabled(true);
                }
                if (msg.what == postIMPRESION) { // Desaparecer ProgressBar
                    progBar.setVisibility(View.GONE);
                    esperando=false;
                }

            }
        };

        mHandler.sendEmptyMessage(cod_inicio); // Iniciar el proceso.
    }

    /**
     * Iniciar las operaciones con el numero de la guia.
     */
    private void sendGuia() {

        if (esperando) return;

        if (! bloqueoSwitch.isChecked()) {
            textMensaje.setText(R.string.bloquear_opciones); // Actualizar mensaje.
            return;
        }

        guia = guiaTxt.getText().toString().trim();
        textMensaje.setText(""); // Limpiar mensaje.

        if (salidaSwitch.isChecked()) {
            //Log.e(TAG, "SALIDA SWITCH");
            handleOperaciones(preSALIDA);
            WebServiceCliente.delay(500);
        }
        if (impresionSwitch.isChecked()) {
            //Log.e(TAG, "IMPRESION SWITCH");
            handleOperaciones(preIMPRESION);
        }
    }
}