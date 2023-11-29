package com.example.caia_movil_v2;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
//import android.util.Log;

import androidx.annotation.NonNull;

import org.xmlpull.v1.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;


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
    //private static final String TAG = "DHL";
    private static final int HELLOWORLD = 1;
    private static final int LOGONUSER = 2;
    private static final int OBTENERIMPRESORASLABEL = 3;
    private static final int SALIDAADUANAS = 4;
    private static final int IMPRIMIRIMAGENESGUIA = 5;

    private static final String NOMBREPREFERENCIAS = "CAIAPref";
    private static final String URLDEFAULT = "https://ec-caia.dhl.com/";

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

    private static final HashMap<String, String> mensajeErrores = new HashMap<>();

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
        // Validate. MUST START WITH https://ec-caia.dhl.com/
        List<String> valid = Arrays.asList("https://ec-caia.dhl.com/");
        String urS = u.trim();
        if ( valid.contains( urS.substring(0, Math.min(urS.length(), 24) ) ) ||
                valid.contains( urS.substring(0, Math.min(urS.length(), 23) ) )
            )
            urlS = urS;
        else {
            throw new MalformedURLException();
        }
    }

    public static String ValidateUrl(String u) throws MalformedURLException {
        // Validate. MUST START WITH https://ec-caia.dhl.com/
        List<String> valid = Arrays.asList("https://ec-caia.dhl.com/");
        String urS = u.trim();
        if ( valid.contains( urS.substring(0, Math.min(urS.length(), 24) ) ) )
            return urS;
        else
            throw new MalformedURLException();
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

    private static void conectar(char [] mensajeS, String operacion) {

        respuesta = "";
        respuestaflag = false;
        mensajeError = "";

        OutputStream os = null;
        BufferedReader reader = null;
        InputStreamReader inputRead = null;

        try {
            // Conexion URL
            URL url = new URL(ValidateUrl( getUrlS()) + operacion);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("content-type", "application/x-www-form-urlencoded");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.connect();

            setMensajeError("Conectado");

            // El cuarto paso: organizar datosy enviar solicitud

            /// Enviar la información en una secuencia
            os = connection.getOutputStream();
            if (mensajeS!=null && mensajeS.length > 0) {
                if (mensajeS[0] != '~')
                    os.write( toBytes(mensajeS) );
                else {
                    //Log.d(TAG, desvariar(mensajeS.substring(1)));
                    mensajeS[0] = 0;
                    byte[] bb= toBytes(desvariar(mensajeS));
                    os.write( bb );
                }
            }

            // El quinto paso: recibir la respuesta del servidor e imprimir
            int responseCode = connection.getResponseCode();
            if (200 == responseCode) {// indica que el servidor respondió con éxito


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
                setMensajeError(""); //junio 14

            } else {
                //Log.e(TAG, connection.getResponseMessage() );
                respuestaflag = false;
                setMensajeError(String.valueOf( responseCode )); // connection.getResponseMessage());

            }
            // Al final desconectar 30 mayo 2023
            connection.disconnect();

        } catch (IOException e) {
            //Log.e(TAG, e.getMessage());
            setMensajeError("IO");

        } finally {
            if (mensajeS!=null)
                Arrays.fill(mensajeS, ' ');

            //close streams
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ioex) {
                    //Log.e(TAG, ioex.getMessage());
                    setMensajeError("IO os error!");
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioex) {
                    //Log.e(TAG, ioex.getMessage());
                    setMensajeError("IO reader error!");
                }
            }
            if (inputRead != null) {
                try {
                    inputRead.close();
                } catch (IOException ioex) {
                    //Log.e(TAG, ioex.getMessage());
                    setMensajeError("IO inputRead error!");
                }
            }
        }

    }

    /**
     * Almacena los datos de la aplicacion con preferencias en un archivo "CAIAPref" en private mode
     *
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
     *
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
            alerta(contexto.getString(R.string.error_url), " Url incorrecto ", contexto);
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
        char[] mensaje = getXML(HELLOWORLD);
        String operacion = getOper(HELLOWORLD);
        new Thread(() -> conectar(mensaje, operacion)).start();

    }

    /**
     * Se invoca a la operacion ObtenerImpresorasLabel del webService
     */
    public static void ObtenerImpresorasLabel() {
        respuesta = "";
        respuestaflag = false;
        char[] mensaje = getXML(OBTENERIMPRESORASLABEL);
        String operacion = getOper(OBTENERIMPRESORASLABEL);
        new Thread(() -> conectar(mensaje, operacion)).start();
    }

    /**
     * Se invoca a la operacion LogonUser del webService
     *
     * @param usuario nombre del usuario
     * @param passwd  contrasenia
     */
    public static void LogonUser(String usuario, String passwd) {
        respuesta = "";
        respuestaflag = false;
        char[] mensaje = getXML(LOGONUSER, variar(usuario), variar(passwd));
        String operacion = getOper(LOGONUSER);
        new Thread(() -> conectar(mensaje, operacion)).start();
    }

    /**
     * Se invoca a la operacion SalidaAduanas del webService
     *
     * @param guia numero de la guia
     */
    public static void SalidaAduanas(String guia) {
        respuesta = "";
        respuestaflag = false;
        char[] mensaje = getXML(SALIDAADUANAS, usuario.toCharArray(), guia.toCharArray(), impresoraActual.toCharArray());
        String operacion = getOper(SALIDAADUANAS);
        new Thread(() -> conectar(mensaje, operacion)).start();
    }

    /**
     * Se invoca a la operacion ImprimirImagenesGuia del webService
     *
     * @param guia numero de la guia
     */
    public static void ImprimirImagenesGuia(String guia) {

        respuesta = "";
        respuestaflag = false;
        char[] mensaje = getXML(IMPRIMIRIMAGENESGUIA, guia.toCharArray(), impresoraActual.toCharArray());
        String operacion = getOper(IMPRIMIRIMAGENESGUIA);
        new Thread(() -> conectar(mensaje, operacion)).start();
    }

    private static char[] getXML(int tipo) {
        return getXML(tipo, null, null, null);
    }

    private static char[] getXML(int tipo, char[] datoA, char[] datoB) {
        return getXML(tipo, datoA, datoB, null);
    }

    /**
     * Construye el string de invocacion POST dependiendo de cada  operacion
     *
     * @param tipo  de operacion
     * @param datoA parametro 1
     * @param datoB parametro 2
     * @param datoC parametro 3
     * @return el texto POST construido
     */
    private static char[] getXML(int tipo, char[] datoA, char[] datoB, char[] datoC) {
        //String parametros;
        char[] param=null;

        switch (tipo) {
            case HELLOWORLD:
                //Web service method = "HelloWorld";
                break;

            case LOGONUSER:
                //Web service method = "LogonUser"; // No String
                char [] p = appendCh(
                            appendCh( "user=".toCharArray(), desvariar(datoA)),
                            appendCh("&pass=".toCharArray(), desvariar(datoB)) ) ;
                param = appendCh("~".toCharArray(),  variar(p));
                break;

            case OBTENERIMPRESORASLABEL:
                //Web service method = "ObtenerImpresorasLabel";
                break;

            case SALIDAADUANAS:
                //Web service method = "SalidaAduanas";
                int indx = indexofArray( datoC); //"15;HPimpresora|25;CannonImpresora|20;OP-COURIER_RICOH MP30|23;LaserGrande"
                if (indx < 0) indx = 0;
                char[] datoC_a = Arrays.copyOfRange(datoC, 0, indx);
                // parametros = "idUsuario=" + (new String(datoA)) + "&shipmentCode=" + (new String(datoB)) + "&idImpresora=" + (new String(datoC_a)); // Tomar solo el codigo.
                char [] tmp = appendCh(
                        appendCh( "idUsuario=".toCharArray(), datoA ) ,
                        appendCh( "&shipmentCode=".toCharArray(), datoB )
                );
                param = appendCh( tmp,
                        appendCh( "&idImpresora=".toCharArray(), datoC_a )
                );

                Arrays.fill(datoC_a, ' ');
                //param=parametros.toCharArray();
                break;

            case IMPRIMIRIMAGENESGUIA:
                //Web service method = "ImprimirImagenesGuia";

                int ind = indexofArray( datoB) ;
                if (ind < 0) ind = 0;
                char[] datoB_a = Arrays.copyOfRange(datoB, 0, ind);
                param = appendCh(
                        appendCh( "ShipmentCode=".toCharArray(), datoA ) ,
                        appendCh( "&IdImpresora=".toCharArray(), datoB_a )
                    );
                //parametros = "ShipmentCode=" + (new String(datoA)) + "&IdImpresora=" + (new String(datoB_a)); // Tomar solo el codigo.
                Arrays.fill(datoB_a, ' ');
                //param = parametros.toCharArray();
                break;
        }

        return param;
    }

    private static String getOper(int tipo) {
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

        return String.format("/%s", metodo);
    }

    /**
     * Procesa la respuesta XML del webservice
     *
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
            setMensajeError("XML");
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
        mensajeError = null;
        mensajeError = mensajeErrores.get(m.trim());
        //Log.d(TAG, "Text:["+m+"]("+mensajeError+")");
        if (mensajeError==null)
            mensajeError = "Error:"+m;
    }

    public static String getMensajeError() {
        return mensajeError;
    }

    public static void loadMensajes() {
        mensajeErrores.put("", "   " );
        mensajeErrores.put("unable", "No se puede resolver el servidor");
        mensajeErrores.put("Conectado", "Conectado." );
        mensajeErrores.put("IO", "Error conexion io." );
        mensajeErrores.put("XML", "Error xml parsing." );
        mensajeErrores.put("500", "codigo 500 error en el servidor." );
        mensajeErrores.put("404", "codigo 404 no encontrado." );
        mensajeErrores.put("REP","Error Repairable GooglePlayServices");
        mensajeErrores.put("NOA","Error NotAvailable GooglePlayServices");
    }

    private static char[] variar(String s) {
        char[] ori = s.toCharArray();
        return variar(ori);
    }
    private static char[] variar( char[] s) {

        int l = s.length;
        char[] zif = new char[l];

        for (int i = 0; i < l; i++)
            zif[i] = (char) (126 - ((int) s[i] - 32));

        return zif;
    }

    private static char[] desvariar(char[] s) {

        int l = s.length;

        ArrayList<Character> lst = new ArrayList<>();

        int i;
        for (i = 0; i < l; i++)
            if (s[i]!=0)
                lst.add( (char) (126 - (int) s[i] + 32) );

        char[] zif = new char[ lst.size() ];
        i=0;
        for (Character c : lst) {
            zif[i] = c;
            i++;
        }

        return zif;
    }

    private static byte[] toBytes(char[] chars) {
        CharBuffer charBuffer = CharBuffer.wrap(chars);
        //ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);
        byte[] bytes = Arrays.copyOfRange(byteBuffer.array(),
                byteBuffer.position(), byteBuffer.limit());
        Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
        return bytes;
    }

    private static int indexofArray(char[] charArr) {

        return IntStream.range(0, charArr.length)
                .filter(i -> charArr[i] == ';')
                .findFirst()
                .orElse(-1);
    }

    private static char[] appendCh(char[] a, char[] b) {
        int la = a.length;
        int lb = b.length;
        char[] ap = new char[la+lb];
        System.arraycopy(a, 0, ap, 0, la);
        System.arraycopy(b, 0, ap, la, lb);

        return ap;
    }
}
