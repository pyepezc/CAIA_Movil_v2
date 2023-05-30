package com.example.caia_movil_v2;
/*
 * Esta clase muestra la pantalla de Configuracion de la App CAIA Movil v2
 *
 * @author Pablo Yepez Contreras <http://mailto:pyepezc@yahoo.com>
 * @version 1.0, 2022/03/07
 *
 * DHL EXPRESS ECUADOR
 */

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.net.MalformedURLException;

import com.example.caia_movil_v2.databinding.FragmentConfigBinding;

public class ConfigFragment extends Fragment {

    /**
     * Constantes
     */
    private static final String TAG = "DHL";
    
    private static final int preSTATUS = 1001;
    private static final int codSTATUS = 1005;
    private static final int postSTATUS = 1010;

    private static final int prePRINTER = 2001;
    private static final int codPRINTER = 2005;
    private static final int postPRINTER = 2010;

    /**
     * Widgets utilizados en la pantalla
     */
    private EditText webServiceText;
    private ProgressBar progBar;
    private Spinner spinPrt;
    private Handler mHandler;

    private FragmentConfigBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        MainActivity main = ((MainActivity) requireActivity());
        main.menuGlobal.findItem(R.id.action_settings).setVisible(false);

        binding = FragmentConfigBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getContext()!=null)
            WebServiceCliente.getPreferences(getContext());

        Log.d(TAG, WebServiceCliente.getUrlS());

        webServiceText = binding.webservice;
        progBar = binding.loading;
        spinPrt = binding.spinImpresoras;
        //textError = binding.textError;

        WebView webview = binding.webview;

        webview.loadUrl(WebServiceCliente.getUrlS());

        webServiceText.setText(WebServiceCliente.getUrlS());
        cargarImpresoras( WebServiceCliente.getImpresoraActual() );

        spinPrt.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view,
                                       int position, long id) {
                Object item = adapterView.getItemAtPosition(position);
                if (item != null) {
                    Log.e(TAG, item.toString() );
                    WebServiceCliente.setImpresoraActual(item.toString());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // TODO Auto-generated method stub

            }
        });

        binding.buttonTest.setOnClickListener(v -> {
            String urlS = webServiceText.getText().toString();

            try {
                WebServiceCliente.setUrl(urlS);
                WebServiceCliente.savePreferences(getContext());

            } catch (MalformedURLException e) {
                //Log.e(TAG, urlS);
                return; // No es correcto el url
            }

            handleOperaciones(preSTATUS);
        });

        binding.buttonImpresora.setOnClickListener(v -> handleOperaciones(prePRINTER));
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
     *  Atiende las operaciones de la pantalla de Configuracion
     *  Consultar el status HelloWorld
     *  Consultar la lista de impresoras
     * @param cod_inicio Codigo de operacion
     */
    private void handleOperaciones(int cod_inicio) {

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {

                //Log.d(TAG, msg.toString());

                // Process for STATUS
                if (msg.what == preSTATUS) { // Encender ProgressBar
                    progBar.setVisibility(View.VISIBLE);
                    binding.textTest.setText("");
                    binding.textError.setText("");

                    mHandler.sendEmptyMessage(codSTATUS);
                }
                if (msg.what == codSTATUS) { // Leer los datos desde el Webservice

                    WebServiceCliente.HelloWorld(); // Llama al web service HellWorld
                    int n = 0;
                    while (!WebServiceCliente.hayRespuesta()) { // Espera hasta que haya una respuesta.
                        WebServiceCliente.delay(300);
                        //Log.d(TAG, String.format("esperando %d", n));
                        if (++n > 32) break;
                    }
                    //Log.d(TAG, "ya");

                    if (WebServiceCliente.hayRespuesta()) {
                        binding.textTest.setText(WebServiceCliente.getRespuesta());
                        binding.textError.setText(WebServiceCliente.getMensajeError());
                    }
                    else {
                        binding.textTest.setText(R.string.ws_no_responde);
                        binding.textError.setText( WebServiceCliente.getMensajeError());
                    }

                    mHandler.sendEmptyMessage(postSTATUS);  // Apagar progressbar
                }
                if (msg.what == postSTATUS) { // Desaparecer ProgressBar
                    progBar.setVisibility(View.GONE);
                    //Log.d(TAG, "Apagar status ");
                }

                // Process for PRINTER
                if (msg.what == prePRINTER) { // Encender ProgressBar
                    progBar.setVisibility(View.VISIBLE);
                    binding.textTest.setText("");
                    //Log.d(TAG, "Encender ");
                    mHandler.sendEmptyMessage(codPRINTER);
                }
                if (msg.what == codPRINTER) { // Leer los datos desde el Webservice

                    WebServiceCliente.ObtenerImpresorasLabel(); // Llama el web service TEST
                    int n = 0;
                    while (!WebServiceCliente.hayRespuesta()) { // Espera hasta que haya una respuesta.
                        WebServiceCliente.delay(200);
                        //Log.d(TAG, String.format("esperan %d", n));
                        if (++n > 15) break;
                    }
                    //Log.d(TAG, "ya printer");

                    if (WebServiceCliente.hayRespuesta()) {
                        binding.textTest.setText( R.string.ok_impresoras);
                        cargarImpresoras(WebServiceCliente.getRespuesta());
                    }
                    else
                        binding.textTest.setText(R.string.ws_no_responde);

                    mHandler.sendEmptyMessage(postPRINTER);  // Apagar progressbar
                }
                if (msg.what == postPRINTER) { // Desaparecer ProgressBar
                    progBar.setVisibility(View.GONE);
                    //Log.d(TAG, "Apagar printer ");
                }
            }
        };

        mHandler.sendEmptyMessage( cod_inicio ); // Iniciar el proceso.
    }

    /**
     * Cargar el string con la lista de impresoras en el Spinner spinPrt
     * @param listaprts lista de impresoras disponibles
     */
    private void cargarImpresoras( String listaprts ) {
        String [] prts = listaprts.split("\\|"); // regex separador pipe
        if (prts.length==0) return;

        // Buscar la posicion de la impresora actual.
        String prtActual = WebServiceCliente.getImpresoraActual();
        int index=0;
        for ( ; index<prts.length; index++)
            if ( prtActual.equals( prts[index] ))
                break;

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, prts);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinPrt.setAdapter(adapter);

        if (index >= prts.length)
            index = 0;

        spinPrt.setSelection(index); // Posicionarse en la impresora Actual
    }

}