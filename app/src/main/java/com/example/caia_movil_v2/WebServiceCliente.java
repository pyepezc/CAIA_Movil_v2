package com.example.caia_movil_v2;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import org.xmlpull.v1.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.*;
import java.security.cert.CertPathValidatorException;


/*
 * Esta clase administra las operaciones del WebService de CAIA de la App CAIA Movil v2
 *
 * @author Pablo Yepez Contreras <http://mailto:pyepezc@yahoo.com>
 * @version 2.0, 2023/05/14
 *
 * DHL EXPRESS ECUADOR
 */
public abstract class WebServiceCliente {

    /**
     * Constantes
     */
    private static final String TAG = "DHL";
    private static final int HELLOWORLD = 1;
    private static final int LOGONUSER = 2;
    private static final int OBTENERIMPRESORASLABEL = 3;
    private static final int SALIDAADUANAS = 4;
    private static final int IMPRIMIRIMAGENESGUIA = 5;

    private static final String NOMBREPREFERENCIAS = "CAIAPref";
    private static final String URLDEFAULT = "https://ec-caia.dhl.com/caia/EC/Servicios/ECMobileProcessNew.asmx";

    /**
     * Atributos
     */
    private static String urlS = URLDEFAULT; // String con la direccion del url.
    private static String impresoraActual; // String con el nombre de la impresora seleccionada.
    private static String usuario; // Usuario con ql que se hizo login.
    private static boolean salidaAduana; // Parametro de salida de aduana
    private static boolean impresionImagenes; // Parametro de Impresion de Imagenes

    private static boolean respuestaflag;
    private static String respuesta;
    private static String mensajeError;

    public static boolean hayRespuesta() {
        return respuestaflag;
    }

    public static String getRespuesta() {
        return respuesta;
    }

    private static final int BUFFER_CAPACITY = 4096; // Denial of Service: StringBuilder
    /**
     * Seleccionar el URL
     *
     * @param u String con el url para crear el wsdl.
     */
    public static void setUrl(@NonNull String u) throws MalformedURLException {
        urlS = u.trim();
        //url = new URL(urlS);
    }

    public static String getUrlS() {
        return urlS;
    }

    public static void setUsuario(String user) {
        usuario = user;
    }

    public static void setImpresoraActual(String impresora) {
        impresoraActual = impresora;
    }

    public static String getImpresoraActual() {
        return impresoraActual;
    }

    public static boolean getSalidaAduana() {
        return salidaAduana;
    }

    public static void setSalidaAduana(boolean activo) {
        salidaAduana = activo;
    }

    public static boolean getImpresionImagenes() {
        return impresionImagenes;
    }

    public static void setImpresionImagenes(boolean activo) {
        impresionImagenes = activo;
    }

    private static void conectar(String mensajeS, String operacion) {

        respuesta = "";
        respuestaflag = false;
        mensajeError = "";

        OutputStream os = null;
        BufferedReader reader = null;
        InputStreamReader inputRead = null;

        try {
            // Conexion URL
            URL url = new URL(getUrlS() + operacion);
            setMensajeError(getUrlS() + operacion);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("content-type", "application/x-www-form-urlencoded");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.connect();

            setMensajeError("Conectado.");

            // El cuarto paso: organizar datosy enviar solicitud
            if ( !operacion.contains("LogonUser") )
                setMensajeError(mensajeS);

            /// Enviar la información en una secuencia
            os = connection.getOutputStream();
            if (mensajeS.length()>0) {
                if (mensajeS.charAt(0) != '~')
                    os.write(mensajeS.getBytes());
                else {
                    os.write(desvariar(mensajeS.substring(1)).getBytes());
                }
            }

            // El quinto paso: recibir la respuesta del servidor e imprimir
            int responseCode = connection.getResponseCode();
            if (200 == responseCode) {// indica que el servidor respondió con éxito
                //Log.d(TAG, "Si responde ");

                /// Obtener el flujo de datos devuelto por la solicitud de conexión actual
                inputRead = new InputStreamReader(connection.getInputStream());
                reader = new BufferedReader(inputRead);

                char[] charData = new char[BUFFER_CAPACITY];
                int i = 0, c;
                while ((c = reader.read()) != -1 && i < BUFFER_CAPACITY) {
                    char character = (char) c;
                    charData[i++] = character;
                }

                respuesta = parseXml(new String(charData));
                respuestaflag = true; //WebService respondio correctamente.

            } else {
                //Log.e(TAG, connection.getResponseMessage() );
                respuestaflag = false;
                setMensajeError(responseCode + connection.getResponseMessage());
                //mensajeError = responseCode + connection.getResponseMessage();

            }
            // Al final desconectar 30 mayo 2023
            connection.disconnect();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            setMensajeError(e.getMessage());

        } finally {
            //close streams
            if (os != null) {
                try {
                    os.close();
                }
                catch(IOException ioex) {
                    Log.e(TAG, ioex.getMessage() );
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                }
                catch(IOException ioex) {
                    Log.e(TAG, ioex.getMessage() );
                }
            }
            if (inputRead != null) {
                try {
                    inputRead.close();
                }
                catch (IOException ioex) {
                    Log.e(TAG, ioex.getMessage() );
                }
            }
        }

    }

    /**
     * Almacena los datos de la aplicacion con preferencias en un archivo "CAIAPref" en private mode
     * @param contexto de la aplicacion
     */
    public static void savePreferences(Context contexto) {

        SharedPreferences sharedPreferences = contexto.getSharedPreferences(NOMBREPREFERENCIAS, MODE_PRIVATE);
        SharedPreferences.Editor caiaEdit = sharedPreferences.edit();

        // write all the data entered by the user in SharedPreference and apply
        caiaEdit.putString("url", getUrlS());
        caiaEdit.putString("printer", getImpresoraActual());
        caiaEdit.putBoolean("salidaAduana", getSalidaAduana());
        caiaEdit.putBoolean("impresionImagenes", getImpresionImagenes());

        caiaEdit.apply();
    }

    /**
     * Recupera los datos de la aplicacion desde las  preferencias
     * @param contexto de la aplicacion
     */
    public static void getPreferences(Context contexto) {
        SharedPreferences sh = contexto.getSharedPreferences(NOMBREPREFERENCIAS, MODE_PRIVATE);

        String urls = sh.getString("url", URLDEFAULT);
        String prts = sh.getString("printer", "");

        setSalidaAduana(sh.getBoolean("salidaAduana", false));
        setImpresionImagenes(sh.getBoolean("impresionImagenes", false));

        try {
            setUrl(urls);

        } catch (MalformedURLException e) {
            alerta(contexto.getString(R.string.error_url), e.getMessage(), contexto);
        }
        // in the EditTexts
        setImpresoraActual(prts);
    }

    /**
     * Se invoca a la operacion HelloWorld del webService
     */
    public static void HelloWorld() {
        respuesta = "";
        respuestaflag = false;

        //Log.d(TAG, "HelloWorld");
        String mensaje = getXML(HELLOWORLD);
        String operacion = getOper(HELLOWORLD);
        new Thread(() -> conectar(mensaje, operacion)).start();

    }

    /**
     * Se invoca a la operacion ObtenerImpresorasLabel del webService
     */
    public static void ObtenerImpresorasLabel() {
        respuesta = "";
        respuestaflag = false;
        String mensaje = getXML(OBTENERIMPRESORASLABEL);
        String operacion = getOper(OBTENERIMPRESORASLABEL);
        new Thread(() -> conectar(mensaje, operacion)).start();
    }

    /**
     * Se invoca a la operacion LogonUser del webService
     *
     * @param usuario nombre del usuario
     * @param passwd contrasenia
     */
    public static void LogonUser(String usuario, String passwd) {
        respuesta = "";
        respuestaflag = false;
        String mensaje = getXML(LOGONUSER, variar(usuario), variar(passwd) );
        String operacion = getOper(LOGONUSER);
        new Thread(() -> conectar(mensaje, operacion)).start();
    }

    /**
     * Se invoca a la operacion SalidaAduanas del webService
     * @param guia numero de la guia
     */
    public static void SalidaAduanas(String guia) {
        respuesta = "";
        respuestaflag = false;
        String mensaje = getXML(SALIDAADUANAS, usuario, guia, impresoraActual);
        String operacion = getOper(SALIDAADUANAS);
        new Thread(() -> conectar(mensaje, operacion)).start();
    }

    /**
     * Se invoca a la operacion ImprimirImagenesGuia del webService
     * @param guia numero de la guia
     */
    public static void ImprimirImagenesGuia(String guia) {

        respuesta = "";
        respuestaflag = false;
        String mensaje = getXML(IMPRIMIRIMAGENESGUIA, guia, impresoraActual);
        String operacion = getOper(IMPRIMIRIMAGENESGUIA);
        new Thread(() -> conectar(mensaje, operacion)).start();
    }

    private static String getXML(int tipo) {
        return getXML(tipo, "", "", "");
    }

    private static String getXML(int tipo, String datoA, String datoB) {
        return getXML(tipo, datoA, datoB, "");
    }

    /**
     * Construye el string de invocacion POST dependiendo de cada  operacion
     * @param tipo de operacion
     * @param datoA parametro 1
     * @param datoB parametro 2
     * @param datoC parametro 3
     * @return el texto POST construido
     */
    private static String getXML(int tipo, String datoA, String datoB, String datoC) {
        String parametros = "";

        switch (tipo) {
            case HELLOWORLD:
                //Web service method = "HelloWorld";
                break;

            case LOGONUSER:
                //Web service method = "LogonUser";
                parametros = "~"+variar("user=" + desvariar(datoA) + "&pass="+ desvariar(datoB) );
                break;

            case OBTENERIMPRESORASLABEL:
                //Web service method = "ObtenerImpresorasLabel";
                break;

            case SALIDAADUANAS:
                //Web service method = "SalidaAduanas";
                int indx = datoC.indexOf(';');
                parametros = "idUsuario=" + datoA + "&shipmentCode="+ datoB + "&idImpresora="+ datoC.substring(0,indx); // Tomar solo el codigo.
                break;

            case IMPRIMIRIMAGENESGUIA:
                //Web service method = "ImprimirImagenesGuia";
                int ind = datoB.indexOf(';');
                parametros = "ShipmentCode=" + datoA + "&IdImpresora="+ datoB.substring(0,ind); // Tomar solo el codigo.
        }

        return parametros;
    }

    private static String getOper( int tipo ) {
        String metodo = "";
        switch (tipo) {
            case HELLOWORLD:
                metodo = "HelloWorld";
                break;

            case LOGONUSER:
                metodo = "LogonUser";
                break;

            case OBTENERIMPRESORASLABEL:
                metodo = "ObtenerImpresorasLabel";
                break;

            case SALIDAADUANAS:
                metodo = "SalidaAduanas";
                break;

            case IMPRIMIRIMAGENESGUIA:
                metodo = "ImprimirImagenesGuia";
                break;
        }

        return String.format("/%s",metodo);
    }

    /**
     * Procesa la respuesta XML del webservice
     * @param xmls respuesta del webservice
     * @return el dato contenido en el xml.
     */
    public static String parseXml(String xmls) {
        String texto = "";
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();

            xpp.setInput(new StringReader(xmls)); // pass input whatever xml you have
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {

                if (eventType == XmlPullParser.TEXT) {
                    //Log.d(TAG, "Text:" + xpp.getText()); // here you get the text from xml
                    texto = xpp.getText();
                }

                eventType = xpp.next();
            }

        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
        return texto;
    }

    public static void delay(long milisegundos) { // Espera milisegundos
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public static void alerta(String titulo, String mensaje, Context contexto) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contexto);
        builder.setTitle(titulo).setMessage(mensaje);
        // Add the buttons
        builder.setPositiveButton(R.string.ok, (dialog, id) -> {
            // User clicked OK button
        });
        AlertDialog dialogo = builder.create();
        dialogo.show();
    }

    public static void setMensajeError(String m) {
        //mensajeError += m+" | ";
        mensajeError = m; // Quitar para hacer pruebas
    }

    public static String getMensajeError() {
        return mensajeError;
    }

    private static String variar(String s) {
        char[] ori = s.toCharArray();
        int l = ori.length;
        char[] zif = new char[l];

        for (int i=0; i<l; i++)
            zif[i] = (char)( 126 - ( (int)ori[i] - 32) );

        return new String(zif);
    }

    private static String desvariar(String s) {
        char[] ori = s.toCharArray();
        int l = ori.length;
        char[] zif = new char[l];

        for (int i=0; i<l; i++)
            zif[i] = (char)( 126 - (int)ori[i] + 32 );

        return new String(zif);
    }
}
